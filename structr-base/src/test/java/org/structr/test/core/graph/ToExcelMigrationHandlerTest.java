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
package org.structr.test.core.graph;

import org.structr.core.graph.ToExcelMigrationHandler;
import org.structr.core.graph.ToExcelMigrationHandler.Verdict;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * How the 7.0 migration classifies and rewrites a call to toExcel().
 *
 * The old second argument was a view name or a list of property names depending on its type, and those are
 * two different keys in the options object now. The tests here are mostly about not guessing that.
 */
public class ToExcelMigrationHandlerTest {

	@Test
	public void testCallsAreFoundInSurroundingCode() {

		final String source = "${to_excel(find('User'), 'public')} ${{ $.log('x'); const a = $.toExcel($.find('User'), ['name', 'id']); Structr.toExcel(users, 'ui'); }}";
		final List<String> calls = ToExcelMigrationHandler.findCalls(source);

		assertEquals("all three calls must be found", 3, calls.size());
		assertEquals("to_excel(find('User'), 'public')", calls.get(0));
		assertEquals("$.toExcel($.find('User'), ['name', 'id'])", calls.get(1));
		assertEquals("Structr.toExcel(users, 'ui')", calls.get(2));
	}

	@Test
	public void testOtherFunctionsAreNotFound() {

		// fromExcel is the other direction, and a method or declaration of the same name is someone else's code
		assertTrue(ToExcelMigrationHandler.findCalls("${{ $.fromExcel(file, 'Sheet0'); }}").isEmpty());
		assertTrue(ToExcelMigrationHandler.findCalls("${{ exporter.toExcel(rows, 'x'); }}").isEmpty());
		assertTrue(ToExcelMigrationHandler.findCalls("${{ function toExcel(rows, view) { return rows; } }}").isEmpty());
		assertTrue(ToExcelMigrationHandler.findCalls("${{ $.toExcelFile(rows, 'x'); }}").isEmpty());
	}

	@Test
	public void testNewStyleCallsAreUpToDate() {

		assertEquals(Verdict.UP_TO_DATE, verdict("toExcel(find('User'), { view: 'public' })"));
		assertEquals(Verdict.UP_TO_DATE, verdict("$.toExcel(users, { view: 'public', sheetName: 'Users' }, projects, { properties: ['id', 'name'] })"));

		// the later options objects may come from variables once the first one shows the new form
		assertEquals(Verdict.UP_TO_DATE, verdict("$.toExcel(users, { view: 'public' }, projects, projectOptions)"));

		// more arguments than the old signature ever accepted
		assertEquals(Verdict.UP_TO_DATE, verdict("$.toExcel(a, aOptions, b, bOptions, c, cOptions, d, dOptions)"));
	}

	@Test
	public void testBrokenCallsAreNotTheMigrationsBusiness() {

		// an options object in the second position is the new form, whatever else is wrong with the call
		assertEquals(Verdict.UP_TO_DATE, verdict("$.toExcel(users, { view: 'public' }, projects)"));

		// fewer than two arguments was never valid
		assertEquals(Verdict.UP_TO_DATE, verdict("$.toExcel(users)"));
		assertEquals(Verdict.UP_TO_DATE, verdict("toExcel()"));
	}

	@Test
	public void testLiteralViewsAndPropertyListsCanBeMigratedAutomatically() {

		assertEquals(Verdict.AUTOMATIC, verdict("toExcel(find('User'), 'public')"));
		assertEquals(Verdict.AUTOMATIC, verdict("toExcel(find('User'), \"public\")"));
		assertEquals(Verdict.AUTOMATIC, verdict("toExcel(find('User'), ['name', 'id'])"));
		assertEquals(Verdict.AUTOMATIC, verdict("toExcel(find('User'), 'public', true, false, 'excel', 1000, 't')"));
		assertEquals(Verdict.AUTOMATIC, verdict("$.toExcel($.find('User'), 'public', true, false, '', 1000, 't')"));
	}

	@Test
	public void testJavaScriptExpressionsInTheTailMoveOver() {

		// in JavaScript the options object is evaluated, so an expression keeps its meaning in there
		assertEquals(Verdict.AUTOMATIC, verdict("$.toExcel(users, 'public', withHeader, $.me.locale === 'de', domain, maxLength)"));
		assertEquals(Verdict.AUTOMATIC, verdict("$.toExcel(users, [nameKey, 'id'])"));
	}

	@Test
	public void testStructrScriptExpressionsInTheTailNeedAPerson() {

		// in StructrScript the options object is a JSON constant, so an expression cannot move into it
		final var flag = ToExcelMigrationHandler.assess("SchemaMethod", "id", "n", "source", "toExcel(find('User'), 'public', this.withHeader)");

		assertEquals(Verdict.MANUAL, flag.verdict());
		assertTrue("the reason must say why, was: " + flag.reason(), flag.reason().contains("constant"));

		assertEquals(Verdict.MANUAL, verdict("toExcel(find('User'), [this.nameKey, 'id'])"));
		assertEquals(Verdict.MANUAL, verdict("toExcel(find('User'), 'public', true, false, 'excel', size(this.limit))"));
	}

	@Test
	public void testAnExpressionInTheSecondPositionNeedsAPerson() {

		// a view name and a property list go to different keys now, and which one this is depends on its value
		final var old = ToExcelMigrationHandler.assess("SchemaMethod", "id", "n", "source", "$.toExcel(users, viewOrProperties, true)");

		assertEquals(Verdict.MANUAL, old.verdict());
		assertTrue("both keys must be named, was: " + old.reason(), old.reason().contains("view") && old.reason().contains("properties"));

		assertEquals(Verdict.MANUAL, verdict("toExcel(find('User'), concat('pub', 'lic'))"));

		// with two arguments it may as well be an options object in a variable, the reason has to say so
		final var ambiguous = ToExcelMigrationHandler.assess("SchemaMethod", "id", "n", "source", "$.toExcel(users, options)");

		assertEquals(Verdict.MANUAL, ambiguous.verdict());
		assertTrue("the reason must allow for an options object, was: " + ambiguous.reason(), ambiguous.reason().contains("up to date"));
	}

	@Test
	public void testAVariableHoldingAnOptionsObjectIsUpToDate() {

		final String source = "{\n\tlet config = { properties: Object.keys(data) };\n\t$.toExcel(data, config);\n}";
		final String call   = ToExcelMigrationHandler.findCalls(source).get(0);

		assertEquals(Verdict.UP_TO_DATE, ToExcelMigrationHandler.assess("SchemaMethod", "id", "n", "source", call, source).verdict());

		// reassigned later in the same object form
		assertEquals(Verdict.UP_TO_DATE, verdictIn("let config = { view: 'public' }; if (x) { config = { view: 'ui' }; } $.toExcel(data, config);"));
	}

	@Test
	public void testAVariableThatMayHoldSomethingElseStillNeedsAPerson() {

		// a view name in a variable is the old signature
		assertEquals(Verdict.MANUAL, verdictIn("const config = 'public'; $.toExcel(data, config);"));

		// one assignment that is not an object is enough to keep the call flagged
		assertEquals(Verdict.MANUAL, verdictIn("let config = { view: 'public' }; if (x) { config = ['id', 'name']; } $.toExcel(data, config);"));

		// a parameter, or a variable assigned from an expression, is unknown
		assertEquals(Verdict.MANUAL, verdictIn("function exportIt(data, config) { return $.toExcel(data, config); }"));
		assertEquals(Verdict.MANUAL, verdictIn("const config = $.retrieve('excelConfig'); $.toExcel(data, config);"));

		// comparisons, arrow functions and properties of the same name are not assignments
		assertEquals(Verdict.MANUAL, verdictIn("if (config === null) { this.config = {}; } $.toExcel(data, config);"));
		assertEquals(Verdict.MANUAL, verdictIn("const f = config => ({}); $.toExcel(data, config);"));
	}

	@Test
	public void testTheRewrites() {

		assertEquals("toExcel(find('User'), { view: 'public' })", ToExcelMigrationHandler.rewrite("toExcel(find('User'), 'public')"));

		// the even older name is replaced along the way
		assertEquals("toExcel(find('User'), { properties: ['name', 'id'] })", ToExcelMigrationHandler.rewrite("to_excel(find('User'), ['name', 'id'])"));

		// every position has its own key, and the localization domain has a new name
		assertEquals("$.toExcel($.find('User'), { view: 'excelView', includeHeader: true, localizeHeader: false, localizationDomain: '', maxCellLength: 1000, overflowMode: 't' })",
			ToExcelMigrationHandler.rewrite("$.toExcel($.find('User'), 'excelView', true, false, '', 1000, 't')"));

		// a call spread over several lines
		assertEquals("Structr.toExcel(users, { view: 'public', includeHeader: false })", ToExcelMigrationHandler.rewrite("Structr.toExcel(users,\n\t'public',\n\tfalse\n)"));

		// JavaScript expressions move over unchanged
		assertEquals("$.toExcel(users, { properties: [nameKey, 'id'], includeHeader: withHeader })", ToExcelMigrationHandler.rewrite("$.toExcel(users, [nameKey, 'id'], withHeader)"));
	}

	@Test
	public void testWhatIsNotAutomaticIsNotRewritten() {

		assertNull(ToExcelMigrationHandler.rewrite("$.toExcel(users, viewOrProperties)"));
		assertNull(ToExcelMigrationHandler.rewrite("toExcel(find('User'), 'public', this.withHeader)"));
		assertNull(ToExcelMigrationHandler.rewrite("toExcel(find('User'), { view: 'public' })"));
	}

	@Test
	public void testEveryAutomaticFindingCanActuallyBeRewritten() {

		// the promise the two modes make to each other: what the dry run calls AUTOMATIC, apply performs
		for (final String call : new String[] {

			"toExcel(find('User'), 'public')",
			"toExcel(find('User'), ['name', 'id'])",
			"toExcel(find('User'), 'public', true, false, 'excel', 1000, 't')",
			"$.toExcel(users, 'public', withHeader)",
			"$.toExcel(users, [nameKey, 'id'])",
			"$.toExcel(users, viewOrProperties)",
			"toExcel(find('User'), 'public', this.withHeader)"
		}) {

			if (ToExcelMigrationHandler.assess("SchemaMethod", "i", "n", "source", call).verdict() == Verdict.AUTOMATIC) {

				assertNotNull("assess() promised AUTOMATIC but rewrite() cannot produce it: " + call, ToExcelMigrationHandler.rewrite(call));
			}
		}
	}

	@Test
	public void testRewrittenCallsAreUpToDate() {

		// a second run must find nothing left to do
		for (final String call : new String[] {

			"toExcel(find('User'), 'public')", "toExcel(find('User'), ['name', 'id'])", "$.toExcel($.find('User'), 'excelView', true, false, '', 1000, 't')"
		}) {

			assertEquals(Verdict.UP_TO_DATE, verdict(ToExcelMigrationHandler.rewrite(call)));
		}
	}

	// ----- private methods -----
	private Verdict verdict(final String call) {

		return ToExcelMigrationHandler.assess("SchemaMethod", "id", "name", "source", call).verdict();
	}

	/** The verdict for the only call in the given source, with that source to look variables up in. */
	private Verdict verdictIn(final String source) {

		final List<String> calls = ToExcelMigrationHandler.findCalls(source);

		assertEquals("expected exactly one call in: " + source, 1, calls.size());

		return ToExcelMigrationHandler.assess("SchemaMethod", "id", "name", "source", calls.get(0), source).verdict();
	}
}
