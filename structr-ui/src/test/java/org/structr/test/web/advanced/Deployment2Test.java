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
import org.structr.common.AccessControllable;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.function.Function;

import static org.testng.AssertJUnit.*;

public class Deployment2Test extends DeploymentTestBase {

	private static final Logger logger = LoggerFactory.getLogger(Deployment2Test.class.getName());

	@Test
	public void test21ExportGrants() {

		Principal user1 = null;
		Principal user2 = null;

		try (final Tx tx = app.tx()) {

			user1 = createTestNode(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "user1")).as(Principal.class);
			user2 = createTestNode(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "user2")).as(Principal.class);

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

			final Page page      = Page.createNewPage(context1, "test21");
			final DOMElement html      = createElement(page, page, "html");
			final DOMElement head      = createElement(page, html, "head");
			createElement(page, head, "title", "test21");

			final DOMElement body = createElement(page, html, "body");
			final DOMElement div1  = createElement(page, body, "div");

			final Content content = createContent(page, div1, "<b>Test</b>");
			content.setProperty(Traits.of(StructrTraits.CONTENT).key("contentType"), "text/html");

			// create grants
			page.as(AccessControllable.class).grant(Permission.read, user2);
			div1.as(AccessControllable.class).grant(Permission.read, user2);
			content.as(AccessControllable.class).grant(Permission.read, user2);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true, false);
	}

	@Test
	public void test22TemplateOwnershipAndGrants() {

		Principal user1 = null;
		Principal user2 = null;

		try (final Tx tx = app.tx()) {

			user1 = createTestNode(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "user1")).as(Principal.class);
			user2 = createTestNode(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "user2")).as(Principal.class);

			tx.success();

		} catch (FrameworkException ex) {
			fail("Unexpected exception.");
		}

		assertNotNull("User was not created, test cannot continue", user1);
		assertNotNull("User was not created, test cannot continue", user2);

		// setup
		try (final Tx tx = app.tx()) {

			// create first page
			final Page page1 = Page.createNewPage(securityContext,   "test22_1");
			final DOMElement html1 = createElement(page1, page1, "html");
			final DOMElement head1 = createElement(page1, html1, "head");
			createElement(page1, head1, "title", "test22_1");

			final DOMElement body1 = createElement(page1, html1, "body");
			final DOMElement div1   = createElement(page1, body1, "div");

			createElement(page1, div1, "div", "test1");
			createElement(page1, div1, "div", "test1");

			final DOMNode component = createComponent(div1);

			// create second page
			final Page page2 = Page.createNewPage(securityContext,   "test22_2");
			final DOMElement html2 = createElement(page2, page2, "html");
			final DOMElement head2 = createElement(page2, html2, "head");
			createElement(page2, head2, "title", "test22_2");

			final DOMElement body2 = createElement(page2, html2, "body");
			final DOMElement div2   = createElement(page2, body2, "div");

			// re-use template from above
			final DOMNode cloned = cloneComponent(component, div2);

			component.as(AccessControllable.class).grant(Permission.read, user1);
			cloned.as(AccessControllable.class).grant(Permission.read, user2);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		doImportExportRoundtrip(true, true, new Function() {

			@Override
			public Object apply(Object t) {

				try (final Tx tx = app.tx()) {

					createTestNode(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "user1")).as(Principal.class);
					createTestNode(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "user2")).as(Principal.class);

					tx.success();

				} catch (FrameworkException ex) {
					fail("Unexpected exception.");
				}

				return null;
			}
		});
	}

	@Test
	public void test23FileOwnershipAndGrants() {

		Principal user1 = null;
		Principal user2 = null;

		try (final Tx tx = app.tx()) {

			user1 = createTestNode(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "user1")).as(Principal.class);
			user2 = createTestNode(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "user2")).as(Principal.class);

			tx.success();

		} catch (FrameworkException ex) {
			fail("Unexpected exception.");
		}

		assertNotNull("User was not created, test cannot continue", user1);
		assertNotNull("User was not created, test cannot continue", user2);

		// setup
		try (final Tx tx = app.tx()) {

			// create some files and folders
			final NodeInterface folder1  = app.create(StructrTraits.FOLDER, new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key("name"), "Folder1"), new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key("includeInFrontendExport"), true));
			final NodeInterface folder2  = app.create(StructrTraits.FOLDER, new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key("name"), "Folder2"), new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key("parent"), folder1));

			final NodeInterface file1  = FileHelper.createFile(securityContext, "test".getBytes(), "text/plain", StructrTraits.FILE, "test1.txt", true);
			final NodeInterface file2  = FileHelper.createFile(securityContext, "test".getBytes(), "text/plain", StructrTraits.FILE, "test2.txt", true);

			file1.as(File.class).setParent(folder2.as(Folder.class));
			file2.as(File.class).setParent(folder2.as(Folder.class));

			folder1.setProperty(Traits.of(StructrTraits.FOLDER).key("owner"), user1);
			folder1.as(AccessControllable.class).grant(Permission.read, user2);

			folder2.setProperty(Traits.of(StructrTraits.FOLDER).key("owner"), user2);
			folder2.as(AccessControllable.class).grant(Permission.write, user1);

			file1.setProperty(Traits.of(StructrTraits.FILE).key("owner"), user1);
			file2.setProperty(Traits.of(StructrTraits.FILE).key("owner"), user2);

			file1.setProperty(Traits.of(StructrTraits.FOLDER).key("owner"), user1);
			file1.as(AccessControllable.class).grant(Permission.read, user2);

			file2.setProperty(Traits.of(StructrTraits.FOLDER).key("owner"), user2);
			file2.as(AccessControllable.class).grant(Permission.write, user1);

			tx.success();

		} catch (IOException | FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		doImportExportRoundtrip(true, true, new Function() {

			@Override
			public Object apply(Object t) {

				try (final Tx tx = app.tx()) {

					createTestNode(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "user1")).as(Principal.class);
					createTestNode(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key("name"), "user2")).as(Principal.class);

					tx.success();

				} catch (FrameworkException ex) {
					fail("Unexpected exception.");
				}

				return null;
			}
		});
	}

	@Test
	public void test24ContentShowConditions() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test24");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test24");

			final DOMElement body        = createElement(page, html, "body");
			final DOMElement div1         = createElement(page, body, "div");
			final Content content1 = createContent(page, div1, "${current.type}");
			final Content content2 = createContent(page, div1, "${find('User', 'name', '@structr')[0].id}");
			final Content content3 = createContent(page, div1, "${find('User', 'name', '@structr')[0].id}");

			content1.setProperty(Traits.of(StructrTraits.DOM_NODE).key("showConditions"), "eq(current.type, 'MyTestFolder')");
			content2.setProperty(Traits.of(StructrTraits.DOM_NODE).key("showConditions"), "if(equal(extract(first(find('User', 'name' 'structr')), 'name'), '@structr'), true, false)");
			content3.setProperty(Traits.of(StructrTraits.DOM_NODE).key("showConditions"), "(((((([]))))))"); // for testing only

			tx.success();


		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test25ExtendedBuiltinTypes() {

		/* This method tests whether files, folders and images that are
		 * considered part of application data (derived from built-in
		 * types) are ignored in the deployment process.
		 */

		// setup
		try (final Tx tx = app.tx()) {

			// create extended folder class
			app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key("name"), "ExtendedFolder"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key("inheritedTraits"), new String[] { StructrTraits.FOLDER })
			);

			// create extended file class
			app.create(StructrTraits.SCHEMA_NODE,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key("name"), "ExtendedFile"),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_NODE).key("inheritedTraits"), new String[] { StructrTraits.FILE })
			);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("Unexpected exception {}", fex.getMessage());
			fail("Unexpected exception.");
		}

		// setup
		try (final Tx tx = app.tx()) {

			final NodeInterface folder1 = app.create("ExtendedFolder", "folder1");
			final NodeInterface folder2 = app.create("ExtendedFolder",
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key("name"), "folder2"),
				new NodeAttribute(Traits.of(StructrTraits.FOLDER).key("parent"), folder1)
			);

			app.create("ExtendedFile",
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key("name"), "file1.txt"),
				new NodeAttribute(Traits.of(StructrTraits.FILE).key("parent"), folder1)
			);

			app.create("ExtendedFile",
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key("name"), "file2.txt"),
				new NodeAttribute(Traits.of(StructrTraits.FILE).key("parent"), folder2)
			);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("Unexpected exception {}", fex.getMessage());
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true, true);
	}

	@Test
	public void test26Escaping() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test25");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test25");

			final DOMElement body        = createElement(page, html, "body");
			final DOMElement div1         = createElement(page, body, "div");
			final Content content1 = createContent(page, div1, "<div><script>var test = '<h3>Title</h3>';</script></div>");

			content1.setProperty(Traits.of(StructrTraits.CONTENT).key("contentType"), "text/html");

			tx.success();


		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		compare(calculateHash(), true);
	}

	@Test
	public void test27FileAttributes() {

		final String fileName1 = "test27_1.txt";
		final String fileName2 = "test27_2.txt";

		// setup
		try (final Tx tx = app.tx()) {

			final NodeInterface file1 = FileHelper.createFile(securityContext, "test".getBytes("utf-8"), "text/plain", StructrTraits.FILE, fileName1, true);
			final NodeInterface file2 = FileHelper.createFile(securityContext, "test".getBytes("utf-8"), "text/plain", StructrTraits.FILE, fileName2, true);

			file1.setProperty(Traits.of(StructrTraits.FILE).key("visibleToPublicUsers"), true);
			file1.setProperty(Traits.of(StructrTraits.FILE).key("visibleToAuthenticatedUsers"), true);
			file1.setProperty(Traits.of(StructrTraits.FILE).key("enableBasicAuth"), true);
			file1.setProperty(Traits.of(StructrTraits.FILE).key("useAsJavascriptLibrary"), true);

			file2.setProperty(Traits.of(StructrTraits.FILE).key("visibleToPublicUsers"), true);
			file2.setProperty(Traits.of(StructrTraits.FILE).key("visibleToAuthenticatedUsers"), true);
			file2.setProperty(Traits.of(StructrTraits.FILE).key("enableBasicAuth"), true);
			file2.setProperty(Traits.of(StructrTraits.FILE).key("includeInFrontendExport"), true);

			tx.success();

		} catch (IOException | FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test
		doImportExportRoundtrip(true);

		// check
		try (final Tx tx = app.tx()) {

			final NodeInterface file1 = app.nodeQuery(StructrTraits.FILE).and(Traits.of(StructrTraits.FILE).key("name"), fileName1).getFirst();
			final NodeInterface file2 = app.nodeQuery(StructrTraits.FILE).and(Traits.of(StructrTraits.FILE).key("name"), fileName2).getFirst();

			assertNotNull("Invalid deployment result", file1);
			assertNotNull("Invalid deployment result", file2);

			assertTrue("Deployment import does not restore attributes correctly",  file1.getProperty(Traits.of(StructrTraits.FILE).key("visibleToPublicUsers")));
			assertTrue("Deployment import does not restore attributes correctly",  file1.getProperty(Traits.of(StructrTraits.FILE).key("visibleToAuthenticatedUsers")));
			assertTrue("Deployment import does not restore attributes correctly",  file1.getProperty(Traits.of(StructrTraits.FILE).key("enableBasicAuth")));
			assertTrue("Deployment import does not restore attributes correctly",  file1.getProperty(Traits.of(StructrTraits.FILE).key("useAsJavascriptLibrary")));
			assertFalse("Deployment import does not restore attributes correctly", file1.getProperty(Traits.of(StructrTraits.FILE).key("includeInFrontendExport")));

			assertTrue("Deployment import does not restore attributes correctly",  file2.getProperty(Traits.of(StructrTraits.FILE).key("visibleToPublicUsers")));
			assertTrue("Deployment import does not restore attributes correctly",  file2.getProperty(Traits.of(StructrTraits.FILE).key("visibleToAuthenticatedUsers")));
			assertTrue("Deployment import does not restore attributes correctly",  file2.getProperty(Traits.of(StructrTraits.FILE).key("enableBasicAuth")));
			assertFalse("Deployment import does not restore attributes correctly", file2.getProperty(Traits.of(StructrTraits.FILE).key("useAsJavascriptLibrary")));
			assertTrue("Deployment import does not restore attributes correctly" , file2.getProperty(Traits.of(StructrTraits.FILE).key("includeInFrontendExport")));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test28MailTemplates() {

		// setup
		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.MAIL_TEMPLATE,
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key("name"),   "template1"),
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key("locale"), "de_DE"),
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key("text"),   "text1"),
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key("visibleToPublicUsers"), true)
			);

			app.create(StructrTraits.MAIL_TEMPLATE,
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key("name"),   "template2"),
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key("locale"), "en"),
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key("text"),   "text2"),
				new NodeAttribute<>(Traits.of(StructrTraits.MAIL_TEMPLATE).key("visibleToAuthenticatedUsers"), true)
			);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test
		doImportExportRoundtrip(true);

		// check
		try (final Tx tx = app.tx()) {

			final NodeInterface template1 = app.nodeQuery(StructrTraits.MAIL_TEMPLATE).and(Traits.of(StructrTraits.MAIL_TEMPLATE).key("name"), "template1").getFirst();
			final NodeInterface template2 = app.nodeQuery(StructrTraits.MAIL_TEMPLATE).and(Traits.of(StructrTraits.MAIL_TEMPLATE).key("name"), "template2").getFirst();

			assertNotNull("Invalid deployment result", template1);
			assertNotNull("Invalid deployment result", template2);

			assertEquals("Invalid MailTemplate deployment result", "template1", template1.getProperty(Traits.of(StructrTraits.MAIL_TEMPLATE).key("name")));
			assertEquals("Invalid MailTemplate deployment result", "de_DE",     template1.getProperty(Traits.of(StructrTraits.MAIL_TEMPLATE).key("locale")));
			assertEquals("Invalid MailTemplate deployment result", "text1",     template1.getProperty(Traits.of(StructrTraits.MAIL_TEMPLATE).key("text")));
			assertEquals("Invalid MailTemplate deployment result", true,        (boolean)template1.getProperty(Traits.of(StructrTraits.MAIL_TEMPLATE).key("visibleToPublicUsers")));
			assertEquals("Invalid MailTemplate deployment result", false,       (boolean)template1.getProperty(Traits.of(StructrTraits.MAIL_TEMPLATE).key("visibleToAuthenticatedUsers")));

			assertEquals("Invalid MailTemplate deployment result", "template2", template2.getProperty(Traits.of(StructrTraits.MAIL_TEMPLATE).key("name")));
			assertEquals("Invalid MailTemplate deployment result", "en",        template2.getProperty(Traits.of(StructrTraits.MAIL_TEMPLATE).key("locale")));
			assertEquals("Invalid MailTemplate deployment result", "text2",     template2.getProperty(Traits.of(StructrTraits.MAIL_TEMPLATE).key("text")));
			assertEquals("Invalid MailTemplate deployment result", false,       (boolean)template2.getProperty(Traits.of(StructrTraits.MAIL_TEMPLATE).key("visibleToPublicUsers")));
			assertEquals("Invalid MailTemplate deployment result", true,        (boolean)template2.getProperty(Traits.of(StructrTraits.MAIL_TEMPLATE).key("visibleToAuthenticatedUsers")));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test29Localizations() {

		// setup
		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.LOCALIZATION,
				new NodeAttribute<>(Traits.of(StructrTraits.LOCALIZATION).key("name"),                "localization1"),
				new NodeAttribute<>(Traits.of(StructrTraits.LOCALIZATION).key("domain"),              "domain1"),
				new NodeAttribute<>(Traits.of(StructrTraits.LOCALIZATION).key("locale"),              "de_DE"),
				new NodeAttribute<>(Traits.of(StructrTraits.LOCALIZATION).key("localizedName"),       "localizedName1")
			);

			app.create(StructrTraits.LOCALIZATION,
				new NodeAttribute<>(Traits.of(StructrTraits.LOCALIZATION).key("name"),                       "localization2"),
				new NodeAttribute<>(Traits.of(StructrTraits.LOCALIZATION).key("domain"),                     "domain2"),
				new NodeAttribute<>(Traits.of(StructrTraits.LOCALIZATION).key("locale"),                     "en"),
				new NodeAttribute<>(Traits.of(StructrTraits.LOCALIZATION).key("localizedName"),              "localizedName2")
			);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test
		doImportExportRoundtrip(true);

		// check
		try (final Tx tx = app.tx()) {

			final NodeInterface localization1 = app.nodeQuery(StructrTraits.LOCALIZATION).and(Traits.of(StructrTraits.LOCALIZATION).key("name"), "localization1").getFirst();
			final NodeInterface localization2 = app.nodeQuery(StructrTraits.LOCALIZATION).and(Traits.of(StructrTraits.LOCALIZATION).key("name"), "localization2").getFirst();

			assertNotNull("Invalid deployment result", localization1);
			assertNotNull("Invalid deployment result", localization2);

			assertEquals("Invalid Localization deployment result", "localization1",  localization1.getProperty(Traits.of(StructrTraits.LOCALIZATION).key("name")));
			assertEquals("Invalid Localization deployment result", "domain1",        localization1.getProperty(Traits.of(StructrTraits.LOCALIZATION).key("domain")));
			assertEquals("Invalid Localization deployment result", "de_DE",          localization1.getProperty(Traits.of(StructrTraits.LOCALIZATION).key("locale")));
			assertEquals("Invalid Localization deployment result", "localizedName1", localization1.getProperty(Traits.of(StructrTraits.LOCALIZATION).key("localizedName")));
			assertEquals("Invalid Localization deployment result", true,             (boolean)localization1.getProperty(Traits.of(StructrTraits.LOCALIZATION).key("visibleToPublicUsers")));
			assertEquals("Invalid Localization deployment result", true,             (boolean)localization1.getProperty(Traits.of(StructrTraits.LOCALIZATION).key("visibleToAuthenticatedUsers")));

			assertEquals("Invalid Localization deployment result", "localization2",  localization2.getProperty(Traits.of(StructrTraits.LOCALIZATION).key("name")));
			assertEquals("Invalid Localization deployment result", "domain2",        localization2.getProperty(Traits.of(StructrTraits.LOCALIZATION).key("domain")));
			assertEquals("Invalid Localization deployment result", "en",             localization2.getProperty(Traits.of(StructrTraits.LOCALIZATION).key("locale")));
			assertEquals("Invalid Localization deployment result", "localizedName2", localization2.getProperty(Traits.of(StructrTraits.LOCALIZATION).key("localizedName")));
			assertEquals("Invalid Localization deployment result", true,             (boolean)localization2.getProperty(Traits.of(StructrTraits.LOCALIZATION).key("visibleToPublicUsers")));
			assertEquals("Invalid Localization deployment result", true,             (boolean)localization2.getProperty(Traits.of(StructrTraits.LOCALIZATION).key("visibleToAuthenticatedUsers")));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test30IncreasingIndentationCountInRoundtrip() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test30");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test30");

			final DOMElement body     = createElement(page, html, "body");
			final DOMElement div1     = createElement(page, body, "div");
			final DOMElement div2     = createElement(page, div1, "div", "This is a test.");
			final DOMElement table1   = createElement(page, div2, "table");
			final DOMElement thead    = createElement(page, table1, "thead");
			final DOMElement tbody    = createElement(page, table1, "tbody");
			final DOMElement tr1      = createElement(page, thead, "tr");
			final DOMElement tr2      = createElement(page, tbody, "tr");
			final DOMElement td11     = createElement(page, tr1, "td", "content11", "Content before <select>");
			final DOMElement td12     = createElement(page, tr1, "td", "content12");
			final DOMElement td21     = createElement(page, tr2, "td", "content21");
			final DOMElement td22     = createElement(page, tr2, "td", "content22");
			final DOMElement select   = createElement(page, td11, "select");
			final DOMElement option1  = createElement(page, select, "option", "value1");
			final DOMElement option2  = createElement(page, select, "option", "value2");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		compare(calculateHash(), true);
	}
}
