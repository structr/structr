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

import org.structr.common.error.FrameworkException;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/** println() writes what print() writes, plus a trailing newline. */
public class PrintlnFunctionTest extends StructrTest {

	@Test
	public void testStructrScript() {

		assertEquals("print() must not append anything", "one two", render("${print('one ', 'two')}"));
		assertEquals("println() must append exactly one newline", "one two\n", render("${println('one ', 'two')}"));

		// without arguments it is just the newline
		assertEquals("println() without arguments must write a single newline", "\n", render("${println()}"));

		assertEquals("two calls must produce two lines", "a\nb\n", render("${println('a')}${println('b')}"));
	}

	@Test
	public void testJavaScript() {

		assertEquals("println() must append exactly one newline", "one two\n", render("${{ $.println('one ', 'two'); }}"));
		assertEquals("print() must not append anything", "one two", render("${{ $.print('one ', 'two'); }}"));
	}

	// ----- private methods -----
	private String render(final String script) {

		final ActionContext ctx = new ActionContext(securityContext);

		try {

			// print()/println() write into the output buffer, which replaceVariables returns
			return Scripting.replaceVariables(ctx, null, script);

		} catch (FrameworkException fex) {

			fail("Unexpected exception while evaluating " + script + ": " + fex.getMessage());
		}

		return null;
	}
}
