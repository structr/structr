/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.files;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.web.StructrUiTest;
import org.structr.web.entity.Folder;
import org.structr.web.maintenance.DirectFileImportCommand;

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

				app.command(DirectFileImportCommand.class).execute(setupParameters(null, null, null, null));
				fail("Direct file import command should not accept empty parameter map.");

			} catch (FrameworkException ex) {
				checkException(ex, 422, "Please provide 'source' attribute for deployment source directory path.");
			}

			// test missing source path
			try (final Tx tx = app.tx()) {

				app.command(DirectFileImportCommand.class).execute(setupParameters(null, "copy", "skip", false));
				fail("Direct file import command should not accept empty source directory");

			} catch (FrameworkException ex) {
				checkException(ex, 422, "Please provide 'source' attribute for deployment source directory path.");
			}

			// test nonexisting source path
			try (final Tx tx = app.tx()) {

				app.command(DirectFileImportCommand.class).execute(setupParameters(nonexistingPath, "copy", "skip", false));
				fail("Direct file import command should not accept empty source directory");

			} catch (FrameworkException ex) {
				checkException(ex, 422, "Source path " + nonexistingPath + " does not exist.");
			}

			// test invalid source path (file instead of directory)
			try (final Tx tx = app.tx()) {

				app.command(DirectFileImportCommand.class).execute(setupParameters(existingButFilePath.toString(), "copy", "skip", false));
				fail("Direct file import command should not accept empty source directory");

			} catch (FrameworkException ex) {
				checkException(ex, 422, "Source path " + existingButFilePath.toString() + " is not a directory.");
			}

			// test invalid value for mode
			try (final Tx tx = app.tx()) {

				app.command(DirectFileImportCommand.class).execute(setupParameters(testDir.toString(), "invalid", "skip", false));
				fail("Direct file import command should not accept invalid 'mode' value");

			} catch (FrameworkException ex) {
				checkException(ex, 422, "Unknown value for 'mode' attribute. Valid values are: copy, move");
			}

			// test invalid value for skip
			try (final Tx tx = app.tx()) {

				app.command(DirectFileImportCommand.class).execute(setupParameters(testDir.toString(), "copy", "invalid", false));
				fail("Direct file import command should not accept invalid 'existing' value");

			} catch (FrameworkException ex) {
				checkException(ex, 422, "Unknown value for 'existing' attribute. Valid values are: skip, overwrite, rename");
			}

		} catch (IOException ioex) {
			fail("Unable to create files");
		}
	}

	@Test
	public void testDirectFileImportSuccess() {

		Path testDir      = null;
		String importPath = null;

		try {

			testDir = Files.createTempDirectory(Paths.get(basePath), "directFileImportTest");

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
			Logger.getLogger(DirectFileImportTest.class.getName()).log(Level.SEVERE, null, ex);
		}


		// verify successful file import
		try (final Tx tx = app.tx()) {

			assertNotNull("Folder should have been created by import", app.nodeQuery(Folder.class).andName(testDir.getFileName().toString()).getFirst());
			assertNotNull("Test file should have been created by import", app.nodeQuery(Folder.class).andName("test1.txt").getFirst());
			assertNotNull("Test file should have been created by import", app.nodeQuery(Folder.class).andName("test2.txt").getFirst());
			assertNotNull("Test file should have been created by import", app.nodeQuery(Folder.class).andName("test3.txt").getFirst());

			tx.success();

		} catch (FrameworkException ex) {
			Logger.getLogger(DirectFileImportTest.class.getName()).log(Level.SEVERE, null, ex);
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

	private Map<String, Object> setupParameters(final Object sourcePath, final Object mode, final Object existing, final Object index) {

		final Map<String, Object> attributes = new LinkedHashMap<>();

		if (sourcePath != null) {
			attributes.put("source",   sourcePath);
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
}
