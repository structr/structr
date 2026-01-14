/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.web.files;

import com.jcraft.jsch.*;
import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.web.entity.User;

import java.io.IOException;

import static org.testng.AssertJUnit.fail;

/**
 * Common class for FTP tests
 *
 *
 */
public abstract class SSHTest extends StructrFileTestBase {

	private static final Logger logger = LoggerFactory.getLogger(SSHTest.class.getName());

	protected User ftpUser;

	protected User createFTPUser(final String username, final String password) throws FrameworkException {

		final PropertyMap props = new PropertyMap();
		final Traits traits     = Traits.of(StructrTraits.USER);

		props.put(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), username);
		props.put(traits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY), password);

		return createTestNodes(StructrTraits.USER, 1, props).get(0).as(User.class);
	}

	/**
	 * Creates an FTP client, a backend user and logs this user in.
	 *
	 * @param username
	 * @return
	 */
	protected ChannelSftp setupSftpClient(final String username, final String password, final boolean isAdmin) {

		try (final Tx tx = app.tx()) {

			ftpUser = createFTPUser(username, password);
			if (isAdmin) {
				ftpUser.setIsAdmin(true);
			}
			tx.success();

		} catch (FrameworkException fex) {
			logger.error("Unable to create SFTP user", fex);
		}

		JSch jsch = new JSch();

		try {

			final Session session = jsch.getSession(username, host, sshPort);

			session.setConfig("StrictHostKeyChecking", "no");
			session.setPassword(password);
			session.connect(5000);

			final Channel channel = session.openChannel("sftp");
			channel.connect(5000);

			return (ChannelSftp)channel;

		} catch (JSchException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	protected void disconnect(final FTPClient ftp) {
		try {
			ftp.disconnect();
		} catch (IOException ex) {
			logger.error("Error while disconnecting from FTP server", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}
}
