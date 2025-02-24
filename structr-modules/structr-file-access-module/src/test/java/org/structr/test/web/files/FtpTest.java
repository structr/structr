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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.web.common.FileHelper;
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
public abstract class FtpTest extends StructrFileTestBase {

	private static final Logger logger = LoggerFactory.getLogger(FtpTest.class.getName());

	protected User ftpUser;

	protected User createFTPUser(final String username, final String password) throws FrameworkException {

		final PropertyMap props = new PropertyMap();
		final Traits traits     = Traits.of(StructrTraits.USER);

		props.put(traits.key("name"), username);
		props.put(traits.key("password"), password);

		return createTestNodes(StructrTraits.USER, 1, props).get(0).as(User.class);
	}

	protected Folder createFTPDirectory(final String path, final String name) throws FrameworkException {

		final PropertyMap props = new PropertyMap();
		final Traits traits     = Traits.of(StructrTraits.FOLDER);

		props.put(traits.key("name"), name);
		props.put(traits.key("owner"), ftpUser);

		Folder dir = createTestNodes(StructrTraits.FOLDER, 1, props).get(0).as(Folder.class);

		if (StringUtils.isNotBlank(path)) {

			NodeInterface parent = FileHelper.getFileByAbsolutePath(securityContext, path);
			if (parent != null && parent.is(StructrTraits.FOLDER)) {

				Folder parentFolder = parent.as(Folder.class);
				dir.setParent(parentFolder);
			}
		}

		logger.info("FTP directory {} created successfully.", dir);

		return dir;
	}

	protected File createFTPFile(final String path, final String name) throws FrameworkException {

		final PropertyMap props = new PropertyMap();
		final Traits traits     = Traits.of(StructrTraits.FILE);

		props.put(traits.key("name"), name);
		props.put(traits.key("size"), 0L);
		props.put(traits.key("owner"), ftpUser);

		File file = createTestNodes(StructrTraits.FILE, 1, props).get(0).as(File.class);

		if (StringUtils.isNotBlank(path)) {

			NodeInterface parent = FileHelper.getFileByAbsolutePath(securityContext, path);
			if (parent != null && parent.is(StructrTraits.FOLDER)) {

				Folder parentFolder = parent.as(Folder.class);
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
	protected FTPClient setupFTPClient(final String username) {

		FTPClient ftp = new FTPClient();

		try {

			ftp.connect("localhost", ftpPort);
			logger.info("Reply from FTP server:", ftp.getReplyString());

			int reply = ftp.getReplyCode();
			assertTrue(FTPReply.isPositiveCompletion(reply));

			String password = "ftpuserpw1";

			try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {
				ftpUser = createFTPUser(username, password);
				tx.success();
			} catch (FrameworkException fex) {
				logger.error("Unable to create FTP user", fex);
			}

			boolean loginSuccess = ftp.login(username, password);
			logger.info("Tried to login as {}/{}: {}", new Object[]{ username, password, loginSuccess});
			assertTrue(loginSuccess);

			reply = ftp.getReplyCode();
			assertEquals(FTPReply.USER_LOGGED_IN, reply);

		} catch (IOException ex) {
			logger.error("Error in FTP test", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		return ftp;

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
