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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.util.AbstractBinaryProcess;
import org.structr.util.AbstractProcess;
import org.structr.util.StreamReader;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/** The process base classes return the complete output on both paths: an argument list, as the media processes use, and a shell command line. */
public class AbstractProcessOutputTest {

	private static final int LINES = 100_000;

	// how many lines of a failing process's error stream AbstractProcess logs
	private static final int LOGGED_ERROR_LINES = 50;

	// much more output than a pipe buffer on both streams, so the readers have to keep up while the process runs
	private static final String SCRIPT = "i=0; while [ $i -lt " + LINES + " ]; do echo \"out $i\"; echo \"err $i\" >&2; i=$((i+1)); done; exit 3";

	@Test
	public void testArgumentListReturnsCompleteOutput() {

		for (int run = 0; run < 5; run++) {

			assertComplete(new TextProcess(List.of("/bin/sh", "-c", SCRIPT), null).call());
		}
	}

	@Test
	public void testErrorStreamIsLoggedOnlyAtItsEnd() {

		final Logger logger                        = (Logger) LoggerFactory.getLogger(AbstractProcess.class.getName());
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();

		appender.start();
		logger.addAppender(appender);

		try {

			assertComplete(new TextProcess(List.of("/bin/sh", "-c", SCRIPT), null).call());

		} finally {

			logger.detachAppender(appender);
		}

		final String logged = appender.list.stream()
			.map(ILoggingEvent::getFormattedMessage)
			.filter(message -> message.contains("exited with exit code 3"))
			.findFirst()
			.orElse("");

		assertTrue(logged, logged.contains("(" + (LINES - LOGGED_ERROR_LINES) + " earlier lines omitted)\n"));
		assertTrue(logged, logged.contains("err " + (LINES - 1) + "\n"));
		assertTrue(logged, logged.length() < StreamReader.LOGGED_CHARS);
	}

	@Test
	public void testTailIsBoundedInCharacters() {

		final String line = "x".repeat(999);

		// eight lines of 1000 characters with their line breaks fit into the limit, nine do not
		final String tail = tailOf((line + "\n").repeat(100), LOGGED_ERROR_LINES);

		assertTrue(tail, tail.startsWith("(92 earlier lines omitted)\n"));
		assertEquals(9, tail.split("\n").length);

		// a last line longer than the limit is kept, cut to its end
		final String longLine = "a".repeat(StreamReader.LOGGED_CHARS) + "end";
		final String cut      = tailOf("first\n" + longLine + "\n", LOGGED_ERROR_LINES);

		assertEquals("(1 earlier lines omitted)\n" + longLine.substring(3) + "\n", cut);
	}

	@Test
	public void testProcessThatCannotStartDoesNotFail() {

		final AbstractProcess<Integer> process = new AbstractProcess<>(SecurityContext.getSuperUserInstance()) {

			@Override
			public List<String> getCommandArguments() {

				return List.of("/nonexistent/structr-test-command");
			}

			@Override
			public StringBuilder getCommandLine() {

				return null;
			}

			@Override
			public Integer processExited(final int exitCode) {

				return exitCode;
			}

			@Override
			public void preprocess() {
			}
		};

		process.setLogBehaviour(0);

		assertEquals(Integer.valueOf(-1), process.call());
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
	private static String tailOf(final String text, final int lines) {

		final StreamReader reader = new StreamReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));

		reader.run();

		return reader.getTail(lines);
	}

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
