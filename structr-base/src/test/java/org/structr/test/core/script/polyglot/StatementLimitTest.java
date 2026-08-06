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
package org.structr.test.core.script.polyglot;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.ResourceLimits;
import org.graalvm.polyglot.Value;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Verifies the observable behaviour of a GraalVM Context when a
 * {@link ResourceLimits#statementLimit(long, java.util.function.Predicate)}
 * is applied — the same mechanism ContextFactory uses to bound runaway
 * scripts.
 */
public class StatementLimitTest {

	@Test
	public void infiniteLoopIsCancelledByStatementLimit() {

		final ResourceLimits limits = ResourceLimits.newBuilder()
			.statementLimit(1_000, null)
			.build();

		// Not a try-with-resources on purpose: after the limit trips, the
		// context is in a cancelled state and close() itself rethrows. We
		// swallow that close-time exception explicitly after asserting on
		// the eval-time one.
		final Context ctx = Context.newBuilder("js").resourceLimits(limits).build();

		try {

			ctx.eval("js", "while (true) {}");
			fail("Expected PolyglotException after statement limit was exceeded");

		} catch (final PolyglotException ex) {

			assertTrue(
				"Exception should indicate resource exhaustion; got: " + ex.getMessage(),
				ex.isCancelled() || ex.isResourceExhausted()
					|| (ex.getMessage() != null && ex.getMessage().contains("Statement count limit"))
			);

		} finally {

			try { ctx.close(); } catch (final PolyglotException ignored) { /* cancelled context */ }
		}
	}

	@Test
	public void normalScriptCompletesBelowStatementLimit() {

		final ResourceLimits limits = ResourceLimits.newBuilder()
			.statementLimit(10_000, null)
			.build();

		try (final Context ctx = Context.newBuilder("js").resourceLimits(limits).build()) {

			final Value result = ctx.eval("js", "let sum = 0; for (let i = 0; i < 10; i++) sum += i; sum;");
			assertEquals(45, result.asInt());
		}
	}
}
