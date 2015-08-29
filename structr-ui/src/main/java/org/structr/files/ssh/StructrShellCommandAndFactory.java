package org.structr.files.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;

/**
 *
 * @author Christian Morgner
 */
public class StructrShellCommandAndFactory implements Command, Factory<Command> {

	private InputStream  is = null;
	private OutputStream os = null;
	private OutputStream es = null;

	@Override
	public void setInputStream(InputStream in) {
		this.is = in;
	}

	@Override
	public void setOutputStream(OutputStream out) {
		this.os = out;
	}

	@Override
	public void setErrorStream(OutputStream err) {
		this.es = err;
	}

	@Override
	public void setExitCallback(ExitCallback callback) {
	}

	@Override
	public void start(Environment env) throws IOException {

		System.out.println("start: " + env);
	}

	@Override
	public void destroy() {
	}

	@Override
	public Command create() {
		return new StructrShellCommandAndFactory();
	}
}
