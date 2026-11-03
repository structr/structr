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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.ConceptType;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

public class GetAvailableServerLogsFunction extends AdvancedScriptingFunction {

	private static final Logger logger = LoggerFactory.getLogger(ServerLogFunction.class.getName());

	@Override
	public String getName() {

		return "getAvailableServerlogs";
	}

	@Override
	public List<Signature> getSignatures() {

		// empty signature, no parameters

		return Signature.forAllScriptingLanguages("");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		return getListOfServerlogFileNames();
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.structrScript("Usage: ${getAvailableServerlogs()}. Example: ${getAvailableServerlogs()}"),
			Usage.javaScript("Usage: ${{ $.getAvailableServerlogs(); }}. Example: ${{ $.getAvailableServerlogs(); }}")
		);
	}

	@Override
	public String getShortDescription() {

		return "Returns a collection of available server logs files.";
	}

	@Override
	public String getLongDescription() {

		return "Returns the current server log file and its rotated logs, sorted by name. The dashboard also lists the request log and its rotated logs.";
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

	public static List<String> getListOfServerlogFileNames() {

		return getListOfServerlogFileNames(false);
	}

	/**
	 * The server log and the request log, each the current file first and its rotated logs newest first,
	 * as the dashboard shows them.
	 */
	public static List<String> getListOfLogFileNames() {

		final List<String> logFiles = new ArrayList<>(newestFirst(getListOfServerlogFileNames(false)));

		logFiles.addAll(newestFirst(getListOfRequestLogFileNames(false)));

		return logFiles;
	}

	/**
	 * @param includeCompressed whether to list rotated logs that were compressed with gzip, which only
	 *                          a reader that decompresses them can use
	 */
	public static List<String> getListOfRequestLogFileNames(final boolean includeCompressed) {

		final String envLogFile = System.getenv("REQUEST_LOG_FILE");
		final String basePath   = Settings.getBasePath();
		final File logFile;

		if (envLogFile != null) {

			logFile = new File(envLogFile);

		} else {

			// logback.xml writes logs/request.log by default
			logFile = new File(basePath.isEmpty() ? "logs" : basePath + File.separator + "logs", "request.log");
		}

		if (logFile.exists()) {

			// relative like the server log paths, so both read the same in the dashboard
			final String parent = logFile.getParent() != null ? logFile.getParent() : ".";

			return getLogFilesInDirectory(parent + File.separator, logFile.getName(), includeCompressed);
		}

		return List.of();
	}

	/**
	 * @param logFiles a log as listed here, the current file first and its rotated logs following, oldest first
	 */
	public static List<String> newestFirst(final List<String> logFiles) {

		if (logFiles.size() < 2) {

			return logFiles;
		}

		final List<String> sorted = new ArrayList<>(logFiles.subList(1, logFiles.size()));

		Collections.reverse(sorted);
		sorted.addFirst(logFiles.getFirst());

		return sorted;
	}

	/**
	 * @param includeCompressed whether to list rotated logs that were compressed with gzip, which only
	 *                          a reader that decompresses them can use
	 */
	public static List<String> getListOfServerlogFileNames(final boolean includeCompressed) {

		final String basePath = Settings.getBasePath();
		if (!basePath.isEmpty()) {

			File logFile;

			// highest priority: log file name from env
			final String envLogFile = System.getenv("LOG_FILE");
			if (envLogFile != null) {

				logFile = new File(envLogFile);

				if (logFile.exists()) {

					String parentDir = logFile.getParent() + File.separator;
					List<String> logsInDir = getLogFilesInDirectory(parentDir, logFile.getName(), includeCompressed);

					if (logsInDir.size() > 1) {

						return logsInDir;
					}
				}
				
				return List.of(envLogFile);
			}

			// second priority: default log file for local installations (logs/server.log)
			final String logPath = basePath.endsWith(File.separator) ? basePath.concat("logs" + File.separator) : basePath.concat(File.separator + "logs" + File.separator);
			logFile = new File(logPath.concat("server.log"));

			if (logFile.exists()) {

				// the rolling policy in logback.xml keeps the rotated logs next to it

				return getLogFilesInDirectory(logPath, logFile.getName(), includeCompressed);
			}

			// third priority: deb installation / log files in /var/log/structr/ (although ENV can also use this path)
			logFile = new File("/var/log/structr/server.log");

			if (logFile.exists()) {

				return getLogFilesInDirectory("/var/log/structr/", logFile.getName(), includeCompressed);
			}

			logger.warn("Could not locate logfile(s)");

		} else {

			logger.warn("Unable to determine base.path from structr.conf, no data input/output possible.");
		}

		return List.of();
	}

	private static List<String> getLogFilesInDirectory(final String logDirectory, final String logFileName, final boolean includeCompressed) {

		// return all files starting with the name of the log file in logDirectory, gzipped ones only on request
		final List<String> logFiles = new ArrayList<>();

		try {

			Files.walkFileTree(Paths.get(logDirectory), EnumSet.noneOf(FileVisitOption.class), 1, new SimpleFileVisitor<>() {

				private boolean isFileOfInterest(final Path file) {

					final String name = file.getFileName().toString();

					return name.startsWith(logFileName) && (includeCompressed || !name.endsWith(".gz"));
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

					if (attrs.isRegularFile() && isFileOfInterest(file)) {

						logFiles.add(file.toString());
					}

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) {

					if (isFileOfInterest(file)) {

						logger.debug("Skipping {}: {}", file, exc.toString());
					}

					return FileVisitResult.CONTINUE;
				}
			});

		} catch (IOException e) {

			logger.warn("Unable to enumerate files in {}",  logDirectory);
		}

		Collections.sort(logFiles);

		return logFiles;
	}
}