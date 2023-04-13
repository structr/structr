/*
 * Copyright (C) 2010-2023 Structr GmbH
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
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.Tx;
import org.structr.test.web.files.SSHTest;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.testng.annotations.Test;

import java.io.BufferedReader;
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
			assertEquals("Invalid result size for SSH root directory", 3, entries.size());

			final LsEntry currentDir      = entries.get(0);
			final LsEntry files           = entries.get(1);
			final LsEntry schema          = entries.get(2);

			// check names
			assertEquals("Invalid current directory name",    ".", currentDir.getFilename());
			assertEquals("Invalid files directory name",      "files", files.getFilename());
			assertEquals("Invalid schema directory name",     "schema", schema.getFilename());

			// check permissions
			assertEquals("Invalid permissions on . directory",          "dr--------", currentDir.getAttrs().getPermissionsString());
			assertEquals("Invalid permissions on files directory",      "drwxrwxr-x", files.getAttrs().getPermissionsString());
			assertEquals("Invalid permissions on schema directory",     "drwxrwxr-x", schema.getAttrs().getPermissionsString());

			// check flags (?)
			assertEquals("Invalid flags on . directory",          12, currentDir.getAttrs().getFlags());
			assertEquals("Invalid flags on files directory",      12, files.getAttrs().getFlags());
			assertEquals("Invalid flags on schema directory",     12, schema.getAttrs().getFlags());

			// check size
			assertEquals("Invalid size on . directory",          0, currentDir.getAttrs().getSize());
			assertEquals("Invalid size on files directory",      0, files.getAttrs().getSize());
			assertEquals("Invalid size on schema directory",     0, schema.getAttrs().getSize());

			final String date = getDateStringDependingOnCurrentDayOfMonth();

			// check string representation
			assertEquals("Invalid string representation of . directory",         "dr--------   1 superadmin superadmin        0 " + date + " .",          currentDir.getLongname());
			assertEquals("Invalid string representation of files directory",     "drwxrwxr-x   1 superadmin superadmin        0 " + date + " files",      files.getLongname());
			assertEquals("Invalid string representation of schema directory",    "drwxrwxr-x   1 superadmin superadmin        0 " + date + " schema",     schema.getLongname());

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

			final Vector files = sftp.ls("/files");

			assertNotNull(files);
			assertEquals(2, files.size());

			try (final OutputStream os = sftp.put("/files/" + name1)) {

				IOUtils.write(testContent1, os);
				os.flush();

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}

			try (final OutputStream os = sftp.put("/files/" + name2)) {

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

			final List<File> files = app.nodeQuery(File.class).sort(AbstractNode.name).getAsList();

			assertEquals("Invalid number of test files", 2, files.size());

			final File file1 = files.get(0);
			final File file2 = files.get(1);

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
			final Vector<LsEntry> entries = sftp.ls("/files");

			// listing contains "." and ".." => 4 entries
			assertEquals("Invalid result size for directory", 4, entries.size());

			final LsEntry file1 = entries.get(2);
			final LsEntry file2 = entries.get(3);

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

			try (final OutputStream os = sftp.put("/files/" + name1)) {

				IOUtils.write(testContent1, os);
				os.flush();

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}

			sftp.rename("/files/" + name1, "/files/" + name2);

		} catch (SftpException ex) {
			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try {
			final String date = getDateStringDependingOnCurrentDayOfMonth();
			final Vector<LsEntry> entries = sftp.ls("/files");

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
	public void test03MoveFile() {

		Settings.SSHPublicKeyOnly.setValue(false);

		ChannelSftp sftp          = setupSftpClient("ftpuser1", "ftpuserpw1", true);
		final String testContent1 = "Test Content öäü";
		final String name1        = "file1.txt";
		final String name2        = "fileöäüß.txt";

		String date = null;

		try {

			sftp.mkdir("/files/dir1");
			sftp.mkdir("/files/dir2");

			try (final OutputStream os = sftp.put("/files/dir1/" + name1)) {

				ByteArrayInputStream is = new ByteArrayInputStream(testContent1.getBytes());
				IOUtils.copy(is, os);
				os.flush();

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}

			sftp.rename("/files/dir1/" + name1, "/files/dir2/" + name2);
			date = getDateStringDependingOnCurrentDayOfMonth();

		} catch (SftpException ex) {

			logger.warn("", ex);
			fail("Unexpected exception: " + ex.getMessage());
		}

		try {
			final Vector<LsEntry> entries = sftp.ls("/files/dir2");

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

			final Vector files = sftp.ls("/files");

			assertNotNull(files);
			// listing contains "." and ".." => 3 entries
			assertEquals(2, files.size());

			try (final OutputStream os = sftp.put("/files/" + name)) {

				IOUtils.write(testContent1, os);
				os.flush();

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}

			try (final OutputStream os = sftp.put("/files/" + name)) {

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

			final List<File> files = app.nodeQuery(File.class).sort(AbstractNode.name).getAsList();

			assertEquals("Invalid number of test files", 1, files.size());

			final File file1 = files.get(0);

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
			final Vector<LsEntry> entries = sftp.ls("/files");

			// listing contains "." and ".." => 3 entries
			assertEquals("Invalid result size for directory", 3, entries.size());

			final LsEntry file1 = entries.get(2);

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

		final ChannelSftp sftp   = setupSftpClient("ftpuser1", "ftpuserpw1", true);
		final String testContent = "Test Content öäü";
		final String name        = "file1.txt";

		try {

			final Vector files = sftp.ls("/files");

			assertNotNull(files);
			assertEquals(2, files.size());

			try (final OutputStream os = sftp.put("/files/" + name)) {

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

			final List<File> files = app.nodeQuery(File.class).sort(AbstractNode.name).getAsList();

			assertEquals("Invalid number of test files", 1, files.size());

			final File file1 = files.get(0);

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

			sftp.rm("/files/" + name);

			sftp.disconnect();

		} catch (SftpException ex) {
			ex.printStackTrace();
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			final List<File> files = app.nodeQuery(File.class).sort(AbstractNode.name).getAsList();

			assertEquals("Invalid number of test files", 0, files.size());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	@Test
	public void test06DeleteDirectory() {

		Settings.SSHPublicKeyOnly.setValue(false);

		final ChannelSftp sftp   = setupSftpClient("ftpuser1", "ftpuserpw1", true);

		try {

			// create some dirs
			sftp.mkdir("/files/test1");
			sftp.mkdir("/files/test2");
			sftp.mkdir("/files/test2/nested1");
			sftp.mkdir("/files/test2/nested1/nested2");

			// delete one dir
			sftp.rmdir("/files/test2/nested1/nested2");

			// byebye
			sftp.disconnect();

		} catch (SftpException ex) {
			fail("Unexpected exception: " + ex.getMessage());
		}

		try (final Tx tx = app.tx()) {

			assertEquals("Folder test1 should exist", "test1", app.nodeQuery(Folder.class).andName("test1").sort(AbstractNode.name).getFirst().getName());
			assertEquals("Folder test2 should exist", "test2", app.nodeQuery(Folder.class).andName("test2").sort(AbstractNode.name).getFirst().getName());
			assertEquals("Folder nested1 should exist", "nested1", app.nodeQuery(Folder.class).andName("nested1").sort(AbstractNode.name).getFirst().getName());
			assertNull("Folder nested2 should have been deleted", app.nodeQuery(Folder.class).andName("nested2").sort(AbstractNode.name).getFirst());

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
