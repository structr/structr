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
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.dom.*;
import org.structr.web.entity.path.PagePath;
import org.structr.web.maintenance.DeployCommand;
import org.structr.web.traits.definitions.FileTraitDefinition;
import org.structr.web.traits.definitions.dom.ContentTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;
import org.structr.web.traits.definitions.dom.PageTraitDefinition;
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

	protected void compare(final String sourceHash, final boolean deleteTestDirectory) {
		compare(sourceHash, deleteTestDirectory, true);
	}

	protected void compare(final String sourceHash, final boolean deleteTestDirectory, final boolean cleanDatabase) {

		doImportExportRoundtrip(deleteTestDirectory, null, cleanDatabase);

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
		doImportExportRoundtrip(deleteTestDirectory, null, true);
	}

	protected void doImportExportRoundtrip(final boolean deleteTestDirectory, final Function callback) {
		doImportExportRoundtrip(deleteTestDirectory, callback, true);
	}

	protected void doImportExportRoundtrip(final boolean deleteTestDirectory, final Function callback, final boolean cleanDatabase) {

		final DeployCommand cmd = app.command(DeployCommand.class);
		final Path tmp          = Paths.get("/tmp/structr-deployment-test" + System.currentTimeMillis() + System.nanoTime());

		try {

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

	protected Path doExport() throws FrameworkException {

		final DeployCommand cmd = app.command(DeployCommand.class);
		final Path tmp          = Paths.get("/tmp/structr-deployment-test" + System.currentTimeMillis() + System.nanoTime());

		// export to temp directory
		final Map<String, Object> firstExportParams = new HashMap<>();
		firstExportParams.put("mode", "export");
		firstExportParams.put("target", tmp.toString());

		// execute deploy command
		cmd.execute(firstExportParams);

		return tmp;
	}

	protected void doImport(final Path path) throws FrameworkException {

		final DeployCommand cmd = app.command(DeployCommand.class);

		// import from exported source
		final Map<String, Object> firstImportParams = new HashMap<>();
		firstImportParams.put("mode", "import");
		firstImportParams.put("source", path.toString());

		// execute deploy command
		cmd.execute(firstImportParams);
	}

	protected void deleteExportAt(final Path path) throws IOException{
		Files.walkFileTree(path, new DeletingFileVisitor());
	}

	protected String calculateHash() {

		final StringBuilder buf = new StringBuilder();

		try (final Tx tx = app.tx()) {

			for (final NodeInterface page : app.nodeQuery(StructrTraits.PAGE).sort(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)).getAsList()) {

				// skip shadow document
				if (page.is(StructrTraits.SHADOW_DOCUMENT)) {
					continue;
				}

				buf.append("Page ");
				buf.append(page.getName());
				buf.append("\n");

				calculateHash(page, buf, 1);
			}

			for (final NodeInterface folder : app.nodeQuery(StructrTraits.FOLDER).sort(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)).getAsList()) {

				if (folder.as(Folder.class).includeInFrontendExport(true)) {

					buf.append("Folder ");
					buf.append(folder.getName());
					buf.append("\n");

					calculateHash(folder, buf, 1);
				}
			}

			for (final NodeInterface file : app.nodeQuery(StructrTraits.FILE).sort(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)).getAsList()) {

				if (file.as(File.class).includeInFrontendExport(true)) {

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

		if (start.is(StructrTraits.SHADOW_DOCUMENT)) {

			for (final DOMNode child : start.as(ShadowDocument.class).getElements()) {

				// only include toplevel elements of the shadow document
				if (child.getParent() == null) {

					calculateHash(child, buf, depth+1);
				}
			}

		} else if (start.is(StructrTraits.DOM_NODE)) {

			//
			if (start.is(StructrTraits.PAGE)) {

				final Page page = start.as(Page.class);

				for (final PagePath path : page.getPaths()) {

					calculateHash(path, buf, depth+1);
				}

			}

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
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.TYPE_PROPERTY)));
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY)));
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY)));

		// include owner in content hash generation!
		final AccessControllable ac = node.as(AccessControllable.class);
		final Principal owner       = ac.getOwnerNode();

		if (owner != null) {

			buf.append(valueOrEmpty(owner, Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
		}

		// include permissions in content hash generation!
		for (final Security r : ac.getSecurityRelationships()) {

			if (r != null) {

				buf.append(r.getSourceNode().getName());
				buf.append(r.getPermissions());
			}
		}

		// DOMNode
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY)));
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY)));
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHOW_FOR_LOCALES_PROPERTY)));
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_FOR_LOCALES_PROPERTY)));
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.SHARED_COMPONENT_CONFIGURATION_PROPERTY)));

		if (node.is(StructrTraits.DOM_NODE)) {

			final Page ownerDocument = node.as(DOMNode.class).getOwnerDocument();
			if (ownerDocument != null) {

				buf.append(valueOrEmpty(ownerDocument, Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			}
		}

		// DOMElement
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.DOM_ELEMENT).key(DOMNodeTraitDefinition.DATA_KEY_PROPERTY)));
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.DOM_ELEMENT).key(DOMNodeTraitDefinition.CYPHER_QUERY_PROPERTY)));
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.DOM_ELEMENT).key(DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY)));

		// Content
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.CONTENT).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY)));
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.CONTENT).key(ContentTraitDefinition.CONTENT_PROPERTY)));

		// Page
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.CACHE_FOR_SECONDS_PROPERTY)));
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.PAGE).key(DOMNodeTraitDefinition.DONT_CACHE_PROPERTY)));
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.PAGE_CREATES_RAW_DATA_PROPERTY)));
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.POSITION_PROPERTY)));
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.SHOW_ON_ERROR_CODES_PROPERTY)));

		// File
		buf.append(valueOrEmpty(node, Traits.of(StructrTraits.FILE).key(FileTraitDefinition.SIZE_PROPERTY)));

		// HTML attributes
		if (node.is(StructrTraits.DOM_ELEMENT)) {

			for (final PropertyKey key : node.as(DOMElement.class).getHtmlAttributes()) {

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

		final NodeInterface template = StructrApp.getInstance().create(StructrTraits.TEMPLATE,
			new NodeAttribute<>(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_PROPERTY), content),
			new NodeAttribute<>(Traits.of(StructrTraits.TEMPLATE).key(DOMNodeTraitDefinition.OWNER_DOCUMENT_PROPERTY), page)
		);

		if (parent != null) {
			parent.appendChild(template.as(DOMNode.class));
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
