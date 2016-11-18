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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.Tx;
import org.structr.web.common.StructrUiTest;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.html.Body;
import org.structr.web.entity.html.Div;
import org.structr.web.entity.html.H1;
import org.structr.web.entity.html.Head;
import org.structr.web.entity.html.Html;
import org.structr.web.entity.html.Title;
import org.structr.web.maintenance.DeployCommand;

public class DeploymentTest extends StructrUiTest {

	@Test
	public void test01SimplePage() {

		// setup
		try (final Tx tx = app.tx()) {

			Page.createSimplePage(securityContext, "test01");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash());
	}

	@Test
	public void test02Visibilities() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page       = Page.createNewPage(securityContext,   "test02");
			final Html html       = createElement(page, page, "html");
			final Head head       = createElement(page, html, "head");
			final Title title     = createElement(page, head, "title");
			final Body body       = createElement(page, html, "body");

			createContent(page, title, "test02");

			// create a div for admin only
			{
				final Div div1        = createElement(page, body, "div");
				final H1 h1           = createElement(page, div1, "h1");
				createContent(page, h1, "private");

				div1.setProperty(DOMNode.showConditions, "me.isAdmin");
			}

			// create a private div
			{
				final Div div1        = createElement(page, body, "div");
				final H1 h1           = createElement(page, div1, "h1");
				createContent(page, h1, "private");

				div1.setProperty(DOMNode.showConditions, "me.isAdmin");
			}

			// create a protected div
			{
				final Div div1        = createElement(page, body, "div");
				final H1 h1           = createElement(page, div1, "h1");
				createContent(page, h1, "protected");

				div1.setProperty(DOMNode.visibleToPublicUsers,        false);
				div1.setProperty(DOMNode.visibleToAuthenticatedUsers,  true);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash());
	}

	// ----- private methods -----
	private void compare(final String sourceHash) {

		// clean database
		try (final Tx tx = app.tx()) {

			for (final DOMNode domNode : app.nodeQuery(DOMNode.class).getAsList()) {
				app.delete(domNode);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		doImportExportRoundtrip();

		assertTrue("Invalid deployment roundtrip result", sourceHash.equals(calculateHash()));
	}


	private void doImportExportRoundtrip() {

		final DeployCommand cmd = app.command(DeployCommand.class);
		final Path tmp          = Paths.get("/tmp/structr-deployment-test" + System.currentTimeMillis() + System.nanoTime());

		try {
			if (tmp != null) {

				// export to temp directory
				final Map<String, Object> firstExportParams = new HashMap<>();
				firstExportParams.put("mode", "export");
				firstExportParams.put("target", tmp.toString());

				// execute deploy command
				cmd.execute(firstExportParams);

				// import from exported source
				final Map<String, Object> firstImportParams = new HashMap<>();
				firstImportParams.put("source", tmp.toString());

				// execute deploy command
				cmd.execute(firstImportParams);

			} else {

				fail("Unable to create temporary directory.");
			}

		} catch (FrameworkException fex) {

			fex.printStackTrace();

		} finally {

			try {
				// clean directories
				Files.walkFileTree(tmp, new DeletingFileVisitor());
				Files.delete(tmp);

			} catch (IOException ioex) {}
		}
	}

	private String calculateHash() {

		final MessageDigest digest = DigestUtils.getMd5Digest();

		try (final Tx tx = app.tx()) {

			for (final Page page : app.nodeQuery(Page.class).sort(AbstractNode.name).getAsList()) {

				calculateHash(page, digest);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}

		return new String(digest.digest(), Charset.forName("utf-8"));
	}

	private void calculateHash(final DOMNode start, final MessageDigest digest) {

		hash(start, digest);

		for (final DOMNode child : start.getProperty(DOMNode.children)) {

			calculateHash(child, digest);
		}
	}

	private void hash(final DOMNode node, final MessageDigest digest) {

		try {

			// AbstractNode
			digest.update(valueOrEmpty(node.getProperty(AbstractNode.name)).getBytes("utf-8"));
			digest.update(valueOrEmpty(node.getProperty(AbstractNode.visibleToPublicUsers)).getBytes("utf-8"));
			digest.update(valueOrEmpty(node.getProperty(AbstractNode.visibleToAuthenticatedUsers)).getBytes("utf-8"));

			// DOMNode specials
			digest.update(valueOrEmpty(node.getProperty(DOMNode.showConditions)).getBytes("utf-8"));
			digest.update(valueOrEmpty(node.getProperty(DOMNode.hideConditions)).getBytes("utf-8"));

		} catch (UnsupportedEncodingException ex) {}
	}

	private String valueOrEmpty(final Object value) {

		if (value != null) {
			return value.toString();
		}

		return "";
	}

	private <T> T createElement(final Page page, final DOMNode parent, final String tag) {

		final T child = (T)page.createElement(tag);
		parent.appendChild((DOMNode)child);

		return child;
	}

	private <T> T createContent(final Page page, final DOMNode parent, final String content) {

		final T child = (T)page.createTextNode(content);
		parent.appendChild((DOMNode)child);

		return child;
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
