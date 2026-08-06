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
package org.structr.core.function;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.ConceptType;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ServerLogFunction extends AdvancedScriptingFunction {

	private static final Logger logger = LoggerFactory.getLogger(ServerLogFunction.class.getName());

	@Override
	public String getName() {

		return "serverlog";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("[ lines = 50 [, truncateLinesAfter = -1 [, logFile = '/var/log/structr.log' (default different based on configuration) [, filter ] ] ] ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		int lines              = 50;
		int truncateLinesAfter = -1;
		String logFileName     = null;
		String filter          = null;

		if (sources != null && sources.length > 0 && sources[0] instanceof Number) {

			lines = ((Number)sources[0]).intValue();
		}

		if (sources != null && sources.length > 1 && sources[1] instanceof Number) {

			truncateLinesAfter = ((Number)sources[1]).intValue();
		}

		if (sources != null && sources.length > 2 && sources[2] instanceof String) {

			logFileName = (String) sources[2];
		}

		if (sources != null && sources.length > 3 && sources[3] instanceof String) {

			filter = (String) sources[3];
		}

		return getServerLog(lines, truncateLinesAfter, logFileName, filter);
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.structrScript("Usage: ${serverlog([lines = 50 [, truncateLinesAfter = -1 [, logFile = '/var/log/structr.log' [, filter ]]]])}. Example: ${serverlog(200, -1, '/var/log/structr.log')}"),
			Usage.javaScript("Usage: ${{ $.serverlog([lines = 50 [, truncateLinesAfter = -1 [, logFile = '/var/log/structr.log' [, filter ]]]]); }}. Example: ${{ $.serverlog(200, -1, '/var/log/structr.log'); }}")
		);
	}

	@Override
	public String getShortDescription() {

		return "Returns the last n lines.";
	}

	@Override
	public String getLongDescription() {

		return "The last n lines are taken from the selected log file and each line can be truncated to a certain length. Optionally, a filter can be applied to only return lines that contain a certain string (case-sensitive).";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.optional("lines", "number of lines to return"),
				Parameter.optional("truncateLinesAfter", "number of characters after which each log line is truncated with \"[...]\""),
				Parameter.optional("logFile", "log file to read from"),
				Parameter.optional("filter", "filter to apply to lines (case-sensitive)")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of("The `getAvailableServerlogs()` function can be used for the `logFile` parameter");
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.System;
	}

	@Override
	public List<Link> getLinkedConcepts() {

		final List<Link> linkedConcepts = super.getLinkedConcepts();

		linkedConcepts.add(Link.to("ispartof", ConceptReference.of(ConceptType.Topic, "Logging")));

		return linkedConcepts;
	}

	public static String getServerLog(final int numberOfLines, final Integer truncateLinesAfter, final String requestedLogfileName, final String filter) {

		final boolean filterEmpty = StringUtils.isBlank(filter);
		int linesToGet = numberOfLines;
		final List<String> logFileNames = GetAvailableServerLogsFunction.getListOfServerlogFileNames();

		final File logFile;

		if (requestedLogfileName != null && logFileNames.contains(requestedLogfileName)) {

			logFile = new File(requestedLogfileName);

		} else if (!logFileNames.isEmpty()) {

			logFile = new File(logFileNames.getFirst());

		} else {

			logFile = null;
		}

		if (logFile != null) {

			try (final ReversedLinesFileReader reader = ReversedLinesFileReader.builder().setPath(logFile.toPath()).setBufferSize(4096).setCharset(StandardCharsets.UTF_8).get()) {

				final StringBuilder sb = new StringBuilder();

				while (linesToGet > 0) {

					String line = reader.readLine();
					if (line == null) {

						linesToGet = 0;

					} else {

						if (truncateLinesAfter > 0 && line.length() > truncateLinesAfter) {

							line = line.substring(0, truncateLinesAfter).concat("[...]");
						}

						if (filterEmpty || line.contains(filter)) {

							sb.insert(0, line.concat("\n"));

							linesToGet--;
						}
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
