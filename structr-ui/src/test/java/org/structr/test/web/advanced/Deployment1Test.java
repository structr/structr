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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.*;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;
import org.structr.web.entity.html.*;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.Object;
import java.util.function.Function;

import static org.testng.AssertJUnit.*;

public class Deployment1Test extends DeploymentTestBase {

	private static final Logger logger = LoggerFactory.getLogger(Deployment1Test.class.getName());

	@Test
	public void test11TemplateInTbody() {

		// setup
		try (final Tx tx = app.tx()) {

			// create first page
			final Page page1 = Page.createNewPage(securityContext,   "test11");
			final Html html1 = createElement(page1, page1, "html");
			final Head head1 = createElement(page1, html1, "head");
			createElement(page1, head1, "title", "test11_1");

			final Body body1  = createElement(page1, html1, "body");
			final Table table = createElement(page1, body1, "table");
			final Tbody tbody = createElement(page1, table, "tbody");

			final Template template1 = createTemplate(page1, tbody, "<tr><td>${user.name}</td></tr>");

			final PropertyMap template1Properties = new PropertyMap();
			template1Properties.put(StructrApp.key(DOMNode.class, "functionQuery"), "find('User')");
			template1Properties.put(StructrApp.key(DOMNode.class, "dataKey"), "user");
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
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test12");

			final Body body      = createElement(page, html, "body");
			final Script script1 = createElement(page, body, "script");
			final Script script2 = createElement(page, body, "script");

			script1.setProperty(StructrApp.key(Script.class, "_html_type"), "text/javascript");

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
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test13");

			final Body body      = createElement(page, html, "body");
			final Script script1 = createElement(page, body, "script", "");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), false);
	}

	@Test
	public void test14FileAttributesInFolders() {

		final String folderPath = "/deeply/nested/Folder Structure/with spaces";
		final String fileName   = "test14.txt";

		// setup
		try (final Tx tx = app.tx()) {

			final Folder folder     = FileHelper.createFolderPath(securityContext, folderPath);
			final File file         = FileHelper.createFile(securityContext, "test".getBytes("utf-8"), "text/plain", File.class, fileName, true);
			final Folder rootFolder = getRootFolder(folder);

			assertNotNull("Root folder should not be null", rootFolder);

			// root folder needs to have "includeInFrontendExport" set
			rootFolder.setProperty(StructrApp.key(Folder.class, "includeInFrontendExport"), true);

			file.setProperty(StructrApp.key(File.class, "parent"), folder);
			file.setProperty(StructrApp.key(File.class, "visibleToPublicUsers"), true);
			file.setProperty(StructrApp.key(File.class, "visibleToAuthenticatedUsers"), true);
			file.setProperty(StructrApp.key(File.class, "enableBasicAuth"), true);
			file.setProperty(StructrApp.key(File.class, "useAsJavascriptLibrary"), true);

			tx.success();

		} catch (IOException | FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test
		doImportExportRoundtrip(true);

		// check
		try (final Tx tx = app.tx()) {

			final Folder folder = app.nodeQuery("Folder").andName("with spaces").getFirst();

			assertNotNull("Invalid deployment result", folder);

			final File file     = app.nodeQuery("File").and(StructrApp.key(File.class, "parent"), folder).and(File.name, fileName).getFirst();

			assertNotNull("Invalid deployment result", file);

			assertEquals("Deployment import does not restore attributes correctly", folder, file.getParent());
			assertTrue("Deployment import does not restore attributes correctly",   file.getProperty(StructrApp.key(File.class, "visibleToPublicUsers")));
			assertTrue("Deployment import does not restore attributes correctly",   file.getProperty(StructrApp.key(File.class, "visibleToAuthenticatedUsers")));
			assertTrue("Deployment import does not restore attributes correctly",   file.getProperty(StructrApp.key(File.class, "enableBasicAuth")));
			assertTrue("Deployment import does not restore attributes correctly",   file.getProperty(StructrApp.key(File.class, "useAsJavascriptLibrary")));

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

			final Folder folder     = FileHelper.createFolderPath(securityContext, folderPath);
			final File file     = FileHelper.createFile(securityContext, "test".getBytes("utf-8"), "text/plain", File.class, fileName, true);
			final Folder rootFolder = getRootFolder(folder);

			assertNotNull("Root folder should not be null", rootFolder);

			// root folder needs to have "includeInFrontendExport" set
			rootFolder.setProperty(StructrApp.key(Folder.class, "includeInFrontendExport"), true);

			file.setProperty(StructrApp.key(File.class, "parent"), folder);
			file.setProperty(StructrApp.key(File.class, "visibleToPublicUsers"), true);
			file.setProperty(StructrApp.key(File.class, "visibleToAuthenticatedUsers"), true);
			file.setProperty(StructrApp.key(File.class, "enableBasicAuth"), true);
			file.setProperty(StructrApp.key(File.class, "useAsJavascriptLibrary"), true);
			file.setProperty(StructrApp.key(File.class, "includeInFrontendExport"), true);

			tx.success();

		} catch (IOException | FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test, don't clean the database but modify the file flags
		doImportExportRoundtrip(true, false, new Function() {

			@Override
			public Object apply(Object t) {

				try (final Tx tx = app.tx()) {

					final File file = app.nodeQuery("File").and(File.name, fileName).getFirst();
					file.setProperty(StructrApp.key(File.class, "visibleToPublicUsers"), false);
					file.setProperty(StructrApp.key(File.class, "visibleToAuthenticatedUsers"), false);
					file.setProperty(StructrApp.key(File.class, "enableBasicAuth"), false);
					file.setProperty(StructrApp.key(File.class, "useAsJavascriptLibrary"), false);
					file.setProperty(StructrApp.key(File.class, "includeInFrontendExport"), false);

					tx.success();

				} catch (FrameworkException fex) {}

				return null;
			}

		});

		// check
		try (final Tx tx = app.tx()) {

			final Folder folder = app.nodeQuery("Folder").andName("with spaces").getFirst();

			assertNotNull("Invalid deployment result", folder);

			final File file     = app.nodeQuery("File").and(StructrApp.key(File.class, "parent"), folder).and(File.name, fileName).getFirst();

			assertNotNull("Invalid deployment result", file);

			assertEquals("Deployment import of existing file does not restore attributes correctly", folder, file.getParent());
			assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(StructrApp.key(File.class, "visibleToPublicUsers")));
			assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(StructrApp.key(File.class, "visibleToAuthenticatedUsers")));
			assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(StructrApp.key(File.class, "enableBasicAuth")));
			assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(StructrApp.key(File.class, "useAsJavascriptLibrary")));
			assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(StructrApp.key(File.class, "includeInFrontendExport")));

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
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test16");

			final Body body = createElement(page, html, "body");
			final Div div1  = createElement(page, body, "div");

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
		compare(calculateHash(), true, false);
	}

	@Test
	public void test17NamedNonSharedTemplateWithChildren() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test17");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test17");

			final Body body = createElement(page, html, "body");

			final Template template = createTemplate(page, body, "${render(children)}");
			template.setProperty(AbstractNode.name, "a-template");

			final Template sharedTemplate = createComponent(template);

			// remove original template from page
			app.delete(template);

			createElement(page, sharedTemplate, "div");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true, false);
	}

	@Test
	public void test18NonNamedNonSharedTemplateWithChildren() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test18");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test18");

			final Body body = createElement(page, html, "body");

			final Template template = createTemplate(page, body, "${render(children)}");

			final Template sharedTemplate = createComponent(template);

			// remove original template from page
			app.delete(template);

			createElement(page, sharedTemplate, "div");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true, false);
	}

	@Test
	public void test19HtmlEntities() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test19");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test19");

			final Body body = createElement(page, html, "body");
			final Div div1  = createElement(page, body, "div");

			final Content content = createContent(page, div1, "<b>Test</b>");
			content.setProperty(StructrApp.key(Content.class, "contentType"), "text/html");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true, false);
	}

	@Test
	public void test20ExportOwnership() {

		Principal user1 = null;
		Principal user2 = null;

		try (final Tx tx = app.tx()) {

			user1 = createTestNode(User.class, new NodeAttribute<>(AbstractNode.name, "user1"));
			user2 = createTestNode(User.class, new NodeAttribute<>(AbstractNode.name, "user2"));

			tx.success();

		} catch (FrameworkException ex) {
			fail("Unexpected exception.");
		}

		assertNotNull("User was not created, test cannot continue", user1);
		assertNotNull("User was not created, test cannot continue", user2);

		// setup
		final SecurityContext context1 = SecurityContext.getInstance(user1, AccessMode.Backend);
		final App app1                 = StructrApp.getInstance(context1);

		try (final Tx tx = app1.tx()) {

			final Page page      = Page.createNewPage(context1,   "test20");
			final Html html      = createElement(page, page, "html");
			final Head head      = createElement(page, html, "head");
			createElement(page, head, "title", "test20");

			final Body body = createElement(page, html, "body");
			final Div div1  = createElement(page, body, "div");

			final Content content = createContent(page, div1, "<b>Test</b>");
			content.setProperty(StructrApp.key(Content.class, "contentType"), "text/html");

			// set owner to different user
			div1.setProperty(AbstractNode.owner, user2);
			content.setProperty(AbstractNode.owner, user2);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true, false);
	}

	@Test
	public void test21PageLinks() {

		String pageId = null;
		String a_uuid = null;

		// setup
		try (final Tx tx = app.tx()) {

			final Page page       = Page.createNewPage(securityContext,   "test02");
			pageId                = page.getUuid();
			final Html html       = createElement(page, page, "html");
			final Head head       = createElement(page, html, "head");
			createElement(page, head, "title", "test11");

			final Body body       = createElement(page, html, "body");

			// create a link which links to same page
			{
				final A a = createElement(page, body, "a");
				createElement(page, a, "a", "link to self");
				a.setLinkable(page);

				a_uuid = a.getUuid();
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		doImportExportRoundtrip(true, true, null);

		// check
		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById("A", a_uuid);
			final LinkSource a       = node.as(LinkSource.class);

			assertNotNull("A element was not created!", a);
			assertNotNull("A has no linked page!", a.getLinkable());

			assertEquals("A element is not linked to page it should be linked to", pageId, a.getLinkable().getUuid());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}
}
