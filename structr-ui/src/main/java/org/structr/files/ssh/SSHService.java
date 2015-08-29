package org.structr.files.ssh;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sshd.SshServer;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.structr.common.StructrConf;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.SingletonService;

/**
 *
 * @author Christian Morgner
 */
public class SSHService implements SingletonService {

	private SshServer server = null;
	private boolean running  = false;

	@Override
	public void injectArguments(Command command) {
	}

	@Override
	public void initialize(final Services services, final StructrConf config) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		server = SshServer.setUpDefaultServer();
		server.setPort(8022);
		server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("hostkey.ser"));
		server.setPasswordAuthenticator(new PasswordAuthenticator() {

			@Override
			public boolean authenticate(final String username, final String password, final ServerSession session) {
				return true;
			}
		});

		server.setFileSystemFactory(new StructrSSHFileSystemFactory());
		server.setCommandFactory(new ScpCommandFactory());

		try {

			server.start();
			running = true;


		} catch (IOException ex) {
			Logger.getLogger(SSHService.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void shutdown() {

		try {

			server.stop(true);
			running = false;

		} catch (InterruptedException ex) {
			Logger.getLogger(SSHService.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void initialized() {
	}

	@Override
	public String getName() {
		return "SSHService";
	}

	@Override
	public boolean isRunning() {
		return server != null && running;
	}

	@Override
	public boolean isVital() {
		return false;
	}
}