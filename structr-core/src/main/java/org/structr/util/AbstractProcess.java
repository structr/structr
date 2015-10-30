/**
 * Copyright (C) 2010-2015 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.util;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;

/**
 *
 *
 */
public abstract class AbstractProcess<T> implements Callable<T> {

	private static final Logger logger = Logger.getLogger(AbstractProcess.class.getName());

	private final AtomicBoolean running       = new AtomicBoolean(true);
	protected SecurityContext securityContext = null;
	private StreamReader stdOut               = null;
	private StreamReader stdErr               = null;
	private String cmd                        = null;
	private int exitCode                      = -1;

	public AbstractProcess(final SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	public abstract StringBuilder getCommandLine();
	public abstract T processExited(final int exitCode);
	public abstract void preprocess();

	@Override
	public T call() {

		try {
			// allow preprocessing
			preprocess();

			final StringBuilder commandLine = getCommandLine();
			if (commandLine != null) {

				cmd = commandLine.toString();

				String[] args = {"/bin/sh", "-c", cmd};

				logger.log(Level.INFO, "Executing {0}", cmd);

				Process proc = Runtime.getRuntime().exec(args);

				// consume streams
				stdOut = new StreamReader(proc.getInputStream(), running);
				stdErr = new StreamReader(proc.getErrorStream(), running);

				stdOut.start();
				stdErr.start();

				exitCode = proc.waitFor();
			}

		} catch (IOException | InterruptedException ex) {

			ex.printStackTrace();
		}

		running.set(false);

		// debugging output
		if (exitCode != 0) {
			logger.log(Level.WARNING, "Process {0} exited with exit code {1}, error stream:\n{2}\n", new Object[] { cmd, exitCode, stdErr.getBuffer() } );
		}

		return processExited(exitCode);
	}

	protected String outputStream() {
		return stdOut.getBuffer();
	}

	protected String errorStream() {
		return stdErr.getBuffer();
	}

	protected int exitCode() {
		return exitCode;
	}
}
