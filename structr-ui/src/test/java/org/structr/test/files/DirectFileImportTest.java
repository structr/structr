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
package org.structr.test.files;

import org.apache.commons.io.IOUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.maintenance.DirectFileImportCommand;
import org.testng.annotations.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.testng.Assert.assertNotEquals;
import static org.testng.AssertJUnit.*;


/**
 */
public class DirectFileImportTest extends StructrUiTest {

	@Test
	public void testDirectFileImportErrors() {

		try  {

			final Path testDir             = Files.createTempDirectory(Paths.get(basePath), "directFileImportTest");
			final String nonexistingPath   = "/this-path-can-not-exist-" + System.currentTimeMillis() + "/structr/tmp";
			final Path existingButFilePath = testDir.resolve("existingFile.txt");

			// create some files
			testDir.resolve("file.txt").toFile().createNewFile();
			existingButFilePath.toFile().createNewFile();

			// test empty map
			try (final Tx tx = app.tx()) {

				app.command(DirectFileImportCommand.class).execute(setupParameters(null, null, null, null, null));
				fail("Direct file import command should not accept empty parameter map.");

			} catch (FrameworkException ex) {
				checkException(ex, 422, "Please provide 'source' attribute for deployment source directory path.");
			}

			// test missing source path
			try (final Tx tx = app.tx()) {

				app.command(DirectFileImportCommand.class).execute(setupParameters(null, "/", "copy", "skip", false));
				fail("Direct file import command should not accept empty source directory");

			} catch (FrameworkException ex) {
				checkException(ex, 422, "Please provide 'source' attribute for deployment source directory path.");
			}

			// test nonexisting source path
			try (final Tx tx = app.tx()) {

				app.command(DirectFileImportCommand.class).execute(setupParameters(nonexistingPath, "/", "copy", "skip", false));
				fail("Direct file import command should not accept empty source directory");

			} catch (FrameworkException ex) {
				checkException(ex, 422, "Source path " + nonexistingPath + " does not exist.");
			}

			// test invalid value for mode
			try (final Tx tx = app.tx()) {

				app.command(DirectFileImportCommand.class).execute(setupParameters(testDir.toString(), "/", "invalid", "skip", false));
				fail("Direct file import command should not accept invalid 'mode' value");

			} catch (FrameworkException ex) {
				checkException(ex, 422, "Unknown value for 'mode' attribute. Valid values are: copy, move");
			}

			// test invalid value for skip
			try (final Tx tx = app.tx()) {

				app.command(DirectFileImportCommand.class).execute(setupParameters(testDir.toString(), "/", "copy", "invalid", false));
				fail("Direct file import command should not accept invalid 'existing' value");

			} catch (FrameworkException ex) {
				checkException(ex, 422, "Unknown value for 'existing' attribute. Valid values are: skip, overwrite, rename");
			}

		} catch (IOException ioex) {
			fail("Unable to create files");
		}
	}

	@Test
	public void testDirectFileImportWithRelativeDirectory() {

		final PropertyKey<String> pathKey = Traits.of("File").key("path");
		final String dirName              = "directFileImportTestRelativeDirectory";
		final Path base                   = Paths.get(basePath);
		Path testDir                      = null;

		try {

			testDir = Files.createDirectory(base.resolve(dirName));

			createTestFile(testDir.resolve(Paths.get("test1.txt")), "test file content 1");
			createTestFile(testDir.resolve(Paths.get("test2.txt")), "test file content 2");
			createTestFile(testDir.resolve(Paths.get("test3.txt")), "test file content 3");

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		// do import
		try (final Tx tx = app.tx()) {

			app.command(DirectFileImportCommand.class).execute(setupParameters(dirName, "/", "COPY", "OVERWRITE", false));
			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}


		// verify successful file import
		try (final Tx tx = app.tx()) {

			assertNotNull("Folder should have been created by import", app.nodeQuery("Folder").and(pathKey, "/" + testDir.getFileName().toString()).sort(Traits.of("NodeInterface").key("name")).getFirst());

			final File file1 = app.nodeQuery("File").and(pathKey, "/" + testDir.getFileName().toString() + "/test1.txt").sort(Traits.of("NodeInterface").key("name")).getFirst().as(File.class);
			final File file2 = app.nodeQuery("File").and(pathKey, "/" + testDir.getFileName().toString() + "/test2.txt").sort(Traits.of("NodeInterface").key("name")).getFirst().as(File.class);
			final File file3 = app.nodeQuery("File").and(pathKey, "/" + testDir.getFileName().toString() + "/test3.txt").sort(Traits.of("NodeInterface").key("name")).getFirst().as(File.class);

			assertNotNull("Test file should have been created by import", file1);
			assertNotNull("Test file should have been created by import", file2);
			assertNotNull("Test file should have been created by import", file3);

			assertEquals("Imported test file content does not match source", "test file content 1", getContent(file1));
			assertEquals("Imported test file content does not match source", "test file content 2", getContent(file2));
			assertEquals("Imported test file content does not match source", "test file content 3", getContent(file3));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}


	@Test
	public void testDirectFileImportWithAbsoluteDirectory() {

		final PropertyKey<String> pathKey = Traits.of("File").key("path");
		Path testDir      = null;
		String importPath = null;

		try {

			testDir = Files.createTempDirectory(Paths.get(basePath), "directFileImportTestAbsoluteDirectory");

			createTestFile(testDir.resolve(Paths.get("test1.txt")), "test file content 1");
			createTestFile(testDir.resolve(Paths.get("test2.txt")), "test file content 2");
			createTestFile(testDir.resolve(Paths.get("test3.txt")), "test file content 3");

			importPath = testDir.toString();

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		// do import
		final Map<String, Object> attributes = new LinkedHashMap<>();

		attributes.put("source", importPath);

		try (final Tx tx = app.tx()) {

			app.command(DirectFileImportCommand.class).execute(attributes);
			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}


		// verify successful file import
		try (final Tx tx = app.tx()) {

			assertNotNull("Folder should have been created by import", app.nodeQuery("Folder").and(pathKey, "/" + testDir.getFileName().toString()).sort(Traits.of("NodeInterface").key("name")).getFirst());

			final File file1 = app.nodeQuery("File").and(pathKey, "/" +  testDir.getFileName().toString() + "/test1.txt").sort(Traits.of("NodeInterface").key("name")).getFirst().as(File.class);
			final File file2 = app.nodeQuery("File").and(pathKey, "/" +  testDir.getFileName().toString() + "/test2.txt").sort(Traits.of("NodeInterface").key("name")).getFirst().as(File.class);
			final File file3 = app.nodeQuery("File").and(pathKey, "/" +  testDir.getFileName().toString() + "/test3.txt").sort(Traits.of("NodeInterface").key("name")).getFirst().as(File.class);

			assertNotNull("Test file should have been created by import", file1);
			assertNotNull("Test file should have been created by import", file2);
			assertNotNull("Test file should have been created by import", file3);

			assertEquals("Imported test file content does not match source", "test file content 1", getContent(file1));
			assertEquals("Imported test file content does not match source", "test file content 2", getContent(file2));
			assertEquals("Imported test file content does not match source", "test file content 3", getContent(file3));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testDirectFileImportWithExistingFileCopySkip() {

		/**
		 * Create test file prior to import, expect file to be unchanged after import
		 * because of import mode "SKIP".
		 */

		try (final Tx tx = app.tx()) {

			FileHelper.createFile(securityContext, "initial content".getBytes("utf-8"), "text/plain", "File", "test.txt", true);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		Path testDir    = null;
		Path importPath = null;

		try {

			testDir    = Files.createTempDirectory(Paths.get(basePath), "directFileImportTestFileCopySkip");
			importPath = testDir.resolve(Paths.get("test.txt"));

			createTestFile(importPath, "test file content 1");

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		try (final Tx tx = app.tx()) {

			app.command(DirectFileImportCommand.class).execute(setupParameters(importPath.toString(), "/", "COPY", "SKIP", false));
			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}


		// verify successful file import
		try (final Tx tx = app.tx()) {

			final List<NodeInterface> files = app.nodeQuery("File").sort(Traits.of("NodeInterface").key("name")).getAsList();

			// import mode SKIP => no change, no additional file
			assertEquals("Only one file should exist after import", 1, files.size());

			final File file = files.get(0).as(File.class);

			assertNotNull("Test file should exist", file);
			assertEquals("Test file name should be test.txt", "test.txt", file.getName());
			assertNull("Test file should NOT have a parent folder", file.getParent());
			assertEquals("Test file content should not be modified by import", "initial content", getContent(file));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		// verify source file exists after import
		assertTrue("Source file should not be deleted after import", importPath.toFile().exists());
	}

	@Test
	public void testDirectFileImportWithNonexistingFileCopySkip() {

		Path testDir    = null;
		Path importPath = null;

		try {

			testDir    = Files.createTempDirectory(Paths.get(basePath), "directFileImportTestFileCopySkip");
			importPath = testDir.resolve(Paths.get("test.txt"));

			createTestFile(importPath, "test file content 1");

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		try (final Tx tx = app.tx()) {

			app.command(DirectFileImportCommand.class).execute(setupParameters(importPath.toString(), "/", "COPY", "SKIP", false));
			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}


		// verify successful file import
		try (final Tx tx = app.tx()) {

			final List<NodeInterface> files = app.nodeQuery("File").sort(Traits.of("NodeInterface").key("name")).getAsList();

			// import mode SKIP => no change, no additional file
			assertEquals("Only one file should exist after import", 1, files.size());

			final File file = files.get(0).as(File.class);

			assertNotNull("Test file should have been created by import", file);
			assertEquals("Test file name should be test.txt", "test.txt", file.getName());
			assertNull("Test file should NOT have a parent folder", file.getParent());
			assertEquals("Test file content does not match source", "test file content 1", getContent(file));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		// verify source file exists after import
		assertTrue("Source file should not be deleted after import", importPath.toFile().exists());
	}

	@Test
	public void testDirectFileImportWithExistingFileCopyOverwrite() {

		/**
		 * Create test file prior to import, expect file to be overwritten after import
		 * because of import mode "OVERWRITE".
		 */

		try (final Tx tx = app.tx()) {

			FileHelper.createFile(securityContext, "initial content".getBytes("utf-8"), "text/plain", "File", "test.txt", true);

			tx.success();

		} catch (FrameworkException | IOException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		Path testDir    = null;
		Path importPath = null;

		try {

			testDir    = Files.createTempDirectory(Paths.get(basePath), "directFileImportTestFileCopyOverwrite");
			importPath = testDir.resolve(Paths.get("test.txt"));

			createTestFile(importPath, "test file content 1");

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		try (final Tx tx = app.tx()) {

			app.command(DirectFileImportCommand.class).execute(setupParameters(importPath.toString(), "/", "COPY", "OVERWRITE", false));
			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}


		// verify successful file import
		try (final Tx tx = app.tx()) {

			final List<NodeInterface> files = app.nodeQuery("File").sort(Traits.of("NodeInterface").key("name")).getAsList();

			// import mode SKIP => no change, no additional file
			assertEquals("Only one file should exist after import", 1, files.size());

			final File file = files.get(0).as(File.class);

			assertNotNull("Test file should have been created by import", file);
			assertEquals("Test file name should be test.txt", "test.txt", file.getName());
			assertNull("Test file should NOT have a parent folder", file.getParent());
			assertEquals("Test file content does not match source", "test file content 1", getContent(file));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		// verify source file exists after import
		assertTrue("Source file should not be deleted after import", importPath.toFile().exists());
	}

	@Test
	public void testDirectFileImportWithNonexistingFileCopyOverwrite() {

		/**
		 * Create test file prior to import, expect file to be overwritten after import
		 * because of import mode "OVERWRITE".
		 */

		Path testDir    = null;
		Path importPath = null;

		try {

			testDir    = Files.createTempDirectory(Paths.get(basePath), "directFileImportTestFileCopyOverwrite");
			importPath = testDir.resolve(Paths.get("test.txt"));

			createTestFile(importPath, "test file content 1");

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		try (final Tx tx = app.tx()) {

			app.command(DirectFileImportCommand.class).execute(setupParameters(importPath.toString(), "/", "COPY", "OVERWRITE", false));
			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}


		// verify successful file import
		try (final Tx tx = app.tx()) {

			final List<NodeInterface> files = app.nodeQuery("File").sort(Traits.of("NodeInterface").key("name")).getAsList();

			// import mode SKIP => no change, no additional file
			assertEquals("Only one file should exist after import", 1, files.size());

			final File file = files.get(0).as(File.class);

			assertNotNull("Test file should have been created by import", file);
			assertEquals("Test file name should be test.txt", "test.txt", file.getName());
			assertNull("Test file should NOT have a parent folder", file.getParent());
			assertEquals("Test file content does not match source", "test file content 1", getContent(file));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		// verify source file exists after import
		assertTrue("Source file should not be deleted after import", importPath.toFile().exists());
	}

	@Test
	public void testDirectFileImportWithExistingFileCopyRename() {

		/**
		 * Create test file prior to import, expect file to be renamed after import
		 * because of import mode "RENAME".
		 */

		try (final Tx tx = app.tx()) {

			FileHelper.createFile(securityContext, "initial content".getBytes("utf-8"), "text/plain", "File", "test.txt", true);

			tx.success();

		} catch (FrameworkException | IOException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		Path testDir    = null;
		Path importPath = null;

		try {

			testDir    = Files.createTempDirectory(Paths.get(basePath), "directFileImportTestFileCopyRename");
			importPath = testDir.resolve(Paths.get("test.txt"));

			createTestFile(importPath, "test file content 1");

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		try (final Tx tx = app.tx()) {

			app.command(DirectFileImportCommand.class).execute(setupParameters(importPath.toString(), "/", "COPY", "RENAME", false));
			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}


		// verify successful file import
		try (final Tx tx = app.tx()) {

			final List<NodeInterface> files = app.nodeQuery("File").sort(Traits.of("NodeInterface").key("name")).getAsList();

			// import mode SKIP => no change, no additional file
			assertEquals("Two files should exist after import", 2, files.size());

			final File file1 = files.get(0).as(File.class);
			final File file2 = files.get(1).as(File.class);

			assertNotNull("Test file should have been created by import", file1);
			assertEquals("Test file name should be test.txt", "test.txt", file1.getName());
			assertNull("Test file should NOT have a parent folder", file1.getParent());
			assertEquals("Test file content does not match source", "test file content 1", getContent(file1));

			assertNotNull("Test file should have been created by import", file2);
			assertNotEquals("Existing file should be renamed", "test.txt", file2.getName());
			assertNull("Test file should NOT have a parent folder", file2.getParent());
			assertEquals("Test file content does not match source", "initial content", getContent(file2));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		// verify source file exists after import
		assertTrue("Source file should not be deleted after import", importPath.toFile().exists());
	}

	@Test
	public void testDirectFileImportWithNonexistingFileCopyRename() {

		Path testDir    = null;
		Path importPath = null;

		try {

			testDir    = Files.createTempDirectory(Paths.get(basePath), "directFileImportTestFileCopyRename");
			importPath = testDir.resolve(Paths.get("test.txt"));

			createTestFile(importPath, "test file content 1");

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		try (final Tx tx = app.tx()) {

			app.command(DirectFileImportCommand.class).execute(setupParameters(importPath.toString(), "/", "COPY", "RENAME", false));
			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}


		// verify successful file import
		try (final Tx tx = app.tx()) {

			final List<NodeInterface> files = app.nodeQuery("File").sort(Traits.of("NodeInterface").key("name")).getAsList();

			// import mode SKIP => no change, no additional file
			assertEquals("Two files should exist after import", 1, files.size());

			final File file1 = files.get(0).as(File.class);

			assertNotNull("Test file should have been created by import", file1);
			assertEquals("Test file name should be test.txt", "test.txt", file1.getName());
			assertNull("Test file should NOT have a parent folder", file1.getParent());
			assertEquals("Test file content does not match source", "test file content 1", getContent(file1));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		// verify source file exists after import
		assertTrue("Source file should not be deleted after import", importPath.toFile().exists());
	}

	@Test
	public void testDirectFileImportWithExistingFileMoveSkip() {

		/**
		 * Create test file prior to import, expect file to be unchanged after import
		 * because of import mode "SKIP".
		 */

		try (final Tx tx = app.tx()) {

			FileHelper.createFile(securityContext, "initial content".getBytes("utf-8"), "text/plain", "File", "test.txt", true);

			tx.success();

		} catch (FrameworkException | IOException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		Path testDir    = null;
		Path importPath = null;

		try {

			testDir    = Files.createTempDirectory(Paths.get(basePath), "directFileImportTestFileMoveSkip");
			importPath = testDir.resolve(Paths.get("test.txt"));

			createTestFile(importPath, "test file content 1");

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		try (final Tx tx = app.tx()) {

			app.command(DirectFileImportCommand.class).execute(setupParameters(importPath.toString(), "/", "MOVE", "SKIP", false));
			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}


		// verify successful file import
		try (final Tx tx = app.tx()) {

			final List<NodeInterface> files = app.nodeQuery("File").sort(Traits.of("NodeInterface").key("name")).getAsList();

			// import mode SKIP => no change, no additional file
			assertEquals("Only one file should exist after import", 1, files.size());

			final File file = files.get(0).as(File.class);

			assertNotNull("Test file should exist", file);
			assertEquals("Test file name should be test.txt", "test.txt", file.getName());
			assertNull("Test file should NOT have a parent folder", file.getParent());
			assertEquals("Test file content should not be modified by import", "initial content", getContent(file));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		// verify source file is not deleted after import because source file was skipped
		assertTrue("Source file should not be deleted after import because it was skipped", importPath.toFile().exists());
	}

	@Test
	public void testDirectFileImportWithNonexistingFileMoveSkip() {

		Path testDir    = null;
		Path importPath = null;

		try {

			testDir    = Files.createTempDirectory(Paths.get(basePath), "directFileImportTestFileMoveSkip");
			importPath = testDir.resolve(Paths.get("test.txt"));

			createTestFile(importPath, "test file content 1");

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		try (final Tx tx = app.tx()) {

			app.command(DirectFileImportCommand.class).execute(setupParameters(importPath.toString(), "/", "MOVE", "SKIP", false));
			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}


		// verify successful file import
		try (final Tx tx = app.tx()) {

			final List<NodeInterface> files = app.nodeQuery("File").sort(Traits.of("NodeInterface").key("name")).getAsList();

			// import mode SKIP => no change, no additional file
			assertEquals("Only one file should exist after import", 1, files.size());

			final File file = files.get(0).as(File.class);

			assertNotNull("Test file should have been created by import", file);
			assertEquals("Test file name should be test.txt", "test.txt", file.getName());
			assertNull("Test file should NOT have a parent folder", file.getParent());
			assertEquals("Test file content does not match source", "test file content 1", getContent(file));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		// verify source file is deleted after import
		assertFalse("Source file should be deleted after import", importPath.toFile().exists());
	}

	@Test
	public void testDirectFileImportWithExistingFileMoveOverwrite() {

		/**
		 * Create test file prior to import, expect file to be overwritten after import
		 * because of import mode "OVERWRITE".
		 */

		try (final Tx tx = app.tx()) {

			FileHelper.createFile(securityContext, "initial content".getBytes("utf-8"), "text/plain", "File", "test.txt", true);

			tx.success();

		} catch (FrameworkException | IOException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		Path testDir    = null;
		Path importPath = null;

		try {

			testDir    = Files.createTempDirectory(Paths.get(basePath), "directFileImportTestFileMoveOverwrite");
			importPath = testDir.resolve(Paths.get("test.txt"));

			createTestFile(importPath, "test file content 1");

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		try (final Tx tx = app.tx()) {

			app.command(DirectFileImportCommand.class).execute(setupParameters(importPath.toString(), "/", "MOVE", "OVERWRITE", false));
			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}


		// verify successful file import
		try (final Tx tx = app.tx()) {

			final List<NodeInterface> files = app.nodeQuery("File").sort(Traits.of("NodeInterface").key("name")).getAsList();

			// import mode SKIP => no change, no additional file
			assertEquals("Only one file should exist after import", 1, files.size());

			final File file = files.get(0).as(File.class);

			assertNotNull("Test file should have been created by import", file);
			assertEquals("Test file name should be test.txt", "test.txt", file.getName());
			assertNull("Test file should NOT have a parent folder", file.getParent());
			assertEquals("Test file content does not match source", "test file content 1", getContent(file));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		// verify source file is deleted after import
		assertFalse("Source file should be deleted after import", importPath.toFile().exists());
	}

	@Test
	public void testDirectFileImportWithNonexistingFileMoveOverwrite() {

		/**
		 * Create test file prior to import, expect file to be overwritten after import
		 * because of import mode "OVERWRITE".
		 */

		Path testDir    = null;
		Path importPath = null;

		try {

			testDir    = Files.createTempDirectory(Paths.get(basePath), "directFileImportTestFileMoveOverwrite");
			importPath = testDir.resolve(Paths.get("test.txt"));

			createTestFile(importPath, "test file content 1");

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		try (final Tx tx = app.tx()) {

			app.command(DirectFileImportCommand.class).execute(setupParameters(importPath.toString(), "/", "MOVE", "OVERWRITE", false));
			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}


		// verify successful file import
		try (final Tx tx = app.tx()) {

			final List<NodeInterface> files = app.nodeQuery("File").sort(Traits.of("NodeInterface").key("name")).getAsList();

			// import mode SKIP => no change, no additional file
			assertEquals("Only one file should exist after import", 1, files.size());

			final File file = files.get(0).as(File.class);

			assertNotNull("Test file should have been created by import", file);
			assertEquals("Test file name should be test.txt", "test.txt", file.getName());
			assertNull("Test file should NOT have a parent folder", file.getParent());
			assertEquals("Test file content does not match source", "test file content 1", getContent(file));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		// verify source file is deleted after import
		assertFalse("Source file should be deleted after import", importPath.toFile().exists());
	}

	@Test
	public void testDirectFileImportWithExistingFileMoveRename() {

		/**
		 * Create test file prior to import, expect file to be renamed after import
		 * because of import mode "RENAME".
		 */

		try (final Tx tx = app.tx()) {

			FileHelper.createFile(securityContext, "initial content".getBytes("utf-8"), "text/plain", "File", "test.txt", true);

			tx.success();

		} catch (FrameworkException | IOException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		Path testDir    = null;
		Path importPath = null;

		try {

			testDir    = Files.createTempDirectory(Paths.get(basePath), "directFileImportTestFileMoveRename");
			importPath = testDir.resolve(Paths.get("test.txt"));

			createTestFile(importPath, "test file content 1");

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		try (final Tx tx = app.tx()) {

			app.command(DirectFileImportCommand.class).execute(setupParameters(importPath.toString(), "/", "MOVE", "RENAME", false));
			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}


		// verify successful file import
		try (final Tx tx = app.tx()) {

			final List<NodeInterface> files = app.nodeQuery("File").sort(Traits.of("NodeInterface").key("name")).getAsList();

			// import mode SKIP => no change, no additional file
			assertEquals("Two files should exist after import", 2, files.size());

			System.out.println(files.stream().map(f -> f.getName()).collect(Collectors.toList()));

			final File file1 = files.get(0).as(File.class);
			final File file2 = files.get(1).as(File.class);

			assertNotNull("Test file should have been created by import", file1);
			assertEquals("Test file name should be test.txt", "test.txt", file1.getName());
			assertNull("Test file should NOT have a parent folder", file1.getParent());
			assertEquals("Test file content does not match source", "test file content 1", getContent(file1));


			assertNotNull("Test file should have been created by import", file2);
			assertNotEquals("Existing file should be renamed", "test.txt", file2.getName());
			assertNull("Test file should NOT have a parent folder", file2.getParent());
			assertEquals("Test file content does not match source", "initial content", getContent(file2));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		// verify source file is deleted after import
		assertFalse("Source file should be deleted after import", importPath.toFile().exists());
	}

	@Test
	public void testDirectFileImportWithNonexistingFileMoveRename() {

		Path testDir    = null;
		Path importPath = null;

		try {

			testDir    = Files.createTempDirectory(Paths.get(basePath), "directFileImportTestFileMoveRename");
			importPath = testDir.resolve(Paths.get("test.txt"));

			createTestFile(importPath, "test file content 1");

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		try (final Tx tx = app.tx()) {

			app.command(DirectFileImportCommand.class).execute(setupParameters(importPath.toString(), "/", "MOVE", "RENAME", false));
			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}


		// verify successful file import
		try (final Tx tx = app.tx()) {

			final List<NodeInterface> files = app.nodeQuery("File").sort(Traits.of("NodeInterface").key("name")).getAsList();

			// import mode SKIP => no change, no additional file
			assertEquals("Two files should exist after import", 1, files.size());

			final File file1 = files.get(0).as(File.class);

			assertNotNull("Test file should have been created by import", file1);
			assertEquals("Test file name should be test.txt", "test.txt", file1.getName());
			assertNull("Test file should NOT have a parent folder", file1.getParent());
			assertEquals("Test file content does not match source", "test file content 1", getContent(file1));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		// verify source file is deleted after import
		assertFalse("Source file should be deleted after import", importPath.toFile().exists());
	}

	@Test
	public void testDirectFileImportWithPattern() {

		final PropertyKey<String> pathKey = Traits.of("File").key("path");
		Path testDir      = null;
		String importPath = null;

		try {

			testDir = Files.createTempDirectory(Paths.get(basePath), "directFileImportTestPattern");

			createTestFile(testDir.resolve(Paths.get("test1.txt")), "test file content 1");
			createTestFile(testDir.resolve(Paths.get("test2.pdf")), "test file content 2");
			createTestFile(testDir.resolve(Paths.get("test3.zip")), "test file content 3");
			createTestFile(testDir.resolve(Paths.get("test4.txt")), "test file content 4");

			importPath = testDir.toString();

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		// do import
		try (final Tx tx = app.tx()) {

			app.command(DirectFileImportCommand.class).execute(setupParameters(testDir.toString() + "/*.txt", "/", "COPY", "OVERWRITE", false));
			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}


		// verify successful file import
		try (final Tx tx = app.tx()) {

			//assertNotNull("Folder should have been created by import",     app.nodeQuery("Folder").andName(testDir.getFileName().toString()).getFirst());
			assertNotNull("Test file should have been created by import",  app.nodeQuery("File").and(pathKey, "/test1.txt").sort(Traits.of("NodeInterface").key("name")).getFirst());
			assertNull("Test file should NOT have been created by import", app.nodeQuery("File").and(pathKey, "/test2.pdf").sort(Traits.of("NodeInterface").key("name")).getFirst());
			assertNull("Test file should NOT have been created by import", app.nodeQuery("File").and(pathKey, "/test3.zip").sort(Traits.of("NodeInterface").key("name")).getFirst());
			assertNotNull("Test file should have been created by import",  app.nodeQuery("File").and(pathKey, "/test4.txt").sort(Traits.of("NodeInterface").key("name")).getFirst());

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}


	// ----- private methods -----
	private void createTestFile(final Path path, final String content) throws IOException {

		try (final FileWriter writer = new FileWriter(path.toFile())) {

			writer.append(content);
			writer.flush();
			writer.close();
		}
	}

	private void checkException(final FrameworkException exception, final int expectedStatusCode, final String expectedMessage) {

		assertEquals("Invalid exception status code",   expectedStatusCode, exception.getStatus());
		assertEquals("Invalid exception error message", expectedMessage,    exception.getMessage());
	}

	private Map<String, Object> setupParameters(final Object sourcePath, final String targetPath, final Object mode, final Object existing, final Object index) {

		final Map<String, Object> attributes = new LinkedHashMap<>();

		if (sourcePath != null) {
			attributes.put("source",   sourcePath);
		}

		if (targetPath != null) {
			attributes.put("target",   targetPath);
		}

		if (mode != null) {
			attributes.put("mode",     mode);
		}

		if (existing != null) {
			attributes.put("existing", existing);
		}

		if (index != null) {
			attributes.put("index",    index);
		}

		return attributes;
	}

	private String getContent(final File file) {

		try (final InputStream is = file.getInputStream()) {

			return IOUtils.toString(is, "utf-8");

		} catch (IOException ioex) {
			ioex.printStackTrace();
			fail("Unexpected exception.");
		}

		return null;
	}
}
