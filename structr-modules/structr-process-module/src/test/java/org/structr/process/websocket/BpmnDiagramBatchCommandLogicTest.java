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
package org.structr.process.websocket;

import org.structr.process.ProcessTraits;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Pure unit test for the type allowlist that scopes {@link BpmnDiagramBatchCommand}
 * to BPMN diagram-domain types (review finding #4). Runs without a database.
 *
 * <p>Guards against the abuse the check closes: a client sending an arbitrary
 * {@code type} (e.g. {@code User}) in a diagram-save batch must be rejected, while
 * legitimate BPMN diagram types are accepted.</p>
 */
public class BpmnDiagramBatchCommandLogicTest {

	@Test
	public void testBpmnDiagramTypesAreAllowed() {

		assertTrue(BpmnDiagramBatchCommand.isAllowedType(ProcessTraits.BPMN_DEFINITIONS));
		assertTrue(BpmnDiagramBatchCommand.isAllowedType(ProcessTraits.BPMN_PROCESS));
		assertTrue(BpmnDiagramBatchCommand.isAllowedType(ProcessTraits.BPMN_ELEMENT));
		assertTrue(BpmnDiagramBatchCommand.isAllowedType(ProcessTraits.BPMN_SEQUENCE_FLOW));
		assertTrue(BpmnDiagramBatchCommand.isAllowedType(ProcessTraits.BPMN_DI_SHAPE));
		assertTrue(BpmnDiagramBatchCommand.isAllowedType(ProcessTraits.BPMN_DI_EDGE));
		assertTrue(BpmnDiagramBatchCommand.isAllowedType(ProcessTraits.BPMN_COLLABORATION));
		assertTrue(BpmnDiagramBatchCommand.isAllowedType(ProcessTraits.BPMN_LANE));
	}

	@Test
	public void testNonDiagramTypesAreRejected() {

		// The documented abuse: creating arbitrary node types via the diagram batch.
		assertFalse(BpmnDiagramBatchCommand.isAllowedType("User"));
		assertFalse(BpmnDiagramBatchCommand.isAllowedType("Group"));
		assertFalse(BpmnDiagramBatchCommand.isAllowedType("Page"));
		assertFalse(BpmnDiagramBatchCommand.isAllowedType("SchemaMethod"));
		assertFalse(BpmnDiagramBatchCommand.isAllowedType("Principal"));

		// Runtime process types are not part of the diagram-editing surface.
		assertFalse(BpmnDiagramBatchCommand.isAllowedType(ProcessTraits.PROCESS_INSTANCE));
		assertFalse(BpmnDiagramBatchCommand.isAllowedType(ProcessTraits.PROCESS_TOKEN));

		assertFalse(BpmnDiagramBatchCommand.isAllowedType(null));
		assertFalse(BpmnDiagramBatchCommand.isAllowedType(""));
	}
}
