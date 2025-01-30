/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;

import java.io.IOException;

import static org.testng.AssertJUnit.*;

/**
 * Common class for FTP tests
 *
 *
 */
public abstract class SSHTest extends StructrFileTestBase {

	private static final Logger logger = LoggerFactory.getLogger(SSHTest.class.getName());

	protected User ftpUser;

	protected User createFTPUser(final String username, final String password) throws FrameworkException {
		PropertyMap props = new PropertyMap();
		props.put(StructrApp.key(Principal.class, "name"), username);
		props.put(StructrApp.key(Principal.class, "password"), password);
		return (User)createTestNodes("User", 1, props).get(0);
	}

	protected Folder createFTPDirectory(final String path, final String name) throws FrameworkException {
		PropertyMap props = new PropertyMap();
		props.put(Folder.name, name);
		props.put(Folder.owner, ftpUser);
		Folder dir = (Folder)createTestNodes("Folder", 1, props).get(0);

		if (StringUtils.isNotBlank(path)) {
			AbstractFile parent = FileHelper.getFileByAbsolutePath(securityContext, path);
			if (parent != null && parent instanceof Folder) {
				Folder parentFolder = (Folder)parent;
				dir.setParent(parentFolder);
			}
		}

		logger.info("FTP directory {} created successfully.", dir);

		return dir;
	}

	protected File createFTPFile(final String path, final String name) throws FrameworkException {
		PropertyMap props = new PropertyMap();
		props.put(StructrApp.key(File.class, "name"), name);
		props.put(StructrApp.key(File.class, "size"), 0L);
		props.put(StructrApp.key(File.class, "owner"), ftpUser);
		File file = (File)createTestNodes("File", 1, props).get(0);

		if (StringUtils.isNotBlank(path)) {
			AbstractFile parent = FileHelper.getFileByAbsolutePath(securityContext, path);
			if (parent != null && parent instanceof Folder) {
				Folder parentFolder = (Folder)parent;
				file.setParent(parentFolder);
			}
		}

		logger.info("FTP file {} created successfully.", file);

		return file;
	}

	protected void assertEmptyDirectory(final FTPClient ftp) {

		FTPFile[] dirs = null;

		try {

			dirs = ftp.listDirectories();

		} catch (IOException ex) {

			logger.error("Error in FTP test", ex);
			fail("Unexpected exception: " + ex.getMessage());

		}

		assertNotNull(dirs);
		assertEquals(0, dirs.length);

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
