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
package org.structr.rest.service;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.NanoTime;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * The formats of the request log. The default is the one the Screaming Frog Log File Analyser reads
 * with the most detail: Apache combined plus the time taken to serve the request.
 */
public enum RequestLogFormat {

	// LogFormat "%h %l %u %t \"%r\" %>s %b %D \"%{Referer}i\" \"%{User-Agent}i\""
	ULTIMATE("ultimate"),

	// LogFormat "%h %l %u %t \"%r\" %>s %b \"%{Referer}i\" \"%{User-Agent}i\""
	APACHE("apache"),

	// time, request line, status and time taken in milliseconds, as written by earlier versions
	LEGACY("legacy");

	private static final Logger logger = LoggerFactory.getLogger(RequestLogFormat.class);

	private final String name;

	RequestLogFormat(final String name) {

		this.name = name;
	}

	public String getName() {

		return name;
	}

	/** A request log in this format, writing through the given writer. */
	public RequestLog createRequestLog(final RequestLog.Writer writer) {

		return switch (this) {

			case ULTIMATE -> new ApacheRequestLog(writer, true);
			case APACHE   -> new ApacheRequestLog(writer, false);

			// the legacy lines started with the date logback put in front, which the request log pattern no longer adds
			case LEGACY   -> new CustomRequestLog(new LegacyDatePrefix(writer), "%t \"%r\" %s %{ms}T");
		};
	}

	/** The format with the given name, or the default with a warning for an unknown one. */
	public static RequestLogFormat of(final String name) {

		for (final RequestLogFormat format : values()) {

			if (format.name.equalsIgnoreCase(name)) {

				return format;
			}
		}

		logger.warn("Unknown request log format '{}', using '{}'", name, ULTIMATE.name);

		return ULTIMATE;
	}

	/**
	 * Apache combined, written the way Apache writes it: "-" for a missing value, and quotes, backslashes
	 * and control characters escaped, so that a header value cannot break the line for a log analyser.
	 */
	private static class ApacheRequestLog extends ContainerLifeCycle implements RequestLog {

		private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.US).withZone(ZoneOffset.UTC);

		private final RequestLog.Writer writer;
		private final boolean timeTaken;

		ApacheRequestLog(final RequestLog.Writer writer, final boolean timeTaken) {

			this.writer    = writer;
			this.timeTaken = timeTaken;

			// started and stopped with the server, like CustomRequestLog does: Jetty's slf4j writer has no logger before
			addBean(writer);
		}

		@Override
		public void log(final Request request, final Response response) {

			final long bytes        = Response.getContentBytesWritten(response);
			final StringBuilder buf = new StringBuilder(256);

			buf.append(Request.getRemoteAddr(request));
			buf.append(" - - [");
			buf.append(TIME.format(Instant.ofEpochMilli(Request.getTimeStamp(request))));
			buf.append("] \"");
			escape(buf, request.getMethod() + " " + request.getHttpURI().getPathQuery() + " " + request.getConnectionMetaData().getProtocol());
			buf.append("\" ");
			buf.append(response.getStatus());
			buf.append(' ');
			buf.append(bytes > 0 ? Long.toString(bytes) : "-");

			if (timeTaken) {

				buf.append(' ');
				buf.append(TimeUnit.NANOSECONDS.toMicros(NanoTime.since(request.getBeginNanoTime())));
			}

			buf.append(" \"");
			escape(buf, request.getHeaders().get(HttpHeader.REFERER));
			buf.append("\" \"");
			escape(buf, request.getHeaders().get(HttpHeader.USER_AGENT));
			buf.append('"');

			try {

				writer.write(buf.toString());

			} catch (IOException ex) {

				logger.warn("Unable to write request log: {}", ex.getMessage());
			}
		}

		private static void escape(final StringBuilder buf, final String value) {

			if (value == null) {

				buf.append('-');
				return;
			}

			for (int i = 0; i < value.length(); i++) {

				final char c = value.charAt(i);

				switch (c) {

					case '"'  -> buf.append("\\\"");
					case '\\' -> buf.append("\\\\");
					case '\n' -> buf.append("\\n");
					case '\r' -> buf.append("\\r");
					case '\t' -> buf.append("\\t");
					default   -> {

						if (c < 0x20 || c == 0x7f) {

							buf.append(String.format("\\x%02x", (int) c));

						} else {

							buf.append(c);
						}
					}
				}
			}
		}
	}

	private static class LegacyDatePrefix extends ContainerLifeCycle implements RequestLog.Writer {

		private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");

		private final RequestLog.Writer writer;

		LegacyDatePrefix(final RequestLog.Writer writer) {

			this.writer = writer;

			addBean(writer);
		}

		@Override
		public void write(final String line) throws IOException {

			writer.write(DATE.format(ZonedDateTime.now()) + line);
		}
	}
}
