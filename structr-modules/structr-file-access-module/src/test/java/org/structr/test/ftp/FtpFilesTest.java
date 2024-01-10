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
package org.structr.test.ftp;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.test.web.files.FtpTest;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.testng.AssertJUnit.*;

/**
 * Tests for FTP files.
 *
 *
 */
public class FtpFilesTest extends FtpTest {

	private static final Logger logger = LoggerFactory.getLogger(FtpFilesTest.class.getName());

	@Test
	public void test00StoreFile() {

		FTPClient ftp = setupFTPClient("ftpuser1");
		final String name1 = "file1";
		final String name2 = "file2";

		try (final Tx tx = app.tx()) {

			FTPFile[] files = ftp.listFiles();

			assertNotNull(files);
			assertEquals(0, files.length);

			ftp.setFileType(FTP.ASCII_FILE_TYPE);
			ftp.setAutodetectUTF8(true);

			// Store a file
			InputStream in = IOUtils.toInputStream("Test Content");
			ftp.storeFile(name1, in);

			in.close();

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		String[] fileNames = null;

		try (final Tx tx = app.tx()) {

			fileNames = ftp.listNames();

			assertNotNull(fileNames);
			assertEquals(1, fileNames.length);
			assertEquals(name1, fileNames[0]);

			// Create second file in /
			createFTPFile(null, name2);

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			fileNames = ftp.listNames();

			assertNotNull(fileNames);
			assertEquals(2, fileNames.length);
			assertEquals(name1, fileNames[0]);
			assertEquals(name2, fileNames[1]);

			ftp.disconnect();

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	@Test
	public void test01ListFiles() {

		FTPClient ftp = setupFTPClient("ftpuser1");
		final String name1 = "file1";
		final String name2 = "file2";

		try (final Tx tx = app.tx()) {

			FTPFile[] files = ftp.listFiles();

			assertNotNull(files);
			assertEquals(0, files.length);

			// Create files by API methods
			createFTPFile(null, name1);

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		String[] fileNames = null;

		try (final Tx tx = app.tx()) {

			fileNames = ftp.listNames();

			assertNotNull(fileNames);
			assertEquals(1, fileNames.length);
			assertEquals(name1, fileNames[0]);

			// Create second file in /
			createFTPFile(null, name2);

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			fileNames = ftp.listNames();

			assertNotNull(fileNames);
			assertEquals(2, fileNames.length);
			assertEquals(name1, fileNames[0]);
			assertEquals(name2, fileNames[1]);

			ftp.disconnect();

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	@Test
	public void test02RenameFile() {

		FTPClient ftp = setupFTPClient("ftpuser1");
		final String name2 = "file2";
		final String name1 = "file1";

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			FTPFile[] files = ftp.listFiles();

			assertNotNull(files);
			assertEquals(0, files.length);

			// Create files by API methods
			createFTPFile(null, name1);

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			ftp.rename(name1, name2);

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			String[] fileNames = ftp.listNames();

			assertNotNull(fileNames);
			assertEquals(1, fileNames.length);
			assertEquals(name2, fileNames[0]);

			ftp.disconnect();

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	@Test
	public void test03MoveFile() {

		FTPClient ftp = setupFTPClient("ftpuser1");
		final String name1 = "file1";
		final String name2 = "dir1";

		try (final Tx tx = app.tx()) {

			FTPFile[] files = ftp.listFiles();

			assertNotNull(files);
			assertEquals(0, files.length);

			// Create files by API methods
			createFTPFile(null, name1);

			tx.success();

		} catch (Exception ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			// Create folder in /
			createFTPDirectory(null, name2);
			tx.success();

		} catch (Exception ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			 // Move file to dir
			ftp.rename("/" + name1, "/" + name2 + "/" + name1);
			tx.success();

		} catch (Exception ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			ftp.changeWorkingDirectory("/" + name2);

			String[] fileNames = ftp.listNames();

			assertNotNull(fileNames);
			assertEquals(1, fileNames.length);
			assertEquals(name1, fileNames[0]);

			ftp.disconnect();

			tx.success();

		} catch (Exception ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	@Test
	public void test04MoveFileToRoot() {

		FTPClient ftp = setupFTPClient("ftpuser1");

		final String name1 = "file1";
		final String name2 = "dir1";

		try (final Tx tx = app.tx()) {

			FTPFile[] files = ftp.listFiles();

			assertNotNull(files);
			assertEquals(0, files.length);

			// Create files by API methods
			createFTPFile(null, name1);
			tx.success();

		} catch (Exception ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			// Create folder in /
			createFTPDirectory(null, name2);
			tx.success();

		} catch (Exception ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			// Move file to dir
			ftp.rename("/" + name1, "/" + name2 + "/" + name1);
			tx.success();

		} catch (Exception ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		String[] fileNames = null;
		try (final Tx tx = app.tx()) {

			ftp.changeWorkingDirectory("/" + name2);

			fileNames = ftp.listNames();

			assertNotNull(fileNames);
			assertEquals(1, fileNames.length);
			assertEquals(name1, fileNames[0]);

			// Move file back to /
			ftp.rename("/" + name2 + "/" + name1, "/" + name1);
			tx.success();

		} catch (Exception ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			ftp.changeWorkingDirectory("/");
			tx.success();

		} catch (Exception ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			fileNames = ftp.listNames();

			assertNotNull(fileNames);
			assertEquals(2, fileNames.length);
			assertEquals(name2, fileNames[0]);
			assertEquals(name1, fileNames[1]);

			ftp.disconnect();

			tx.success();

		} catch (Exception ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	@Test
	public void test05LargeFile() {

		final FTPClient ftp = setupFTPClient("ftpuser1");
		final String name1  = "file1";
		final int size      = 32768;

		try (final Tx tx = app.tx()) {

			FTPFile[] files = ftp.listFiles();

			assertNotNull(files);
			assertEquals(0, files.length);

			ftp.setFileType(FTP.ASCII_FILE_TYPE);
			ftp.setAutodetectUTF8(true);

			// Store a file
			InputStream in = IOUtils.toInputStream(new String(new byte[size], 0, size));
			ftp.storeFile(name1, in);

			in.close();

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			final FTPFile[] files = ftp.listFiles();

			assertNotNull(files);
			assertEquals(1, files.length);
			assertEquals(name1, files[0].getName());
			assertEquals(size, files[0].getSize());

			ftp.disconnect();

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	@Test
	public void test06OverwriteFile() {

		FTPClient ftp = setupFTPClient("ftpuser1");
		final String name1 = "file1";

		try (final Tx tx = app.tx()) {

			FTPFile[] files = ftp.listFiles();

			assertNotNull(files);
			assertEquals(0, files.length);

			ftp.setFileType(FTP.ASCII_FILE_TYPE);
			ftp.setAutodetectUTF8(true);

			// Store a file
			InputStream in = IOUtils.toInputStream("Initial Content");
			ftp.storeFile(name1, in);

			in.close();

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			// Store a file
			InputStream in = IOUtils.toInputStream("Overwritten Content");
			ftp.storeFile(name1, in);

			in.close();

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			final FTPFile[] files          = ftp.listFiles();

			assertNotNull(files);
			assertEquals(1, files.length);
			assertEquals(name1, files[0].getName());

			ftp.retrieveFile(files[0].getName(), os);

			final byte[] data    = os.toByteArray();
			final String content = new String(data, 0, data.length);

			assertEquals("Invalid content for overwritten file", "Overwritten Content", content);

			ftp.disconnect();

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	@Test
	public void test07DeleteFile() {

		final FTPClient ftp = setupFTPClient("ftpuser1");
		final String name1  = "file1";
		final int size      = 32768;

		// create file
		try (final Tx tx = app.tx()) {

			FTPFile[] files = ftp.listFiles();

			assertNotNull(files);
			assertEquals(0, files.length);

			ftp.setFileType(FTP.ASCII_FILE_TYPE);
			ftp.setAutodetectUTF8(true);

			// Store a file
			InputStream in = IOUtils.toInputStream(new String(new byte[size], 0, size));
			ftp.storeFile(name1, in);

			in.close();

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		// verify existance
		try (final Tx tx = app.tx()) {

			final FTPFile[] files = ftp.listFiles();

			assertNotNull(files);
			assertEquals(1, files.length);
			assertEquals(name1, files[0].getName());
			assertEquals(size, files[0].getSize());

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		// delete file
		try (final Tx tx = app.tx()) {

			ftp.deleteFile(name1);

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		// verify deletion
		try (final Tx tx = app.tx()) {

			final FTPFile[] files = ftp.listFiles();

			assertNotNull(files);
			assertEquals(0, files.length);

			ftp.disconnect();

			tx.success();

		} catch (IOException | FrameworkException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}
	}
}
