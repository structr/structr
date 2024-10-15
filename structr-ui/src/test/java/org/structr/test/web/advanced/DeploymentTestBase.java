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
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.entity.Security;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.dom.*;
import org.structr.web.maintenance.DeployCommand;
import org.structr.websocket.command.CloneComponentCommand;
import org.structr.websocket.command.CreateComponentCommand;
import org.w3c.dom.Node;

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

			for (final Page page : app.nodeQuery(Page.class).sort(AbstractNode.name).getAsList()) {

				// skip shadow document
				if (page instanceof ShadowDocument) {
					continue;
				}

				buf.append("Page ");
				buf.append(page.getName());
				buf.append("\n");

				calculateHash(page, buf, 1);
			}

			for (final Folder folder : app.nodeQuery(Folder.class).sort(AbstractNode.name).getAsList()) {

				if (folder.includeInFrontendExport()) {

					buf.append("Folder ");
					buf.append(folder.getName());
					buf.append("\n");

					calculateHash(folder, buf, 1);
				}
			}

			for (final File file : app.nodeQuery(File.class).sort(AbstractNode.name).getAsList()) {

				if (file.includeInFrontendExport()) {

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

		if (start instanceof ShadowDocument) {

			for (final DOMNode child : ((ShadowDocument)start).getElements()) {

				// only include toplevel elements of the shadow document
				if (child.getParent() == null) {

					calculateHash(child, buf, depth+1);
				}
			}

		} else if (start instanceof DOMNode) {

			for (final DOMNode child : ((DOMNode)start).getChildren()) {

				calculateHash(child, buf, depth+1);
			}
		}

		buf.append("\n");
		buf.append(StringUtils.leftPad("", depth*4));
		buf.append("}\n");
	}

	protected void hash(final NodeInterface node, final StringBuilder buf) {

		// AbstractNode
		buf.append(valueOrEmpty(node, AbstractNode.type));
		buf.append(valueOrEmpty(node, AbstractNode.name));
		buf.append(valueOrEmpty(node, AbstractNode.visibleToPublicUsers));
		buf.append(valueOrEmpty(node, AbstractNode.visibleToAuthenticatedUsers));

		// include owner in content hash generation!
		final PrincipalInterface owner = node.getOwnerNode();
		if (owner != null) {

			buf.append(valueOrEmpty(owner, AbstractNode.name));
		}

		// include permissions in content hash generation!
		for (final Security r : node.getSecurityRelationships()) {

			if (r != null) {

				buf.append(r.getSourceNode().getName());
				buf.append(r.getPermissions());
			}
		}

		// DOMNode
		buf.append(valueOrEmpty(node, StructrApp.key(DOMNode.class, "showConditions")));
		buf.append(valueOrEmpty(node, StructrApp.key(DOMNode.class, "hideConditions")));
		buf.append(valueOrEmpty(node, StructrApp.key(DOMNode.class, "showForLocales")));
		buf.append(valueOrEmpty(node, StructrApp.key(DOMNode.class, "hideForLocales")));
		buf.append(valueOrEmpty(node, StructrApp.key(DOMNode.class, "hideOnIndex")));
		buf.append(valueOrEmpty(node, StructrApp.key(DOMNode.class, "hideOnDetail")));
		buf.append(valueOrEmpty(node, StructrApp.key(DOMNode.class, "renderDetails")));
		buf.append(valueOrEmpty(node, StructrApp.key(DOMNode.class, "sharedComponentConfiguration")));

		if (node instanceof DOMNode) {

			final Page ownerDocument = ((DOMNode)node).getOwnerDocument();
			if (ownerDocument != null) {

				buf.append(valueOrEmpty(ownerDocument, AbstractNode.name));
			}
		}

		// DOMElement
		buf.append(valueOrEmpty(node, StructrApp.key(DOMElement.class, "dataKey")));
		buf.append(valueOrEmpty(node, StructrApp.key(DOMElement.class, "restQuery")));
		buf.append(valueOrEmpty(node, StructrApp.key(DOMElement.class, "cypherQuery")));
		buf.append(valueOrEmpty(node, StructrApp.key(DOMElement.class, "xpathQuery")));
		buf.append(valueOrEmpty(node, StructrApp.key(DOMElement.class, "functionQuery")));

		// Content
		buf.append(valueOrEmpty(node, StructrApp.key(Content.class, "contentType")));
		buf.append(valueOrEmpty(node, StructrApp.key(Content.class, "content")));

		// Page
		buf.append(valueOrEmpty(node, StructrApp.key(Page.class, "cacheForSeconds")));
		buf.append(valueOrEmpty(node, StructrApp.key(Page.class, "dontCache")));
		buf.append(valueOrEmpty(node, StructrApp.key(Page.class, "pageCreatesRawData")));
		buf.append(valueOrEmpty(node, StructrApp.key(Page.class, "position")));
		buf.append(valueOrEmpty(node, StructrApp.key(Page.class, "showOnErrorCodes")));

		// HTML attributes
		if (node instanceof DOMElement) {

			for (final PropertyKey key : ((DOMElement)node).getHtmlAttributes()) {

				buf.append(valueOrEmpty(node, key));
			}
		}

		for (final PropertyKey key : node.getPropertyKeys(PropertyView.All)) {

			if (!key.isPartOfBuiltInSchema()) {

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

	protected <T extends Node> T createElement(final Page page, final DOMNode parent, final String tag, final String... content) {

		final T child = (T)page.createElement(tag);
		parent.appendChild((DOMNode)child);

		if (content != null && content.length > 0) {

			for (final String text : content) {

				final Node node = page.createTextNode(text);
				child.appendChild(node);
			}
		}

		return child;
	}

	protected Template createTemplate(final Page page, final DOMNode parent, final String content) throws FrameworkException {

		final Template template = StructrApp.getInstance().create(Template.class,
			new NodeAttribute<>(StructrApp.key(Template.class, "content"), content),
			new NodeAttribute<>(StructrApp.key(Template.class, "ownerDocument"), page)
		);

		if (parent != null) {
			parent.appendChild((DOMNode)template);
		}

		return template;
	}

	protected <T> T createContent(final Page page, final DOMNode parent, final String content) {

		final T child = (T)page.createTextNode(content);
		parent.appendChild((DOMNode)child);

		return child;
	}

	protected <T> T createComment(final Page page, final DOMNode parent, final String comment) {

		final T child = (T)page.createComment(comment);
		parent.appendChild((DOMNode)child);

		return child;
	}

	protected <T> T createComponent(final DOMNode node) throws FrameworkException {
		return (T) new CreateComponentCommand().create(node);
	}

	protected <T> T cloneComponent(final DOMNode node, final DOMNode parentNode) throws FrameworkException {
		return (T) new CloneComponentCommand().cloneComponent(node, parentNode);
	}

	protected Folder getRootFolder(final Folder folder) {

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
