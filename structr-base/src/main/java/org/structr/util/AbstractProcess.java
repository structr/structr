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
package org.structr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 *
 */
public abstract class AbstractProcess<T> implements Callable<T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractProcess.class.getName());

	private final AtomicBoolean running       = new AtomicBoolean(true);
	protected SecurityContext securityContext = null;
	private StreamReader stdOut               = null;
	private StreamReader stdErr               = null;
	private int exitCode                      = -1;

	private Settings.SCRIPT_PROCESS_LOG_STYLE logBehaviour = Settings.SCRIPT_PROCESS_LOG_STYLE.get(Settings.LogScriptProcessCommandLine.getValue());

	public AbstractProcess(final SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	public abstract StringBuilder getCommandLine();
	public abstract T processExited(final int exitCode);
	public abstract void preprocess();

	public StringBuilder getLogLine() {
		return getCommandLine();
	}

	private boolean shouldLogCommandWhenExecuting() {
		return (getLogBehaviour() != Settings.SCRIPT_PROCESS_LOG_STYLE.NOTHING);
	}

	@Override
	public T call() {

		try {

			preprocess();

			final StringBuilder commandLine = getCommandLine();
			if (commandLine != null) {

				String[] args = {"/bin/sh", "-c", commandLine.toString() };

				if (shouldLogCommandWhenExecuting()) {

					logger.info("Executing {}", getLogLine().toString());
				}

				Process proc = Runtime.getRuntime().exec(args);

				// consume streams
				stdOut = new StreamReader(proc.getInputStream(), running);
				stdErr = new StreamReader(proc.getErrorStream(), running);

				stdOut.start();
				stdErr.start();

				setExitCode(proc.waitFor());
			}

		} catch (IOException | InterruptedException ex) {

			logger.warn("", ex);
		}

		running.set(false);

		// debugging output
		if (exitCode() != 0) {

			logger.warn("Process {} exited with exit code {}, error stream:\n{}\n", getLogLine().toString(), exitCode(), stdErr.getBuffer());
		}

		return processExited(exitCode);
	}

	protected String outputStream() {
		return stdOut.getBuffer();
	}

	protected String errorStream() {
		return stdErr.getBuffer();
	}

	private int exitCode() {
		return exitCode;
	}

	private void setExitCode(final int exitCode) {
		this.exitCode = exitCode;
	}

	public void setLogBehaviour(final int logBehaviour) {
		this.logBehaviour = Settings.SCRIPT_PROCESS_LOG_STYLE.get(logBehaviour);
	}

	public Settings.SCRIPT_PROCESS_LOG_STYLE getLogBehaviour() {
		return this.logBehaviour;
	}
}
