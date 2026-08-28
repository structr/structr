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
import org.structr.core.graph.Tx;
import org.structr.core.script.polyglot.config.ScriptConfig;
import org.structr.schema.action.Actions;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Covers async/await in embedded JavaScript snippets -- the ones wrapped by
 * {@code JSFunctionTranspiler}, which is what {@code wrapJsInMain} selects and what a
 * SchemaMethod uses by default.
 *
 * <p>The wrapper is an async arrow that the host calls, rather than a script that awaits at top
 * level. That distinction is the whole feature: a module using top-level await returns the module
 * evaluation promise, which fulfils with undefined, so the script's return value disappears. Two
 * earlier attempts at async support were reverted for exactly that reason, so
 * {@link #testWrappedScriptStillReturnsItsValue()} and the tests below are as much a guard against
 * reintroducing top-level await as they are a test of await itself.</p>
 */
public class AsyncScriptingTest extends StructrTest {

	// ----- helpers -----

	private Object evaluate(final String source, final boolean wrapJsInMain) throws FrameworkException {

		return Actions.execute(securityContext, null, "${{" + source + "}}", Collections.EMPTY_MAP, "asyncTest", null,
			ScriptConfig.builder().wrapJsInMain(wrapJsInMain).build());
	}

	/** Wrapped in the async arrow, as a SchemaMethod is by default. */
	private Object wrapped(final String source) throws FrameworkException {

		return evaluate(source, true);
	}

	/** Not wrapped -- evaluated as a module, answering its completion value, as an inline ${{ }} does. */
	private Object unwrapped(final String source) throws FrameworkException {

		return evaluate(source, false);
	}

	// ----- the wrapper still behaves like the synchronous one it replaced -----

	@Test
	public void testWrappedScriptStillReturnsItsValue() {

		try (final Tx tx = app.tx()) {

			assertEquals("A wrapped script must still return the value it returns", "hello", wrapped("return 'hello';"));
			assertEquals("A wrapped script must still see its own local state", Integer.valueOf(3), wrapped("let a = 1; let b = 2; return a + b;"));
			assertNull("A wrapped script that returns nothing must still answer null", wrapped("let a = 1;"));

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	// ----- await -----

	@Test
	public void testAwaitReturnsTheResolvedValue() {

		try (final Tx tx = app.tx()) {

			assertEquals("await on a resolved promise must answer its value",
				"resolved", wrapped("return await Promise.resolve('resolved');"));

			assertEquals("await on a plain value must answer that value",
				Integer.valueOf(42), wrapped("return await 42;"));

			assertEquals("an awaited async function must answer its return value",
				"inner", wrapped("const inner = async () => 'inner'; return await inner();"));

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testAsyncChainsAndDependenciesResolve() {

		try (final Tx tx = app.tx()) {

			assertEquals("sequential awaits must both resolve",
				Integer.valueOf(3), wrapped("const a = await Promise.resolve(1); const b = await Promise.resolve(2); return a + b;"));

			assertEquals("Promise.all must resolve every element",
				"1-2-3", wrapped("const r = await Promise.all([Promise.resolve(1), Promise.resolve(2), 3]); return r.join('-');"));

			assertEquals("a nested async function must resolve",
				"out:in", wrapped("const inner = async () => await Promise.resolve('in'); return 'out:' + await inner();"));

			assertEquals("a then() chain must resolve",
				Integer.valueOf(20), wrapped("return await Promise.resolve(1).then(v => v + 1).then(v => v * 10);"));

			assertEquals("an await that depends on a previous one must resolve",
				"a>b", wrapped("const a = await Promise.resolve('a'); const b = await Promise.resolve(a + '>b'); return b;"));

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testAwaitCanCallStructrFunctions() {

		try (final Tx tx = app.tx()) {

			assertEquals("a built-in called through await must answer normally",
				"VGVzdA==", wrapped("return await $.base64encode('Test');"));

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	// ----- failures inside an async body stay failures -----

	@Test
	public void testRejectedPromiseBecomesAScriptError() {

		try (final Tx tx = app.tx()) {

			try {

				wrapped("return await Promise.reject(new Error('rejected on purpose'));");
				fail("A rejected promise must raise a script error rather than answering null");

			} catch (final FrameworkException expected) {

				assertEquals(422, expected.getStatus());
			}

			try {

				wrapped("throw new Error('thrown on purpose');");
				fail("A throw inside an async body must raise a script error");

			} catch (final FrameworkException expected) {

				assertEquals(422, expected.getStatus());
			}

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testAssertInsideAnAsyncBodyKeepsItsStatusCode() {

		try (final Tx tx = app.tx()) {

			// $.assert() throws an AssertException carrying its own status code. It has to survive being
			// thrown from inside an async function, where it is a promise rejection before it is an
			// exception again -- otherwise every assert in every method silently changes meaning.
			try {

				wrapped("$.assert(false, 418, 'assert from an async body'); return 'not reached';");
				fail("A failing assert must raise an exception");

			} catch (final FrameworkException expected) {

				assertEquals("The assert's own status code must survive the async round trip", 418, expected.getStatus());
				assertTrue("The assert's message must survive the async round trip",
					expected.getMessage() != null && expected.getMessage().contains("assert from an async body"));
			}

			// and the passing case still passes
			assertEquals("passed", wrapped("$.assert(true, 418, 'not thrown'); return 'passed';"));

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testPromiseThatNeverSettlesIsReported() {

		try (final Tx tx = app.tx()) {

			try {

				wrapped("return await new Promise(() => {});");
				fail("A promise that never settles must raise a script error, not hang or answer null");

			} catch (final FrameworkException expected) {

				assertEquals(422, expected.getStatus());
				assertTrue("The error must say the promise never resolved, not 'Attempt to unwrap pending promise'; got: "
					+ expected.getMessage(), expected.getMessage().contains("never resolved"));
			}

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	// ----- the unwrapped path is deliberately unchanged -----

	@Test
	public void testUnwrappedScriptStillReturnsItsCompletionValue() {

		try (final Tx tx = app.tx()) {

			assertEquals("An unwrapped script must still answer its last expression",
				Integer.valueOf(42), unwrapped("40 + 2;"));

			assertEquals("An unwrapped script must still reach Structr functions",
				"VGVzdA==", unwrapped("$.base64encode('Test');"));

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testUnwrappedScriptResolvesAPromiseItReturns() {

		try (final Tx tx = app.tx()) {

			// Not wrapped, so nothing awaits this for the script. It used to answer null, because the old
			// PromiseConsumer read its result after the context had already been closed.
			assertEquals("A promise as the completion value must resolve to its value",
				Integer.valueOf(7), unwrapped("(async () => 7)();"));

			try {

				unwrapped("(async () => { throw new Error('rejected on purpose'); })();");
				fail("A rejected promise as the completion value must raise a script error");

			} catch (final FrameworkException expected) {

				assertEquals(422, expected.getStatus());
			}

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}
}
