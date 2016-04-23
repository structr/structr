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
import java.nio.file.FileSystem;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.config.keys.PublicKeyEntryResolver;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.structr.api.service.Command;
import org.structr.core.Services;
import org.structr.api.service.SingletonService;
import org.structr.api.service.StructrServices;
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

	private static final Logger logger = Logger.getLogger(SSHService.class.getName());

	public static final String APPLICATION_SSH_PORT = "application.ssh.port";

	private SshServer server = null;
	private boolean running = false;

	@Override
	public void injectArguments(final Command command) {
	}

	@Override
	public void initialize(final StructrServices services, final Properties config) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		server = SshServer.setUpDefaultServer();

		server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get("db/structr_hostkey")));
		server.setPort(Services.parseInt(APPLICATION_SSH_PORT, 8022));
		server.setPasswordAuthenticator(this);
		server.setPublickeyAuthenticator(this);
		server.setFileSystemFactory(this);
		server.setSubsystemFactories(getSftpSubsystem());
		server.setShellFactory(this);

		final ScpCommandFactory scp = new ScpCommandFactory.Builder()
			//.withDelegate(this)
			//.addEventListener(new StructrScpTransferEventListener());
			.build();

		server.setCommandFactory(scp);

		try {

			server.start();
			running = true;

		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void shutdown() {

		try {

			server.stop(true);
			running = false;

		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
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
	public FileSystem createFileSystem(final Session session) throws IOException {
		return new StructrSSHFileSystem(session);
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
			logger.log(Level.WARNING, "", t);

			isValid = false;
		}

		return isValid;
	}

	@Override
	public boolean authenticate(final String username, final PublicKey key, final ServerSession session) {

		boolean isValid = false;

		if (key == null) {
			return isValid;
		}

		try (final Tx tx = StructrApp.getInstance().tx()) {

			final Principal principal = StructrApp.getInstance().nodeQuery(Principal.class).andName(username).getFirst();
			if (principal != null) {

				final String pubKeyData = principal.getProperty(Principal.publicKey);

				if (pubKeyData != null) {

					final PublicKey pubKey = PublicKeyEntry.parsePublicKeyEntry(pubKeyData).resolvePublicKey(PublicKeyEntryResolver.FAILING);

					isValid = KeyUtils.compareKeys(pubKey, key);
				}
			}

			tx.success();

		} catch (Throwable t) {
			logger.log(Level.WARNING, "", t);

			isValid = false;
		}

		return isValid;
	}

	@Override
	public org.apache.sshd.server.Command createCommand(final String command) {

		final StructrShellCommand cmd = new StructrShellCommand() {

			@Override
			public void start(final Environment env) throws IOException {

				super.start(env);

				// non-interactively handle the command
				this.handleLine(command);
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

		list.add(new SftpSubsystemFactory());

		return list;
	}
}
