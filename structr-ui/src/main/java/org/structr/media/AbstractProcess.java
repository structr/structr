package org.structr.media;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;

/**
 *
 * @author Christian Morgner
 */
public abstract class AbstractProcess<T> implements Callable<T> {

	private static final Logger logger = Logger.getLogger(AbstractProcess.class.getName());

	protected SecurityContext securityContext = null;
	private final AtomicBoolean running       = new AtomicBoolean(true);
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
			logger.log(Level.WARNING, "Process {0} exited with exit code {1}, error stream:{2}\n", new Object[] { cmd, exitCode, stdErr.getBuffer() } );
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
