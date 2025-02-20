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

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.test.web.files.SSHTest;
import org.structr.web.entity.File;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.testng.AssertJUnit.*;

/**
 * Tests for the SSH file interface.
 *
 *
 */
public class SSHFilesTest extends SSHTest {

	private static final Logger logger = LoggerFactory.getLogger(SSHFilesTest.class.getName());

	@Test
	public void test00RootDirectoriesAndAttributes() {

		Settings.SSHPublicKeyOnly.setValue(false);

		final ChannelSftp sftp = setupSftpClient("ftpuser1", "ftpuserpw1", true);

		try {

			final Vector<LsEntry> entries = sftp.ls("/");

			// listing contains "." => 5 entries
			assertEquals("Invalid result size for SSH root directory", 1, entries.size());

			final LsEntry currentDir      = entries.get(0);

			// check names
			assertEquals("Invalid current directory name",    ".", currentDir.getFilename());

			// check permissions
			assertEquals("Invalid permissions on . directory",          "drwxrwxr-x", currentDir.getAttrs().getPermissionsString());

			// check flags (?)
			assertEquals("Invalid flags on . directory",          12, currentDir.getAttrs().getFlags());

			// check size
			assertEquals("Invalid size on . directory",          0, currentDir.getAttrs().getSize());

			final String date = getDateStringDependingOnCurrentDayOfMonth();

			// check string representation
			assertEquals("Invalid string representation of . directory",         "drwxrwxr-x   1 superadmin superadmin        0 " + date + " .",          currentDir.getLongname());

			sftp.disconnect();

		} catch (SftpException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

	}

	@Test
	public void test00StoreFile() {

		Settings.SSHPublicKeyOnly.setValue(false);

		ChannelSftp sftp          = setupSftpClient("ftpuser1", "ftpuserpw1", true);
		final String testContent1 = "Test Content öäü";
		final String testContent2 = "Test Content 2";
		final String name1        = "file1.txt";
		final String name2        = "fileöäüß.txt";

		try {

			final Vector files = sftp.ls("/");

			assertNotNull(files);
			assertEquals(1, files.size());

			try (final OutputStream os = sftp.put("/" + name1)) {

				IOUtils.write(testContent1, os);
				os.flush();

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}

			try (final OutputStream os = sftp.put("/" + name2)) {

				IOUtils.write(testContent2, os);
				os.flush();

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}

		} catch (SftpException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			final PropertyKey<String> nameKey = Traits.of("File").key("name");
			final List<NodeInterface> files   = app.nodeQuery("File").sort(nameKey).getAsList();

			assertEquals("Invalid number of test files", 2, files.size());

			final File file1 = files.get(0).as(File.class);
			final File file2 = files.get(1).as(File.class);

			assertEquals("Invalid test file name", name1, file1.getName());
			assertEquals("Invalid test file name", name2, file2.getName());

			try {
				assertEquals("Invalid test file content", testContent1, IOUtils.toString(file1.getInputStream()));
				assertEquals("Invalid test file content", testContent2, IOUtils.toString(file2.getInputStream()));

			} catch (IOException ioex) {
				fail("Unexpected exception: " + ioex.getMessage());
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception: " + fex.getMessage());
		}

		try {
			final Vector<LsEntry> entries = sftp.ls("/");

			// listing contains "." and ".." => 4 entries
			assertEquals("Invalid result size for directory", 3, entries.size());

			final LsEntry file1 = entries.get(1);
			final LsEntry file2 = entries.get(2);

			// check names
			assertEquals("Invalid test file name", name1, file1.getFilename());
			assertEquals("Invalid test file name", name2, file2.getFilename());

			// check permissions
			assertEquals("Invalid permissions on test file", "-rw-------", file1.getAttrs().getPermissionsString());
			assertEquals("Invalid permissions on test file", "-rw-------", file2.getAttrs().getPermissionsString());

			// check flags (?)
			assertEquals("Invalid flags on test file", 13, file1.getAttrs().getFlags());
			assertEquals("Invalid flags on test file", 13, file2.getAttrs().getFlags());

			// check size
			assertEquals("Invalid test file size", 19, file1.getAttrs().getSize());
			assertEquals("Invalid test file size", 14, file2.getAttrs().getSize());

			final String date = getDateStringDependingOnCurrentDayOfMonth();

			// check string representation
			assertEquals("Invalid string representation of test file",         "-rw-------   1 ftpuser1 ftpuser1       19 " + date + " " + name1, file1.getLongname());
			assertEquals("Invalid string representation of test file",         "-rw-------   1 ftpuser1 ftpuser1       14 " + date + " " + name2, file2.getLongname());

			sftp.disconnect();

		} catch (SftpException ex) {
			ex.printStackTrace();
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	@Test
	public void test02RenameFile() {

		Settings.SSHPublicKeyOnly.setValue(false);

		ChannelSftp sftp          = setupSftpClient("ftpuser1", "ftpuserpw1", true);
		final String testContent1 = "Test Content öäü";
		final String name1        = "file1.txt";
		final String name2        = "fileöäüß.txt";

		try {

			try (final OutputStream os = sftp.put("/" + name1)) {

				IOUtils.write(testContent1, os);
				os.flush();

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}

			sftp.rename("/" + name1, "/" + name2);

		} catch (SftpException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try {
			final String date = getDateStringDependingOnCurrentDayOfMonth();
			final Vector<LsEntry> entries = sftp.ls("/");

			// listing contains "." and ".." => 2 entries
			assertEquals("Invalid result size for directory", 2, entries.size());

			final LsEntry file = entries.get(1);

			// check attributes
			assertEquals("Invalid test file name", name2, file.getFilename());
			assertEquals("Invalid permissions on test file", "-rw-------", file.getAttrs().getPermissionsString());
			assertEquals("Invalid flags on test file", 13, file.getAttrs().getFlags());
			assertEquals("Invalid test file size", 19, file.getAttrs().getSize());
			assertEquals("Invalid string representation of test file",         "-rw-------   1 ftpuser1 ftpuser1       19 " + date + " " + name2, file.getLongname());

			sftp.disconnect();

		} catch (SftpException ex) {
			ex.printStackTrace();
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	@Test
	public void test03MoveFile() {

		Settings.SSHPublicKeyOnly.setValue(false);

		ChannelSftp sftp          = setupSftpClient("ftpuser1", "ftpuserpw1", true);
		final String testContent1 = "Test Content öäü";
		final String name1        = "file1.txt";
		final String name2        = "fileöäüß.txt";

		String date = null;

		try {

			sftp.mkdir("/dir1");
			sftp.mkdir("/dir2");

			try (final OutputStream os = sftp.put("/dir1/" + name1)) {

				ByteArrayInputStream is = new ByteArrayInputStream(testContent1.getBytes());
				IOUtils.copy(is, os);
				os.flush();

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}

			sftp.rename("/dir1/" + name1, "/dir2/" + name2);
			date = getDateStringDependingOnCurrentDayOfMonth();

		} catch (SftpException ex) {

			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try {
			final Vector<LsEntry> entries = sftp.ls("/dir2");

			// listing contains "." and ".." => 3 entries
			assertEquals("Invalid result size for directory", 3, entries.size());

			final LsEntry file = entries.get(2);

			// check attributes
			assertEquals("Invalid test file name", name2, file.getFilename());
			assertEquals("Invalid permissions on test file", "-rw-------", file.getAttrs().getPermissionsString());
			assertEquals("Invalid flags on test file", 13, file.getAttrs().getFlags());
			assertEquals("Invalid test file size", 19, file.getAttrs().getSize());
			assertEquals("Invalid string representation of test file",         "-rw-------   1 ftpuser1 ftpuser1       19 " + date + " " + name2, file.getLongname());

			sftp.disconnect();

		} catch (SftpException ex) {
			ex.printStackTrace();
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	@Test
	public void test04OverwriteFile() {

		Settings.SSHPublicKeyOnly.setValue(false);

		ChannelSftp sftp          = setupSftpClient("ftpuser1", "ftpuserpw1", true);
		final String testContent1 = "Initial Content";
		final String testContent2 = "Overwritten Content";
		final String name         = "file1.txt";

		try {

			final Vector files = sftp.ls("/");

			assertNotNull(files);
			// listing contains "." => 1 entry
			assertEquals(1, files.size());

			try (final OutputStream os = sftp.put("/" + name)) {

				IOUtils.write(testContent1, os);
				os.flush();

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}

			try (final OutputStream os = sftp.put("/" + name)) {

				IOUtils.write(testContent2, os);
				os.flush();

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}

		} catch (SftpException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			final PropertyKey<String> nameKey = Traits.of("File").key("name");
			final List<NodeInterface> files   = app.nodeQuery("File").sort(nameKey).getAsList();

			assertEquals("Invalid number of test files", 1, files.size());

			final File file1 = files.get(0).as(File.class);

			assertEquals("Invalid test file name", name, file1.getName());

			try {
				assertEquals("Invalid test file content", testContent2, IOUtils.toString(file1.getInputStream()));

			} catch (IOException ioex) {
				fail("Unexpected exception: " + ioex.getMessage());
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception: " + fex.getMessage());
		}

		try {
			final Vector<LsEntry> entries = sftp.ls("/");

			// listing contains "." and file => 2 entries
			assertEquals("Invalid result size for directory", 2, entries.size());

			final LsEntry file1 = entries.get(1);

			// check names
			assertEquals("Invalid test file name", name, file1.getFilename());

			// check permissions
			assertEquals("Invalid permissions on test file", "-rw-------", file1.getAttrs().getPermissionsString());

			// check flags (?)
			assertEquals("Invalid flags on test file", 13, file1.getAttrs().getFlags());

			// check size
			assertEquals("Invalid test file size", 19, file1.getAttrs().getSize());

			final String date = getDateStringDependingOnCurrentDayOfMonth();

			// check string representation
			assertEquals("Invalid string representation of test file",         "-rw-------   1 ftpuser1 ftpuser1       19 " + date + " " + name, file1.getLongname());

			sftp.disconnect();

		} catch (SftpException ex) {
			ex.printStackTrace();
			fail("Unexpected exception: " + ex.getMessage());
		}
	}

	@Test
	public void test05DeleteFile() {

		Settings.SSHPublicKeyOnly.setValue(false);

		final ChannelSftp sftp            = setupSftpClient("ftpuser1", "ftpuserpw1", true);
		final PropertyKey<String> nameKey = Traits.of("File").key("name");
		final String testContent          = "Test Content öäü";
		final String name                 = "file1.txt";

		try {

			final Vector files = sftp.ls("/");

			assertNotNull(files);
			assertEquals(1, files.size());

			try (final OutputStream os = sftp.put("/" + name)) {

				IOUtils.write(testContent, os);
				os.flush();

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}

		} catch (SftpException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> files = app.nodeQuery("File").sort(nameKey).getAsList();

			assertEquals("Invalid number of test files", 1, files.size());

			final File file1 = files.get(0).as(File.class);

			assertEquals("Invalid test file name", name, file1.getName());

			try {
				assertEquals("Invalid test file content", testContent, IOUtils.toString(file1.getInputStream()));

			} catch (IOException ioex) {
				fail("Unexpected exception: " + ioex.getMessage());
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception: " + fex.getMessage());
		}

		try {

			sftp.rm("/" + name);

			sftp.disconnect();

		} catch (SftpException ex) {
			ex.printStackTrace();
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> files = app.nodeQuery("File").sort(nameKey).getAsList();

			assertEquals("Invalid number of test files", 0, files.size());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void test06DeleteDirectory() {

		Settings.SSHPublicKeyOnly.setValue(false);

		final ChannelSftp sftp            = setupSftpClient("ftpuser1", "ftpuserpw1", true);
		final PropertyKey<String> nameKey = Traits.of("File").key("name");

		try {

			// create some dirs
			sftp.mkdir("/test1");
			sftp.mkdir("/test2");
			sftp.mkdir("/test2/nested1");
			sftp.mkdir("/test2/nested1/nested2");

			// delete one dir
			sftp.rmdir("/test2/nested1/nested2");

			// byebye
			sftp.disconnect();

		} catch (SftpException ex) {
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			assertEquals("Folder test1 should exist", "test1", app.nodeQuery("Folder").andName("test1").sort(nameKey).getFirst().getName());
			assertEquals("Folder test2 should exist", "test2", app.nodeQuery("Folder").andName("test2").sort(nameKey).getFirst().getName());
			assertEquals("Folder nested1 should exist", "nested1", app.nodeQuery("Folder").andName("nested1").sort(nameKey).getFirst().getName());
			assertNull("Folder nested2 should have been deleted", app.nodeQuery("Folder").andName("nested2").sort(nameKey).getFirst());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	// ----- private methods -----
	private String getDateStringDependingOnCurrentDayOfMonth() {

		final Calendar cal = GregorianCalendar.getInstance();

		if (cal.get(Calendar.DAY_OF_MONTH) < 10) {

			return new SimpleDateFormat("MMM  d HH:mm", Locale.ENGLISH).format(System.currentTimeMillis());
		}

		return new SimpleDateFormat("MMM dd HH:mm", Locale.ENGLISH).format(System.currentTimeMillis());
	}
}
