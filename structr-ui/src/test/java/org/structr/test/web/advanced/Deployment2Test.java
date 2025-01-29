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
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Localization;
import org.structr.core.entity.MailTemplate;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.html.*;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.Object;
import java.util.function.Function;

import static org.testng.AssertJUnit.*;

public class Deployment2Test extends DeploymentTestBase {

	private static final Logger logger = LoggerFactory.getLogger(Deployment2Test.class.getName());

	@Test
	public void test21ExportGrants() {

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

			final Page page      = Page.createNewPage(context1, "test21");
			final Html html      = createElement(page, page, "html");
			final Head head      = createElement(page, html, "head");
			createElement(page, head, "title", "test21");

			final Body body = createElement(page, html, "body");
			final Div div1  = createElement(page, body, "div");

			final Content content = createContent(page, div1, "<b>Test</b>");
			content.setProperty(Traits.of("Content").key("contentType"), "text/html");

			// create grants
			page.grant(Permission.read, user2);
			div1.grant(Permission.read, user2);
			content.grant(Permission.read, user2);

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

			user1 = createTestNode(User.class, new NodeAttribute<>(AbstractNode.name, "user1"));
			user2 = createTestNode(User.class, new NodeAttribute<>(AbstractNode.name, "user2"));

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
			final Html html1 = createElement(page1, page1, "html");
			final Head head1 = createElement(page1, html1, "head");
			createElement(page1, head1, "title", "test22_1");

			final Body body1 = createElement(page1, html1, "body");
			final Div div1   = createElement(page1, body1, "div");

			createElement(page1, div1, "div", "test1");
			createElement(page1, div1, "div", "test1");

			final Div component = createComponent(div1);

			// create second page
			final Page page2 = Page.createNewPage(securityContext,   "test22_2");
			final Html html2 = createElement(page2, page2, "html");
			final Head head2 = createElement(page2, html2, "head");
			createElement(page2, head2, "title", "test22_2");

			final Body body2 = createElement(page2, html2, "body");
			final Div div2   = createElement(page2, body2, "div");

			// re-use template from above
			final Div cloned = cloneComponent(component, div2);

			component.grant(Permission.read, user1);
			cloned.grant(Permission.read, user2);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		doImportExportRoundtrip(true, true, new Function() {

			@Override
			public Object apply(Object t) {

				try (final Tx tx = app.tx()) {

					createTestNode(User.class, new NodeAttribute<>(AbstractNode.name, "user1"));
					createTestNode(User.class, new NodeAttribute<>(AbstractNode.name, "user2"));

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

			user1 = createTestNode(User.class, new NodeAttribute<>(AbstractNode.name, "user1"));
			user2 = createTestNode(User.class, new NodeAttribute<>(AbstractNode.name, "user2"));

			tx.success();

		} catch (FrameworkException ex) {
			fail("Unexpected exception.");
		}

		assertNotNull("User was not created, test cannot continue", user1);
		assertNotNull("User was not created, test cannot continue", user2);

		// setup
		try (final Tx tx = app.tx()) {

			// create some files and folders
			final Folder folder1  = app.create("Folder", new NodeAttribute<>(Folder.name, "Folder1"), new NodeAttribute<>(Traits.of("Folder").key("includeInFrontendExport"), true));
			final Folder folder2  = app.create("Folder", new NodeAttribute<>(Folder.name, "Folder2"), new NodeAttribute<>(Traits.of("Folder").key("parent"), folder1));

			final File file1  = FileHelper.createFile(securityContext, "test".getBytes(), "text/plain", File.class, "test1.txt", true);
			final File file2  = FileHelper.createFile(securityContext, "test".getBytes(), "text/plain", File.class, "test2.txt", true);

			file1.setParent(folder2);
			file2.setParent(folder2);

			folder1.setProperty(Folder.owner, user1);
			folder1.grant(Permission.read, user2);

			folder2.setProperty(Folder.owner, user2);
			folder2.grant(Permission.write, user1);

			file1.setProperty(File.owner, user1);
			file2.setProperty(File.owner, user2);

			file1.setProperty(Folder.owner, user1);
			file1.grant(Permission.read, user2);

			file2.setProperty(Folder.owner, user2);
			file2.grant(Permission.write, user1);

			tx.success();

		} catch (IOException | FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		doImportExportRoundtrip(true, true, new Function() {

			@Override
			public Object apply(Object t) {

				try (final Tx tx = app.tx()) {

					createTestNode(User.class, new NodeAttribute<>(AbstractNode.name, "user1"));
					createTestNode(User.class, new NodeAttribute<>(AbstractNode.name, "user2"));

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
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test24");

			final Body body        = createElement(page, html, "body");
			final Div div1         = createElement(page, body, "div");
			final Content content1 = createContent(page, div1, "${current.type}");
			final Content content2 = createContent(page, div1, "${find('User', 'name', '@structr')[0].id}");
			final Content content3 = createContent(page, div1, "${find('User', 'name', '@structr')[0].id}");

			content1.setProperty(Traits.of("DOMNode").key("showConditions"), "eq(current.type, 'MyTestFolder')");
			content2.setProperty(Traits.of("DOMNode").key("showConditions"), "if(equal(extract(first(find('User', 'name' 'structr')), 'name'), '@structr'), true, false)");
			content3.setProperty(Traits.of("DOMNode").key("showConditions"), "(((((([]))))))"); // for testing only

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
			app.create("SchemaNode",
				new NodeAttribute<>(Traits.of("SchemaNode").key("name"), "ExtendedFolder"),
				new NodeAttribute<>(Traits.of("SchemaNode").key("extendsClass"), app.nodeQuery("SchemaNode").andName("Folder").getFirst())
			);

			// create extended file class
			app.create("SchemaNode",
				new NodeAttribute<>(Traits.of("SchemaNode").key("name"), "ExtendedFile"),
				new NodeAttribute<>(Traits.of("SchemaNode").key("extendsClass"), app.nodeQuery("SchemaNode").andName("File").getFirst())
			);

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("Unexpected exception {}", fex.getMessage());
			fail("Unexpected exception.");
		}

		// setup
		try (final Tx tx = app.tx()) {

			final NodeInterface folder1 = app.create(StructrApp.getConfiguration().getNodeEntityClass("ExtendedFolder"), "folder1");
			final NodeInterface folder2 = app.create(StructrApp.getConfiguration().getNodeEntityClass("ExtendedFolder"),
				new NodeAttribute<>(Folder.name, "folder2"),
				new NodeAttribute(Traits.of("Folder").key("parent"), folder1)
			);

			app.create(StructrApp.getConfiguration().getNodeEntityClass("ExtendedFile"),
				new NodeAttribute<>(File.name, "file1.txt"),
				new NodeAttribute(Traits.of("File").key("parent"), folder1)
			);

			app.create(StructrApp.getConfiguration().getNodeEntityClass("ExtendedFile"),
				new NodeAttribute<>(File.name, "file2.txt"),
				new NodeAttribute(Traits.of("File").key("parent"), folder2)
			);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("Unexpected exception {}", fex.getMessage());
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), false, true);
	}

	@Test
	public void test26Escaping() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test25");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test25");

			final Body body        = createElement(page, html, "body");
			final Div div1         = createElement(page, body, "div");
			final Content content1 = createContent(page, div1, "<div><script>var test = '<h3>Title</h3>';</script></div>");

			content1.setProperty(Traits.of("Content").key("contentType"), "text/html");

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

			final File file1 = FileHelper.createFile(securityContext, "test".getBytes("utf-8"), "text/plain", File.class, fileName1, true);
			final File file2 = FileHelper.createFile(securityContext, "test".getBytes("utf-8"), "text/plain", File.class, fileName2, true);

			file1.setProperty(Traits.of("File").key("visibleToPublicUsers"), true);
			file1.setProperty(Traits.of("File").key("visibleToAuthenticatedUsers"), true);
			file1.setProperty(Traits.of("File").key("enableBasicAuth"), true);
			file1.setProperty(Traits.of("File").key("useAsJavascriptLibrary"), true);

			file2.setProperty(Traits.of("File").key("visibleToPublicUsers"), true);
			file2.setProperty(Traits.of("File").key("visibleToAuthenticatedUsers"), true);
			file2.setProperty(Traits.of("File").key("enableBasicAuth"), true);
			file2.setProperty(Traits.of("File").key("includeInFrontendExport"), true);

			tx.success();

		} catch (IOException | FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test
		doImportExportRoundtrip(true);

		// check
		try (final Tx tx = app.tx()) {

			final File file1 = app.nodeQuery("File").and(File.name, fileName1).getFirst();
			final File file2 = app.nodeQuery("File").and(File.name, fileName2).getFirst();

			assertNotNull("Invalid deployment result", file1);
			assertNotNull("Invalid deployment result", file2);

			assertTrue("Deployment import does not restore attributes correctly",  file1.getProperty(Traits.of("File").key("visibleToPublicUsers")));
			assertTrue("Deployment import does not restore attributes correctly",  file1.getProperty(Traits.of("File").key("visibleToAuthenticatedUsers")));
			assertTrue("Deployment import does not restore attributes correctly",  file1.getProperty(Traits.of("File").key("enableBasicAuth")));
			assertTrue("Deployment import does not restore attributes correctly",  file1.getProperty(Traits.of("File").key("useAsJavascriptLibrary")));
			assertFalse("Deployment import does not restore attributes correctly", file1.getProperty(Traits.of("File").key("includeInFrontendExport")));

			assertTrue("Deployment import does not restore attributes correctly",  file2.getProperty(Traits.of("File").key("visibleToPublicUsers")));
			assertTrue("Deployment import does not restore attributes correctly",  file2.getProperty(Traits.of("File").key("visibleToAuthenticatedUsers")));
			assertTrue("Deployment import does not restore attributes correctly",  file2.getProperty(Traits.of("File").key("enableBasicAuth")));
			assertFalse("Deployment import does not restore attributes correctly", file2.getProperty(Traits.of("File").key("useAsJavascriptLibrary")));
			assertTrue("Deployment import does not restore attributes correctly" , file2.getProperty(Traits.of("File").key("includeInFrontendExport")));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test28MailTemplates() {

		// setup
		try (final Tx tx = app.tx()) {

			app.create("MailTemplate",
				new NodeAttribute<>(Traits.of("MailTemplate").key("name"),   "template1"),
				new NodeAttribute<>(Traits.of("MailTemplate").key("locale"), "de_DE"),
				new NodeAttribute<>(Traits.of("MailTemplate").key("text"),   "text1"),
				new NodeAttribute<>(Traits.of("MailTemplate").key("visibleToPublicUsers"), true)
			);

			app.create("MailTemplate",
				new NodeAttribute<>(Traits.of("MailTemplate").key("name"),   "template2"),
				new NodeAttribute<>(Traits.of("MailTemplate").key("locale"), "en"),
				new NodeAttribute<>(Traits.of("MailTemplate").key("text"),   "text2"),
				new NodeAttribute<>(Traits.of("MailTemplate").key("visibleToAuthenticatedUsers"), true)
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

			final MailTemplate template1 = app.nodeQuery("MailTemplate").and(MailTemplate.name, "template1").getFirst();
			final MailTemplate template2 = app.nodeQuery("MailTemplate").and(MailTemplate.name, "template2").getFirst();

			assertNotNull("Invalid deployment result", template1);
			assertNotNull("Invalid deployment result", template2);

			assertEquals("Invalid MailTemplate deployment result", "template1", template1.getProperty(Traits.of("MailTemplate").key("name")));
			assertEquals("Invalid MailTemplate deployment result", "de_DE",     template1.getProperty(Traits.of("MailTemplate").key("locale")));
			assertEquals("Invalid MailTemplate deployment result", "text1",     template1.getProperty(Traits.of("MailTemplate").key("text")));
			assertEquals("Invalid MailTemplate deployment result", true,        (boolean)template1.getProperty(Traits.of("MailTemplate").key("visibleToPublicUsers")));
			assertEquals("Invalid MailTemplate deployment result", false,       (boolean)template1.getProperty(Traits.of("MailTemplate").key("visibleToAuthenticatedUsers")));

			assertEquals("Invalid MailTemplate deployment result", "template2", template2.getProperty(Traits.of("MailTemplate").key("name")));
			assertEquals("Invalid MailTemplate deployment result", "en",        template2.getProperty(Traits.of("MailTemplate").key("locale")));
			assertEquals("Invalid MailTemplate deployment result", "text2",     template2.getProperty(Traits.of("MailTemplate").key("text")));
			assertEquals("Invalid MailTemplate deployment result", false,       (boolean)template2.getProperty(Traits.of("MailTemplate").key("visibleToPublicUsers")));
			assertEquals("Invalid MailTemplate deployment result", true,        (boolean)template2.getProperty(Traits.of("MailTemplate").key("visibleToAuthenticatedUsers")));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test29Localizations() {

		// setup
		try (final Tx tx = app.tx()) {

			app.create("Localization",
				new NodeAttribute<>(Traits.of("Localization").key("name"),                "localization1"),
				new NodeAttribute<>(Traits.of("Localization").key("domain"),              "domain1"),
				new NodeAttribute<>(Traits.of("Localization").key("locale"),              "de_DE"),
				new NodeAttribute<>(Traits.of("Localization").key("localizedName"),       "localizedName1")
			);

			app.create("Localization",
				new NodeAttribute<>(Traits.of("Localization").key("name"),                       "localization2"),
				new NodeAttribute<>(Traits.of("Localization").key("domain"),                     "domain2"),
				new NodeAttribute<>(Traits.of("Localization").key("locale"),                     "en"),
				new NodeAttribute<>(Traits.of("Localization").key("localizedName"),              "localizedName2")
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

			final Localization localization1 = app.nodeQuery("Localization").and(Localization.name, "localization1").getFirst();
			final Localization localization2 = app.nodeQuery("Localization").and(Localization.name, "localization2").getFirst();

			assertNotNull("Invalid deployment result", localization1);
			assertNotNull("Invalid deployment result", localization2);

			assertEquals("Invalid Localization deployment result", "localization1",  localization1.getProperty(Traits.of("Localization").key("name")));
			assertEquals("Invalid Localization deployment result", "domain1",        localization1.getProperty(Traits.of("Localization").key("domain")));
			assertEquals("Invalid Localization deployment result", "de_DE",          localization1.getProperty(Traits.of("Localization").key("locale")));
			assertEquals("Invalid Localization deployment result", "localizedName1", localization1.getProperty(Traits.of("Localization").key("localizedName")));
			assertEquals("Invalid Localization deployment result", true,             (boolean)localization1.getProperty(Traits.of("Localization").key("visibleToPublicUsers")));
			assertEquals("Invalid Localization deployment result", true,             (boolean)localization1.getProperty(Traits.of("Localization").key("visibleToAuthenticatedUsers")));

			assertEquals("Invalid Localization deployment result", "localization2",  localization2.getProperty(Traits.of("Localization").key("name")));
			assertEquals("Invalid Localization deployment result", "domain2",        localization2.getProperty(Traits.of("Localization").key("domain")));
			assertEquals("Invalid Localization deployment result", "en",             localization2.getProperty(Traits.of("Localization").key("locale")));
			assertEquals("Invalid Localization deployment result", "localizedName2", localization2.getProperty(Traits.of("Localization").key("localizedName")));
			assertEquals("Invalid Localization deployment result", true,             (boolean)localization2.getProperty(Traits.of("Localization").key("visibleToPublicUsers")));
			assertEquals("Invalid Localization deployment result", true,             (boolean)localization2.getProperty(Traits.of("Localization").key("visibleToAuthenticatedUsers")));

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
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test30");

			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");
			final Div div2        = createElement(page, div1, "div", "This is a test.");
			final Table table1    = createElement(page, div2, "table");
			final Thead thead     = createElement(page, table1, "thead");
			final Tbody tbody     = createElement(page, table1, "tbody");
			final Tr tr1          = createElement(page, thead, "tr");
			final Tr tr2          = createElement(page, tbody, "tr");
			final Td td11         = createElement(page, tr1, "td", "content11", "Content before <select>");
			final Td td12         = createElement(page, tr1, "td", "content12");
			final Td td21         = createElement(page, tr2, "td", "content21");
			final Td td22         = createElement(page, tr2, "td", "content22");
			final Select select   = createElement(page, td11, "select");
			final Option option1  = createElement(page, select, "option", "value1");
			final Option option2  = createElement(page, select, "option", "value2");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		compare(calculateHash(), true);
	}
}
