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
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.process.bpmn.BpmnImporter;
import org.structr.process.traits.definitions.BpmnBaseNodeTraitDefinition;
import org.structr.process.traits.definitions.BpmnElementTraitDefinition;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Resolution rules for {@code <structr:methodRef name="..."/>}. A methodRef carries only a
 * name, and HAS_METHOD is One-to-Many, so a careless lookup can silently move a method away
 * from the process that owns it. These tests pin the strict behaviour of
 * {@code BpmnImporter#resolveMethodRef}: adopt a method only when the match is unambiguous
 * and the method isn't already owned by someone else, otherwise warn and skip.
 */
public class BpmnMethodRefResolutionTest extends AbstractProcessEngineTest {

	/**
	 * A methodRef must not adopt a method that already belongs to another BpmnElement.
	 * Names of BPMN-attached methods are globally unique (the SchemaMethod uniqueness
	 * validator treats every {@code schemaNode == null} method as one level), so an
	 * unrelated process referencing a common name like "refMethod" would otherwise take
	 * the method away from its owner -- ensureCardinality drops the previous relationship.
	 */
	@Test
	public void testMethodRefDoesNotStealMethodOwnedByAnotherElement() {

		final String ownerElementId;
		final String stolenMethodId;

		// an existing, unrelated process owning a method named "refMethod"
		try (final Tx tx = app.tx()) {

			final NodeInterface otherDef  = new BpmnImporter(securityContext).importBpmn(loadResource("/methodref-other-process.bpmn"));
			final NodeInterface otherProc = firstProcess(otherDef);
			final NodeInterface otherTask = elementByBpmnId(otherProc, "UserTask_2");
			assertNotNull("UserTask_2 not imported", otherTask);

			final NodeInterface refMethod = app.create(StructrTraits.SCHEMA_METHOD, (String) null);
			refMethod.setProperty(refMethod.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "refMethod");

			appendElementMethod(otherTask, refMethod);

			ownerElementId = otherTask.getUuid();
			stolenMethodId = refMethod.getUuid();

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected setup failure: " + fex.getMessage());
			return;
		}

		// importing a different process that references "refMethod" by name must not take it
		try (final Tx tx = app.tx()) {

			final NodeInterface defNode  = new BpmnImporter(securityContext).importBpmn(loadResource("/bug-methods-clobber.bpmn"));
			final NodeInterface procNode = firstProcess(defNode);
			final NodeInterface userTask = elementByBpmnId(procNode, "UserTask_1");
			assertNotNull("UserTask_1 not imported", userTask);

			final List<String> names = methodNames(userTask);

			assertFalse("methodRef must not adopt a method owned by another element; found: " + names, names.contains("refMethod"));
			assertTrue("the element's own task-listener method must still be attached; found: " + names, names.contains("onCreate"));

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected import failure: " + fex.getMessage());
		}

		// and the original owner must still have it
		try (final Tx tx = app.tx()) {

			final NodeInterface owner  = app.getNodeById(ownerElementId);
			final NodeInterface method = app.getNodeById(stolenMethodId);
			assertNotNull("the referenced method must not have been deleted", method);
			assertTrue("the original owner must keep its method; found: " + methodNames(owner), methodNames(owner).contains("refMethod"));

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected verification failure: " + fex.getMessage());
		}
	}

	/**
	 * A methodRef must never resolve to a method that belongs to a type: those are class
	 * methods (including the service-class stubs the importer scaffolds), not process
	 * handlers. The previous implementation fell back to a typed match when no unattached
	 * method of that name existed.
	 */
	@Test
	public void testMethodRefDoesNotAdoptTypedMethod() {

		try (final Tx tx = app.tx()) {

			final NodeInterface type = app.create(StructrTraits.SCHEMA_NODE, "MethodRefTestType");

			final NodeInterface typedMethod = app.create(StructrTraits.SCHEMA_METHOD, (String) null);
			typedMethod.setProperty(typedMethod.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "refMethod");
			typedMethod.setProperty(typedMethod.getTraits().key(SchemaMethodTraitDefinition.SCHEMA_NODE_PROPERTY), type);

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected setup failure: " + fex.getMessage());
			return;
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode  = new BpmnImporter(securityContext).importBpmn(loadResource("/bug-methods-clobber.bpmn"));
			final NodeInterface procNode = firstProcess(defNode);
			final NodeInterface userTask = elementByBpmnId(procNode, "UserTask_1");
			assertNotNull("UserTask_1 not imported", userTask);

			final List<String> names = methodNames(userTask);

			assertFalse("methodRef must not adopt a method owned by a type; found: " + names, names.contains("refMethod"));
			assertTrue("the element's own task-listener method must still be attached; found: " + names, names.contains("onCreate"));

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected import failure: " + fex.getMessage());
		}
	}

	/**
	 * The supported case: exactly one unattached SchemaMethod of that name exists (what a
	 * deployment import leaves behind), so the reference resolves and the method is attached.
	 */
	@Test
	public void testMethodRefResolvesUniqueUnattachedMethod() {

		try (final Tx tx = app.tx()) {

			final NodeInterface refMethod = app.create(StructrTraits.SCHEMA_METHOD, (String) null);
			refMethod.setProperty(refMethod.getTraits().key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "refMethod");

			final NodeInterface defNode  = new BpmnImporter(securityContext).importBpmn(loadResource("/bug-methods-clobber.bpmn"));
			final NodeInterface procNode = firstProcess(defNode);
			final NodeInterface userTask = elementByBpmnId(procNode, "UserTask_1");
			assertNotNull("UserTask_1 not imported", userTask);

			final List<String> names = methodNames(userTask);

			assertTrue("a unique unattached method must be adopted; found: " + names, names.contains("refMethod"));
			assertTrue("the element's own task-listener method must still be attached; found: " + names, names.contains("onCreate"));

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected import failure: " + fex.getMessage());
		}
	}

	/**
	 * Two independent processes may each declare a task listener calling "onCreate" -- a
	 * completely normal thing to do. Both must import, since nothing in BPMN says handler
	 * names are global.
	 */
	@Test
	public void testTwoProcessesMayUseTheSameListenerMethodName() {

		try (final Tx tx = app.tx()) {

			new BpmnImporter(securityContext).importBpmn(loadResource("/methodref-other-process.bpmn"));
			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected import failure for the first process: " + fex.getMessage());
			return;
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode  = new BpmnImporter(securityContext).importBpmn(loadResource("/bug-methods-clobber.bpmn"));
			final NodeInterface procNode = firstProcess(defNode);
			final NodeInterface userTask = elementByBpmnId(procNode, "UserTask_1");
			assertNotNull("UserTask_1 not imported", userTask);

			assertTrue("the second process must get its own 'onCreate' handler; found: " + methodNames(userTask), methodNames(userTask).contains("onCreate"));

			tx.success();

		} catch (final FrameworkException fex) {

			fail("A second process using the same listener method name must import: " + fex.getMessage());
		}
	}

	/**
	 * Deleting a process must take its listener handler methods with it. HAS_METHOD is an
	 * ownership relationship, and these methods have no {@code schemaNode}, so leaving them
	 * behind surfaces them in the Code area as user-defined functions -- new "global
	 * functions" appearing out of nowhere every time a process is deleted, which a later
	 * import could then adopt.
	 */
	@Test
	public void testDeletingProcessDeletesItsHandlerMethods() {

		final String procUuid;

		try (final Tx tx = app.tx()) {

			final NodeInterface defNode  = new BpmnImporter(securityContext).importBpmn(loadResource("/bug-methods-clobber.bpmn"));
			final NodeInterface procNode = firstProcess(defNode);
			final NodeInterface userTask = elementByBpmnId(procNode, "UserTask_1");
			assertNotNull("UserTask_1 not imported", userTask);
			assertTrue("expected the task-listener handler to be attached; found: " + methodNames(userTask), methodNames(userTask).contains("onCreate"));

			procUuid = procNode.getUuid();

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected import failure: " + fex.getMessage());
			return;
		}

		try (final Tx tx = app.tx()) {

			app.delete(app.getNodeById(procUuid));
			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected deletion failure: " + fex.getMessage());
			return;
		}

		try (final Tx tx = app.tx()) {

			assertEquals("deleting the process must not leave its handler method behind as a user-defined function",
				0, methodsNamed("onCreate").size());

			tx.success();

		} catch (final FrameworkException fex) {

			fail("Unexpected verification failure: " + fex.getMessage());
		}
	}

	// ----- helpers -----

	private List<NodeInterface> methodsNamed(final String name) throws FrameworkException {

		return app.nodeQuery(StructrTraits.SCHEMA_METHOD).name(name).getAsList();
	}

	private void appendElementMethod(final NodeInterface element, final NodeInterface method) throws FrameworkException {

		final PropertyKey<Iterable<NodeInterface>> methodsKey = element.getTraits().key(BpmnElementTraitDefinition.METHODS_PROPERTY);
		final List<NodeInterface> methods                     = collect(element.getProperty(methodsKey));

		methods.add(method);
		element.setProperty(methodsKey, methods);
	}

	private List<String> methodNames(final NodeInterface element) {

		final List<String> names = new ArrayList<>();

		for (final NodeInterface m : collect(element.getProperty(element.getTraits().key(BpmnElementTraitDefinition.METHODS_PROPERTY)))) {

			names.add(m.getName());
		}

		return names;
	}
}
