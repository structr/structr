/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.script;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.StructrTest;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.TestUser;
import org.structr.core.graph.Tx;
import org.structr.schema.action.ActionContext;

/**
 *
 *
 */
public class ScriptingTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(ScriptingTest.class.getName());
	/*

	public void testExtractScripts() {

		testExtraction("${function test() { return 'blah'; } return test();}");
		testExtraction("${function test() { return '{' + \"{\"; } return test();}");
		testExtraction("${function test() {{{ return '{' + \"{\" + '}'; }}} return test();}");

		// test multiple sources directly after each other
		final List<String> scripts = Scripting.extractScripts("${{this.aString}}${upper(true)}${lower(false)}");
		assertEquals("Invalid script extraction result", "${{this.aString}}", scripts.get(0));
		assertEquals("Invalid script extraction result", "${upper(true)}", scripts.get(1));
		assertEquals("Invalid script extraction result", "${lower(false)}", scripts.get(2));

		final List<String> scripts2 = Scripting.extractScripts("${{this.aString}}${{{upper(true)}}}{}{{${{{{lower(false)}}}}");
		assertEquals("Invalid script extraction result", "${{this.aString}}", scripts2.get(0));
		assertEquals("Invalid script extraction result", "${{{upper(true)}}}", scripts2.get(1));
		assertEquals("Invalid script extraction result", "${{{{lower(false)}}}}", scripts2.get(2));

		final List<String> scripts3 = Scripting.extractScripts("}}}{{$[[]{&/(&){}{{${{{{lower(false)}}}}}}}{{}{}{");
		assertEquals("Invalid script extraction result", "${{{{lower(false)}}}}", scripts3.get(0));

	}

	public void testJavascript() {

		try (final Tx tx = app.tx()) {

			final ActionContext actionContext = new ActionContext(securityContext);
			final TestOne test                = createTestNode(TestOne.class);

			test.setProperty(TestOne.anInt             , 1);
			test.setProperty(TestOne.aLong             , 2L);
			test.setProperty(TestOne.aDouble           , 3.0);
			test.setProperty(TestOne.aDate             , new Date());
			test.setProperty(TestOne.anEnum            , TestOne.Status.One);
			test.setProperty(TestOne.aString           , "täst");
			test.setProperty(TestOne.aBoolean          , true);
			test.setProperty(TestOne.anotherString     , "oneTwoThree${{{");
			test.setProperty(TestOne.stringWithQuotes  , "''\"\"''");

			assertEquals("Invalid JavaScript evaluation result", "test", Scripting.replaceVariables(actionContext, test, "${{ return 'test' }}"));
			assertEquals("Invalid JavaScript evaluation result", "1",    Scripting.replaceVariables(actionContext, test, "${{ return Structr.get('this').anInt; }}"));
			assertEquals("Invalid JavaScript evaluation result", "2",    Scripting.replaceVariables(actionContext, test, "${{ return Structr.get('this').aLong; }}"));
			assertEquals("Invalid JavaScript evaluation result", "3.0",  Scripting.replaceVariables(actionContext, test, "${{ return Structr.get('this').aDouble; }}"));

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	public void testPhp() {

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);

			assertEquals("Invalid PHP scripting evaluation result", "✓ Hello World from PHP!", Scripting.evaluate(ctx, null, "${quercus{<?\necho \"✓ Hello World from PHP!\";\n#phpinfo();\n?>}}"));

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	public void testRenjin() {

		//final String source = "${Renjin{print(\"Hello World from R!\")\n#plot(sin, -pi, 2*pi)\n# Define the cars vector with 5 values\ncars <- c(1, 3, 6, 4, 9)\n# Graph the cars vector with all defaults\nplot(cars)}}";

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);

			final Object result     = Scripting.evaluate(ctx, null, "${Renjin{print(\"Hello World from R!\")}}");
			assertEquals("Invalid R scripting evaluation result", "[1] \"Hello World from R!\"\n", result);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	public void testRuby() {

		final String source = "${ruby{puts \"Hello World from Ruby!\"}}";

		try (final Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);
			final Object result     = Scripting.evaluate(ctx, null, source);

			System.out.println(result);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}
	*/

	public void testPython() {

		try (final Tx tx = app.tx()) {

			final TestUser testUser = createTestNode(TestUser.class, "testuser");
			final ActionContext ctx = new ActionContext(SecurityContext.getInstance(testUser, AccessMode.Backend));

			//assertEquals("Invalid python scripting evaluation result", "Hello World from Python!\n", Scripting.evaluate(ctx, null, "${python{print \"Hello World from Python!\"}}"));

			System.out.println(Scripting.evaluate(ctx, null, "${python{print(Structr.get('me').id)}}"));

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	/*

	private void testExtraction(final String source) {

		final List<String> scripts = Scripting.extractScripts(source);
		assertEquals("Invalid script extraction result", source, scripts.get(0));
	}
	*/
}
