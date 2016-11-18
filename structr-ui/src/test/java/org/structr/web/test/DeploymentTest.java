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
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.web.common.StructrUiTest;
import org.structr.web.entity.dom.Comment;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;
import org.structr.web.entity.html.Body;
import org.structr.web.entity.html.Div;
import org.structr.web.entity.html.H1;
import org.structr.web.entity.html.Head;
import org.structr.web.entity.html.Html;
import org.structr.web.entity.html.Link;
import org.structr.web.entity.html.Script;
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
				createContent(page, h1, "private - ${find('User')}");

				div1.setProperty(DOMNode.showConditions, "me.isAdmin");
			}

			// create a private div
			{
				final Div div1        = createElement(page, body, "div");
				final H1 h1           = createElement(page, div1, "h1");
				createContent(page, h1, "private - test abcdefghjiklmnopqrstuvwyzöäüßABCDEFGHIJKLMNOPQRSTUVWXYZÖÄÜ?\"'");

				div1.setProperty(DOMNode.showConditions, "me.isAdmin");
			}

			// create a protected div
			{
				final Div div1        = createElement(page, body, "div");
				final H1 h1           = createElement(page, div1, "h1");
				createContent(page, h1, "protected - $%&/()=?¼½¬{[]}");

				div1.setProperty(DOMNode.visibleToPublicUsers,        false);
				div1.setProperty(DOMNode.visibleToAuthenticatedUsers,  true);
			}

			// create a public div
			{
				final Div div1        = createElement(page, body, "div");
				final H1 h1           = createElement(page, div1, "h1");
				createContent(page, h1, "public");

				div1.setProperty(DOMNode.visibleToPublicUsers,        true);
				div1.setProperty(DOMNode.visibleToAuthenticatedUsers, true);
			}

			// create a public only div
			{
				final Div div1        = createElement(page, body, "div");
				final H1 h1           = createElement(page, div1, "h1");
				createContent(page, h1, "public only");

				div1.setProperty(DOMNode.visibleToPublicUsers,         true);
				div1.setProperty(DOMNode.visibleToAuthenticatedUsers, false);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash());
	}

	@Test
	public void test03ContentTypes() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page       = Page.createNewPage(securityContext,   "test03");
			final Html html       = createElement(page, page, "html");
			final Head head       = createElement(page, html, "head");
			final Title title     = createElement(page, head, "title");
			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");
			final Script script   = createElement(page, div1, "script");
			final Content content = createContent(page, script,
				"$(function () {\n\n" +
				"$('a[data-toggle=\"tab\"]').on('click', function (e) {\n\n" +
				"var id = $(e.target).attr(\"href\").substr(1) // activated tab\n" +
				"window.location.hash = id;\n" +
				"});\n\n" +
				"});"
			);

			content.setProperty(Content.contentType, "application/javascript");

			createContent(page, title, "test03");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash());
	}

	@Test
	public void test04ContentTypes() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page       = Page.createNewPage(securityContext,   "test04");
			final Html html       = createElement(page, page, "html");
			final Head head       = createElement(page, html, "head");
			final Title title     = createElement(page, head, "title");
			final Link link1      = createElement(page, head, "link");
			final Link link2      = createElement(page, head, "link");
			final Comment comment = createComment(page, head, "commentöäüÖÄÜß+#");
			final Link link3      = createElement(page, head, "link");

			link3.setProperty(Link._href, "/");
			link3.setProperty(Link._media, "screen");
			link3.setProperty(Link._type, "stylesheet");

			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");
			final H1 h1           = createElement(page, div1, "h1");

			createContent(page,    h1, "private");
			createContent(page, title, "test04");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash());
	}

	@Test
	public void test05SimpleTemplate() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page       = Page.createNewPage(securityContext,   "test04");
			final Html html       = createElement(page, page, "html");
			final Head head       = createElement(page, html, "head");
			final Title title     = createElement(page, head, "title");
			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");

			createContent(page, title, "test04");

			final Template template = createTemplate(page, div1, "template source - öäüÖÄÜß'\"'`");

			template.setProperty(Template.functionQuery, "find('User')");
			template.setProperty(Template.dataKey, "user");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash());
	}

	// ----- private methods -----
	private void compare(final String sourceHash) {

		doImportExportRoundtrip();

		//assertTrue("Invalid deployment roundtrip result", sourceHash.equals(calculateHash()));
		assertEquals("Invalid deployment roundtrip result", sourceHash, calculateHash());
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

				// clean database
				try (final Tx tx = app.tx()) {

					for (final DOMNode domNode : app.nodeQuery(DOMNode.class).getAsList()) {
						app.delete(domNode);
					}

					tx.success();

				} catch (FrameworkException fex) {
					fail("Unexpected exception.");
				}

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

		final StringBuilder buf = new StringBuilder();

		try (final Tx tx = app.tx()) {

			for (final Page page : app.nodeQuery(Page.class).sort(AbstractNode.name).getAsList()) {

				System.out.println("#############################");

				calculateHash(page, buf, 0);
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}

		return buf.toString();//DigestUtils.md5Hex(buf.toString());
	}

	private void calculateHash(final DOMNode start, final StringBuilder buf, final int depth) {

		buf.append(start.getType()).append("{");

		hash(start, buf);

		// indent
		for (int i=0; i<depth; i++) {
			System.out.print("    ");
		}

		System.out.println(start.getType());

		for (final DOMNode child : start.getProperty(DOMNode.children)) {

			calculateHash(child, buf, depth+1);
		}

		buf.append("}");
	}

	private void hash(final DOMNode node, final StringBuilder buf) {

		// AbstractNode
		buf.append(valueOrEmpty(node, AbstractNode.name));
		buf.append(valueOrEmpty(node, AbstractNode.visibleToPublicUsers));
		buf.append(valueOrEmpty(node, AbstractNode.visibleToAuthenticatedUsers));

		// DOMNode
		buf.append(valueOrEmpty(node, DOMNode.showConditions));
		buf.append(valueOrEmpty(node, DOMNode.hideConditions));
		buf.append(valueOrEmpty(node, DOMNode.showForLocales));
		buf.append(valueOrEmpty(node, DOMNode.hideForLocales));
		buf.append(valueOrEmpty(node, DOMNode.hideOnIndex));
		buf.append(valueOrEmpty(node, DOMNode.hideOnDetail));
		buf.append(valueOrEmpty(node, DOMNode.renderDetails));

		final Page ownerDocument = node.getProperty(DOMNode.ownerDocument);
		if (ownerDocument != null) {

			buf.append(valueOrEmpty(ownerDocument, AbstractNode.name));
		}

		// DOMElement
		buf.append(valueOrEmpty(node, DOMElement.dataKey));
		buf.append(valueOrEmpty(node, DOMElement.restQuery));
		buf.append(valueOrEmpty(node, DOMElement.cypherQuery));
		buf.append(valueOrEmpty(node, DOMElement.xpathQuery));
		buf.append(valueOrEmpty(node, DOMElement.functionQuery));

		// Content
		buf.append(valueOrEmpty(node, Content.contentType));
		buf.append(valueOrEmpty(node, Content.content));

		// Page
		buf.append(valueOrEmpty(node, Page.cacheForSeconds));
		buf.append(valueOrEmpty(node, Page.dontCache));
		buf.append(valueOrEmpty(node, Page.pageCreatesRawData));
		buf.append(valueOrEmpty(node, Page.position));
		buf.append(valueOrEmpty(node, Page.showOnErrorCodes));

		// HTML attributes
		if (node instanceof DOMElement) {

			for (final PropertyKey key : ((DOMElement)node).getHtmlAttributes()) {

				buf.append(valueOrEmpty(node, key));
			}
		}
	}

	private String valueOrEmpty(final GraphObject obj, final PropertyKey key) {

		final Object value = obj.getProperty(key);
		if (value != null) {

			return key.jsonName() + "=" + value.toString() + ";";
		}

		return "";
	}

	private <T> T createElement(final Page page, final DOMNode parent, final String tag) {

		final T child = (T)page.createElement(tag);
		parent.appendChild((DOMNode)child);

		return child;
	}

	private Template createTemplate(final Page page, final DOMNode parent, final String content) throws FrameworkException {

		final Template template = StructrApp.getInstance().create(Template.class,
			new NodeAttribute<>(Template.content, content),
			new NodeAttribute<>(Template.ownerDocument, page)
		);

		parent.appendChild((DOMNode)template);

		return template;
	}

	private <T> T createContent(final Page page, final DOMNode parent, final String content) {

		final T child = (T)page.createTextNode(content);
		parent.appendChild((DOMNode)child);

		return child;
	}

	private <T> T createComment(final Page page, final DOMNode parent, final String comment) {

		final T child = (T)page.createComment(comment);
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
