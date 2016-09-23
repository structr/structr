/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.test;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.web.common.StructrUiTest;
import org.structr.web.maintenance.DeployCommand;

public class DeploymentTest extends StructrUiTest {

	public void test00SimpleDeploymentRoundtrip() {

		doImportExportRoundtrip(readFileFromJar("/test/deployment/test01.html"));
	}

	// ----- private methods -----
	private String readFileFromJar(final String name) {

		final InputStream stream = this.getClass().getResourceAsStream(name);
		final StringBuilder buf  = new StringBuilder();

		if (stream != null) {

			try (final BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {

				reader.lines().forEach((final String line) -> {
					buf.append(line);
					buf.append("\n");
				});

			} catch (IOException ioex) {
				ioex.printStackTrace();
			}
		}

		return buf.toString().trim();
	}

	private void doImportExportRoundtrip(final String source) {

		final Path tmp = createTempDir();
		if (tmp != null) {

			// create tmp directory for roundtrip to import and export
			try {

				final Path pages = tmp.resolve("pages");

				Files.createDirectories(pages);

				try (final Writer writer = new OutputStreamWriter(new FileOutputStream(pages.resolve("test.html").toFile()))) {

					writer.append(source);
					writer.flush();
					writer.close();
				}

				// import source for an initial version, compare two consecutive exports
				final Map<String, Object> initialImportParams = new HashMap<>();
				initialImportParams.put("source", tmp.toString());

				// execute deploy command
				app.command(DeployCommand.class).execute(initialImportParams);

				// clean directory
				Files.walkFileTree(tmp, new DeletingFileVisitor());

				// export to same directory
				final Map<String, Object> firstExportParams = new HashMap<>();
				firstExportParams.put("mode", "export");
				firstExportParams.put("target", tmp.toString());

				// execute deploy command
				app.command(DeployCommand.class).execute(firstExportParams);

				// obtain exported source file
				final String firstExportedSource = new String(Files.readAllBytes(pages.resolve("test.html")), Charset.forName("utf-8")).trim();

				// import from exported source
				final Map<String, Object> secondImportParams = new HashMap<>();
				secondImportParams.put("source", tmp.toString());

				// execute deploy command
				app.command(DeployCommand.class).execute(secondImportParams);

				// clean directory again
				Files.walkFileTree(tmp, new DeletingFileVisitor());

				// export again
				final Map<String, Object> secondExportParams = new HashMap<>();
				secondExportParams.put("mode", "export");
				secondExportParams.put("target", tmp.toString());

				// execute deploy command
				app.command(DeployCommand.class).execute(secondExportParams);

				// obtain exported source file
				final String secondExportedSource = new String(Files.readAllBytes(pages.resolve("test.html")), Charset.forName("utf-8")).trim();

				assertEquals("Invalid import/export roundtrip for DeployCommand from source to first export",  source, firstExportedSource);
				assertEquals("Invalid import/export roundtrip for DeployCommand from first to second export",  firstExportedSource, secondExportedSource);
				assertEquals("Invalid import/export roundtrip for DeployCommand from second export to source", secondExportedSource, source);

			} catch (IOException | FrameworkException t) {

				t.printStackTrace();
				fail("Unexpected exception.");

			} finally {

				try {
					// clean directories
					Files.walkFileTree(tmp, new DeletingFileVisitor());
					Files.delete(tmp);

				} catch (IOException ignore) {
				}
			}

		} else {

			fail("Unable to create temporary directory.");
		}
	}

	private Path createTempDir() {


		try {

			return Files.createTempDirectory("structr-deployment-test");

		} catch (IOException ioex) {
			ioex.printStackTrace();
		}

		return null;
	}

	// ----- nested classes -----
	private static class DeletingFileVisitor implements FileVisitor<Path> {

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

			Files.delete(file);

			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {

			Files.delete(dir);

			return FileVisitResult.CONTINUE;
		}

	}
}
