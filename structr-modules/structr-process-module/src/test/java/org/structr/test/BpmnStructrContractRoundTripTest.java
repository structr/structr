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
import org.structr.process.bpmn.BpmnExporter;
import org.structr.process.bpmn.BpmnImporter;
import org.structr.process.entity.BpmnDefinitions;
import org.structr.process.traits.definitions.BpmnElementTraitDefinition;
import org.structr.process.traits.definitions.BpmnProcessTraitDefinition;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Round-trip test for the Structr-native process/UI contract: {@code <structr:subject>} +
 * {@code <structr:subjectContract>} must be read on import (populating {@code subjectType} and the
 * per-step {@code subjectFormView} / {@code subjectWritableView} / {@code instructions}) AND
 * written back on export, so an import → export → re-import cycle preserves the contract.
 */
public class BpmnStructrContractRoundTripTest extends AbstractProcessEngineTest {

	@Test
	public void testStructrContractImportExportReimport() throws FrameworkException {

		final String xml = loadResource("/structr-contract.bpmn");
		final String exported;

		// ----- import: the structr: extensions populate the contract properties -----
		try (final Tx tx = app.tx()) {

			final NodeInterface defNode  = new BpmnImporter(securityContext).importBpmn(xml);
			final NodeInterface procNode = firstProcess(defNode);
			assertNotNull("import produced a process", procNode);

			assertEquals("process subject type from <structr:subject>", "Claim", subjectType(procNode));

			final NodeInterface a = elementByBpmnId(procNode, "Task_A");
			final NodeInterface b = elementByBpmnId(procNode, "Task_B");

			assertEquals("Task_A form view",     "v_a",    formView(a));
			assertEquals("Task_A writable view", "w_a",    writableView(a));
			assertEquals("Task_A instructions",  "Fill A", instructions(a));

			assertEquals("Task_B form view", "v_b", formView(b));
			assertNull("Task_B declares no writable view", writableView(b));

			// ----- export: the contract must be written back as structr: extensions -----
			exported = new BpmnExporter().exportBpmn(defNode.as(BpmnDefinitions.class));

			tx.success();
		}

		assertTrue("exported XML carries <structr:subject>",         exported.contains("subject") && exported.contains("type=\"Claim\""));
		assertTrue("exported XML carries <structr:subjectContract>", exported.contains("subjectContract"));
		assertTrue("exported XML carries the form view reference",   exported.contains("formView=\"v_a\""));
		assertTrue("exported XML carries the writable view",         exported.contains("writableView=\"w_a\""));
		assertTrue("exported XML carries the instructions",          exported.contains("instructions=\"Fill A\""));

		// ----- re-import the exported XML: the contract survives the round trip -----
		try (final Tx tx = app.tx()) {

			final NodeInterface defNode  = new BpmnImporter(securityContext).importBpmn(exported);
			final NodeInterface procNode = firstProcess(defNode);

			assertEquals("subject type survives round trip", "Claim", subjectType(procNode));

			final NodeInterface a = elementByBpmnId(procNode, "Task_A");
			assertEquals("form view survives round trip",     "v_a",    formView(a));
			assertEquals("writable view survives round trip", "w_a",    writableView(a));
			assertEquals("instructions survive round trip",   "Fill A", instructions(a));

			tx.success();
		}
	}

	// ----- helpers -----

	private String subjectType(final NodeInterface proc) {
		return proc.getProperty(proc.getTraits().key(BpmnProcessTraitDefinition.SUBJECT_TYPE_PROPERTY));
	}

	private String formView(final NodeInterface element) {
		return element.getProperty(element.getTraits().key(BpmnElementTraitDefinition.SUBJECT_FORM_VIEW_PROPERTY));
	}

	private String writableView(final NodeInterface element) {
		return element.getProperty(element.getTraits().key(BpmnElementTraitDefinition.SUBJECT_WRITABLE_VIEW_PROPERTY));
	}

	private String instructions(final NodeInterface element) {
		return element.getProperty(element.getTraits().key(BpmnElementTraitDefinition.INSTRUCTIONS_PROPERTY));
	}
}
