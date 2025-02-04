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
package org.structr.test.web.advanced;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.AccessControllable;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Security;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.dom.*;
import org.structr.web.maintenance.DeployCommand;
import org.structr.websocket.command.CloneComponentCommand;
import org.structr.websocket.command.CreateComponentCommand;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

public abstract class DeploymentTestBase extends StructrUiTest {

	// ----- private methods -----
	protected void compare(final String sourceHash, final boolean deleteTestDirectory) {
		compare(sourceHash, deleteTestDirectory, true);
	}

	protected void compare(final String sourceHash, final boolean deleteTestDirectory, final boolean cleanDatabase) {

		doImportExportRoundtrip(deleteTestDirectory, cleanDatabase, null);

		final String roundtripHash = calculateHash();

		if (!sourceHash.equals(roundtripHash)) {

			System.out.println("########## Expected:");
			System.out.println(sourceHash);

			System.out.println("########## Actual:");
			System.out.println(roundtripHash);

			System.out.println("########## Difference:");
			System.out.println(StringUtils.difference(sourceHash, roundtripHash));

			fail("Invalid deployment roundtrip result");
		}
	}

	protected void doImportExportRoundtrip(final boolean deleteTestDirectory) {
		doImportExportRoundtrip(deleteTestDirectory, true, null);
	}

	protected void doImportExportRoundtrip(final boolean deleteTestDirectory, final boolean cleanDatabase, final Function callback) {

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

				if (cleanDatabase) {
					cleanDatabase();
				}

				// apply callback if present
				if (callback != null) {
					callback.apply(null);
				}

				// import from exported source
				final Map<String, Object> firstImportParams = new HashMap<>();
				firstImportParams.put("mode", "import");
				firstImportParams.put("source", tmp.toString());

				// execute deploy command
				cmd.execute(firstImportParams);

			} else {

				fail("Unable to create temporary directory.");
			}

		} catch (Throwable t) {

			t.printStackTrace();

			fail("Unexpected exception: " + t.getMessage());

		} finally {

			if (deleteTestDirectory) {

				try {
					// clean directories
					Files.walkFileTree(tmp, new DeletingFileVisitor());
					Files.delete(tmp);

				} catch (IOException ioex) {}
			}
		}
	}

	protected String calculateHash() {

		final StringBuilder buf = new StringBuilder();

		try (final Tx tx = app.tx()) {

			for (final NodeInterface page : app.nodeQuery("Page").sort(Traits.of("NodeInterface").key("name")).getAsList()) {

				// skip shadow document
				if (page.is("ShadowDocument")) {
					continue;
				}

				buf.append("Page ");
				buf.append(page.getName());
				buf.append("\n");

				calculateHash(page, buf, 1);
			}

			for (final NodeInterface folder : app.nodeQuery("Folder").sort(Traits.of("NodeInterface").key("name")).getAsList()) {

				if (folder.as(Folder.class).includeInFrontendExport()) {

					buf.append("Folder ");
					buf.append(folder.getName());
					buf.append("\n");

					calculateHash(folder, buf, 1);
				}
			}

			for (final NodeInterface file : app.nodeQuery("File").sort(Traits.of("NodeInterface").key("name")).getAsList()) {

				if (file.as(File.class).includeInFrontendExport()) {

					buf.append("File ");
					buf.append(file.getName());
					buf.append("\n");

					calculateHash(file, buf, 1);
				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}

		return buf.toString();//DigestUtils.md5Hex(buf.toString());
	}

	protected void calculateHash(final NodeInterface start, final StringBuilder buf, final int depth) {

		final int indent = depth + 1;

		buf.append(StringUtils.leftPad("", depth*4));
		buf.append(start.getType()).append(" {\n");

		buf.append(StringUtils.leftPad("", indent*4));
		hash(start, buf);

		if (start.is("ShadowDocument")) {

			for (final DOMNode child : start.as(ShadowDocument.class).getElements()) {

				// only include toplevel elements of the shadow document
				if (child.getParent() == null) {

					calculateHash(child, buf, depth+1);
				}
			}

		} else if (start.is("DOMNode")) {

			for (final DOMNode child : start.as(DOMNode.class).getChildren()) {

				calculateHash(child, buf, depth+1);
			}
		}

		buf.append("\n");
		buf.append(StringUtils.leftPad("", depth*4));
		buf.append("}\n");
	}

	protected void hash(final NodeInterface node, final StringBuilder buf) {

		// AbstractNode
		buf.append(valueOrEmpty(node, Traits.of("NodeInterface").key("type")));
		buf.append(valueOrEmpty(node, Traits.of("NodeInterface").key("name")));
		buf.append(valueOrEmpty(node, Traits.of("NodeInterface").key("visibleToPublicUsers")));
		buf.append(valueOrEmpty(node, Traits.of("NodeInterface").key("visibleToAuthenticatedUsers")));

		// include owner in content hash generation!
		final AccessControllable ac = node.as(AccessControllable.class);
		final Principal owner       = ac.getOwnerNode();

		if (owner != null) {

			buf.append(valueOrEmpty(owner, Traits.of("NodeInterface").key("name")));
		}

		// include permissions in content hash generation!
		for (final Security r : ac.getSecurityRelationships()) {

			if (r != null) {

				buf.append(r.getSourceNode().getName());
				buf.append(r.getPermissions());
			}
		}

		// DOMNode
		buf.append(valueOrEmpty(node, Traits.of("DOMNode").key("showConditions")));
		buf.append(valueOrEmpty(node, Traits.of("DOMNode").key("hideConditions")));
		buf.append(valueOrEmpty(node, Traits.of("DOMNode").key("showForLocales")));
		buf.append(valueOrEmpty(node, Traits.of("DOMNode").key("hideForLocales")));
		buf.append(valueOrEmpty(node, Traits.of("DOMNode").key("sharedComponentConfiguration")));

		if (node instanceof DOMNode) {

			final Page ownerDocument = node.as(DOMNode.class).getOwnerDocument();
			if (ownerDocument != null) {

				buf.append(valueOrEmpty(ownerDocument, Traits.of("NodeInterface").key("name")));
			}
		}

		// DOMElement
		buf.append(valueOrEmpty(node, Traits.of("DOMElement").key("dataKey")));
		buf.append(valueOrEmpty(node, Traits.of("DOMElement").key("restQuery")));
		buf.append(valueOrEmpty(node, Traits.of("DOMElement").key("cypherQuery")));
		buf.append(valueOrEmpty(node, Traits.of("DOMElement").key("functionQuery")));

		// Content
		buf.append(valueOrEmpty(node, Traits.of("Content").key("contentType")));
		buf.append(valueOrEmpty(node, Traits.of("Content").key("content")));

		// Page
		buf.append(valueOrEmpty(node, Traits.of("Page").key("cacheForSeconds")));
		buf.append(valueOrEmpty(node, Traits.of("Page").key("dontCache")));
		buf.append(valueOrEmpty(node, Traits.of("Page").key("pageCreatesRawData")));
		buf.append(valueOrEmpty(node, Traits.of("Page").key("position")));
		buf.append(valueOrEmpty(node, Traits.of("Page").key("showOnErrorCodes")));

		// HTML attributes
		if (node instanceof DOMElement) {

			for (final PropertyKey key : ((DOMElement)node).getHtmlAttributes()) {

				buf.append(valueOrEmpty(node, key));
			}
		}

		for (final PropertyKey key : node.getPropertyKeys(PropertyView.All)) {

			// fixme
			if (key.isDynamic()) {

				buf.append(valueOrEmpty(node, key));
			}
		}

		buf.append("\n");
	}

	protected String valueOrEmpty(final GraphObject obj, final PropertyKey key) {

		final Object value = obj.getProperty(key);
		if (value != null) {

			return key.jsonName() + ": \"" + value.toString() + "\";";
		}

		return "";
	}

	protected DOMElement createElement(final Page page, final DOMNode parent, final String tag, final String... content) throws FrameworkException {

		final DOMElement child = page.createElement(tag);
		parent.appendChild(child);

		if (content != null && content.length > 0) {

			for (final String text : content) {

				final Content node = page.createTextNode(text);
				child.appendChild(node);
			}
		}

		return child;
	}

	protected Template createTemplate(final Page page, final DOMNode parent, final String content) throws FrameworkException {

		final NodeInterface template = StructrApp.getInstance().create("Template",
			new NodeAttribute<>(Traits.of("Template").key("content"), content),
			new NodeAttribute<>(Traits.of("Template").key("ownerDocument"), page)
		);

		if (parent != null) {
			parent.appendChild((DOMNode)template);
		}

		return template.as(Template.class);
	}

	protected Content createContent(final Page page, final DOMNode parent, final String content) throws FrameworkException {

		final Content child = page.createTextNode(content);

		parent.appendChild(child);

		return child;
	}

	protected Comment createComment(final Page page, final DOMNode parent, final String comment) throws FrameworkException {

		final Comment child = page.createComment(comment);

		parent.appendChild(child);

		return child;
	}

	protected DOMNode createComponent(final DOMNode node) throws FrameworkException {
		return new CreateComponentCommand().create(node);
	}

	protected DOMNode cloneComponent(final DOMNode node, final DOMNode parentNode) throws FrameworkException {
		return new CloneComponentCommand().cloneComponent(node, parentNode);
	}

	protected NodeInterface getRootFolder(final Folder folder) {

		Folder parent = folder;
		boolean root  = false;

		while (parent != null && !root) {

			if (parent.getParent() != null) {

				parent = parent.getParent();

			} else {

				root = true;
			}
		}

		return parent;
	}

	protected void assertFalse(final String message, final boolean value) {
		assertTrue(message, !value);
	}

	// ----- nested classes -----
	protected static class DeletingFileVisitor implements FileVisitor<Path> {

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
