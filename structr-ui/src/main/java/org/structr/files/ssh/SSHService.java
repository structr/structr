/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.files.ssh;

import java.io.IOException;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.structr.common.StructrConf;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.SingletonService;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.rest.auth.AuthHelper;

/**
 *
 *
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
		server.setSubsystemFactories(getSftpSubsystem());
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

		final StructrShellCommand cmd = new StructrShellCommand() {

			@Override
			public void start(final Environment env) throws IOException {

				super.start(env);

				// non-interactively handle the command
				this. handleLine(command);
				this.flush();
				this.handleExit();
			}

			@Override
			public boolean isInteractive() {
				return false;
			}
		};

		return cmd;
	}

	@Override
	public org.apache.sshd.server.Command create() {
		return new StructrShellCommand();
	}

	// ----- private methods -----
	private List<NamedFactory<org.apache.sshd.server.Command>> getSftpSubsystem() {

		final List<NamedFactory<org.apache.sshd.server.Command>> list = new LinkedList<>();

		list.add(new SftpSubsystem.Factory());

		return list;
	}
}
