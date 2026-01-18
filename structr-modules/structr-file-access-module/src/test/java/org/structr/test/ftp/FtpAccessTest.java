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
package org.structr.test.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.test.web.files.FtpTest;
import org.structr.web.entity.Folder;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.AssertJUnit.*;

/**
 * Tests for FTP service.
 *
 *
 */
public class FtpAccessTest extends FtpTest {

	private static final Logger logger = LoggerFactory.getLogger(FtpAccessTest.class.getName());

	@Test
	public void test01LoginFailed() {

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			FTPClient ftpClient = new FTPClient();

			ftpClient.connect("127.0.0.1", ftpPort);
			logger.info("Reply from FTP server:", ftpClient.getReplyString());

			int reply = ftpClient.getReplyCode();
			assertTrue(FTPReply.isPositiveCompletion(reply));

			boolean loginSuccess = ftpClient.login("jt978hasdl", "lj3498ha");
			logger.info("Try to login as jt978hasdl/lj3498ha:", loginSuccess);
			assertFalse(loginSuccess);

			ftpClient.disconnect();

		} catch (IOException | FrameworkException ex) {
			logger.error("Error in FTP test", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

	}

	@Test
	public void test02LoginSuccess() {

		FTPClient ftpClilent = setupFTPClient("ftpuser1");
		disconnect(ftpClilent);

	}

	@Test
	public void test03UserAccessToDirectory() {

		FTPClient client1 = setupFTPClient("ftpuser1");

		final String name1 = "FTPdir1";
		FTPFile[] dirs = null;

		try (final Tx tx = app.tx()) {

			dirs = client1.listDirectories();

			assertNotNull(dirs);
			assertEquals(0, dirs.length);

			// Create folder by API methods
			createFTPDirectory(null, name1);

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			dirs = client1.listDirectories();

			assertNotNull(dirs);
			assertEquals(1, dirs.length);
			assertEquals(name1, dirs[0].getName());

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		// Try to access the directory as another user, result should be empty

		FTPClient client2 = setupFTPClient("ftpuser2");

		try (final Tx tx = app.tx()) {

			dirs = client2.listDirectories();

			assertNotNull(dirs);
			assertEquals(0, dirs.length);

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

	}

	@Test
	public void test04UserAccessToSubdirectory() {

		FTPClient client1 = setupFTPClient("ftpuser1");

		final String name1 = "FTPdir1";
		final String name2 = "FTPdir2";

		FTPFile[] dirs = null;

		Folder dir1 = null;
		Folder dir2 = null;

		try (final Tx tx = app.tx()) {

			dirs = client1.listDirectories();

			assertNotNull(dirs);
			assertEquals(0, dirs.length);

			// Create folders by API methods
			dir1 = createFTPDirectory(null, name1);
			dir2 = createFTPDirectory(dir1.getPath(), name2);

			// Make dir1 visible to authenticated users
			dir1.setVisibleToAuthenticatedUsers(true);

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			dirs = client1.listDirectories();

			assertNotNull(dirs);
			assertEquals(1, dirs.length);
			assertEquals(name1, dirs[0].getName());

			client1.changeWorkingDirectory(dir1.getPath());
			FTPFile[] subdirs = client1.listDirectories();

			assertNotNull(subdirs);
			assertEquals(1, subdirs.length);
			assertEquals(name2, subdirs[0].getName());

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		// Try to access the directories as another user.

		// client2 should see dir1

		FTPClient client2 = setupFTPClient("ftpuser2");

		try (final Tx tx = app.tx()) {

			dirs = client2.listDirectories();

			assertNotNull(dirs);
			assertEquals(1, dirs.length);
			assertEquals(name1, dirs[0].getName());

			client2.changeWorkingDirectory(dir1.getPath());
			FTPFile[] subdirs = client2.listDirectories();

			assertNotNull(subdirs);
			assertEquals(0, subdirs.length);

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

	}
}
