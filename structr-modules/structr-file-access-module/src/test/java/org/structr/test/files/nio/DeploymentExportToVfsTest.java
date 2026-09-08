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
package org.structr.test.files.nio;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.test.web.StructrUiTest;
import org.structr.web.maintenance.DeployCommand;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * The acceptance test for the virtual filesystem: a deployment export written to it.
 *
 * Everything else about this provider is unit-tested in isolation, which cannot answer the only question
 * that matters: whether real code that was written against the local filesystem works when it is pointed
 * at this one. DeployCommand is that real code, and it is unusually demanding of a Path implementation:
 * it relativizes, walks trees, resolves children, creates directories and writes through channels.
 *
 * The local export is the reference. Producing the same tree twice is the assertion.
 */
public class DeploymentExportToVfsTest extends StructrUiTest {

	private static final String TEST_PAGE_NAME = "test-page";

	@Test
	public void testExportToTheVirtualFilesystemMatchesALocalExport() {

		final DeployCommand cmd = app.command(DeployCommand.class);
		final Path local        = Paths.get("/tmp/structr-vfs-acceptance-" + System.currentTimeMillis());
		final String vfsTarget  = "structr:///vfs-acceptance";

		createSomethingToExport();

		try {

			export(cmd, local.toString());
			export(cmd, vfsTarget);

			final Path vfs = Paths.get(URI.create(vfsTarget));

			final Set<String> localTree = treeOf(local, local);
			final Set<String> vfsTree   = treeOf(vfs, vfs);

			assertTrue("the local export produced nothing, so there is nothing to compare", localTree.size() > 0);
			assertEquals("the virtual filesystem must receive the same tree as the local one", localTree, vfsTree);

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception: " + t);

		} finally {

			deleteQuietly(local);
		}
	}

	@Test
	public void testTheExportedFilesHoldTheSameBytes() {

		final DeployCommand cmd = app.command(DeployCommand.class);
		final Path local        = Paths.get("/tmp/structr-vfs-content-" + System.currentTimeMillis());
		final String vfsTarget  = "structr:///vfs-content";

		createSomethingToExport();

		try {

			export(cmd, local.toString());
			export(cmd, vfsTarget);

			final Path vfs = Paths.get(URI.create(vfsTarget));

			// the tree test next door compares names. Matching names over differing bytes is the failure
			// this one is here to catch: a file created but never written looks identical in a listing.
			int compared = 0;

			for (final String relative : treeOf(local, local)) {

				if (relative.endsWith("/")) {

					continue;
				}

				final Path localFile = local.resolve(relative);
				final Path vfsFile   = vfs.resolve(relative);

				assertTrue("missing on the virtual filesystem: " + relative, Files.exists(vfsFile));

				final byte[] localBytes = Files.readAllBytes(localFile);
				final byte[] vfsBytes   = Files.readAllBytes(vfsFile);

				// deployment.conf carries the export timestamp, so the two runs differ by design
				if (relative.equals("deployment.conf")) {

					continue;
				}

				assertEquals("different size for " + relative, localBytes.length, vfsBytes.length);
				assertEquals("different content for " + relative,
					new String(localBytes, StandardCharsets.UTF_8), new String(vfsBytes, StandardCharsets.UTF_8));

				compared++;
			}

			// a comparison loop that ran zero times passes without asserting anything
			assertTrue("nothing was actually compared", compared > 5);

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception: " + t);

		} finally {

			deleteQuietly(local);
		}
	}

	@Test
	public void testARoundTripThroughTheVirtualFilesystem() {

		final DeployCommand cmd = app.command(DeployCommand.class);
		final String vfsTarget  = "structr:///vfs-roundtrip";

		createSomethingToExport();

		try {

			export(cmd, vfsTarget);

			// Delete the page, but NOT the database: this filesystem IS the database, so the export lives
			// in the very nodes a cleanDatabase() would remove, and the import would find its own source
			// gone. Removing only the subject of the test leaves the exported files standing.
			deleteAllPages();

			assertEquals("the page must be gone before the import", 0, countPages());

			final Map<String, Object> params = new HashMap<>();

			params.put("mode",   "import");
			params.put("source", vfsTarget);

			cmd.execute(params);

			assertEquals("the page must come back from the virtual filesystem", 1, countPages());

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception: " + t);
		}
	}

	// ----- private methods -----
	private void deleteAllPages() {

		try (final Tx tx = app.tx()) {

			for (final NodeInterface page : app.nodeQuery("Page").name(TEST_PAGE_NAME).getAsList()) {

				app.delete(page);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unable to delete pages: " + fex.getMessage());
		}
	}

	/**
	 * How many pages named like the test page exist.
	 *
	 * By name, not by type: __ShadowDocument__ is a Page as well and is created during an import, so a
	 * bare type count answers two for one page and says nothing about whether the right one came back.
	 */
	private int countPages() {

		try (final Tx tx = app.tx()) {

			final int count = app.nodeQuery("Page").name(TEST_PAGE_NAME).getAsList().size();

			tx.success();

			return count;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unable to count pages: " + fex.getMessage());
		}

		return -1;
	}

	private void export(final DeployCommand cmd, final String target) throws FrameworkException {

		final Map<String, Object> params = new HashMap<>();

		params.put("mode",   "export");
		params.put("target", target);

		cmd.execute(params);
	}

	/** Every path below root, as strings relative to it, so the two filesystems are comparable. */
	private Set<String> treeOf(final Path root, final Path current) throws IOException {

		final Set<String> result = new TreeSet<>();

		if (!Files.exists(current)) {

			return result;
		}

		try (final DirectoryStream<Path> stream = Files.newDirectoryStream(current)) {

			for (final Path child : stream) {

				final String relative = root.relativize(child).toString();

				if (Files.isDirectory(child)) {

					result.add(relative + "/");
					result.addAll(treeOf(root, child));

				} else {

					result.add(relative);
				}
			}
		}

		return result;
	}

	private void createSomethingToExport() {

		try (final Tx tx = app.tx()) {

			app.create("Page", TEST_PAGE_NAME);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unable to create test content: " + fex.getMessage());
		}
	}

	private void deleteQuietly(final Path path) {

		try {

			if (Files.exists(path)) {

				Files.walk(path).sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> {

					try { Files.deleteIfExists(p); } catch (IOException ignored) {}
				});
			}

		} catch (IOException ignored) {}
	}
}
