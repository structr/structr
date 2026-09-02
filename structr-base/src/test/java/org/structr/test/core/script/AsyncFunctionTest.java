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

import org.structr.common.ContextStore;
import org.structr.common.error.FrameworkException;
import org.structr.core.function.Functions;
import org.structr.core.script.Scripting;
import org.structr.core.script.polyglot.config.ScriptConfig;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Actions;
import org.structr.schema.action.Function;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotSame;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * The generic half of the async function mechanism: which functions offer an async variant, how it is
 * reached, and what the worker is allowed to see.
 *
 * The behaviour of the variant against a real server is in {@code AsyncOutboundHttpTest}.
 */
public class AsyncFunctionTest extends StructrTest {

	/** The complete set of built-in functions that may run off the calling thread. */
	private static final List<String> ASYNC_CAPABLE = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "FETCH");

	private Object wrapped(final String source) throws FrameworkException {

		return Actions.execute(securityContext, null, "${{" + source + "}}", Collections.EMPTY_MAP, "asyncFunctionTest", null,
			ScriptConfig.builder().wrapJsInMain(true).build());
	}

	// ----- the opt-in itself -----

	@Test
	public void testExactlyTheExpectedFunctionsAreAsyncCapable() {

		// Pinned deliberately. isAsyncCapable() is inherited, so a new subclass of an opted-in function
		// acquires the flag silently -- which is exactly what POSTMultiPart does and has to undo. This
		// test is the structural guard: it fails both when someone deletes that override and when someone
		// opts a function in without thinking about the contract.
		for (final String name : ASYNC_CAPABLE) {

			final Function<Object, Object> func = Functions.get(name);

			assertNotSame("expected " + name + "() to be registered", null, func);
			assertTrue(name + "() must be async-capable", func.isAsyncCapable());
		}

		assertFalse("POSTMultiPart() must NOT inherit async-capability from POST(): it reads File nodes and their storage providers",
			Functions.get("POSTMultiPart").isAsyncCapable());

		for (final String name : Functions.getNames()) {

			if (!ASYNC_CAPABLE.contains(name)) {

				assertFalse(name + "() declares itself async-capable but is not in the expected set", Functions.get(name).isAsyncCapable());
			}
		}
	}

	// ----- reachability -----

	@Test
	public void testAsyncMemberIsReadableOnlyWhereItApplies() {

		try (final org.structr.core.graph.Tx tx = app.tx()) {

			// GraalJS asks isMemberReadable before it reads, so a hasMember that disagrees with getMember
			// makes the whole feature silently undefined rather than failing
			assertEquals("$.GET.async must be a function", "function", wrapped("return typeof $.GET.async;"));
			assertEquals("$.GET.async must be readable and enumerable consistently", true,
				wrapped("return Object.keys($.GET).includes('async');"));

			assertEquals("a function that is not async-capable must not offer async", "undefined", wrapped("return typeof $.log.async;"));
			assertEquals("POSTMultiPart must not offer async", "undefined", wrapped("return typeof $.POSTMultiPart.async;"));

			// the real namespaced members still resolve, and async has not displaced them
			assertEquals("namespaced members must be unaffected", "function", wrapped("return typeof $.log.warn;"));

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void testStructrScriptCannotReachTheAsyncVariant() {

		try (final org.structr.core.graph.Tx tx = app.tx()) {

			final ActionContext ctx = new ActionContext(securityContext);

			// Nothing is registered under "GET.async", so StructrScript -- which resolves a dotted name
			// against the registry rather than through FunctionWrapper -- cannot find it. Note the dot also
			// suppresses the "Unknown function" error, because that spelling is how a method call on an
			// entity is written, so the result is null rather than a failure.
			assertNull("StructrScript must not resolve GET.async", Scripting.evaluate(ctx, null, "${GET.async('http://localhost:1/')}", "test"));

			assertNull("nothing may be registered under the dotted name", Functions.get("GET.async"));

			tx.success();

		} catch (final FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	// ----- what the worker sees -----

	@Test
	public void testContextStoreSnapshotIsIndependentInBothDirections() {

		final ContextStore original = new ContextStore();

		original.addHeader("X-Before", "1");
		original.store("key", "before");
		original.setConstant("c", "before");
		original.setValidateCertificates(false);
		original.setSortKey("name");
		original.setSortDescending(true);
		original.setRangeStart(1);
		original.setRangeEnd(2);

		final ContextStore snapshot = original.snapshot();

		assertEquals("the snapshot must carry the headers", "1", snapshot.getHeaders().get("X-Before"));
		assertEquals("the snapshot must carry the request store", "before", snapshot.retrieve("key"));
		assertEquals("the snapshot must carry the constants", "before", snapshot.getConstant("c"));
		assertEquals("the snapshot must carry the certificate flag", false, snapshot.isValidateCertificates());
		assertEquals("the snapshot must carry the sort key", "name", snapshot.getSortKey());
		assertEquals("the snapshot must carry the sort order", true, snapshot.getSortDescending());
		assertEquals("the snapshot must carry the query range start", 1, snapshot.getRangeStart());

		// a later change on the calling thread must not reach a call already started
		original.addHeader("X-After", "1");
		original.store("key", "after");

		assertFalse("a header added after the snapshot must not appear in it", snapshot.getHeaders().containsKey("X-After"));
		assertEquals("a stored value changed after the snapshot must not appear in it", "before", snapshot.retrieve("key"));

		// and a worker's own writes must not escape into the caller's store
		snapshot.addHeader("X-Worker", "1");
		snapshot.store("key", "worker");

		assertFalse("a header added by the worker must not reach the caller", original.getHeaders().containsKey("X-Worker"));
		assertEquals("a value stored by the worker must not reach the caller", "after", original.retrieve("key"));

		// shared on purpose: copying would drop mail the worker added, dropping it would give it a second container
		assertSame("the mail container must be shared, not copied", original.getAdvancedMailContainer(), original.snapshot().getAdvancedMailContainer());
	}

	@Test
	public void testDetachedContextKeepsTheCallersLanguageAndItsOwnBuffers() {

		final ActionContext ctx = new ActionContext(securityContext);

		ctx.setScriptingEngine(ActionContext.ScriptingEngine.JS);
		ctx.addHeader("X-Before", "1");

		final ActionContext detached = ctx.detached();

		// carried over: every HTTP verb reports a bad argument through ctx.isJavaScriptContext(), and the
		// public copy constructor drops this -- a detached context built with that one would print
		// StructrScript usage to a JavaScript caller
		assertTrue("the detached context must report the caller's language", detached.isJavaScriptContext());
		assertEquals("the detached context must see the headers as they were", "1", detached.getHeaders().get("X-Before"));

		ctx.addHeader("X-After", "1");
		assertFalse("the detached context must not see later headers", detached.getHeaders().containsKey("X-After"));

		// its own, so that two threads are not appending to one buffer
		assertNotSame("the detached context must not share the error buffer", ctx.getErrorBuffer(), detached.getErrorBuffer());

		// shared on purpose; the contract is that a worker reads nothing from it
		assertSame("the detached context must share the security context", ctx.getSecurityContext(), detached.getSecurityContext());
	}

	// ----- what a rejection tells the author -----

	private String rejectionMessageOf(final String source) {

		try {

			Actions.execute(securityContext, null, "${{" + source + "}}", Collections.EMPTY_MAP, "rejectionTest", null,
				ScriptConfig.builder().wrapJsInMain(false).build());

			return "DID NOT THROW";

		} catch (final FrameworkException fex) {

			return fex.getMessage();
		}
	}

	@Test
	public void testARejectionSaysWhatItWasRejectedWith() {

		// A guest Error has members but no meta name the object conversion in PolyglotWrapper.unwrap
		// recognises. Without an explicit branch for it, it reaches that method's fall-through and becomes
		// null, which reports the rejection as "rejected with no reason given".
		assertTrue("a rejection with an Error must name it, was: " + rejectionMessageOf("Promise.reject(new Error('nope'))"),
			rejectionMessageOf("Promise.reject(new Error('nope'))").contains("Error: nope"));

		// the realistic shape: any async function that throws
		assertTrue("an async function throwing an Error must name it, was: " + rejectionMessageOf("(async () => { throw new Error('boom'); })()"),
			rejectionMessageOf("(async () => { throw new Error('boom'); })()").contains("Error: boom"));

		// subclasses and built-in error types carry their own name
		assertTrue("a TypeError must name itself", rejectionMessageOf("Promise.reject(new TypeError('bad type'))").contains("TypeError: bad type"));
		assertTrue("a custom Error subclass must name itself",
			rejectionMessageOf("class MyErr extends Error { constructor(m) { super(m); this.name = 'MyErr'; } }; Promise.reject(new MyErr('custom'))").contains("MyErr: custom"));

		// the shapes that already worked must keep working -- isException() is false for a plain object, so
		// the map conversion is untouched
		assertTrue("a string rejection is unchanged", rejectionMessageOf("Promise.reject('plainstring')").contains("plainstring"));
		assertTrue("a number rejection is unchanged", rejectionMessageOf("Promise.reject(42)").contains("42"));
		assertTrue("an object rejection is still converted to a map", rejectionMessageOf("Promise.reject({ code: 5 })").contains("code=5"));

		// and a rejection with genuinely nothing still says so
		assertTrue("an empty rejection still reports no reason", rejectionMessageOf("Promise.reject()").contains("no reason given"));
	}
}
