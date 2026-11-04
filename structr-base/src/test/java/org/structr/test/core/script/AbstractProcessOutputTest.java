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
import org.structr.util.AbstractBinaryProcess;
import org.structr.util.AbstractProcess;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

/** The process base classes return the complete output on both paths: an argument list, as the media processes use, and a shell command line. */
public class AbstractProcessOutputTest {

	private static final int LINES = 100_000;

	// much more output than a pipe buffer on both streams, so the readers have to keep up while the process runs
	private static final String SCRIPT = "i=0; while [ $i -lt " + LINES + " ]; do echo \"out $i\"; echo \"err $i\" >&2; i=$((i+1)); done; exit 3";

	@Test
	public void testArgumentListReturnsCompleteOutput() {

		for (int run = 0; run < 5; run++) {

			assertComplete(new TextProcess(List.of("/bin/sh", "-c", SCRIPT), null).call());
		}
	}

	@Test
	public void testCommandLineReturnsCompleteOutput() {

		for (int run = 0; run < 5; run++) {

			assertComplete(new TextProcess(null, SCRIPT).call());
		}
	}

	@Test
	public void testBinaryProcessCopiesCompleteOutput() {

		for (int run = 0; run < 5; run++) {

			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final Result result             = new BinaryProcess(List.of("/bin/sh", "-c", SCRIPT), out).call();

			assertEquals(3, result.exitCode);
			assertEquals(LINES, out.toString().split("\n").length);
			assertEquals(LINES, result.stderr.split("\n").length);
		}
	}

	// ----- private methods -----
	private static void assertComplete(final Result result) {

		final String[] out = result.stdout.split("\n");
		final String[] err = result.stderr.split("\n");

		assertEquals(3, result.exitCode);
		assertEquals(LINES, out.length);
		assertEquals(LINES, err.length);
		assertEquals("out " + (LINES - 1), out[LINES - 1]);
		assertEquals("err " + (LINES - 1), err[LINES - 1]);
	}

	private record Result(int exitCode, String stdout, String stderr) {}

	private static class TextProcess extends AbstractProcess<Result> {

		private final List<String> arguments;
		private final String commandLine;

		TextProcess(final List<String> arguments, final String commandLine) {

			super(SecurityContext.getSuperUserInstance());

			this.arguments   = arguments;
			this.commandLine = commandLine;

			setLogBehaviour(0);
		}

		@Override
		public List<String> getCommandArguments() {

			return arguments;
		}

		@Override
		public StringBuilder getCommandLine() {

			return commandLine != null ? new StringBuilder(commandLine) : null;
		}

		@Override
		public Result processExited(final int exitCode) {

			return new Result(exitCode, outputStream(), errorStream());
		}

		@Override
		public void preprocess() {
		}
	}

	private static class BinaryProcess extends AbstractBinaryProcess<Result> {

		private final List<String> arguments;

		BinaryProcess(final List<String> arguments, final ByteArrayOutputStream out) {

			super(SecurityContext.getSuperUserInstance(), out);

			this.arguments = arguments;

			setLogBehaviour(0);
		}

		@Override
		public List<String> getCommandArguments() {

			return arguments;
		}

		@Override
		public StringBuilder getCommandLine() {

			return null;
		}

		@Override
		public Result processExited(final int exitCode) {

			return new Result(exitCode, null, errorStream());
		}

		@Override
		public void preprocess() {
		}
	}
}
