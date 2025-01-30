/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GetAvailableServerLogsFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_GET_AVAILABLE_SERVERLOGS    = "Usage: ${get_available_serverlogs()}. Example: ${get_available_serverlogs()}";
	public static final String ERROR_MESSAGE_GET_AVAILABLE_SERVERLOGS_JS = "Usage: ${{ $.get_available_serverlogs(); }}. Example: ${{ $.get_available_serverlogs(); }}";

	private static final Logger logger = LoggerFactory.getLogger(ServerLogFunction.class.getName());

	@Override
	public String getName() {
		return "get_available_serverlogs";
	}

	@Override
	public String getSignature() {
		return "";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		return getListOfServerlogFileNames();
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_GET_AVAILABLE_SERVERLOGS_JS : ERROR_MESSAGE_GET_AVAILABLE_SERVERLOGS);
	}

	@Override
	public String shortDescription() {
		return "Returns the last n lines from the server log file";
	}

	public static List<String> getListOfServerlogFileNames() {

		final String basePath = Settings.getBasePath();

		if (!basePath.isEmpty()) {

			File logFile;

			// highest priority: log file name from env
			final String envLogFile = System.getenv("LOG_FILE");
			if (envLogFile != null) {

				logFile = new File(envLogFile);
				if (logFile.exists()) {

					if (envLogFile.equals("/var/log/structr.log")) {

						return getLogFilesInVarLog();

					} else {

						return List.of(envLogFile);
					}
				}
			}

			// second priority: default log file for local installations (logs/server.log)
			final String logPath = basePath.endsWith(File.separator) ? basePath.concat("logs" + File.separator) : basePath.concat(File.separator + "logs" + File.separator);
			logFile = new File(logPath.concat("server.log"));
			if (logFile.exists()) {

				return List.of(logPath.concat("server.log"));
			}

			// third priority: deb installation / log files in /var/log/ (although ENV can also use this path)
			logFile = new File("/var/log/structr.log");
			if (logFile.exists()) {

				return getLogFilesInVarLog();
			}

			logger.warn("Could not locate logfile(s)");

		} else {

			logger.warn("Unable to determine base.path from structr.conf, no data input/output possible.");
		}

		return null;
	}

	private static List<String> getLogFilesInVarLog() {

		// return all files starting with "structr.log" in /var/log
		try (Stream<Path> paths = Files.walk(Paths.get("/var/log/"))) {

			return paths
					.filter(Files::isRegularFile)
					.filter(path -> {
						final String name = path.toFile().getName();
						return name.startsWith("structr.log") && !name.endsWith(".gz");
					})
					.map(Path::toString)
					.sorted(Comparator.naturalOrder())
					.collect(Collectors.toList());

		} catch (IOException e) {

			logger.warn("Unable to enumerate files in /var/log/");
		}

		return null;
	}
}