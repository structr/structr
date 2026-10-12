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
package org.structr.test.common.helper;

import org.structr.common.helper.ServerLogReader;
import org.structr.common.helper.ServerLogReader.Level;
import org.structr.common.helper.ServerLogReader.Result;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

public class ServerLogReaderTest {

	private static final String INFO_1  = "2026-10-10 12:00:00.001 [main] INFO  org.structr.core.Services - Starting services";
	private static final String WARN_1  = "2026-10-10 12:00:00.002 [HttpService-12] WARN  org.structr.rest.Servlet - Slow request";
	private static final String ERROR_1 = "2026-10-10 12:00:00.003 [HttpService-13] ERROR org.structr.core.Script - Script failed: ERRÖR in Größe";
	private static final String TRACE_1 = "java.lang.IllegalStateException: broken";
	private static final String TRACE_2 = "\tat org.structr.core.Script.run(Script.java:42)";
	private static final String INFO_2  = "2026-10-10 12:00:00.004 [main] INFO  org.structr.core.Services - Mentions an ERROR in the message";
	private static final String ERROR_2 = "2026-10-10 12:00:00.005 [main] ERROR org.structr.core.Services - Second error";

	@Test
	public void testTailReturnsWholeEntriesAndTheEndOffset() throws IOException {

		final Path file = log(INFO_1, WARN_1, ERROR_1, TRACE_1, TRACE_2, INFO_2, ERROR_2);

		assertEquals(List.of(INFO_2, ERROR_2), ServerLogReader.tail(file, 2, null, null).lines());

		// three lines reach into the error, which comes back with its whole trace
		final Result result = ServerLogReader.tail(file, 3, null, null);

		assertEquals(List.of(ERROR_1, TRACE_1, TRACE_2, INFO_2, ERROR_2), result.lines());
		assertEquals(Files.size(file), result.offset());
		assertFalse(result.rotated());
	}

	@Test
	public void testLevelIsAThresholdReadFromTheLevelColumn() throws IOException {

		final Path file = log(INFO_1, WARN_1, ERROR_1, TRACE_1, TRACE_2, INFO_2, ERROR_2);

		// the INFO line that mentions ERROR in its message is not an error
		assertEquals(List.of(ERROR_1, TRACE_1, TRACE_2, ERROR_2), ServerLogReader.tail(file, 100, Level.ERROR, null).lines());
		assertEquals(List.of(WARN_1, ERROR_1, TRACE_1, TRACE_2, ERROR_2), ServerLogReader.tail(file, 100, Level.WARN, null).lines());
		assertEquals(7, ServerLogReader.tail(file, 100, Level.TRACE, null).lines().size());
	}

	@Test
	public void testFilterMatchesTheWholeEntry() throws IOException {

		final Path file = log(INFO_1, WARN_1, ERROR_1, TRACE_1, TRACE_2, INFO_2, ERROR_2);

		// the text is only in the trace, the entry comes back from its first line
		assertEquals(List.of(ERROR_1, TRACE_1, TRACE_2), ServerLogReader.tail(file, 100, null, "Script.java").lines());
		assertEquals(List.of(ERROR_1, TRACE_1, TRACE_2), ServerLogReader.tail(file, 100, Level.ERROR, "Größe").lines());
		assertEquals(List.of(), ServerLogReader.tail(file, 100, Level.ERROR, "Slow request").lines());
	}

	@Test
	public void testSinceReturnsOnlyWhatCameAfterTheOffset() throws IOException {

		final Path file    = log(INFO_1, WARN_1);
		final Result first = ServerLogReader.tail(file, 50, null, null);

		append(file, ERROR_1, TRACE_1, TRACE_2, INFO_2);

		final Result second = ServerLogReader.since(file, first.offset(), 50, null, null);

		assertEquals(List.of(ERROR_1, TRACE_1, TRACE_2, INFO_2), second.lines());
		assertEquals(Files.size(file), second.offset());

		// nothing new
		assertEquals(List.of(), ServerLogReader.since(file, second.offset(), 50, null, null).lines());
	}

	@Test
	public void testSinceLeavesALineThatIsStillBeingWritten() throws IOException {

		final Path file = log(INFO_1);
		final long size = Files.size(file);

		Files.writeString(file, ERROR_2.substring(0, 20), StandardCharsets.UTF_8, StandardOpenOption.APPEND);

		final Result partial = ServerLogReader.since(file, size, 50, null, null);

		assertEquals(List.of(), partial.lines());
		assertEquals(size, partial.offset());

		Files.writeString(file, ERROR_2.substring(20) + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);

		assertEquals(List.of(ERROR_2), ServerLogReader.since(file, partial.offset(), 50, null, null).lines());
	}

	@Test
	public void testSinceContinuesWhereItStopped() throws IOException {

		final Path file    = log(INFO_1, WARN_1, ERROR_1, TRACE_1, TRACE_2, INFO_2, ERROR_2);
		final List<String> all = new ArrayList<>();
		long offset        = 0;

		// two lines at a time, oldest first, nothing lost or repeated
		for (int i = 0; i < 10; i++) {

			final Result result = ServerLogReader.since(file, offset, 2, null, null);

			all.addAll(result.lines());
			offset = result.offset();
		}

		assertEquals(List.of(INFO_1, WARN_1, ERROR_1, TRACE_1, TRACE_2, INFO_2, ERROR_2), all);

		// with a level, the entries that do not match are passed over and the offset still moves on
		final Result errors = ServerLogReader.since(file, 0, 1, Level.ERROR, null);

		assertEquals(List.of(ERROR_1, TRACE_1, TRACE_2), errors.lines());
		assertEquals(List.of(ERROR_2), ServerLogReader.since(file, errors.offset(), 1, Level.ERROR, null).lines());
	}

	@Test
	public void testSinceBeyondTheEndMeansTheLogWasRotated() throws IOException {

		final Path file     = log(INFO_1, WARN_1);
		final Result result = ServerLogReader.since(file, Files.size(file) + 1000, 50, null, null);

		assertTrue(result.rotated());
		assertEquals(List.of(INFO_1, WARN_1), result.lines());
	}

	@Test
	public void testLinesBeforeTheFirstTimestampAndLargeFiles() throws IOException {

		final List<String> lines = new ArrayList<>(List.of("continued from the previous file"));

		for (int i = 0; i < 2000; i++) {

			lines.add(String.format("2026-10-10 12:00:%02d.%03d [main] INFO  org.structr.Test - line %d with some padding to cross chunk borders", i / 1000, i % 1000, i));
		}

		final Path file = log(lines.toArray(new String[0]));

		assertEquals(lines, ServerLogReader.tail(file, 5000, null, null).lines());
		assertEquals(lines, ServerLogReader.since(file, 0, 5000, null, null).lines());
		assertEquals(List.of(), ServerLogReader.tail(file, 5000, Level.WARN, null).lines());
	}

	@Test
	public void testCompressedLogsAnswerLikePlainOnes() throws IOException {

		final String[] lines = { INFO_1, WARN_1, ERROR_1, TRACE_1, TRACE_2, INFO_2, ERROR_2 };
		final Path plain     = log(lines);
		final Path gzipped   = gzip(lines);

		assertTrue(ServerLogReader.isCompressed(gzipped));
		assertFalse(ServerLogReader.isCompressed(plain));

		for (final int max : new int[] { 1, 2, 3, 4, 100 }) {

			assertEquals(ServerLogReader.tail(plain, max, null, null).lines(), ServerLogReader.tailCompressed(gzipped, max, null, null).lines());
			assertEquals(ServerLogReader.tail(plain, max, Level.WARN, null).lines(), ServerLogReader.tailCompressed(gzipped, max, Level.WARN, null).lines());
			assertEquals(ServerLogReader.tail(plain, max, null, "Script.java").lines(), ServerLogReader.tailCompressed(gzipped, max, null, "Script.java").lines());
		}

		assertEquals(-1L, ServerLogReader.tailCompressed(gzipped, 50, null, null).offset());
	}

	@Test
	public void testParseLevel() {

		assertEquals(Level.WARN, ServerLogReader.parseLevel("warn"));
		assertEquals(null, ServerLogReader.parseLevel("CRITICAL"));
		assertEquals(null, ServerLogReader.parseLevel(""));
	}

	// ----- private methods -----
	private Path log(final String... lines) throws IOException {

		final Path file = Files.createTempFile("server", ".log");

		file.toFile().deleteOnExit();

		append(file, lines);

		return file;
	}

	private Path gzip(final String... lines) throws IOException {

		final Path file = Files.createTempFile("server", ".log.gz");

		file.toFile().deleteOnExit();

		try (final OutputStream out = new GZIPOutputStream(Files.newOutputStream(file))) {

			out.write((String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
		}

		return file;
	}

	private void append(final Path file, final String... lines) throws IOException {

		Files.writeString(file, String.join("\n", lines) + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
	}
}
