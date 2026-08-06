/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.process.bpmn.interop;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.core.traits.definitions.SchemaViewTraitDefinition;
import org.structr.process.traits.definitions.BpmnElementTraitDefinition;
import org.structr.process.traits.definitions.BpmnProcessTraitDefinition;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns the vendor-neutral forms collected by the {@link BpmnVendorAdapter}s into a real Structr
 * schema type, because Structr is schema-type-based and has no type-optional path: a process with
 * no subject type cannot render a form, so when a foreign model carries forms but no type, we
 * <em>synthesize</em> one rather than degrade.
 *
 * <h3>The mapping (one subject per process)</h3>
 * <ul>
 *   <li>A Structr process instance has exactly one subject, so all of a process's user-task forms
 *       describe fields of the <em>same</em> type. We therefore take the <b>union</b> of every
 *       task form's fields and make each a {@code SchemaProperty} on one synthesized
 *       {@code SchemaNode}.</li>
 *   <li>Each task's own field list becomes a {@code SchemaView} on that type, wired to the step as
 *       its {@code subjectFormView} -- exactly the per-step "which fields" contract a hand-built
 *       process uses. Read-only fields (when the vendor marks them) are excluded from a second
 *       {@code subjectWritableView}.</li>
 * </ul>
 *
 * <h3>Boundaries</h3>
 * <ul>
 *   <li><b>Explicit names, not suppression.</b> A {@code subjectType} declared up front (e.g. via
 *       {@code <structr:subject>}) is used as the synthesized type's <em>name</em> -- it does not
 *       skip synthesis, so the type and views are still built from the vendor forms, just under a
 *       stable, round-trippable name instead of one derived from the process name. Synthesis only
 *       no-ops when there are no vendor forms at all.</li>
 *   <li><b>Hand-authored per-step contracts win.</b> A step that already declares a
 *       {@code subjectFormView} (e.g. from {@code <structr:subjectContract>}) is left untouched;
 *       synthesis only wires steps that have no view yet.</li>
 *   <li><b>Idempotent.</b> Type, properties and views are find-or-create by name, so re-importing
 *       a newer version of the same process reuses the synthesized type instead of duplicating it.</li>
 *   <li><b>Schema settles on commit.</b> This only creates schema nodes; {@code Traits.of(type)} and
 *       the views become live after the transaction commits and the schema recompiles. Nothing in
 *       the import transaction needs the synthesized type compiled, so that ordering is fine.</li>
 * </ul>
 */
public final class SubjectTypeSynthesizer {

	private static final Logger logger = LoggerFactory.getLogger(SubjectTypeSynthesizer.class);

	/**
	 * Property names already defined on every node (via {@link NodeInterfaceTraitDefinition} /
	 * {@link GraphObjectTraitDefinition}). A form field of one of these names is included in the
	 * views but NOT re-created as a SchemaProperty, so we never collide with an inherited key. Built
	 * from the base-trait constants (not string literals) so a rename there fails this build rather
	 * than letting the set silently drift out of sync.
	 */
	private static final Set<String> BASE_PROPERTY_NAMES = Set.of(
		GraphObjectTraitDefinition.ID_PROPERTY,
		GraphObjectTraitDefinition.TYPE_PROPERTY,
		GraphObjectTraitDefinition.CREATED_BY_PROPERTY,
		GraphObjectTraitDefinition.CREATED_DATE_PROPERTY,
		GraphObjectTraitDefinition.LAST_MODIFIED_DATE_PROPERTY,
		GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,
		GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY,
		NodeInterfaceTraitDefinition.NAME_PROPERTY,
		NodeInterfaceTraitDefinition.OWNER_PROPERTY,
		NodeInterfaceTraitDefinition.HIDDEN_PROPERTY
	);

	private SubjectTypeSynthesizer() {}

	/**
	 * Synthesize a subject type for {@code procNode} from {@code forms} and wire each step's view.
	 *
	 * @return the subject type name now on the process (existing or synthesized), or null when
	 *         there was nothing to do
	 */
	public static String synthesize(final App app, final NodeInterface procNode, final List<VendorTaskForm> forms, final Map<String, NodeInterface> elementMap) throws FrameworkException {

		final Traits procTraits                  = procNode.getTraits();
		final PropertyKey<String> subjectTypeKey = procTraits.key(BpmnProcessTraitDefinition.SUBJECT_TYPE_PROPERTY);
		final String existing = procNode.getProperty(subjectTypeKey);

		// Nothing to synthesize without vendor forms; return whatever subject type is already set.
		if (forms == null || forms.isEmpty()) {

			return existing;
		}

		// The type NAME is the explicit subjectType when one is declared (e.g. via <structr:subject>),
		// otherwise it is derived from the process name. Declaring the name does NOT suppress
		// synthesis -- the schema type and per-step views are still built from the vendor forms; an
		// explicit name just makes the result stable and round-trippable instead of tied to the
		// process name.
		final String typeName = StringUtils.isNotBlank(existing) ? existing : sanitizeTypeName(deriveBaseName(procNode));
		if (typeName == null) {

			logger.warn("BPMN import: could not derive a valid subject type name for process '{}'; skipping form synthesis.", procNode.getUuid());

			return null;
		}

		final NodeInterface schemaNode = findOrCreateType(app, typeName);
		final Map<String, String> unionFieldTypes = collectFieldTypes(forms, procNode);
		final int createdProps                     = createProperties(app, schemaNode, unionFieldTypes);

		if (StringUtils.isBlank(existing)) {

			procNode.setProperty(subjectTypeKey, typeName);
		}

		final int viewCount = wireStepViews(app, schemaNode, forms, elementMap);

		logger.info("BPMN import: synthesized subject type '{}' ({} new field(s)) from {} vendor form(s) on process '{}'; wired {} step view(s).",
			typeName, createdProps, forms.size(), procNode.getUuid(), viewCount);

		return typeName;
	}

	/**
	 * Union of every task form's fields; the first declaration of a name wins its type. A later task
	 * re-declaring the same key with a different type is ignored and logged -- a process has one
	 * subject, so a field name must resolve to a single type.
	 */
	private static Map<String, String> collectFieldTypes(final List<VendorTaskForm> forms, final NodeInterface procNode) {

		final Map<String, String> unionFieldTypes = new LinkedHashMap<>();

		for (final VendorTaskForm form : forms) {

			for (final VendorFormField field : form.fields()) {

				final String previousType = unionFieldTypes.putIfAbsent(field.key(), field.structrType());
				if (previousType != null && !previousType.equals(field.structrType())) {

					logger.warn("BPMN import: form field '{}' is declared as both '{}' and '{}' across the tasks of process '{}'; keeping the first ('{}').",
						field.key(), previousType, field.structrType(), procNode.getUuid(), previousType);
				}
			}
		}

		return unionFieldTypes;
	}

	/** Create a SchemaProperty for each non-inherited field name; returns how many were newly created. */
	private static int createProperties(final App app, final NodeInterface schemaNode, final Map<String, String> unionFieldTypes) throws FrameworkException {

		int createdProps = 0;

		for (final Map.Entry<String, String> entry : unionFieldTypes.entrySet()) {

			if (!BASE_PROPERTY_NAMES.contains(entry.getKey())) {

				if (findOrCreateProperty(app, schemaNode, entry.getKey(), entry.getValue())) {

					createdProps++;
				}
			}
		}

		return createdProps;
	}

	/**
	 * Wire each user-task step to a per-step form {@code SchemaView} (and, when the vendor marks some
	 * fields read-only, a writable view naming the editable subset). A step that already declares a
	 * form view (hand-authored contract) is left untouched. Returns the number of steps wired.
	 */
	private static int wireStepViews(final App app, final NodeInterface schemaNode, final List<VendorTaskForm> forms, final Map<String, NodeInterface> elementMap) throws FrameworkException {

		// Guard against two task ids that sanitize to the same view name (e.g. "Task-A" / "Task_A"):
		// the first keeps the plain name, later collisions get a deterministic "_n" suffix so no view
		// silently overwrites another. The loop runs in stable document order, so re-imports stay
		// idempotent (the same task always resolves to the same view name).
		final Set<String> usedViewNames = new HashSet<>();
		int viewCount = 0;

		for (final VendorTaskForm form : forms) {

			final NodeInterface element = elementMap.get(form.taskBpmnId());
			if (element == null) {

				logger.warn("BPMN import: form references user task '{}' not found in the element map; view not wired.", form.taskBpmnId());
				continue;
			}

			final Traits elemTraits = element.getTraits();

			// Respect an explicit per-step contract: a step that already declares its form view
			// (e.g. from <structr:subjectContract>) is left untouched -- hand-authored contracts win.
			if (StringUtils.isNotBlank(element.getProperty(elemTraits.key(BpmnElementTraitDefinition.SUBJECT_FORM_VIEW_PROPERTY)))) {

				continue;
			}

			final List<String> formFields = form.fields().stream().map(VendorFormField::key).toList();
			final String formViewName     = uniqueName(viewName("form_", form.taskBpmnId()), usedViewNames);

			findOrCreateView(app, schemaNode, formViewName, String.join(",", formFields));
			element.setProperty(elemTraits.key(BpmnElementTraitDefinition.SUBJECT_FORM_VIEW_PROPERTY), formViewName);
			viewCount++;

			// Wire a writable view whenever the vendor marks ANY field read-only -- it names the
			// editable subset (possibly empty, when every field is read-only). Leaving it unset means
			// "all shown fields are writable", so skipping the all-read-only case would silently make
			// a fully locked form editable; we only omit the view when nothing is restricted at all.
			final List<String> writableFields = form.fields().stream().filter(f -> !f.readOnly()).map(VendorFormField::key).toList();
			if (writableFields.size() < formFields.size()) {

				final String writeViewName = uniqueName(viewName("write_", form.taskBpmnId()), usedViewNames);
				findOrCreateView(app, schemaNode, writeViewName, String.join(",", writableFields));
				element.setProperty(elemTraits.key(BpmnElementTraitDefinition.SUBJECT_WRITABLE_VIEW_PROPERTY), writeViewName);
			}
		}

		return viewCount;
	}

	// ----- find-or-create helpers (idempotent across re-imports) -----

	private static NodeInterface findOrCreateType(final App app, final String typeName) throws FrameworkException {

		final NodeInterface existing = app.nodeQuery(StructrTraits.SCHEMA_NODE).name(typeName).getFirst();
		if (existing != null) {

			return existing;
		}

		return app.create(StructrTraits.SCHEMA_NODE, typeName);
	}

	/** @return true if a new property was created (false when it already existed). */
	private static boolean findOrCreateProperty(final App app, final NodeInterface schemaNode, final String name, final String structrType) throws FrameworkException {

		final Traits pt                           = Traits.of(StructrTraits.SCHEMA_PROPERTY);
		final PropertyKey<NodeInterface> nodeKey  = pt.key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY);
		final PropertyKey<String> nameKey         = pt.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final NodeInterface found = app.nodeQuery(StructrTraits.SCHEMA_PROPERTY).key(nodeKey, schemaNode).key(nameKey, name).getFirst();

		if (found != null) {

			return false;
		}

		app.create(StructrTraits.SCHEMA_PROPERTY,
			new NodeAttribute<>(nameKey, name),
			new NodeAttribute<>(pt.key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY), structrType),
			new NodeAttribute<>(nodeKey, schemaNode));

		return true;
	}

	private static void findOrCreateView(final App app, final NodeInterface schemaNode, final String viewName, final String fieldsCsv) throws FrameworkException {

		final Traits vt                          = Traits.of(StructrTraits.SCHEMA_VIEW);
		final PropertyKey<NodeInterface> nodeKey = vt.key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY);
		final PropertyKey<String> nameKey        = vt.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final PropertyKey<String> ngpKey         = vt.key(SchemaViewTraitDefinition.NON_GRAPH_PROPERTIES_PROPERTY);
		final NodeInterface found = app.nodeQuery(StructrTraits.SCHEMA_VIEW).key(nodeKey, schemaNode).key(nameKey, viewName).getFirst();

		if (found != null) {

			// keep the field list in sync when a newer version of the process is re-imported
			found.setProperty(ngpKey, fieldsCsv);

			return;
		}

		app.create(StructrTraits.SCHEMA_VIEW, new NodeAttribute<>(nameKey, viewName), new NodeAttribute<>(ngpKey, fieldsCsv), new NodeAttribute<>(nodeKey, schemaNode));
	}

	// ----- name derivation -----

	private static String deriveBaseName(final NodeInterface procNode) {

		final Traits t    = procNode.getTraits();
		final String name = procNode.getProperty(t.key(BpmnProcessTraitDefinition.PROCESS_NAME_PROPERTY));

		if (StringUtils.isNotBlank(name)) {

			return name;
		}

		return procNode.getProperty(t.key(BpmnProcessTraitDefinition.PROCESS_ID_PROPERTY));
	}

	/** Reduce a process name to a valid Structr type name (letters/digits, leading letter). */
	private static String sanitizeTypeName(final String base) {

		if (base == null) {

			return null;
		}

		final StringBuilder sb = new StringBuilder();

		for (int i = 0; i < base.length(); i++) {

			final char c = base.charAt(i);
			if (Character.isLetterOrDigit(c)) {

				sb.append(c);
			}
		}

		if (sb.length() == 0) {

			return null;
		}

		if (!Character.isLetter(sb.charAt(0))) {

			sb.insert(0, 'T');
		}

		sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));

		return sb.toString();
	}

	/** A stable, valid view name from a step's bpmnId (non-identifier chars become underscores). */
	private static String viewName(final String prefix, final String taskBpmnId) {

		final StringBuilder sb = new StringBuilder(prefix);

		for (int i = 0; i < taskBpmnId.length(); i++) {

			final char c = taskBpmnId.charAt(i);
			sb.append(Character.isLetterOrDigit(c) ? c : '_');
		}

		return sb.toString();
	}

	/** Keep a view name unique within one synthesis pass; a collision gets a deterministic "_n" suffix. */
	private static String uniqueName(final String candidate, final Set<String> used) {

		String name = candidate;
		int n       = 2;

		while (!used.add(name)) {

			name = candidate + "_" + n++;
		}

		return name;
	}
}
