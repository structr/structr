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
import org.graalvm.polyglot.Value;
import org.structr.api.config.Settings;
import org.structr.core.script.polyglot.AccessProvider;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Verifies the observable behaviour of a GraalVM Context built with the
 * {@link org.graalvm.polyglot.PolyglotAccess} policy returned by
 * {@link AccessProvider#getPolyglotAccessConfig()} for each value of
 * {@code application.scripting.polyglot.access}.
 *
 * Under {@code ALL}, the {@code Polyglot} interop global is visible to
 * script code (enabling {@code Polyglot.eval}, {@code Polyglot.import}
 * and {@code Polyglot.export}). Under {@code NONE}, that global is
 * withheld and scripts cannot reach into other languages or the cross-
 * language bindings table.
 */
public class AccessProviderTest {

	private String previousValue;

	@BeforeMethod
	public void setUp() {

		previousValue = Settings.ScriptingPolyglotAccess.getValue();
	}

	@AfterMethod
	public void tearDown() {

		Settings.ScriptingPolyglotAccess.setValue(previousValue);
	}

	/**
	 * Guards the deliberate default ALL so existing admin-authored
	 * schema methods that rely on Polyglot.eval across languages
	 * do not silently break on upgrade.
	 */
	@Test
	public void defaultValueIsAll() {

		assertEquals("ALL", Settings.ScriptingPolyglotAccess.getDefaultValue());
	}

	@Test
	public void polyglotInteropAvailableInScriptUnderAll() {

		Settings.ScriptingPolyglotAccess.setValue("ALL");

		try (final Context ctx = Context.newBuilder("js")
				.allowPolyglotAccess(AccessProvider.getPolyglotAccessConfig())
				.build()) {

			final Value typeOfPolyglot = ctx.eval("js", "typeof Polyglot");
			assertEquals("object", typeOfPolyglot.asString());

			// Cross-language bindings table is reachable (even with nothing
			// in it) — this is the path a script would use to share values
			// with another language.
			final Value typeOfImport = ctx.eval("js", "typeof Polyglot.import");
			assertEquals("function", typeOfImport.asString());
		}
	}

	@Test
	public void polyglotInteropWithheldFromScriptUnderNone() {

		Settings.ScriptingPolyglotAccess.setValue("NONE");

		try (final Context ctx = Context.newBuilder("js")
				.allowPolyglotAccess(AccessProvider.getPolyglotAccessConfig())
				.build()) {

			final Value typeOfPolyglot = ctx.eval("js", "typeof Polyglot");
			assertEquals("undefined", typeOfPolyglot.asString());
		}
	}

	@Test
	public void pureScriptExecutionStillWorksUnderNone() {

		// Sanity check: tightening the sandbox must not break scripts that
		// stay within a single language. Without this guarantee, NONE is
		// effectively a kill switch for all scripting.
		Settings.ScriptingPolyglotAccess.setValue("NONE");

		try (final Context ctx = Context.newBuilder("js")
				.allowPolyglotAccess(AccessProvider.getPolyglotAccessConfig())
				.build()) {

			final Value result = ctx.eval("js", "[1, 2, 3].reduce((a, b) => a + b, 0)");
			assertEquals(6, result.asInt());
		}
	}
}
