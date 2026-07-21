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

import org.testng.annotations.Test;

import static org.structr.process.bpmn.BpmnImporter.csvToFunctionExpression;
import static org.testng.AssertJUnit.assertEquals;

/**
 * FAILING reproduction for pure-logic findings in {@link BpmnImporter}. Asserts
 * intended behaviour and currently FAILS. No graph / database needed.
 */
public class BpmnImporterLogicBugTest {

	/**
	 * IMP-6: {@code csvToFunctionExpression} splits on <em>every</em> comma, so a
	 * single {@code ${...}} expression that itself contains a comma is torn apart
	 * (the second half is then wrapped as {@code user(b)}}), corrupting the
	 * resulting performer expression.
	 */
	@Test
	public void testCommaBearingExpressionIsNotSplit() {

		// One candidate expression, a function call with two args -- must stay intact.
		final String result = csvToFunctionExpression("${fn(a,b)}", "user");

		assertEquals("A single ${...} expression containing a comma must not be split", "${fn(a,b)}", result);
	}
}
