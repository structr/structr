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
package org.structr.process.bpmn;

import org.structr.process.traits.definitions.BpmnProcessListenerTraitDefinition;
import org.structr.process.traits.definitions.BpmnTaskListenerTraitDefinition;
import org.testng.annotations.Test;

import static org.structr.process.bpmn.BpmnImporter.CAMUNDA_NS;
import static org.structr.process.bpmn.BpmnImporter.translateListenerEvent;
import static org.structr.process.bpmn.BpmnImporter.translateProcessListenerEvent;
import static org.testng.AssertJUnit.assertEquals;

/**
 * Pure unit tests for the Camunda -&gt; Structr listener-event name translation in
 * {@link BpmnImporter}. Runs without {@code StructrUiTest} -- no graph / database.
 *
 * <p>The non-obvious mapping these tests pin down is {@code delete -> cancelled}:
 * Camunda's {@code delete} task-listener event fires when a task is removed
 * <em>without</em> being completed normally (process cancelled, interrupting
 * boundary event, instance deletion), which is exactly Structr's
 * {@code cancelled}. Normal completion is the separate {@code complete ->
 * completed} case, so there is no double mapping. If someone "corrects"
 * {@code delete} to something else, these tests fail on purpose.</p>
 */
public class BpmnListenerEventTranslationTest {

	private static final String STRUCTR_NS = "http://structr.org/schema/process/1.0";

	// ------------------------------------------------------------------
	// Task listeners (Camunda namespace)
	// ------------------------------------------------------------------

	@Test
	public void testCamundaDeleteMapsToCancelled() {

		// The event under scrutiny: Camunda 'delete' == task ended without
		// completing == Structr 'cancelled'. NOT 'completed'.
		assertEquals(BpmnTaskListenerTraitDefinition.EVENT_CANCELLED, translateListenerEvent("delete", CAMUNDA_NS));
		assertEquals("cancelled", translateListenerEvent("delete", CAMUNDA_NS));
	}

	@Test
	public void testCamundaTaskListenerEventTranslation() {

		assertEquals(BpmnTaskListenerTraitDefinition.EVENT_CREATED,   translateListenerEvent("create", CAMUNDA_NS));
		assertEquals(BpmnTaskListenerTraitDefinition.EVENT_ASSIGNED,  translateListenerEvent("assignment", CAMUNDA_NS));
		assertEquals(BpmnTaskListenerTraitDefinition.EVENT_COMPLETED, translateListenerEvent("complete", CAMUNDA_NS));
		assertEquals(BpmnTaskListenerTraitDefinition.EVENT_CANCELLED, translateListenerEvent("delete", CAMUNDA_NS));
	}

	@Test
	public void testUnknownCamundaTaskEventPassesThrough() {

		// No Structr equivalent (e.g. Camunda's 'timeout' / 'update') -- kept verbatim.
		assertEquals("timeout", translateListenerEvent("timeout", CAMUNDA_NS));
		assertEquals("update", translateListenerEvent("update", CAMUNDA_NS));
	}

	@Test
	public void testNonCamundaTaskNamespacePassesThrough() {

		// structr:taskListener (and any other namespace) already speaks Structr's
		// vocabulary, so the raw event is returned untranslated.
		assertEquals("cancelled", translateListenerEvent("cancelled", STRUCTR_NS));
		assertEquals("declined", translateListenerEvent("declined", STRUCTR_NS));
		// 'delete' is only special in the Camunda namespace -- elsewhere it is not rewritten.
		assertEquals("delete", translateListenerEvent("delete", STRUCTR_NS));
	}

	// ------------------------------------------------------------------
	// Process / execution listeners (Camunda namespace)
	// ------------------------------------------------------------------

	@Test
	public void testCamundaProcessListenerEventTranslation() {

		assertEquals(BpmnProcessListenerTraitDefinition.EVENT_STARTED,   translateProcessListenerEvent("start", CAMUNDA_NS));
		assertEquals(BpmnProcessListenerTraitDefinition.EVENT_COMPLETED, translateProcessListenerEvent("end", CAMUNDA_NS));
	}

	@Test
	public void testUnknownCamundaProcessEventPassesThrough() {

		assertEquals("take", translateProcessListenerEvent("take", CAMUNDA_NS));
	}

	@Test
	public void testNonCamundaProcessNamespacePassesThrough() {

		assertEquals("started", translateProcessListenerEvent("started", STRUCTR_NS));
		assertEquals("start", translateProcessListenerEvent("start", STRUCTR_NS));
	}
}
