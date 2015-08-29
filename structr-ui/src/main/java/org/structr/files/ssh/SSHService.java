package org.structr.files.ssh;

import java.io.IOException;
import java.security.PublicKey;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.structr.common.StructrConf;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.SingletonService;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.AuthHelper;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;

/**
 *
 * @author Christian Morgner
 */
public class SSHService implements SingletonService, PasswordAuthenticator, PublickeyAuthenticator, FileSystemFactory, CommandFactory, Factory<org.apache.sshd.server.Command> {

	public static final String APPLICATION_SSH_PORT = "application.ssh.port";

	private SshServer server = null;
	private boolean running  = false;

	@Override
	public void injectArguments(final Command command) {
	}

	@Override
	public void initialize(final Services services, final StructrConf config) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		server = SshServer.setUpDefaultServer();

		server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("db/structr_hostkey"));
		server.setPort(Services.parseInt(APPLICATION_SSH_PORT, 8022));
		server.setPasswordAuthenticator(this);
		server.setPublickeyAuthenticator(this);
		server.setFileSystemFactory(this);
		server.setShellFactory(this);
		server.setCommandFactory(new ScpCommandFactory(this));

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

	@Override
	public FileSystemView createFileSystemView(final Session session) throws IOException {
		return new StructrSSHFileSystemView(session);
	}

	@Override
	public boolean authenticate(final String username, final String password, final ServerSession session) {

		boolean isValid = false;

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final Principal principal = AuthHelper.getPrincipalForPassword(AbstractNode.name, username, password);
			if (principal != null) {

				isValid = true;
			}

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();

			isValid = false;
		}

		return isValid;
	}

	@Override
	public boolean authenticate(final String username, final PublicKey key, final ServerSession session) {

		// no pubkey auth yet
		return false;
	}

	@Override
	public org.apache.sshd.server.Command createCommand(final String command) {
		return create();
	}

	@Override
	public org.apache.sshd.server.Command create() {
		return new StructrShellCommand();
	}
}
