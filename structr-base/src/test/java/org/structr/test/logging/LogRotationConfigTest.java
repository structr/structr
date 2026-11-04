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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/** The shipped logback.xml: Structr rotates the server and request logs, or leaves them to an outside rotator like logrotate. */
public class LogRotationConfigTest {

	private static final String REQUEST_LOGGER = "org.eclipse.jetty.server.RequestLog";

	private LoggerContext context = null;
	private Path directory        = null;

	@BeforeMethod
	public void createDirectory() throws Exception {

		directory = Files.createTempDirectory("structr-log-rotation");
	}

	@AfterMethod
	public void cleanUp() throws Exception {

		if (context != null) {

			context.stop();
			context = null;
		}

		try (final Stream<Path> paths = Files.walk(directory)) {

			paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
		}
	}

	@Test
	public void testStructrRotatesByDefault() throws Exception {

		configure(null);

		assertRotated(serverLog(), "server.log");
		assertRotated(requestLog(), "request.log");
	}

	@Test
	public void testStructrRotatesWhenSelectedExplicitly() throws Exception {

		configure("structr");

		assertRotated(serverLog(), "server.log");
		assertRotated(requestLog(), "request.log");
	}

	@Test
	public void testExternalRotationWritesPlainFiles() throws Exception {

		configure("external");

		assertPlain(serverLog(), "server.log");
		assertPlain(requestLog(), "request.log");
	}

	@Test
	public void testTheRequestLogContainsOnlyTheMessage() throws Exception {

		for (final String mode : new String[] { "structr", "external" }) {

			configure(mode);

			context.getLogger(REQUEST_LOGGER).info("66.249.66.1 - - [03/Nov/2026:11:00:56 +0000] \"GET / HTTP/1.1\" 200 5");
			assertNoErrors();
			context.stop();
			context = null;

			assertEquals(mode, "66.249.66.1 - - [03/Nov/2026:11:00:56 +0000] \"GET / HTTP/1.1\" 200 5\n", read("request.log"));

			Files.delete(directory.resolve("request.log"));
		}
	}

	@Test
	public void testExternalRotationContinuesCleanlyAfterCopyTruncate() throws Exception {

		configure("external");

		final Logger logger = context.getLogger(REQUEST_LOGGER);
		final Path log      = directory.resolve("request.log");

		logger.info("before");

		// what logrotate's copytruncate does while Structr keeps the file open
		Files.copy(log, directory.resolve("request.log.1"));

		try (final RandomAccessFile file = new RandomAccessFile(log.toFile(), "rw")) {

			file.setLength(0);
		}

		logger.info("after");
		assertNoErrors();

		assertEquals("before\n", read("request.log.1"));
		assertEquals("the next line starts at the beginning, without a gap of zero bytes", "after\n", read("request.log"));
	}

	@Test
	public void testAnUnknownModeLogsToTheConsoleOnly() throws Exception {

		configure("unknown");

		assertNull(serverLog());
		assertNull(requestLog());
		assertFalse(Files.exists(directory.resolve("server.log")));
	}

	// ----- private methods -----
	private void configure(final String rotation) throws Exception {

		context = new LoggerContext();

		// SLF4J sets the MDC adapter on the context it creates, without one every append fails
		context.setMDCAdapter(new LogbackMDCAdapter());

		context.putProperty("LOG_FILE", directory.resolve("server.log").toString());
		context.putProperty("REQUEST_LOG_FILE", directory.resolve("request.log").toString());

		if (rotation != null) {

			context.putProperty("LOG_ROTATION", rotation);
		}

		final JoranConfigurator configurator = new JoranConfigurator();

		configurator.setContext(context);
		configurator.doConfigure(LogRotationConfigTest.class.getResource("/logback.xml"));
		context.start();

		if (rotation == null || "structr".equals(rotation) || "external".equals(rotation)) {

			assertNoErrors();
		}
	}

	private void assertNoErrors() {

		assertTrue("logback reported errors: " + context.getStatusManager().getCopyOfStatusList(), new StatusUtil(context).getHighestLevel(0) < Status.ERROR);
	}

	private Appender<?> serverLog() {

		return context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("SERVERLOG");
	}

	private Appender<?> requestLog() {

		return context.getLogger(REQUEST_LOGGER).getAppender("REQUESTLOG");
	}

	private void assertRotated(final Appender<?> appender, final String fileName) {

		assertTrue(fileName + " is rotated by Structr", appender instanceof RollingFileAppender);

		final RollingFileAppender<?> rolling = (RollingFileAppender<?>) appender;
		final TimeBasedRollingPolicy<?> policy = (TimeBasedRollingPolicy<?>) rolling.getRollingPolicy();

		assertEquals(directory.resolve(fileName).toString(), rolling.getFile());
		assertEquals("daily and compressed", directory.resolve(fileName) + ".%d.gz", policy.getFileNamePattern());
		assertEquals(30, policy.getMaxHistory());
	}

	private void assertPlain(final Appender<?> appender, final String fileName) {

		assertTrue(fileName + " is written", appender instanceof FileAppender);
		assertFalse(fileName + " is not rotated by Structr", appender instanceof RollingFileAppender);
		assertEquals(directory.resolve(fileName).toString(), ((FileAppender<?>) appender).getFile());
	}

	private String read(final String fileName) throws Exception {

		return Files.readString(directory.resolve(fileName), StandardCharsets.UTF_8);
	}
}
