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
package org.structr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.Callable;

/**
 *
 *
 */
public abstract class AbstractBinaryProcess<T> implements Callable<T> {

	private static final Logger logger = LoggerFactory.getLogger(AbstractBinaryProcess.class.getName());

	private static final long STREAM_DRAIN_MILLIS = 10_000;

	protected SecurityContext securityContext = null;
	private CopyingStreamReader stdOut        = null;
	private OutputStream out                  = null;
	private StreamReader stdErr               = null;
	private int exitCode                      = -1;

	private Settings.SCRIPT_PROCESS_LOG_STYLE logBehaviour = Settings.SCRIPT_PROCESS_LOG_STYLE.get(Settings.LogScriptProcessCommandLine.getValue());

	public AbstractBinaryProcess(final SecurityContext securityContext, final OutputStream out) {

		this.securityContext = securityContext;
		this.out             = out;
	}

	public abstract StringBuilder getCommandLine();
	public abstract T processExited(final int exitCode);
	public abstract void preprocess();

	public StringBuilder getLogLine() {

		final StringBuilder commandLine = getCommandLine();
		if (commandLine != null) {

			return commandLine;
		}

		final List<String> arguments = getCommandArguments();
		if (arguments != null) {

			return new StringBuilder(String.join(" ", arguments));
		}

		return new StringBuilder();
	}

	/** The command as a list of arguments, executed without a shell, or null to fall back to {@link #getCommandLine()}. */
	public List<String> getCommandArguments() {

		return null;
	}

	/** What the process reads from its standard input, or null for an empty one; never logged. */
	public InputStream getInput() {

		return null;
	}

	private boolean shouldLogCommandWhenExecuting() {

		return (getLogBehaviour() != Settings.SCRIPT_PROCESS_LOG_STYLE.NOTHING);
	}

	@Override
	public T call() {

		try {

			preprocess();

			final List<String> arguments    = getCommandArguments();
			final StringBuilder commandLine = arguments == null ? getCommandLine() : null;

			if (arguments != null || commandLine != null) {

				final String[] args = arguments != null
					? arguments.toArray(new String[0])
					: new String[] { "/bin/sh", "-c", commandLine.toString() };

				if (shouldLogCommandWhenExecuting()) {

					logger.info("Executing {}", getLogLine().toString());
				}

				Process proc = Runtime.getRuntime().exec(args);

				// consume streams
				stdOut = new CopyingStreamReader(proc.getInputStream(), out);
				stdErr = new StreamReader(proc.getErrorStream());

				stdOut.start();
				stdErr.start();

				new StdinWriter(getInput(), proc.getOutputStream()).start();

				setExitCode(proc.waitFor());

				// the output may still be in the pipe when the process has exited; bounded, a background child can keep it open
				final long drainUntil = System.currentTimeMillis() + STREAM_DRAIN_MILLIS;

				stdOut.join(Math.max(1, drainUntil - System.currentTimeMillis()));
				stdErr.join(Math.max(1, drainUntil - System.currentTimeMillis()));
			}

		} catch (IOException | InterruptedException ex) {

			logger.warn("", ex);
		}

		// debugging output
		if (exitCode() != 0) {

			logger.warn("Process '{}' exited with exit code {}, error stream:\n{}\n", getLogLine().toString(), exitCode(), stdErr.getBuffer());
		}

		return processExited(exitCode());
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
