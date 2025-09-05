/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.test.web.files.FtpTest;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.AssertJUnit.*;

/**
 * Tests for FTP directories.
 *
 *
 */
public class FtpDirectoriesTest extends FtpTest {

	private static final Logger logger = LoggerFactory.getLogger(FtpDirectoriesTest.class.getName());

	@Test
	public void test01ListDirectories() {

		final String name1 = "FTPdir1";
		final String name2 = "FTPdir2";

		FTPClient ftp = setupFTPClient("ftpuser1");
		FTPFile[] dirs = null;

		try (final Tx tx = app.tx()) {

			dirs = ftp.listDirectories();

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

			dirs = ftp.listDirectories();

			assertNotNull(dirs);
			assertEquals(1, dirs.length);
			assertEquals(name1, dirs[0].getName());

			// Create second folder in /
			createFTPDirectory(null, name2);

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			dirs = ftp.listDirectories();

			assertNotNull(dirs);
			assertEquals(2, dirs.length);
			assertEquals(name1, dirs[0].getName());
			assertEquals(name2, dirs[1].getName());

			ftp.disconnect();

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	@Test
	public void test02MkDir() {

		FTPClient ftp = setupFTPClient("ftpuser1");

		FTPFile[] dirs = null;

		final String name1 = "FTPdir1";
		final String name2 = "FTPdir2";
		boolean success = false;

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			assertEmptyDirectory(ftp);

			// Create folder by mkdir FTP command
			success = ftp.makeDirectory(name1);
			assertTrue(success);

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			dirs = ftp.listDirectories();

			assertNotNull(dirs);
			assertEquals(1, dirs.length);
			assertEquals(name1, dirs[0].getName());

			// Create second folder in /
			success = ftp.makeDirectory(name2);
			assertTrue(success);

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			dirs = ftp.listDirectories();

			assertNotNull(dirs);
			assertEquals(2, dirs.length);
			assertEquals(name1, dirs[0].getName());
			assertEquals(name2, dirs[1].getName());

			ftp.disconnect();

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	@Test
	public void test03MkdirCd() {

		FTPClient ftp = setupFTPClient("ftpuser1");
		final String name1 = "/FTPdir1";

		try (final Tx tx = app.tx()) {

			FTPFile[] dirs = ftp.listDirectories();

			assertNotNull(dirs);
			assertEquals(0, dirs.length);

			// Create folder by mkdir FTP command
			ftp.makeDirectory(name1);

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			ftp.changeWorkingDirectory(name1);

			assertEmptyDirectory(ftp);

			String newWorkingDirectory = ftp.printWorkingDirectory();
			assertEquals(name1, newWorkingDirectory);

			ftp.disconnect();

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	@Test
	public void test04MkdirCdMkdirCd() {

		FTPClient ftp = setupFTPClient("ftpuser1");

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			assertEmptyDirectory(ftp);

			String name1 = "/FTPdir1";

			// Create folder by mkdir FTP command
			ftp.makeDirectory(name1);

			ftp.changeWorkingDirectory(name1);

			String newWorkingDirectory = ftp.printWorkingDirectory();
			assertEquals(name1, newWorkingDirectory);

			assertEmptyDirectory(ftp);

			String name2 = name1.concat("/").concat("FTPdir2");

			// Create folder by mkdir FTP command
			ftp.makeDirectory(name2);

			ftp.changeWorkingDirectory(name2);

			newWorkingDirectory = ftp.printWorkingDirectory();
			assertEquals(name2, newWorkingDirectory);

			assertEmptyDirectory(ftp);

			ftp.disconnect();

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	@Test
	public void test05CdUp() {

		FTPClient ftp = setupFTPClient("ftpuser1");

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			assertEmptyDirectory(ftp);

			String name1 = "/FTPdir1";

			// Create folder by mkdir FTP command
			ftp.makeDirectory(name1);

			ftp.changeWorkingDirectory(name1);

			String name2 = name1.concat("/").concat("FTPdir2");

			// Create folder by mkdir FTP command
			ftp.makeDirectory(name2);
			ftp.changeWorkingDirectory(name2);

			boolean success = ftp.changeToParentDirectory();
			assertTrue(success);

			String newWorkingDirectory = ftp.printWorkingDirectory();
			assertEquals(name1, newWorkingDirectory);

			ftp.disconnect();

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.error("Error", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	@Test
	public void test06CdTwoUp() {

		FTPClient ftp = setupFTPClient("ftpuser1");

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			assertEmptyDirectory(ftp);

			String name1 = "/FTPdir1";

			// Create folder by mkdir FTP command
			ftp.makeDirectory(name1);

			ftp.changeWorkingDirectory(name1);

			String name2 = name1.concat("/").concat("FTPdir2");

			// Create folder by mkdir FTP command
			ftp.makeDirectory(name2);

			ftp.changeWorkingDirectory(name2);

			String name3 = name2.concat("/").concat("FTPdir3");

			// Create folder by mkdir FTP command
			ftp.makeDirectory(name3);

			ftp.changeWorkingDirectory(name3);

			ftp.changeToParentDirectory();
			ftp.changeToParentDirectory();

			String newWorkingDirectory = ftp.printWorkingDirectory();
			assertEquals(name1, newWorkingDirectory);

			ftp.disconnect();

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.error("Error", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	@Test
	public void test07CdToSiblingDirectory() {

		FTPClient ftp = setupFTPClient("ftpuser1");

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			FTPFile[] dirs = ftp.listDirectories();

			assertNotNull(dirs);
			assertEquals(0, dirs.length);

			String name1 = "/FTPdir1";
			String name2 = "/FTPdir2";

			// Create folders by mkdir FTP command
			ftp.makeDirectory(name1);
			ftp.makeDirectory(name2);

			ftp.changeWorkingDirectory(name1);

			String newWorkingDirectory = ftp.printWorkingDirectory();
			assertEquals(name1, newWorkingDirectory);

			ftp.changeWorkingDirectory("../" + name2);

			newWorkingDirectory = ftp.printWorkingDirectory();
			assertEquals(name2, newWorkingDirectory);

			ftp.disconnect();

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.error("Error while changing FTP directories", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	@Test
	public void test08CdRoot() {

		FTPClient ftp = setupFTPClient("ftpuser1");

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			FTPFile[] dirs = ftp.listDirectories();

			assertNotNull(dirs);
			assertEquals(0, dirs.length);

			String name1 = "/FTPdir1";

			// Create folder by mkdir FTP command
			ftp.makeDirectory(name1);

			ftp.changeWorkingDirectory(name1);

			assertEmptyDirectory(ftp);

			String newWorkingDirectory = ftp.printWorkingDirectory();
			assertEquals(name1, newWorkingDirectory);

			ftp.changeWorkingDirectory("/");

			newWorkingDirectory = ftp.printWorkingDirectory();
			assertEquals("/", newWorkingDirectory);

			ftp.disconnect();

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.error("Error while changing FTP directories", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

}
