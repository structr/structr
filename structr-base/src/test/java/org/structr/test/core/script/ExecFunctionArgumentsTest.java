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

import org.structr.common.SecurityContext;
import org.structr.core.function.ExecBinaryFunction;
import org.structr.core.function.ExecFunction;
import org.structr.schema.action.ActionContext;
import org.testng.annotations.AfterClass;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.structr.test.core.script.ExecTestScripts.printed;
import static org.testng.AssertJUnit.assertArrayEquals;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/** exec() and execBinary(): parameters reach the script as they are, input arrives on stdin, output is complete. */
public class ExecFunctionArgumentsTest {

	// each value would run a command or break out of the quoting if a shell interpreted it
	private static final List<String> VALUES = List.of("Invoice $(echo INJECTED)", "Invoice `echo INJECTED`", "x\\\"; echo INJECTED; \\\"", "two  spaces", "$HOME", "");

	private final ExecTestScripts scripts = new ExecTestScripts("arguments");

	@BeforeClass
	public void createScripts() throws Exception {

		scripts.create();
	}

	@AfterClass
	public void removeScripts() throws Exception {

		scripts.remove();
	}

	// ----- parameters -----
	@Test
	public void testExecPassesParametersUninterpreted() throws Exception {

		assertEquals(printed(VALUES), exec(scripts.printArgs, parameters()));
	}

	@Test
	public void testExecBinaryPassesParametersUninterpreted() throws Exception {

		assertEquals(printed(VALUES), execBinary(scripts.printArgs, parameters()));
	}

	@Test
	public void testParametersAsArgumentsOfTheirOwnAreTheSame() throws Exception {

		final List<Object> arguments = new ArrayList<>(List.of(scripts.printArgs));
		arguments.addAll(parameters());

		assertEquals("exec", printed(VALUES), exec(arguments.toArray()));

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		arguments.add(0, out);

		new ExecBinaryFunction().apply(context(), null, arguments.toArray());

		assertEquals("execBinary", printed(VALUES), out.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void testANullParameterIsRejectedInBothModes() throws Exception {

		final List<Object> withNull = new ArrayList<>();
		withNull.add("a");
		withNull.add(null);

		assertNull("collection mode", exec(scripts.printArgs, withNull));
		assertNull("argument mode", exec(scripts.printArgs, "a", null));
		assertNull("argument mode, first parameter", exec(scripts.printArgs, null, "a"));
	}

	@Test
	public void testNonAsciiParametersArriveUnchanged() throws Exception {

		// the JVM encodes arguments with the platform encoding, so a C locale turns them into '?', with or without a shell
		if (!StandardCharsets.UTF_8.name().equals(System.getProperty("native.encoding"))) {

			throw new SkipException("platform encoding is " + System.getProperty("native.encoding") + ", not UTF-8: pass non-ASCII values as input instead");
		}

		final List<String> values = List.of("äöü ß", "€ 日本語", "emoji 😀");

		assertEquals(printed(values), exec(scripts.printArgs, values));
		assertEquals(printed(values), exec(new Object[] { scripts.printArgs, values.get(0), values.get(1), values.get(2) }));
	}

	@Test
	public void testNonAsciiInputArrivesUnchangedInAnyLocale() throws Exception {

		final String value = "äöü ß € 日本語 😀";

		assertEquals(value + "\n", exec(scripts.cat, List.of(), null, value));
	}

	@Test
	public void testAScriptWithoutShebangStillRuns() throws Exception {

		assertEquals(printed(VALUES), exec(scripts.printArgsNoShebang, parameters()));
	}

	// ----- input -----
	@Test
	public void testExecPassesInputOnStdin() throws Exception {

		final String secret = "pa$$ \"word\" äöü";

		assertEquals(secret + "\n", exec(scripts.cat, List.of(), null, secret));
	}

	@Test
	public void testInputWorksWithALogBehaviour() throws Exception {

		for (final int logBehaviour : new int[] { 0, 1, 2 }) {

			assertEquals("logBehaviour " + logBehaviour, "secret\n", exec(scripts.cat, List.of(), logBehaviour, "secret"));
		}
	}

	@Test
	public void testWithoutInputStdinIsEmptyAndClosed() throws Exception {

		// a script reading stdin used to wait forever, because it was never closed
		assertEquals("", exec(scripts.cat, List.of()));
		assertEquals("", exec(scripts.cat, List.of(), 0, null));
	}

	@Test
	public void testExecBinaryPassesBinaryInputUnchanged() throws Exception {

		final byte[] bytes = new byte[256];

		for (int i = 0; i < bytes.length; i++) {

			bytes[i] = (byte) i;
		}

		assertArrayEquals(bytes, execBinaryBytes(scripts.cat, List.of(), 0, bytes));
	}

	@Test
	public void testLargeInputAndOutputDoNotBlockEachOther() throws Exception {

		// far more than a pipe buffer in both directions: written and read at the same time, or both sides wait forever
		final byte[] bytes = new byte[4 * 1024 * 1024];
		new Random(42).nextBytes(bytes);

		assertArrayEquals(bytes, execBinaryBytes(scripts.cat, List.of(), 0, bytes));
	}

	@Test
	public void testAScriptThatIgnoresItsInputStillFinishes() throws Exception {

		final byte[] bytes = new byte[4 * 1024 * 1024];

		assertEquals(printed(List.of("a")), exec(scripts.printArgs, List.of("a"), 0, bytes));
	}

	// ----- output -----
	@Test
	public void testTheOutputOfAFailingScriptIsReturned() throws Exception {

		assertEquals("partial\n", exec(scripts.fail, List.of()));
	}

	@Test
	public void testABackgroundChildDoesNotBlockTheResult() throws Exception {

		final long start = System.currentTimeMillis();

		assertEquals("before\n", exec(scripts.background, List.of()));
		assertTrue("returned after the drain limit, not when the child ended", System.currentTimeMillis() - start < 15_000);
	}

	@Test
	public void testTheCompleteOutputIsReturned() throws Exception {

		// the output used to be read before the reader thread had taken it, so a call could return an empty result
		for (int i = 0; i < 20; i++) {

			assertEquals("run " + i, printed(VALUES), exec(scripts.printArgs, parameters()));
		}
	}

	// ----- private methods -----
	private static Object exec(final Object... sources) throws Exception {

		return new ExecFunction().apply(context(), null, sources);
	}

	private static String execBinary(final String key, final List<Object> parameters) throws Exception {

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		new ExecBinaryFunction().apply(context(), null, new Object[] { out, key, parameters });

		return out.toString(StandardCharsets.UTF_8);
	}

	private static byte[] execBinaryBytes(final String key, final List<Object> parameters, final Object logBehaviour, final Object input) throws Exception {

		final ByteArrayOutputStream out = new ByteArrayOutputStream();

		new ExecBinaryFunction().apply(context(), null, new Object[] { out, key, parameters, logBehaviour, input });

		return out.toByteArray();
	}

	private static List<Object> parameters() {

		// a masked parameter is passed the same way
		final List<Object> parameters = new ArrayList<>(VALUES);
		parameters.set(2, Map.of("value", VALUES.get(2), "mask", true));

		return parameters;
	}

	private static ActionContext context() {

		return new ActionContext(SecurityContext.getSuperUserInstance());
	}
}
