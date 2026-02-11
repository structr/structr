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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.*;
import org.structr.web.entity.dom.*;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.LinkableTraitDefinition;
import org.structr.web.traits.definitions.dom.ContentTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;
import org.structr.web.traits.definitions.html.Script;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.function.Function;

import static org.testng.AssertJUnit.*;

public class Deployment1Test extends DeploymentTestBase {

	private static final Logger logger = LoggerFactory.getLogger(Deployment1Test.class.getName());

	@Test
	public void test10SharedComponent() {

		// setup
		try (final Tx tx = app.tx()) {

			// create first page
			final Page page1 = Page.createNewPage(securityContext,   "test10_1");
			final DOMElement html1 = createElement(page1, page1, "html");
			final DOMElement head1 = createElement(page1, html1, "head");
			createElement(page1, head1, "title", "test10_1");

			final DOMElement body1 = createElement(page1, html1, "body");
			final DOMElement div1   = createElement(page1, body1, "div");

			createElement(page1, div1, "div", "test1");
			createElement(page1, div1, "div", "test1");

			final DOMNode component = createComponent(div1);

			// create second page
			final Page page2 = Page.createNewPage(securityContext,   "test10_2");
			final DOMElement html2 = createElement(page2, page2, "html");
			final DOMElement head2 = createElement(page2, html2, "head");
			createElement(page2, head2, "title", "test10_2");

			final DOMElement body2 = createElement(page2, html2, "body");
			final DOMElement div2   = createElement(page2, body2, "div");

			// re-use template from above
			cloneComponent(component, div2);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test11TemplateInTbody() {

		// setup
		try (final Tx tx = app.tx()) {

			// create first page
			final Page page1 = Page.createNewPage(securityContext,   "test11");
			final DOMElement html1 = createElement(page1, page1, "html");
			final DOMElement head1 = createElement(page1, html1, "head");
			createElement(page1, head1, "title", "test11_1");

			final DOMElement body1  = createElement(page1, html1, "body");
			final DOMElement table = createElement(page1, body1, "table");
			final DOMElement tbody = createElement(page1, table, "tbody");

			final Template template1 = createTemplate(page1, tbody, "<tr><td>${user.name}</td></tr>");

			final PropertyMap template1Properties = new PropertyMap();
			template1Properties.put(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY), "find('User')");
			template1Properties.put(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.DATA_KEY_PROPERTY), "user");
			template1.setProperties(template1.getSecurityContext(), template1Properties);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test12EmptyContentElementWithContentType() {

		// setup
		try (final Tx tx = app.tx()) {

			// create first page
			final Page page = Page.createNewPage(securityContext,   "test12");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test12");

			final DOMElement body      = createElement(page, html, "body");
			final DOMElement script1 = createElement(page, body, "script");
			final DOMElement script2 = createElement(page, body, "script");

			script1.setProperty(Traits.of(StructrTraits.SCRIPT).key(Script.TYPE_PROPERTY), "text/javascript");

			createContent(page, script1, "");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test13EmptyContentElement() {

		// setup
		try (final Tx tx = app.tx()) {

			// create first page
			final Page page = Page.createNewPage(securityContext,   "test13");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test13");

			final DOMElement body      = createElement(page, html, "body");
			final DOMElement script1 = createElement(page, body, "script", "");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test14FileAttributesInFolders() {

		final String folderPath = "/deeply/nested/Folder Structure/with spaces";
		final String fileName   = "test14.txt";

		// setup
		try (final Tx tx = app.tx()) {

			final NodeInterface folder     = FileHelper.createFolderPath(securityContext, folderPath);
			final NodeInterface file       = FileHelper.createFile(securityContext, "test".getBytes("utf-8"), "text/plain", StructrTraits.FILE, fileName, true);
			final NodeInterface rootFolder = getRootFolder(folder.as(Folder.class));

			assertNotNull("Root folder should not be null", rootFolder);

			// root folder needs to have "includeInFrontendExport" set
			rootFolder.setProperty(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true);

			file.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder);
			file.setProperty(Traits.of(StructrTraits.FILE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);
			file.setProperty(Traits.of(StructrTraits.FILE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
			file.setProperty(Traits.of(StructrTraits.FILE).key(LinkableTraitDefinition.ENABLE_BASIC_AUTH_PROPERTY), true);

			tx.success();

		} catch (IOException | FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test
		doImportExportRoundtrip(true);

		// check
		try (final Tx tx = app.tx()) {

			final NodeInterface folder = app.nodeQuery(StructrTraits.FOLDER).name("with spaces").getFirst();

			assertNotNull("Folder was not created correctly", folder);

			final NodeInterface file = app.nodeQuery(StructrTraits.FILE).key(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder).key(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), fileName).getFirst();

			assertNotNull("File was not created correctly", file);

			assertEquals("Deployment import does not restore attributes correctly", folder, file.as(File.class).getParent());
			assertTrue("Deployment import does not restore attributes correctly",  file.getProperty(Traits.of(StructrTraits.FILE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY)));
			assertTrue("Deployment import does not restore attributes correctly",  file.getProperty(Traits.of(StructrTraits.FILE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY)));
			assertTrue("Deployment import does not restore attributes correctly",  file.getProperty(Traits.of(StructrTraits.FILE).key(LinkableTraitDefinition.ENABLE_BASIC_AUTH_PROPERTY)));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

	}

	@Test
	public void test15FileAttributesOnUpdate() {

		final String folderPath = "/deeply/nested/Folder Structure/with spaces";
		final String fileName   = "test15.txt";

		// setup
		try (final Tx tx = app.tx()) {

			final NodeInterface folder     = FileHelper.createFolderPath(securityContext, folderPath);
			final NodeInterface file       = FileHelper.createFile(securityContext, "test".getBytes("utf-8"), "text/plain", StructrTraits.FILE, fileName, true);
			final NodeInterface rootFolder = getRootFolder(folder.as(Folder.class));

			assertNotNull("Root folder should not be null", rootFolder);

			// root folder needs to have "includeInFrontendExport" set
			rootFolder.setProperty(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true);

			file.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder);
			file.setProperty(Traits.of(StructrTraits.FILE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);
			file.setProperty(Traits.of(StructrTraits.FILE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
			file.setProperty(Traits.of(StructrTraits.FILE).key(LinkableTraitDefinition.ENABLE_BASIC_AUTH_PROPERTY), true);
			file.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true);

			tx.success();

		} catch (IOException | FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test, don't clean the database but modify the file flags
		doImportExportRoundtrip(true, new Function() {

			@Override
			public Object apply(Object t) {

				try (final Tx tx = app.tx()) {

					final NodeInterface file = app.nodeQuery(StructrTraits.FILE).key(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), fileName).getFirst();
					file.setProperty(Traits.of(StructrTraits.FILE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), false);
					file.setProperty(Traits.of(StructrTraits.FILE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), false);
					file.setProperty(Traits.of(StructrTraits.FILE).key(LinkableTraitDefinition.ENABLE_BASIC_AUTH_PROPERTY), false);
					file.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), false);

					tx.success();

				} catch (FrameworkException fex) {}

				return null;
			}

		}, false);

		// check
		try (final Tx tx = app.tx()) {

			final NodeInterface folder = app.nodeQuery(StructrTraits.FOLDER).name("with spaces").getFirst();

			assertNotNull("Folder was not created", folder);

			final NodeInterface file = app.nodeQuery(StructrTraits.FILE).key(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder).key(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), fileName).getFirst();

			assertNotNull("File was not created", file);

			assertEquals("Deployment import of existing file does not restore attributes correctly", folder, file.as(File.class).getParent());
			assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(Traits.of(StructrTraits.FILE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY)));
			assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(Traits.of(StructrTraits.FILE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY)));
			assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(Traits.of(StructrTraits.FILE).key(LinkableTraitDefinition.ENABLE_BASIC_AUTH_PROPERTY)));
			assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY)));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

	}

	@Test
	public void test15FileAttributesOnUpdateWithCleanDatabase() {

		final String folderPath = "/deeply/nested/Folder Structure/with spaces";
		final String fileName   = "test15.txt";

		// setup
		try (final Tx tx = app.tx()) {

			final NodeInterface folder     = FileHelper.createFolderPath(securityContext, folderPath);
			final NodeInterface file       = FileHelper.createFile(securityContext, "test".getBytes("utf-8"), "text/plain", StructrTraits.FILE, fileName, true);
			final NodeInterface rootFolder = getRootFolder(folder.as(Folder.class));

			assertNotNull("Root folder should not be null", rootFolder);

			// root folder needs to have "includeInFrontendExport" set
			rootFolder.setProperty(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true);

			file.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder);
			file.setProperty(Traits.of(StructrTraits.FILE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY), true);
			file.setProperty(Traits.of(StructrTraits.FILE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
			file.setProperty(Traits.of(StructrTraits.FILE).key(LinkableTraitDefinition.ENABLE_BASIC_AUTH_PROPERTY), true);
			file.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true);

			tx.success();

		} catch (IOException | FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		doImportExportRoundtrip(true, null);

		// check
		try (final Tx tx = app.tx()) {

			final NodeInterface folder = app.nodeQuery(StructrTraits.FOLDER).name("with spaces").getFirst();

			assertNotNull("Folder was not created", folder);

			final NodeInterface file = app.nodeQuery(StructrTraits.FILE).key(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder).key(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), fileName).getFirst();

			assertNotNull("File was not created", file);

			assertEquals("Deployment import of existing file does not restore attributes correctly", folder, file.as(File.class).getParent());
			assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(Traits.of(StructrTraits.FILE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY)));
			assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(Traits.of(StructrTraits.FILE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY)));
			assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(Traits.of(StructrTraits.FILE).key(LinkableTraitDefinition.ENABLE_BASIC_AUTH_PROPERTY)));
			assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY)));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

	}

	@Test
	public void test16SharedTemplateWithChildren() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test16");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test16");

			final DOMElement body = createElement(page, html, "body");
			final DOMElement div1  = createElement(page, body, "div");

			final Template template = createTemplate(page, div1, "template source - öäüÖÄÜß'\"'`");

			createElement(page, template, "div");
			final DOMNode table = createElement(page, template, "table");
			final DOMNode tr    = createElement(page, table, "tr");
			createElement(page, tr, "td");
			createElement(page, tr, "td");

			createComponent(template);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test17NamedNonSharedTemplateWithChildren() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test17");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test17");

			final DOMElement body = createElement(page, html, "body");

			final Template template = createTemplate(page, body, "${render(children)}");
			template.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "a-template");

			final DOMNode sharedTemplate = createComponent(template);

			// remove original template from page
			app.delete(template);

			createElement(page, sharedTemplate, "div");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test18NonNamedNonSharedTemplateWithChildren() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test18");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test18");

			final DOMElement body = createElement(page, html, "body");

			final Template template = createTemplate(page, body, "${render(children)}");

			final DOMNode sharedTemplate = createComponent(template);

			// remove original template from page
			app.delete(template);

			createElement(page, sharedTemplate, "div");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test19HtmlEntities() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test19");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test19");

			final DOMElement body = createElement(page, html, "body");
			final DOMElement div1  = createElement(page, body, "div");

			final Content content = createContent(page, div1, "<b>Test</b>");
			content.setProperty(Traits.of(StructrTraits.CONTENT).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/html");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}
}
