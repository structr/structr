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
package org.structr.test.logging;

import org.structr.api.config.Settings;
import org.structr.core.function.GetAvailableServerLogsFunction;
import org.structr.core.function.ServerLogFunction;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.testng.AssertJUnit.assertEquals;

/** The log files the dashboard offers: the server log and the request log, each with its rotated logs. */
public class AvailableLogFilesTest {

	private String basePath = null;
	private Path directory  = null;
	private Path logs       = null;

	@BeforeMethod
	public void createLogs() throws Exception {

		// the environment variables take precedence over the base path and cannot be set from here
		if (System.getenv("LOG_FILE") != null || System.getenv("REQUEST_LOG_FILE") != null) {

			throw new SkipException("LOG_FILE or REQUEST_LOG_FILE is set");
		}

		basePath  = Settings.BasePath.getValue();
		directory = Files.createTempDirectory("structr-available-logs");
		logs      = Files.createDirectory(directory.resolve("logs"));

		Settings.BasePath.setValue(directory.toString());
	}

	@AfterMethod
	public void cleanUp() throws Exception {

		if (basePath != null) {

			Settings.BasePath.setValue(basePath);
		}

		try (final Stream<Path> paths = Files.walk(directory)) {

			paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
		}
	}

	@Test
	public void testServerLogFirstThenRequestLogEachNewestFirst() throws Exception {

		write("server.log", "server.log.2026-11-01", "server.log.2026-11-02", "server.log.2026-10-31.gz", "request.log", "request.log.2026-11-01", "request.log.2026-11-02", "request.log.2026-10-31.gz", "http-access-statistics.json");

		assertEquals(List.of(
			path("server.log"), path("server.log.2026-11-02"), path("server.log.2026-11-01"),
			path("request.log"), path("request.log.2026-11-02"), path("request.log.2026-11-01")
		), GetAvailableServerLogsFunction.getListOfLogFileNames());
	}

	@Test
	public void testWithoutARequestLogOnlyTheServerLogIsListed() throws Exception {

		write("server.log", "server.log.2026-11-01");

		assertEquals(List.of(path("server.log"), path("server.log.2026-11-01")), GetAvailableServerLogsFunction.getListOfLogFileNames());
	}

	@Test
	public void testTheDashboardCanReadTheRequestLog() throws Exception {

		write("server.log", "request.log");

		final List<String> dashboard = GetAvailableServerLogsFunction.getListOfLogFileNames();

		assertEquals("request.log\n", ServerLogFunction.getLog(10, -1, path("request.log"), null, dashboard));
		assertEquals("server.log\n", ServerLogFunction.getLog(10, -1, null, null, dashboard));
	}

	@Test
	public void testServerLogNeverReadsTheRequestLog() throws Exception {

		write("server.log", "request.log");

		assertEquals("a request log passed as logFile is not read", "server.log\n", ServerLogFunction.getServerLog(10, -1, path("request.log"), null));
	}

	@Test
	public void testWithoutAServerLogTheRequestLogIsNotTheDefault() throws Exception {

		// logging to stdout only, as in some containers
		write("request.log");

		assertEquals("", ServerLogFunction.getServerLog(10, -1, null, null));
		assertEquals("", ServerLogFunction.getServerLog(10, -1, path("request.log"), null));
	}

	@Test
	public void testAFileNotListedIsNotRead() throws Exception {

		write("server.log", "other.log");

		assertEquals("unknown names fall back to the current server log", "server.log\n", ServerLogFunction.getServerLog(10, -1, path("other.log"), null));
	}

	// ----- private methods -----
	private void write(final String... names) throws Exception {

		for (final String name : names) {

			Files.writeString(logs.resolve(name), name + "\n", StandardCharsets.UTF_8);
		}
	}

	private String path(final String name) {

		return logs.resolve(name).toString();
	}
}
