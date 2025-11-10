/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

public class ServerLogFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_SERVERLOG = "Usage: ${serverlog([lines = 50 [, truncateLinesAfter = -1 [, logFile = '/var/log/structr.log' ]]])}. Example: ${serverlog(200, -1, '/var/log/structr.log')}";
	public static final String ERROR_MESSAGE_SERVERLOG_JS = "Usage: ${{ $.serverlog([lines = 50 [, truncateLinesAfter = -1 [, logFile = '/var/log/structr.log' ]]]); }}. Example: ${{ $.serverlog(200, -1, '/var/log/structr.log'); }}";

	private static final Logger logger = LoggerFactory.getLogger(ServerLogFunction.class.getName());

	@Override
	public String getName() {
		return "serverlog";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("[ lines = 50 [, truncateLinesAfter = -1 [, logFile = '/var/log/structr.log' (default different based on configuration) ] ] ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		int lines              = 50;
		int truncateLinesAfter = -1;
		String logFileName     = null;

		if (sources != null && sources.length > 0 && sources[0] instanceof Number) {

			lines = ((Number)sources[0]).intValue();
		}

		if (sources != null && sources.length > 1 && sources[1] instanceof Number) {

			truncateLinesAfter = ((Number)sources[1]).intValue();
		}

		if (sources != null && sources.length > 2 && sources[2] instanceof String) {

			logFileName = (String) sources[2];
		}

		return getServerLog(lines, truncateLinesAfter, logFileName);
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_SERVERLOG_JS : ERROR_MESSAGE_SERVERLOG);
	}

	@Override
	public String getShortDescription() {
		return "Returns the last n lines from the server log file";
	}

	public static String getServerLog(final int numberOfLines, final Integer truncateLinesAfter, final String requestedLogfileName) {

		int lines = numberOfLines;

		List<String> logFileNames = GetAvailableServerLogsFunction.getListOfServerlogFileNames();

		final File logFile;
		if (requestedLogfileName != null && logFileNames.contains(requestedLogfileName)) {

			logFile = new File(requestedLogfileName);

		} else if (logFileNames.size() > 0) {

			logFile = new File(logFileNames.get(0));

		} else {

			logFile = null;
		}

		if (logFile != null) {

			try (final ReversedLinesFileReader reader = new ReversedLinesFileReader(logFile, Charset.forName("utf-8"))) {

				final StringBuilder sb = new StringBuilder();

				while (lines > 0) {

					String line = reader.readLine();

					if (line == null) {

						lines = 0;

					} else {

						if (truncateLinesAfter > 0 && line.length() > truncateLinesAfter) {

							line = line.substring(0, truncateLinesAfter).concat("[...]");
						}

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
