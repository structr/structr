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
package org.structr.test.core.script;

import org.structr.core.script.Scripting;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Unit tests for Scripting.extractScripts() — verifies that braces inside
 * comments and string literals are not counted as structural braces.
 */
public class ScriptExtractTest {

	@Test
	public void testExtractScriptsBasic() {

		final String source  = "${{ $.log('Test'); }}";
		final List<String> r = Scripting.extractScripts(source);

		assertEquals("Basic script extraction must yield exactly one result", 1, r.size());
		assertEquals(source, r.get(0));
	}

	@Test
	public void testExtractScriptsWithNestedBraces() {

		final String source  = "${{ if (x) { return 1; } }}";
		final List<String> r = Scripting.extractScripts(source);

		assertEquals("Nested braces must be handled correctly", 1, r.size());
		assertEquals(source, r.get(0));
	}

	@Test
	public void testExtractScriptsWithSingleQuoteString() {

		final String source  = "${{ $.log('Test 1');\n'{'\n$.log('Test 2');\n}}";
		final List<String> r = Scripting.extractScripts(source);

		assertEquals("{ in single-quoted string must not be counted as structural brace", 1, r.size());
		assertEquals(source, r.get(0));
	}

	@Test
	public void testExtractScriptsWithDoubleQuoteString() {

		final String source  = "${{ $.log('Test 1');\n\"{\"\n$.log('Test 2');\n}}";
		final List<String> r = Scripting.extractScripts(source);

		assertEquals("{ in double-quoted string must not be counted as structural brace", 1, r.size());
		assertEquals(source, r.get(0));
	}

	@Test
	public void testExtractScriptsWithLineComment() {

		final String source  = "${{ $.log('Test 1');\n// {\n$.log('Test 2');\n}}";
		final List<String> r = Scripting.extractScripts(source);

		assertEquals("{ in line comment must not be counted as structural brace", 1, r.size());
		assertEquals(source, r.get(0));
	}

	@Test
	public void testExtractScriptsWithBlockComment() {

		final String source  = "${{ $.log('Test 1');\n/* { */\n$.log('Test 2');\n}}";
		final List<String> r = Scripting.extractScripts(source);

		assertEquals("{ in block comment must not be counted as structural brace", 1, r.size());
		assertEquals(source, r.get(0));
	}

	@Test
	public void testExtractScriptsWithMultilineBlockComment() {

		final String source  = "${{ $.log('Test 1');\n/*\n * {\n */\n$.log('Test 2');\n}}";
		final List<String> r = Scripting.extractScripts(source);

		assertEquals("{ in multi-line block comment must not be counted as structural brace", 1, r.size());
		assertEquals(source, r.get(0));
	}

	@Test
	public void testExtractScriptsWithTemplateLiteral() {

		final String source  = "${{ $.log('Test 1');\n`{`\n$.log('Test 2');\n}}";
		final List<String> r = Scripting.extractScripts(source);

		assertEquals("{ in template literal must not be counted as structural brace", 1, r.size());
		assertEquals(source, r.get(0));
	}

	@Test
	public void testExtractScriptsMultipleScripts() {

		final String source  = "prefix ${{ $.log('a'); }} middle ${{ $.log('b'); }} suffix";
		final List<String> r = Scripting.extractScripts(source);

		assertEquals("Two script blocks must be extracted", 2, r.size());
		assertEquals("${{ $.log('a'); }}", r.get(0));
		assertEquals("${{ $.log('b'); }}", r.get(1));
	}

	@Test
	public void testUnclosedBlockIsReportedWithItsState() {

		final List<String> reports = new ArrayList<>();

		// a string that is opened and never closed
		final List<String> r = Scripting.extractScripts("<main>${{ const html = $.this.body; return html.replace('<', '); }}</main>", (excerpt, state) -> reports.add(excerpt + " | " + state));

		assertEquals("the unclosed block is not extracted", 0, r.size());
		assertEquals(1, reports.size());
		assertTrue(reports.get(0), reports.get(0).startsWith("${{ const html = $.this.body;"));
		assertTrue(reports.get(0), reports.get(0).endsWith("inside a single-quoted string"));

		reports.clear();

		Scripting.extractScripts("${ concat('a', 'b') ", (excerpt, state) -> reports.add(state));

		assertEquals(List.of("1 brace(s) still open"), reports);
	}

	@Test
	public void testClosedBlocksAreNotReported() {

		final List<String> reports = new ArrayList<>();

		for (final String source : new String[] {
			"${{ $.log('Test'); }}",
			"${{ $.log('it\\'s \\\"quoted\\\"'); }}",
			"<p>${current.name}</p><p>${{ return 1; }}</p>",
			"<script>const greeting = `Hello ${name}`; ${{ return 'x'; }}</script>",
			"no expression at all"
		}) {

			Scripting.extractScripts(source, (excerpt, state) -> reports.add(source + " | " + state));
		}

		assertEquals(List.of(), reports);
	}

	@Test
	public void testRegularExpressionLiteralsAreSkippedWhole() {

		for (final String block : new String[] {
			// the production case: quotes inside a regular expression literal
			"${{ const html = $.this.body; return html.replace(/href=\"([^\"]+)'/g, '$1'); }}",
			"${{ s.replace(/[{}'\"`]/g, '') }}",
			"${{ s.split(/[/]/) }}",
			"${{ s.replace(/a\\/b'/, \"x\") }}",
			"${{ return /}/.test(x); }}",
			"${{ if (typeof /x'/ === 'object') { $.log(1); } }}",
			"${{ const m = s.match(/\\d+\"/gi); }}",
			"${{ list.filter(x => /^'/.test(x)) }}"
		}) {

			assertBlock(block);
		}
	}

	@Test
	public void testDivisionIsNotARegularExpression() {

		for (final String block : new String[] {
			"${{ a / b }}",
			"${{ x[i] / 2 }}",
			"${{ f() / 3 }}",
			"${{ (a + b) / c }}",
			"${{ total / count / 2 }}",
			// read as a regular expression, '/ b; y = \"/' would swallow a quote and the block would never end
			"${{ x = a / b; y = \"/\"; }}",
			"${{ ratio = 'a'.length / 2; z = '}'; }}"
		}) {

			assertBlock(block);
		}
	}

	@Test
	public void testCommentsAndStructrScriptWithSlashes() {

		for (final String block : new String[] {
			"${{ // it's a \"comment\" with / slashes\n return 1; }}",
			"${{ /* don't / \"quote\" */ return 1; }}",
			"${concat('a/b', \"c/d\")}",
			"${{ $.log('http://example.com/') }}"
		}) {

			assertBlock(block);
		}
	}

	// ----- private methods -----
	private void assertBlock(final String block) {

		final List<String> reports = new ArrayList<>();
		final List<String> r       = Scripting.extractScripts("<p>" + block + "</p>", (excerpt, state) -> reports.add(state));

		assertEquals(block + " must not be reported", List.of(), reports);
		assertEquals(block, List.of(block), r);
	}
}
