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
package org.structr.test.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.structr.common.trace.CallTrace;

import java.util.List;
import java.util.function.Supplier;

/**
 * Captures what the code under test logs, across all loggers, to assert how often a failure was
 * reported.
 */
public final class LogCapture {

	private LogCapture() {}

	/**
	 * Runs the given code and answers every warning or error that any logger wrote meanwhile and that
	 * mentions the given marker, in its message or in the message of a logged exception or its causes.
	 */
	public static List<String> mentions(final String marker, final Supplier<?> code) {
		return mentions(marker, 0, code);
	}

	/**
	 * As above, for code that fails on another thread: waits up to the given time for the first
	 * mention, and a little longer for any repetition of it.
	 */
	public static List<String> mentions(final String marker, final long awaitMillis, final Supplier<?> code) {

		final Logger root                          = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();

		appender.start();
		root.addAppender(appender);

		try {

			code.get();

			final long deadline = System.currentTimeMillis() + awaitMillis;

			while (awaitMillis > 0 && System.currentTimeMillis() < deadline && snapshot(appender).stream().noneMatch(e -> mentions(e, marker))) {
				sleep(100);
			}

			if (awaitMillis > 0) {
				sleep(500);
			}

		} finally {

			root.detachAppender(appender);
		}

		return snapshot(appender).stream()
			.filter(e -> e.getLevel().isGreaterOrEqual(Level.WARN))
			.filter(e -> mentions(e, marker))
			.map(ILoggingEvent::getFormattedMessage)
			.toList();
	}

	/**
	 * Runs the given code with the call trace's debug check switched on, and answers the frames it
	 * found starting a call flow without an entry frame: ways into Structr that no entry frame covers.
	 */
	public static List<String> flowsWithoutEntryFrame(final Supplier<?> code) {

		final Logger logger                        = (Logger) LoggerFactory.getLogger(CallTrace.class);
		final Level level                          = logger.getLevel();
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();

		appender.start();
		logger.addAppender(appender);
		logger.setLevel(Level.DEBUG);

		try {

			code.get();

		} finally {

			logger.setLevel(level);
			logger.detachAppender(appender);
		}

		return snapshot(appender).stream()
			.filter(e -> e.getLevel() == Level.DEBUG)
			.map(ILoggingEvent::getFormattedMessage)
			.filter(m -> m.contains("without an entry frame"))
			.toList();
	}

	// appenders append under their own lock, from whichever thread logs
	private static List<ILoggingEvent> snapshot(final ListAppender<ILoggingEvent> appender) {

		synchronized (appender) {
			return List.copyOf(appender.list);
		}
	}

	private static void sleep(final long millis) {

		try {

			Thread.sleep(millis);

		} catch (InterruptedException ignore) {
		}
	}

	private static boolean mentions(final ILoggingEvent event, final String marker) {

		if (String.valueOf(event.getFormattedMessage()).contains(marker)) {
			return true;
		}

		for (IThrowableProxy proxy = event.getThrowableProxy(); proxy != null; proxy = proxy.getCause()) {

			if (String.valueOf(proxy.getMessage()).contains(marker)) {
				return true;
			}
		}

		return false;
	}
}
