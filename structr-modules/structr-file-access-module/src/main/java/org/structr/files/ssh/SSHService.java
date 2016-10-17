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
import java.nio.file.CopyOption;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.config.keys.PublicKeyEntryResolver;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.DirectoryHandle;
import org.apache.sshd.server.subsystem.sftp.FileHandle;
import org.apache.sshd.server.subsystem.sftp.Handle;
import org.apache.sshd.server.subsystem.sftp.SftpEventListener;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.Command;
import org.structr.api.service.SingletonService;
import org.structr.api.service.StructrServices;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.files.ssh.filesystem.StructrFilesystem;
import org.structr.rest.auth.AuthHelper;

/**
 *
 *
 */
public class SSHService implements SingletonService, PasswordAuthenticator, PublickeyAuthenticator, FileSystemFactory, Factory<org.apache.sshd.server.Command>, SftpEventListener {

	private static final Logger logger = LoggerFactory.getLogger(SSHService.class.getName());

	public static final String APPLICATION_SSH_PORT = "application.ssh.port";

	private SshServer server                = null;
	private boolean running                 = false;
	private SecurityContext securityContext = null;

	@Override
	public void injectArguments(final Command command) {
	}

	@Override
	public void initialize(final StructrServices services, final Properties config) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

		server = SshServer.setUpDefaultServer();

		final SimpleGeneratorHostKeyProvider hostKeyProvider = new SimpleGeneratorHostKeyProvider(Paths.get("db/structr_hostkey"));
		hostKeyProvider.setAlgorithm(KeyUtils.RSA_ALGORITHM);


		server.setKeyPairProvider(hostKeyProvider);
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
			logger.error("", ex);
		}
	}

	@Override
	public void shutdown() {

		try {

			server.stop(true);
			running = false;

		} catch (IOException ex) {
			logger.error("", ex);
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
		//return new StructrSSHFileSystem(securityContext, session);
		return new StructrFilesystem(securityContext);
	}

	@Override
	public boolean authenticate(final String username, final String password, final ServerSession session) {

		boolean isValid     = false;
		Principal principal = null;

		try (final Tx tx = StructrApp.getInstance().tx()) {

			principal = AuthHelper.getPrincipalForPassword(AbstractNode.name, username, password);
			if (principal != null) {

				isValid = true;
				securityContext = SecurityContext.getInstance(principal, AccessMode.Backend);
			}

			tx.success();

		} catch (Throwable t) {
			logger.warn("", t);

			isValid = false;
		}

		try {
			if (isValid) {
				session.setAuthenticated();
			}

		} catch (IOException ex) {
			logger.error("", ex);
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

				securityContext = SecurityContext.getInstance(principal, AccessMode.Backend);

				final String pubKeyData = principal.getProperty(Principal.publicKey);

				if (pubKeyData != null) {

					final PublicKey pubKey = PublicKeyEntry.parsePublicKeyEntry(pubKeyData).resolvePublicKey(PublicKeyEntryResolver.FAILING);

					isValid = KeyUtils.compareKeys(pubKey, key);
				}
			}

			tx.success();

		} catch (Throwable t) {
			logger.warn("", t);

			isValid = false;
		}

		try {
			if (isValid) {
				session.setAuthenticated();
			}

		} catch (IOException ex) {
			logger.error("", ex);
		}

		return isValid;
	}

	private Tx currentTransaction = null;

	@Override
	public org.apache.sshd.server.Command create() {
		return new StructrConsoleCommand(securityContext);
	}

	// ----- private methods -----
	private void beginTransaction() {

		if (currentTransaction == null) {

			currentTransaction = StructrApp.getInstance(securityContext).tx(true, false, false);
		}
	}

	private void endTransaction() {

		if (currentTransaction != null) {

			try {
				currentTransaction.success();
				currentTransaction.close();

			} catch (Throwable t) {

				t.printStackTrace();

			} finally {

				currentTransaction = null;
			}
		}
	}

	// ----- interface SftpEventListener -----
	@Override
	public void initialized(ServerSession session, int version) {
	}

	@Override
	public void destroying(ServerSession session) {
	}

	@Override
	public void open(ServerSession session, String remoteHandle, Handle localHandle) {
		beginTransaction();
	}

	@Override
	public void read(ServerSession session, String remoteHandle, DirectoryHandle localHandle, Map<String, Path> entries) {
	}

	@Override
	public void read(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, byte[] data, int dataOffset, int dataLen, int readLen) {
	}

	@Override
	public void write(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, byte[] data, int dataOffset, int dataLen) {
	}

	@Override
	public void blocking(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, long length, int mask) {
	}

	@Override
	public void blocked(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, long length, int mask, Throwable thrown) {
	}

	@Override
	public void unblocking(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, long length) {
	}

	@Override
	public void unblocked(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, long length, Boolean result, Throwable thrown) {
	}

	@Override
	public void close(ServerSession session, String remoteHandle, Handle localHandle) {
		endTransaction();
	}

	@Override
	public void creating(ServerSession session, Path path, Map<String, ?> attrs) {
		beginTransaction();
	}

	@Override
	public void created(ServerSession session, Path path, Map<String, ?> attrs, Throwable thrown) {
		endTransaction();
	}

	@Override
	public void moving(ServerSession session, Path srcPath, Path dstPath, Collection<CopyOption> opts) {
		beginTransaction();
	}

	@Override
	public void moved(ServerSession session, Path srcPath, Path dstPath, Collection<CopyOption> opts, Throwable thrown) {
		endTransaction();
	}

	@Override
	public void removing(ServerSession session, Path path) {
		beginTransaction();
	}

	@Override
	public void removed(ServerSession session, Path path, Throwable thrown) {
		endTransaction();
	}

	@Override
	public void linking(ServerSession session, Path source, Path target, boolean symLink) {
		beginTransaction();
	}

	@Override
	public void linked(ServerSession session, Path source, Path target, boolean symLink, Throwable thrown) {
		endTransaction();
	}

	@Override
	public void modifyingAttributes(ServerSession session, Path path, Map<String, ?> attrs) {
	}

	@Override
	public void modifiedAttributes(ServerSession session, Path path, Map<String, ?> attrs, Throwable thrown) {
	}

	// ----- private methods -----
	private List<NamedFactory<org.apache.sshd.server.Command>> getSftpSubsystem() {

		final List<NamedFactory<org.apache.sshd.server.Command>> list = new LinkedList<>();

		final SftpSubsystemFactory factory = new SftpSubsystemFactory();
		list.add(factory);

		factory.addSftpEventListener(this);


		return list;
	}
}
