/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.websocket.command;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.traits.definitions.ComponentConfigurationTraitDefinition;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads the declarative {@code __visibilityMappingSpec} payload that
 * process-bound widgets emit from their dialog and creates a corresponding
 * {@code VisibilityMapping} bound to the freshly inserted widget root.
 *
 * <p>Lives here, in structr-base, even though the resulting node type is
 * defined by the process module: the type name {@code VisibilityMapping} is
 * already declared in {@link StructrTraits}, and the trait may simply be
 * absent at runtime (process module not loaded). In that case the helper is a
 * no-op and the widget still installs normally.</p>
 *
 * <p>Spec shape (JSON, sent as a single flat data-map entry):
 * <pre>{
 *   "visibleWhen":  "task-available",
 *   "boundProcess": "&lt;BpmnProcess UUID&gt;",
 *   "boundStep":    "&lt;BpmnElement UUID&gt;"
 * }</pre>
 * The denormalized {@code boundProcessId} and {@code boundStepBpmnId} are
 * filled in here by looking up the referenced nodes; the client does not have
 * to know about them.</p>
 */
public class WidgetAutoVisibilityMappingHelper {

	public static final String SPEC_DATA_KEY = "__visibilityMappingSpec";

	private static final Logger logger = LoggerFactory.getLogger(WidgetAutoVisibilityMappingHelper.class);
	private static final Gson    gson   = new Gson();

	/**
	 * Extract the spec from {@code data} (also removing it so it does not
	 * pollute downstream XML slot substitution) and, if present, create the
	 * VisibilityMapping bound to {@code rootNode}.
	 *
	 * <p>Visibility-flag inheritance from the append parent now lives in
	 * {@link WidgetVisibilityFlagInheritor} and runs unconditionally on
	 * every widget append, so a widget without an auto-VM still picks up
	 * the page's audience the same way a manually-created DOMNode does.</p>
	 *
	 * <p>Best-effort: any failure is logged at warn level; the widget install
	 * itself does not get unwound.</p>
	 */
	public static void applyAndConsume(final SecurityContext securityContext, final DOMNode rootNode, final Map<String, Object> data) {

		if (data == null) return;
		final Object raw = data.remove(SPEC_DATA_KEY);
		if (raw == null) return;
		if (rootNode == null) {
			logger.warn("WidgetAutoVisibilityMappingHelper: spec present but no widget root node available; skipping VM creation.");
			return;
		}

		if (!Traits.exists(StructrTraits.VISIBILITY_MAPPING)) {
			// Process module not loaded. The widget that emitted this spec
			// must therefore have come from a process-aware deployment that
			// no longer is one. Log and move on.
			logger.warn("WidgetAutoVisibilityMappingHelper: VisibilityMapping trait is not registered; skipping VM creation.");
			return;
		}

		final Map<String, Object> spec;
		try {
			spec = gson.fromJson(String.valueOf(raw), Map.class);
		} catch (JsonSyntaxException jex) {
			logger.warn("WidgetAutoVisibilityMappingHelper: malformed __visibilityMappingSpec '{}': {}", raw, jex.getMessage());
			return;
		}
		if (spec == null) return;

		final String visibleWhen     = stringOrNull(spec.get("visibleWhen"));
		final String boundProcessId  = stringOrNull(spec.get("boundProcess"));   // UUID
		final String boundStepId     = stringOrNull(spec.get("boundStep"));      // UUID, optional for process-level states

		if (visibleWhen == null || visibleWhen.isEmpty() || boundProcessId == null || boundProcessId.isEmpty()) {
			logger.warn("WidgetAutoVisibilityMappingHelper: incomplete spec (visibleWhen='{}', boundProcess='{}'); skipping VM creation.",
				visibleWhen, boundProcessId);
			return;
		}

		try {
			final App app = StructrApp.getInstance(securityContext);

			final NodeInterface processNode = app.getNodeById(boundProcessId);
			if (processNode == null) {
				logger.warn("WidgetAutoVisibilityMappingHelper: BpmnProcess '{}' not found; skipping VM creation.", boundProcessId);
				return;
			}
			final String denormalizedProcessId = processNode.getProperty(processNode.getTraits().key("processId"));

			NodeInterface stepNode      = null;
			String denormalizedStepBpmnId = null;
			if (boundStepId != null && !boundStepId.isEmpty()) {
				stepNode = app.getNodeById(boundStepId);
				if (stepNode == null) {
					logger.warn("WidgetAutoVisibilityMappingHelper: BpmnElement '{}' not found; creating VM without bound step.", boundStepId);
				} else {
					denormalizedStepBpmnId = stepNode.getProperty(stepNode.getTraits().key("bpmnId"));
				}
			}

			final Traits vmTraits = Traits.of(StructrTraits.VISIBILITY_MAPPING);
			final PropertyMap props = new PropertyMap();
			props.put(vmTraits.key("domNode"),         rootNode);
			props.put(vmTraits.key("visibleWhen"),     visibleWhen);
			props.put(vmTraits.key("boundProcess"),    processNode);
			props.put(vmTraits.key("boundProcessId"),  denormalizedProcessId);
			if (stepNode != null) {
				props.put(vmTraits.key("boundStep"),         stepNode);
				props.put(vmTraits.key("boundStepBpmnId"),   denormalizedStepBpmnId);
			}

			app.create(StructrTraits.VISIBILITY_MAPPING, props);

			// Seed fieldSet from the bound step's view: the UI dev sees the
			// view-derived fields list pre-populated and can fine-tune each
			// field via the Configured Fields UI (which lazily creates
			// DataAdapterField nodes only when a non-default config is set).
			// Skipped if the step does not declare a subjectType+subjectFormView.
			if (stepNode != null) {
				seedFieldSetFromView(rootNode, stepNode);
			}

		} catch (FrameworkException fex) {
			logger.warn("WidgetAutoVisibilityMappingHelper: failed to create VisibilityMapping for widget root '{}': {}",
				rootNode.getUuid(), fex.getMessage());
		}
	}

	/**
	 * Read {@code subjectType} + {@code subjectFormView} from the bound step,
	 * resolve the view to its property keys, and write the comma-joined names
	 * into the widget's {@code ComponentConfiguration.fieldSet}.
	 *
	 * <p>Per-field tuning ({@code renderTemplate}, {@code editTemplate},
	 * {@code label}, {@code columns}, ...) stays a separate concern: a
	 * {@code DataAdapterField} node is created lazily only when the UI dev sets
	 * a non-default value via the Configured Fields UI. We do not pre-create
	 * those nodes here.</p>
	 *
	 * <p>If {@code rootNode} itself has no ComponentConfiguration, the helper
	 * walks descendants to find the first one carrying it: process-bound
	 * widgets occasionally wrap their config-bearing form node in an outer
	 * container.</p>
	 *
	 * <p>Skipped silently when:
	 * <ul>
	 *   <li>The step has no {@code subjectType} or no {@code subjectFormView}.</li>
	 *   <li>The named view does not exist on the subject type.</li>
	 *   <li>No ComponentConfiguration is found in the widget subtree.</li>
	 * </ul>
	 * </p>
	 */
	public static void seedFieldSetFromView(final DOMNode rootNode, final NodeInterface stepNode) throws FrameworkException {

		final Traits stepTraits = stepNode.getTraits();
		if (!stepTraits.hasKey("subjectType")) return;

		final String subjectType = stepNode.getProperty(stepTraits.key("subjectType"));
		if (subjectType == null || subjectType.isEmpty()) return;
		if (!Traits.exists(subjectType))                  return;

		final String declaredView = stepTraits.hasKey("subjectFormView")
			? stepNode.getProperty(stepTraits.key("subjectFormView"))
			: null;

		final Traits subjectTraits = Traits.of(subjectType);
		final String fields        = fieldsForSubject(subjectTraits, declaredView);

		if (fields.isEmpty()) {

			logger.warn("WidgetAutoVisibilityMappingHelper: subject type '{}' has no usable fields (tried view '{}' and 'custom'), leaving the field set alone.",
				subjectType, declaredView);
			return;
		}

		final ComponentConfiguration config = findComponentConfiguration(rootNode);
		if (config == null) {
			logger.warn("WidgetAutoVisibilityMappingHelper: no ComponentConfiguration on widget root '{}' or its subtree; skipping fieldSet seeding.",
				rootNode.getUuid());
			return;
		}

		config.setProperty(config.getTraits().key(ComponentConfigurationTraitDefinition.FIELD_SET_PROPERTY), fields);
	}

	/**
	 * The comma-joined field list to seed for a subject type: the declared
	 * {@code subjectFormView} when the type has it, otherwise the {@code custom} view.
	 *
	 * <p>{@code custom} is the right fallback because it holds exactly the schema author's own
	 * properties -- every dynamically registered key is added to it automatically (see
	 * {@code Trait#registerPropertyKey}). {@code ui} and {@code public} are deliberately NOT
	 * used: they carry framework properties (owner, visibility flags, timestamps) that would
	 * fill a generated form with fields nobody asked for.</p>
	 *
	 * <p>System-internal and read-only keys are dropped in either case -- a form is for editing.
	 * An empty result leaves the field set untouched, so a subject type with no custom
	 * properties keeps the configuration's default rather than losing it.</p>
	 *
	 * <p>Without this fallback a step declaring only a {@code subjectType} kept the
	 * ComponentConfiguration's default field set of {@code name}, and the generated form showed
	 * exactly one field.</p>
	 */
	private static String fieldsForSubject(final Traits subjectTraits, final String declaredView) {

		final Set<String> viewNames = subjectTraits.getViewNames();

		for (final String candidate : new String[] { declaredView, PropertyView.Custom }) {

			if (candidate != null && !candidate.isEmpty() && viewNames.contains(candidate)) {

				final String fields = subjectTraits.getPropertyKeysForView(candidate).stream()
					.filter(key -> !key.isSystemInternal() && !key.isReadOnly())
					.map(PropertyKey::jsonName)
					.collect(Collectors.joining(","));

				if (!fields.isEmpty()) {

					return fields;
				}
			}
		}

		return "";
	}

	public static ComponentConfiguration findComponentConfiguration(final DOMNode rootNode) {

		final ComponentConfiguration onRoot = rootNode.getComponentConfiguration();
		if (onRoot != null) return onRoot;

		for (final NodeInterface descendant : rootNode.getAllChildNodes()) {
			if (descendant.is(StructrTraits.DOM_NODE)) {
				final ComponentConfiguration c = descendant.as(DOMNode.class).getComponentConfiguration();
				if (c != null) return c;
			}
		}
		return null;
	}

	private static String stringOrNull(final Object o) {
		return (o == null) ? null : String.valueOf(o);
	}
}
