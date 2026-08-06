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
package org.structr.test;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.core.traits.definitions.SchemaViewTraitDefinition;
import org.structr.process.traits.definitions.BpmnElementTraitDefinition;
import org.structr.process.traits.definitions.BpmnProcessTraitDefinition;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * End-to-end test for the vendor-form import path: a Camunda BPMN with {@code camunda:formData}
 * on its user tasks must, on import, be turned into a synthesized Structr subject type with
 * per-step views wired to each step's {@code subjectFormView} -- because Structr is schema-type
 * based and a type-less process cannot render a form. See {@code BpmnVendorAdapter} for the
 * design pillars and {@code SubjectTypeSynthesizer} for the mapping.
 */
public class BpmnVendorFormImportTest extends AbstractProcessEngineTest {

	@Test
	public void testCamundaFormsSynthesizeSubjectTypeAndViews() throws FrameworkException {

		final String procId = importProcess("/camunda-forms.bpmn");

		try (final Tx tx = app.tx()) {

			final NodeInterface procNode = app.getNodeById(procId);
			assertNotNull("imported process should exist", procNode);

			// 1. The process now declares the synthesized subject type (from its name).
			final String subjectType = procNode.getProperty(procNode.getTraits().key(BpmnProcessTraitDefinition.SUBJECT_TYPE_PROPERTY));
			assertEquals("subject type is synthesized from the process name", "Beschaffungsanforderung", subjectType);

			// 2. A SchemaNode of that name exists, carrying the UNION of all task fields as
			//    SchemaProperties (title from task 1, approved from task 2, amount shared).
			final NodeInterface schemaNode = app.nodeQuery(StructrTraits.SCHEMA_NODE).name("Beschaffungsanforderung").getFirst();
			assertNotNull("synthesized SchemaNode should exist", schemaNode);

			final Set<String> propNames = propertyNamesOf(schemaNode);
			assertTrue("title property synthesized",    propNames.contains("title"));
			assertTrue("amount property synthesized",    propNames.contains("amount"));
			assertTrue("approved property synthesized",  propNames.contains("approved"));

			// 3. Per-task views hold each task's own field subset (declared order preserved).
			final Map<String, String> views = viewsOf(schemaNode);
			assertEquals("Antrag form view lists its two fields",   "title,amount",    views.get("form_Task_Antrag"));
			assertEquals("Approval form view lists its two fields", "amount,approved", views.get("form_Task_Approval"));

			// The Approval step marks 'amount' read-only, so a writable view is carved out with
			// only the editable field; the Antrag step has no read-only field, so none is made.
			assertEquals("Approval writable view excludes the read-only field", "approved", views.get("write_Task_Approval"));
			assertNull("Antrag has no read-only field, so no writable view", views.get("write_Task_Antrag"));

			// 4. Each user-task step is wired to its form view; the Approval step also to its
			//    writable view. The Antrag step's writable view stays unset (all fields writable).
			final NodeInterface antrag   = elementByBpmnId(procNode, "Task_Antrag");
			final NodeInterface approval = elementByBpmnId(procNode, "Task_Approval");

			assertEquals("Antrag step wired to its form view",     "form_Task_Antrag",   formView(antrag));
			assertNull("Antrag step has no writable view",         writableView(antrag));
			assertEquals("Approval step wired to its form view",   "form_Task_Approval", formView(approval));
			assertEquals("Approval step wired to its writable view","write_Task_Approval", writableView(approval));

			tx.success();
		}
	}

	@Test
	public void testExplicitSubjectNamesTheSynthesizedType() throws FrameworkException {

		// A <structr:subject type="ProcurementSubject"/> declares the name up front. It must NOT
		// suppress synthesis (the process still carries Camunda forms): the type is built under the
		// explicit name, not the process-name-derived "Beschaffungsanforderung", and the per-step
		// views are still wired.
		final String procId = importProcess("/camunda-forms-explicit-subject.bpmn");

		try (final Tx tx = app.tx()) {

			final NodeInterface procNode = app.getNodeById(procId);
			assertEquals("explicit <structr:subject> names the type", "ProcurementSubject",
				procNode.getProperty(procNode.getTraits().key(BpmnProcessTraitDefinition.SUBJECT_TYPE_PROPERTY)));

			// synthesized under the explicit name (not the process name)
			assertNotNull("type synthesized under the explicit name", app.nodeQuery(StructrTraits.SCHEMA_NODE).name("ProcurementSubject").getFirst());
			assertNull("no type under the process name when an explicit one is given", app.nodeQuery(StructrTraits.SCHEMA_NODE).name("Beschaffungsanforderung").getFirst());

			final NodeInterface schemaNode = app.nodeQuery(StructrTraits.SCHEMA_NODE).name("ProcurementSubject").getFirst();
			assertTrue("fields still synthesized from the Camunda forms", propertyNamesOf(schemaNode).contains("title"));

			// per-step view still wired
			final NodeInterface antrag = elementByBpmnId(procNode, "Task_Antrag");
			assertEquals("step view wired despite the explicit subject", "form_Task_Antrag", formView(antrag));

			tx.success();
		}
	}

	@Test
	public void testReimportReusesSynthesizedTypeIdempotently() throws FrameworkException {

		// Import the same Camunda process twice (as a re-import of a newer version would). Synthesis
		// is find-or-create by name, so the second pass must reuse the SchemaNode / properties /
		// views rather than duplicate them.
		importProcess("/camunda-forms.bpmn");
		importProcess("/camunda-forms.bpmn");

		try (final Tx tx = app.tx()) {

			assertEquals("re-import reuses the synthesized type rather than duplicating it", 1, countByName(StructrTraits.SCHEMA_NODE, "Beschaffungsanforderung"));

			// Views are keyed by name too, so no duplicates of the per-step views either.
			assertEquals("form view for the Antrag step is not duplicated", 1, countByName(StructrTraits.SCHEMA_VIEW, "form_Task_Antrag"));

			tx.success();
		}
	}

	@Test
	public void testAllReadOnlyFormStillGetsAWritableView() throws FrameworkException {

		// A user task whose every field is read-only must NOT be left with an unset writable view:
		// the convention "no writable view -> all fields writable" would silently turn a fully locked
		// form into a fully editable one. Synthesis therefore wires a writable view that names the
		// (empty) editable subset instead of omitting it.
		final String procId = importProcess("/camunda-forms-all-readonly.bpmn");

		try (final Tx tx = app.tx()) {

			final NodeInterface procNode = app.getNodeById(procId);
			final NodeInterface only     = elementByBpmnId(procNode, "Task_Only");

			assertEquals("form view wired to the step", "form_Task_Only", formView(only));

			// The regression guard: this used to be null (-> "everything writable") for an
			// all-read-only form; it must now point at a view.
			assertEquals("all-read-only step still gets a writable view", "write_Task_Only", writableView(only));

			final NodeInterface schemaNode   = app.nodeQuery(StructrTraits.SCHEMA_NODE).name("ReadonlyForm").getFirst();
			final Map<String, String> views = viewsOf(schemaNode);

			assertTrue("writable view node exists", views.containsKey("write_Task_Only"));

			final String writable = views.get("write_Task_Only");
			assertTrue("all-read-only writable view names no editable field", writable == null || writable.isEmpty());

			tx.success();
		}
	}

	@Test
	public void testConflictingFieldTypesKeepTheFirstDeclaration() throws FrameworkException {

		// Two tasks declare the same field key ('amount') with different types. The union is
		// first-declaration-wins, so the synthesized property keeps the first task's type (Long),
		// not the second's (String). The synthesizer also logs a warning about the clash.
		importProcess("/camunda-forms-type-conflict.bpmn");

		try (final Tx tx = app.tx()) {

			final NodeInterface schemaNode = app.nodeQuery(StructrTraits.SCHEMA_NODE).name("ConflictForm").getFirst();
			assertNotNull("synthesized type exists", schemaNode);

			assertEquals("first-declared type wins the union", "Long", propertyTypeOf(schemaNode, "amount"));

			tx.success();
		}
	}

	@Test
	public void testCollidingViewNamesAreDisambiguated() throws FrameworkException {

		// Two user-task ids that sanitize to the same view name ("Task-A" and "Task.A" both -> Task_A)
		// must NOT share a view: the first keeps the plain name, the second gets a deterministic
		// suffix, so neither step's field set silently overwrites the other's.
		final String procId = importProcess("/camunda-forms-viewname-collision.bpmn");

		try (final Tx tx = app.tx()) {

			final NodeInterface procNode = app.getNodeById(procId);
			final NodeInterface first    = elementByBpmnId(procNode, "Task-A");
			final NodeInterface second   = elementByBpmnId(procNode, "Task.A");

			assertEquals("first colliding step keeps the plain view name", "form_Task_A", formView(first));
			assertEquals("second colliding step gets a disambiguated name", "form_Task_A_2", formView(second));

			// each view carries its OWN field, proving no overwrite
			final NodeInterface schemaNode   = app.nodeQuery(StructrTraits.SCHEMA_NODE).name("CollisionForm").getFirst();
			final Map<String, String> views = viewsOf(schemaNode);

			assertEquals("first view lists its own field",  "alpha", views.get("form_Task_A"));
			assertEquals("second view lists its own field", "beta",  views.get("form_Task_A_2"));

			tx.success();
		}
	}

	private int countByName(final String type, final String name) throws FrameworkException {

		int n = 0;

		for (final NodeInterface ignored : app.nodeQuery(type).name(name).getResultStream()) {

			n++;
		}

		return n;
	}

	// ----- helpers -----

	private Set<String> propertyNamesOf(final NodeInterface schemaNode) throws FrameworkException {

		final Traits pt                          = Traits.of(StructrTraits.SCHEMA_PROPERTY);
		final PropertyKey<NodeInterface> nodeKey = pt.key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY);
		final Set<String> names                  = new HashSet<>();

		for (final NodeInterface prop : app.nodeQuery(StructrTraits.SCHEMA_PROPERTY).key(nodeKey, schemaNode).getResultStream()) {

			names.add(prop.getProperty(prop.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
		}

		return names;
	}

	private String propertyTypeOf(final NodeInterface schemaNode, final String propertyName) throws FrameworkException {

		final Traits pt                          = Traits.of(StructrTraits.SCHEMA_PROPERTY);
		final PropertyKey<NodeInterface> nodeKey = pt.key(SchemaPropertyTraitDefinition.SCHEMA_NODE_PROPERTY);
		final PropertyKey<String> nameKey        = pt.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final NodeInterface prop = app.nodeQuery(StructrTraits.SCHEMA_PROPERTY).key(nodeKey, schemaNode).key(nameKey, propertyName).getFirst();

		if (prop == null) {

			return null;
		}

		return prop.getProperty(prop.getTraits().key(SchemaPropertyTraitDefinition.PROPERTY_TYPE_PROPERTY));
	}

	private Map<String, String> viewsOf(final NodeInterface schemaNode) throws FrameworkException {

		final Traits vt                          = Traits.of(StructrTraits.SCHEMA_VIEW);
		final PropertyKey<NodeInterface> nodeKey = vt.key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY);
		final PropertyKey<String> ngpKey         = vt.key(SchemaViewTraitDefinition.NON_GRAPH_PROPERTIES_PROPERTY);
		final Map<String, String> views          = new HashMap<>();

		for (final NodeInterface view : app.nodeQuery(StructrTraits.SCHEMA_VIEW).key(nodeKey, schemaNode).getResultStream()) {

			final String name = view.getProperty(view.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY));
			views.put(name, view.getProperty(ngpKey));
		}

		return views;
	}

	private String formView(final NodeInterface element) {

		return element.getProperty(element.getTraits().key(BpmnElementTraitDefinition.SUBJECT_FORM_VIEW_PROPERTY));
	}

	private String writableView(final NodeInterface element) {

		return element.getProperty(element.getTraits().key(BpmnElementTraitDefinition.SUBJECT_WRITABLE_VIEW_PROPERTY));
	}
}
