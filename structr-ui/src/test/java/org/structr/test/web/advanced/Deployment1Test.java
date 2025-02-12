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
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.*;
import org.structr.web.entity.dom.*;
import org.testng.annotations.Test;

import java.io.IOException;
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
			final DOMElement html1 = createElement(page1, page1, "html");
			final DOMElement head1 = createElement(page1, html1, "head");
			createElement(page1, head1, "title", "test11_1");

			final DOMElement body1  = createElement(page1, html1, "body");
			final DOMElement table = createElement(page1, body1, "table");
			final DOMElement tbody = createElement(page1, table, "tbody");

			final Template template1 = createTemplate(page1, tbody, "<tr><td>${user.name}</td></tr>");

			final PropertyMap template1Properties = new PropertyMap();
			template1Properties.put(Traits.of("DOMNode").key("functionQuery"), "find('User')");
			template1Properties.put(Traits.of("DOMNode").key("dataKey"), "user");
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

			script1.setProperty(Traits.of("Script").key("_html_type"), "text/javascript");

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
			final NodeInterface file       = FileHelper.createFile(securityContext, "test".getBytes("utf-8"), "text/plain", "File", fileName, true);
			final NodeInterface rootFolder = getRootFolder(folder.as(Folder.class));

			assertNotNull("Root folder should not be null", rootFolder);

			// root folder needs to have "includeInFrontendExport" set
			rootFolder.setProperty(Traits.of("Folder").key("includeInFrontendExport"), true);

			file.setProperty(Traits.of("File").key("parent"), folder);
			file.setProperty(Traits.of("File").key("visibleToPublicUsers"), true);
			file.setProperty(Traits.of("File").key("visibleToAuthenticatedUsers"), true);
			file.setProperty(Traits.of("File").key("enableBasicAuth"), true);
			file.setProperty(Traits.of("File").key("useAsJavascriptLibrary"), true);

			tx.success();

		} catch (IOException | FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test
		doImportExportRoundtrip(true);

		// check
		try (final Tx tx = app.tx()) {

			final NodeInterface folder = app.nodeQuery("Folder").andName("with spaces").getFirst();

			assertNotNull("Folder was not created correctly", folder);

			final NodeInterface file = app.nodeQuery("File").and(Traits.of("File").key("parent"), folder).and(Traits.of("File").key("name"), fileName).getFirst();

			assertNotNull("File was not created correctly", file);

			assertEquals("Deployment import does not restore attributes correctly", folder, file.as(File.class).getParent());
			assertTrue("Deployment import does not restore attributes correctly",  file.getProperty(Traits.of("File").key("visibleToPublicUsers")));
			assertTrue("Deployment import does not restore attributes correctly",  file.getProperty(Traits.of("File").key("visibleToAuthenticatedUsers")));
			assertTrue("Deployment import does not restore attributes correctly",  file.getProperty(Traits.of("File").key("enableBasicAuth")));
			assertTrue("Deployment import does not restore attributes correctly",  file.getProperty(Traits.of("File").key("useAsJavascriptLibrary")));

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
			final NodeInterface file       = FileHelper.createFile(securityContext, "test".getBytes("utf-8"), "text/plain", "File", fileName, true);
			final NodeInterface rootFolder = getRootFolder(folder.as(Folder.class));

			assertNotNull("Root folder should not be null", rootFolder);

			// root folder needs to have "includeInFrontendExport" set
			rootFolder.setProperty(Traits.of("Folder").key("includeInFrontendExport"), true);

			file.setProperty(Traits.of("File").key("parent"), folder);
			file.setProperty(Traits.of("File").key("visibleToPublicUsers"), true);
			file.setProperty(Traits.of("File").key("visibleToAuthenticatedUsers"), true);
			file.setProperty(Traits.of("File").key("enableBasicAuth"), true);
			file.setProperty(Traits.of("File").key("useAsJavascriptLibrary"), true);
			file.setProperty(Traits.of("File").key("includeInFrontendExport"), true);

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

					final NodeInterface file = app.nodeQuery("File").and(Traits.of("File").key("name"), fileName).getFirst();
					file.setProperty(Traits.of("File").key("visibleToPublicUsers"), false);
					file.setProperty(Traits.of("File").key("visibleToAuthenticatedUsers"), false);
					file.setProperty(Traits.of("File").key("enableBasicAuth"), false);
					file.setProperty(Traits.of("File").key("useAsJavascriptLibrary"), false);
					file.setProperty(Traits.of("File").key("includeInFrontendExport"), false);

					tx.success();

				} catch (FrameworkException fex) {}

				return null;
			}

		});

		// check
		try (final Tx tx = app.tx()) {

			final NodeInterface folder = app.nodeQuery("Folder").andName("with spaces").getFirst();

			assertNotNull("Folder was not created", folder);

			final NodeInterface file = app.nodeQuery("File").and(Traits.of("File").key("parent"), folder).and(Traits.of("File").key("name"), fileName).getFirst();

			assertNotNull("File was not created", file);

			assertEquals("Deployment import of existing file does not restore attributes correctly", folder, file.as(File.class).getParent());
			assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(Traits.of("File").key("visibleToPublicUsers")));
			assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(Traits.of("File").key("visibleToAuthenticatedUsers")));
			assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(Traits.of("File").key("enableBasicAuth")));
			assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(Traits.of("File").key("useAsJavascriptLibrary")));
			assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(Traits.of("File").key("includeInFrontendExport")));

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
		compare(calculateHash(), true, false);
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
			template.setProperty(Traits.of("NodeInterface").key("name"), "a-template");

			final DOMNode sharedTemplate = createComponent(template);

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
		compare(calculateHash(), true, false);
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
			content.setProperty(Traits.of("Content").key("contentType"), "text/html");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true, false);
	}

	@Test
	public void test20ExportOwnership() {

		NodeInterface user1 = null;
		NodeInterface user2 = null;

		try (final Tx tx = app.tx()) {

			user1 = createTestNode("User", new NodeAttribute<>(Traits.of("NodeInterface").key("name"), "user1"));
			user2 = createTestNode("User", new NodeAttribute<>(Traits.of("NodeInterface").key("name"), "user2"));

			tx.success();

		} catch (FrameworkException ex) {
			fail("Unexpected exception.");
		}

		assertNotNull("User was not created, test cannot continue", user1);
		assertNotNull("User was not created, test cannot continue", user2);

		// setup
		final SecurityContext context1 = SecurityContext.getInstance(user1.as(User.class), AccessMode.Backend);
		final App app1                 = StructrApp.getInstance(context1);

		try (final Tx tx = app1.tx()) {

			final Page page       = Page.createNewPage(context1,   "test20");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test20");

			final DOMElement body = createElement(page, html, "body");
			final DOMElement div1 = createElement(page, body, "div");

			final Content content = createContent(page, div1, "<b>Test</b>");
			content.setProperty(Traits.of("Content").key("contentType"), "text/html");

			// set owner to different user
			div1.setProperty(Traits.of("NodeInterface").key("owner"), user2);
			content.setProperty(Traits.of("NodeInterface").key("owner"), user2);

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
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");

			createElement(page, head, "title", "test11");

			final DOMElement body       = createElement(page, html, "body");

			// create a link which links to same page
			{
				final DOMElement a = createElement(page, body, "a");

				createElement(page, a, "a", "link to self");

				a.as(LinkSource.class).setLinkable(page.as(Linkable.class));

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
