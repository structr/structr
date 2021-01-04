/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.core.function;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import org.apache.commons.io.input.ReversedLinesFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

public class ServerLogFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_SERVERLOG = "Usage: ${serverlog([lines=50])}. Example: ${serverlog(200)}";
	public static final String ERROR_MESSAGE_SERVERLOG_JS = "Usage: ${{Structr.serverlog([n=50])}}. Example: ${{Structr.serverlog(200)}}";

	private static final Logger logger = LoggerFactory.getLogger(ServerLogFunction.class.getName());

	@Override
	public String getName() {
		return "serverlog";
	}

	@Override
	public String getSignature() {
		return "[ lines=50 ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		int lines = 50;

		if (sources != null && sources.length > 0 && sources[0] instanceof Number) {

			lines = ((Number)sources[0]).intValue();

		}

		return getServerLog(lines);
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_SERVERLOG_JS : ERROR_MESSAGE_SERVERLOG);
	}

	@Override
	public String shortDescription() {
		return "Returns the last n lines from the server log file";
	}

	public static String getServerLog(final int numberOfLines) {

		int lines = numberOfLines;

		final File logFile = getServerlogFile();

		if (logFile != null) {

			try (final ReversedLinesFileReader reader = new ReversedLinesFileReader(logFile, Charset.forName("utf-8"))) {

				final StringBuilder sb = new StringBuilder();

				while (lines > 0) {
					final String line = reader.readLine();

					if (line == null) {
						lines = 0;
					} else {
						sb.insert(0, line.concat("\n"));
						lines--;
					}

				}

				return sb.toString();

			} catch (IOException ex) {
				logger.warn("", ex);
			}
		}

		return "";
	}
}
