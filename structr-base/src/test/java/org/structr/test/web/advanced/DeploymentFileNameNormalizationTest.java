/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.web.advanced;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.maintenance.DeployCommand;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.maintenance.deploy.FileCleanupVisitor;
import org.structr.web.maintenance.deploy.FileImportVisitor;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * A file name containing an umlaut can exist in either Unicode normal form: macOS stores it
 * decomposed (NFD, "u" + combining diaeresis) and Structr keeps whatever the upload delivered,
 * while Linux and git (core.precomposeunicode) use the composed form (NFC). The two render
 * identically but are different strings, so every place that compares a name from the database
 * with a name from the filesystem has to agree on a form first.
 *
 * Two things go wrong when they do not: the importer reports the same file both as
 * configured-but-missing and as present-but-not-configured, and the export cleanup pass deletes
 * the very file the export has just written because it cannot find it in the manifest.
 */
public class DeploymentFileNameNormalizationTest extends StructrUiTest {

	private static final String NFC_NAME = Normalizer.normalize("Grüße.png", Normalizer.Form.NFC);
	private static final String NFD_NAME = Normalizer.normalize("Grüße.png", Normalizer.Form.NFD);

	/**
	 * A file whose name is stored decomposed must still be present, with its content, after an
	 * export. This is the cleanup pass: it walks the exported tree and removes everything it
	 * cannot find in the manifest it just wrote.
	 */
	@Test
	public void testExportKeepsFileWithDecomposedName() {

		assertTrue("the two normal forms are identical, the test cannot detect the bug", !NFC_NAME.equals(NFD_NAME));

		// a file whose NAME IN THE DATABASE is decomposed, as an upload from macOS leaves it.
		// Only files flagged for the frontend export are exported at all, so the flag has to be set
		// or the export would skip it and the assertion below would pass for the wrong reason.
		try (final Tx tx = app.tx()) {

			final NodeInterface file = FileHelper.createFile(securityContext, "test".getBytes(StandardCharsets.UTF_8), "image/png", StructrTraits.FILE, NFD_NAME, false);

			file.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true);

			tx.success();

		} catch (FrameworkException | IOException ex) {

			ex.printStackTrace();
			fail("Unexpected exception while creating the test file.");
		}

		final Path tmp = Paths.get("/tmp/structr-normalization-export" + System.currentTimeMillis() + System.nanoTime());

		try {

			final Map<String, Object> exportParams = new HashMap<>();
			exportParams.put("mode",   "export");
			exportParams.put("target", tmp.toString());

			app.command(DeployCommand.class).execute(exportParams);

			// the exported file must still exist: the cleanup pass must not have removed it
			final Path filesDir = tmp.resolve("files");
			final long exported = Files.exists(filesDir)
				? Files.walk(filesDir).filter(Files::isRegularFile).filter(p -> p.getFileName().toString().contains("e.png")).count()
				: 0L;

			assertEquals("the exported file was removed by the cleanup pass", 1L, exported);

			// and files.json must mention it, so the export is internally consistent
			final String filesJson = Files.readString(tmp.resolve("files.json"));
			assertTrue("files.json does not mention the exported file", filesJson.contains("e.png"));

		} catch (FrameworkException | IOException ex) {

			ex.printStackTrace();
			fail("Unexpected exception during export.");

		} finally {

			deleteQuietly(tmp);
		}
	}

	/**
	 * The cleanup pass removes everything it cannot find in the manifest. It must not treat a
	 * file as unknown merely because the manifest spells its name in the other normal form, which
	 * is the case for any export directory written by an older version or checked out from git on
	 * a differently-normalizing filesystem.
	 */
	@Test
	public void testCleanupKeepsFileWhenOnlyNormalFormDiffers() {

		final Path base = createExportWithFileNamed(NFD_NAME);

		try {

			// the manifest spells the same name in the other form
			final Map<String, Object> metadata = new LinkedHashMap<>();
			metadata.put("/" + NFC_NAME, fileEntry());

			Files.walkFileTree(base, new FileCleanupVisitor(base, metadata));

			final long remaining = Files.walk(base).filter(Files::isRegularFile).count();

			assertEquals("the cleanup pass deleted a file that IS in the manifest, only in the other normal form", 1L, remaining);

		} catch (IOException ioex) {

			ioex.printStackTrace();
			fail("Unexpected exception during cleanup.");

		} finally {

			deleteQuietly(base);
		}
	}

	/**
	 * The importer must find a manifest entry for a file whose name on disk differs only in
	 * normal form, which is what a checkout on a differently-normalizing filesystem produces.
	 */
	@Test
	public void testImportToleratesNormalizationMismatch() {

		final Path basePath = createExportWithFileNamed(NFC_NAME);

		// files.json refers to the very same file in the OTHER normal form
		final Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("/" + NFD_NAME, fileEntry());

		final FileImportVisitor visitor = new FileImportVisitor(securityContext, basePath, metadata);

		try {

			Files.walkFileTree(basePath, visitor);

		} catch (IOException ioex) {

			ioex.printStackTrace();
			fail("Unexpected exception while walking the export directory.");
		}

		final FileImportVisitor.FileImportProblems problems = visitor.getFileImportProblems();

		assertTrue(
			"the file was reported as a problem although only its Unicode normal form differs:\n" + problems.getProblemsText(),
			!problems.hasAnyProblems()
		);

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> files = app.nodeQuery(StructrTraits.FILE).getAsList();

			assertEquals("the file was not imported", 1, files.size());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while checking the imported file.");

		} finally {

			deleteQuietly(basePath);
		}
	}

	// ----- private methods -----
	private Path createExportWithFileNamed(final String fileName) {

		try {

			final Path base = Files.createTempDirectory("structr-normalization-test");

			Files.write(base.resolve(fileName), "test".getBytes(StandardCharsets.UTF_8));

			return base;

		} catch (IOException ioex) {

			ioex.printStackTrace();
			fail("Unable to create the test export directory.");
		}

		return null;
	}

	private Map<String, Object> fileEntry() {

		final Map<String, Object> entry = new HashMap<>();

		entry.put("type",                        StructrTraits.FILE);
		entry.put("visibleToPublicUsers",        false);
		entry.put("visibleToAuthenticatedUsers", false);

		return entry;
	}

	private void deleteQuietly(final Path path) {

		if (path == null || !Files.exists(path)) {
			return;
		}

		try (final var paths = Files.walk(path)) {

			paths.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> {
				try { Files.deleteIfExists(p); } catch (IOException ignore) {}
			});

		} catch (IOException ignore) {
		}
	}
}
