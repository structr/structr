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

import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

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
}
