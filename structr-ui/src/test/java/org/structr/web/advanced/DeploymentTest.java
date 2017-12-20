/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.web.advanced;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Localization;
import org.structr.core.entity.MailTemplate;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SchemaMethod;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.Security;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonType;
import org.structr.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;
import org.structr.web.entity.Widget;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.ShadowDocument;
import org.structr.web.entity.dom.Template;
import org.structr.web.entity.html.Body;
import org.structr.web.entity.html.Div;
import org.structr.web.entity.html.Head;
import org.structr.web.entity.html.Html;
import org.structr.web.entity.html.Li;
import org.structr.web.entity.html.Link;
import org.structr.web.entity.html.Option;
import org.structr.web.entity.html.P;
import org.structr.web.entity.html.Script;
import org.structr.web.entity.html.Select;
import org.structr.web.entity.html.Table;
import org.structr.web.entity.html.Tbody;
import org.structr.web.entity.html.Td;
import org.structr.web.entity.html.Thead;
import org.structr.web.entity.html.Tr;
import org.structr.web.entity.html.Ul;
import org.structr.web.importer.Importer;
import org.structr.web.maintenance.DeployCommand;
import org.structr.web.maintenance.deploy.DeploymentCommentHandler;
import org.structr.websocket.command.CloneComponentCommand;
import org.structr.websocket.command.CreateComponentCommand;
import org.w3c.dom.Node;

public class DeploymentTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(DeploymentTest.class.getName());

	@Test
	public void test01SimplePage() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createSimplePage(securityContext, "test01");

			// test special properties
			page.setProperty(StructrApp.key(Page.class, "showOnErrorCodes"), "404");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test02Visibilities() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page       = Page.createNewPage(securityContext,   "test02");
			final Html html       = createElement(page, page, "html");
			final Head head       = createElement(page, html, "head");
			createElement(page, head, "title", "test02");

			final Body body       = createElement(page, html, "body");

			// create a div for admin only
			{
				final Div div1 = createElement(page, body, "div");
				createElement(page, div1, "h1", "private - ${find('User')}");

				div1.setProperty(StructrApp.key(DOMNode.class, "showConditions"), "me.isAdmin");
			}

			// create a private div
			{
				final Div div1 = createElement(page, body, "div");
				 createElement(page, div1, "h1", "private - test abcdefghjiklmnopqrstuvwyzöäüßABCDEFGHIJKLMNOPQRSTUVWXYZÖÄÜ?\"'");

				div1.setProperty(StructrApp.key(DOMNode.class, "showConditions"), "me.isAdmin");
			}

			// create a protected div
			{
				final Div div1 = createElement(page, body, "div");
				createElement(page, div1, "h1", "protected - $%&/()=?¼½¬{[]}");

				final PropertyMap div1Properties = new PropertyMap();
				div1Properties.put(DOMNode.visibleToPublicUsers,        false);
				div1Properties.put(DOMNode.visibleToAuthenticatedUsers,  true);
				div1.setProperties(div1.getSecurityContext(), div1Properties);
			}

			// create a public div
			{
				final Div div1 = createElement(page, body, "div");
				createElement(page, div1, "h1", "public");

				final PropertyMap div1Properties = new PropertyMap();
				div1Properties.put(DOMNode.visibleToPublicUsers,         true);
				div1Properties.put(DOMNode.visibleToAuthenticatedUsers,  true);
				div1.setProperties(div1.getSecurityContext(), div1Properties);
			}

			// create a public only div
			{
				final Div div1 = createElement(page, body, "div");
				createElement(page, div1, "h1", "public only");

				final PropertyMap div1Properties = new PropertyMap();
				div1Properties.put(DOMNode.visibleToPublicUsers,         true);
				div1Properties.put(DOMNode.visibleToAuthenticatedUsers,  false);
				div1.setProperties(div1.getSecurityContext(), div1Properties);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test03ContentTypes() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test03");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test03");

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

			// workaround for strange importer behaviour
			script.setProperty(StructrApp.key(Script.class, "_html_type"), "text/javascript");
			content.setProperty(StructrApp.key(Content.class, "contentType"), "text/javascript");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test04ContentTypes() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test04");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test04");
			createElement(page, head, "link");
			createElement(page, head, "link");
			createComment(page, head, "commentöäüÖÄÜß+#");

			final Link link3  = createElement(page, head, "link");

			final PropertyMap link3Properties = new PropertyMap();
			link3Properties.put(StructrApp.key(Link.class, "_html_href"), "/");
			link3Properties.put(StructrApp.key(Link.class, "_html_media"), "screen");
			link3Properties.put(StructrApp.key(Link.class, "_html_type"), "stylesheet");
			link3.setProperties(link3.getSecurityContext(), link3Properties);

			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");
			createElement(page, div1, "h1", "private");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test05SimpleTemplateInPage() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test05");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test05");

			final Body body = createElement(page, html, "body");
			final Div div1  = createElement(page, body, "div");

			final Template template = createTemplate(page, div1, "template source - öäüÖÄÜß'\"'`");

			final PropertyMap templateProperties = new PropertyMap();
			templateProperties.put(StructrApp.key(Template.class, "functionQuery"), "find('User')");
			templateProperties.put(StructrApp.key(Template.class, "dataKey"), "user");
			template.setProperties(template.getSecurityContext(), templateProperties);

			// append children to template object
			createElement(page, template, "div");
			createElement(page, template, "div");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test06SimpleTemplateInSharedComponents() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test06");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test06");

			final Body body = createElement(page, html, "body");
			createElement(page, body, "div");

			final ShadowDocument shadowDocument = CreateComponentCommand.getOrCreateHiddenDocument();
			createTemplate(shadowDocument, null, "template source - öäüÖÄÜß'\"'`");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test07SimpleSharedTemplate() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test07");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test07");

			final Body body = createElement(page, html, "body");
			final Div div1  = createElement(page, body, "div");

			final Template template = createTemplate(page, div1, "template source - öäüÖÄÜß'\"'`");

			createComponent(template);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test08SharedTemplateInTwoPages() {

		// setup
		try (final Tx tx = app.tx()) {

			// create first page
			final Page page1 = Page.createNewPage(securityContext,   "test08_1");
			final Html html1 = createElement(page1, page1, "html");
			final Head head1 = createElement(page1, html1, "head");
			createElement(page1, head1, "title", "test08_1");

			final Body body1 = createElement(page1, html1, "body");
			final Div div1   = createElement(page1, body1, "div");

			final Template template1 = createTemplate(page1, div1, "template source - öäüÖÄÜß'\"'`");
			final Template component = createComponent(template1);


			// create second page
			final Page page2 = Page.createNewPage(securityContext,   "test08_2");
			final Html html2 = createElement(page2, page2, "html");
			final Head head2 = createElement(page2, html2, "head");
			createElement(page2, head2, "title", "test08_2");

			final Body body2 = createElement(page2, html2, "body");
			final Div div2   = createElement(page2, body2, "div");

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
	public void test09SharedTemplatesWithChildren() {

		// setup
		try (final Tx tx = app.tx()) {

			// create first page
			final Page page1 = Page.createNewPage(securityContext,   "test09_1");
			final Html html1 = createElement(page1, page1, "html");
			final Head head1 = createElement(page1, html1, "head");
			createElement(page1, head1, "title", "test09_1");

			final Body body1 = createElement(page1, html1, "body");
			final Div div1   = createElement(page1, body1, "div");

			final Template template1 = createTemplate(page1, div1, "template source - öäüÖÄÜß'\"'`");
			createElement(page1, template1, "div", "test1");
			createElement(page1, template1, "div", "test1");

			final Template component = createComponent(template1);

			// create second page
			final Page page2 = Page.createNewPage(securityContext,   "test09_2");
			final Html html2 = createElement(page2, page2, "html");
			final Head head2 = createElement(page2, html2, "head");
			createElement(page2, head2, "title", "test09_2");

			final Body body2 = createElement(page2, html2, "body");
			final Div div2   = createElement(page2, body2, "div");

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
	public void test10SharedComponent() {

		// setup
		try (final Tx tx = app.tx()) {

			// create first page
			final Page page1 = Page.createNewPage(securityContext,   "test10_1");
			final Html html1 = createElement(page1, page1, "html");
			final Head head1 = createElement(page1, html1, "head");
			createElement(page1, head1, "title", "test10_1");

			final Body body1 = createElement(page1, html1, "body");
			final Div div1   = createElement(page1, body1, "div");

			createElement(page1, div1, "div", "test1");
			createElement(page1, div1, "div", "test1");

			final Div component = createComponent(div1);

			// create second page
			final Page page2 = Page.createNewPage(securityContext,   "test10_2");
			final Html html2 = createElement(page2, page2, "html");
			final Head head2 = createElement(page2, html2, "head");
			createElement(page2, head2, "title", "test10_2");

			final Body body2 = createElement(page2, html2, "body");
			final Div div2   = createElement(page2, body2, "div");

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
			final File file     = FileHelper.createFile(securityContext, "test".getBytes("utf-8"), "text/plain", File.class, fileName);
			final Folder rootFolder = getRootFolder(folder);

			Assert.assertNotNull("Root folder should not be null", rootFolder);

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

			final Folder folder = app.nodeQuery(Folder.class).andName("with spaces").getFirst();

			Assert.assertNotNull("Invalid deployment result", folder);

			final File file     = app.nodeQuery(File.class).and(StructrApp.key(File.class, "parent"), folder).and(File.name, fileName).getFirst();

			Assert.assertNotNull("Invalid deployment result", file);

			Assert.assertEquals("Deployment import does not restore attributes correctly", folder, file.getParent());
			Assert.assertTrue("Deployment import does not restore attributes correctly",   file.getProperty(StructrApp.key(File.class, "visibleToPublicUsers")));
			Assert.assertTrue("Deployment import does not restore attributes correctly",   file.getProperty(StructrApp.key(File.class, "visibleToAuthenticatedUsers")));
			Assert.assertTrue("Deployment import does not restore attributes correctly",   file.getProperty(StructrApp.key(File.class, "enableBasicAuth")));
			Assert.assertTrue("Deployment import does not restore attributes correctly",   file.getProperty(StructrApp.key(File.class, "useAsJavascriptLibrary")));

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
			final File file     = FileHelper.createFile(securityContext, "test".getBytes("utf-8"), "text/plain", File.class, fileName);
			final Folder rootFolder = getRootFolder(folder);

			Assert.assertNotNull("Root folder should not be null", rootFolder);

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

					final File file = app.nodeQuery(File.class).and(File.name, fileName).getFirst();
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

			final Folder folder = app.nodeQuery(Folder.class).andName("with spaces").getFirst();

			Assert.assertNotNull("Invalid deployment result", folder);

			final File file     = app.nodeQuery(File.class).and(StructrApp.key(File.class, "parent"), folder).and(File.name, fileName).getFirst();

			Assert.assertNotNull("Invalid deployment result", file);

			Assert.assertEquals("Deployment import of existing file does not restore attributes correctly", folder, file.getParent());
			Assert.assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(StructrApp.key(File.class, "visibleToPublicUsers")));
			Assert.assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(StructrApp.key(File.class, "visibleToAuthenticatedUsers")));
			Assert.assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(StructrApp.key(File.class, "enableBasicAuth")));
			Assert.assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(StructrApp.key(File.class, "useAsJavascriptLibrary")));
			Assert.assertTrue("Deployment import of existing file does not restore attributes correctly", file.getProperty(StructrApp.key(File.class, "includeInFrontendExport")));

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

		Assert.assertNotNull("User was not created, test cannot continue", user1);
		Assert.assertNotNull("User was not created, test cannot continue", user2);

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

		Assert.assertNotNull("User was not created, test cannot continue", user1);
		Assert.assertNotNull("User was not created, test cannot continue", user2);

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
			content.setProperty(StructrApp.key(Content.class, "contentType"), "text/html");

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

		Assert.assertNotNull("User was not created, test cannot continue", user1);
		Assert.assertNotNull("User was not created, test cannot continue", user2);

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

		Assert.assertNotNull("User was not created, test cannot continue", user1);
		Assert.assertNotNull("User was not created, test cannot continue", user2);

		// setup
		try (final Tx tx = app.tx()) {

			// create some files and folders
			final Folder folder1  = app.create(Folder.class, new NodeAttribute<>(Folder.name, "Folder1"), new NodeAttribute<>(StructrApp.key(Folder.class, "includeInFrontendExport"), true));
			final Folder folder2  = app.create(Folder.class, new NodeAttribute<>(Folder.name, "Folder2"), new NodeAttribute<>(StructrApp.key(Folder.class, "parent"), folder1));

			final File file1  = FileHelper.createFile(securityContext, "test".getBytes(), "text/plain", File.class, "test1.txt");
			final File file2  = FileHelper.createFile(securityContext, "test".getBytes(), "text/plain", File.class, "test2.txt");

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

			content1.setProperty(StructrApp.key(DOMNode.class, "showConditions"), "eq(current.type, 'MyTestFolder')");
			content2.setProperty(StructrApp.key(DOMNode.class, "showConditions"), "if(equal(extract(first(find('User', 'name' 'structr')), 'name'), '@structr'), true, false)");
			content3.setProperty(StructrApp.key(DOMNode.class, "showConditions"), "(((((([]))))))"); // for testing only

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
			app.create(SchemaNode.class,
				new NodeAttribute<>(SchemaNode.name, "ExtendedFolder"),
				new NodeAttribute<>(SchemaNode.extendsClass, "org.structr.dynamic.Folder")
			);

			// create extended file class
			app.create(SchemaNode.class,
				new NodeAttribute<>(SchemaNode.name, "ExtendedFile"),
				new NodeAttribute<>(SchemaNode.extendsClass, "org.structr.dynamic.File")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// setup
		try (final Tx tx = app.tx()) {

			final NodeInterface folder1 = app.create(StructrApp.getConfiguration().getNodeEntityClass("ExtendedFolder"), "folder1");
			final NodeInterface folder2 = app.create(StructrApp.getConfiguration().getNodeEntityClass("ExtendedFolder"),
				new NodeAttribute<>(Folder.name, "folder2"),
				new NodeAttribute(StructrApp.key(Folder.class, "parent"), folder1)
			);

			app.create(StructrApp.getConfiguration().getNodeEntityClass("ExtendedFile"),
				new NodeAttribute<>(File.name, "file1.txt"),
				new NodeAttribute(StructrApp.key(File.class, "parent"), folder1)
			);

			app.create(StructrApp.getConfiguration().getNodeEntityClass("ExtendedFile"),
				new NodeAttribute<>(File.name, "file2.txt"),
				new NodeAttribute(StructrApp.key(File.class, "parent"), folder2)
			);

			tx.success();

		} catch (FrameworkException fex) {
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

			content1.setProperty(StructrApp.key(Content.class, "contentType"), "text/html");

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

			final File file1 = FileHelper.createFile(securityContext, "test".getBytes("utf-8"), "text/plain", File.class, fileName1);
			final File file2 = FileHelper.createFile(securityContext, "test".getBytes("utf-8"), "text/plain", File.class, fileName2);

			file1.setProperty(StructrApp.key(File.class, "visibleToPublicUsers"), true);
			file1.setProperty(StructrApp.key(File.class, "visibleToAuthenticatedUsers"), true);
			file1.setProperty(StructrApp.key(File.class, "enableBasicAuth"), true);
			file1.setProperty(StructrApp.key(File.class, "useAsJavascriptLibrary"), true);

			file2.setProperty(StructrApp.key(File.class, "visibleToPublicUsers"), true);
			file2.setProperty(StructrApp.key(File.class, "visibleToAuthenticatedUsers"), true);
			file2.setProperty(StructrApp.key(File.class, "enableBasicAuth"), true);
			file2.setProperty(StructrApp.key(File.class, "includeInFrontendExport"), true);

			tx.success();

		} catch (IOException | FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test
		doImportExportRoundtrip(true);

		// check
		try (final Tx tx = app.tx()) {

			final File file1 = app.nodeQuery(File.class).and(File.name, fileName1).getFirst();
			final File file2 = app.nodeQuery(File.class).and(File.name, fileName2).getFirst();

			Assert.assertNotNull("Invalid deployment result", file1);
			Assert.assertNotNull("Invalid deployment result", file2);

			Assert.assertTrue("Deployment import does not restore attributes correctly",  file1.getProperty(StructrApp.key(File.class, "visibleToPublicUsers")));
			Assert.assertTrue("Deployment import does not restore attributes correctly",  file1.getProperty(StructrApp.key(File.class, "visibleToAuthenticatedUsers")));
			Assert.assertTrue("Deployment import does not restore attributes correctly",  file1.getProperty(StructrApp.key(File.class, "enableBasicAuth")));
			Assert.assertTrue("Deployment import does not restore attributes correctly",  file1.getProperty(StructrApp.key(File.class, "useAsJavascriptLibrary")));
			Assert.assertFalse("Deployment import does not restore attributes correctly", file1.getProperty(StructrApp.key(File.class, "includeInFrontendExport")));

			Assert.assertTrue("Deployment import does not restore attributes correctly",  file2.getProperty(StructrApp.key(File.class, "visibleToPublicUsers")));
			Assert.assertTrue("Deployment import does not restore attributes correctly",  file2.getProperty(StructrApp.key(File.class, "visibleToAuthenticatedUsers")));
			Assert.assertTrue("Deployment import does not restore attributes correctly",  file2.getProperty(StructrApp.key(File.class, "enableBasicAuth")));
			Assert.assertFalse("Deployment import does not restore attributes correctly", file2.getProperty(StructrApp.key(File.class, "useAsJavascriptLibrary")));
			Assert.assertTrue("Deployment import does not restore attributes correctly" , file2.getProperty(StructrApp.key(File.class, "includeInFrontendExport")));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test28MailTemplates() {

		// setup
		try (final Tx tx = app.tx()) {

			app.create(MailTemplate.class,
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "name"),   "template1"),
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "locale"), "de_DE"),
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "text"),   "text1"),
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "visibleToPublicUsers"), true)
			);

			app.create(MailTemplate.class,
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "name"),   "template2"),
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "locale"), "en"),
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "text"),   "text2"),
				new NodeAttribute<>(StructrApp.key(MailTemplate.class, "visibleToAuthenticatedUsers"), true)
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

			final MailTemplate template1 = app.nodeQuery(MailTemplate.class).and(MailTemplate.name, "template1").getFirst();
			final MailTemplate template2 = app.nodeQuery(MailTemplate.class).and(MailTemplate.name, "template2").getFirst();

			Assert.assertNotNull("Invalid deployment result", template1);
			Assert.assertNotNull("Invalid deployment result", template2);

			Assert.assertEquals("Invalid MailTemplate deployment result", "template1", template1.getProperty(StructrApp.key(MailTemplate.class, "name")));
			Assert.assertEquals("Invalid MailTemplate deployment result", "de_DE",     template1.getProperty(StructrApp.key(MailTemplate.class, "locale")));
			Assert.assertEquals("Invalid MailTemplate deployment result", "text1",     template1.getProperty(StructrApp.key(MailTemplate.class, "text")));
			Assert.assertEquals("Invalid MailTemplate deployment result", true,        template1.getProperty(StructrApp.key(MailTemplate.class, "visibleToPublicUsers")));
			Assert.assertEquals("Invalid MailTemplate deployment result", false,       template1.getProperty(StructrApp.key(MailTemplate.class, "visibleToAuthenticatedUsers")));

			Assert.assertEquals("Invalid MailTemplate deployment result", "template2", template2.getProperty(StructrApp.key(MailTemplate.class, "name")));
			Assert.assertEquals("Invalid MailTemplate deployment result", "en",        template2.getProperty(StructrApp.key(MailTemplate.class, "locale")));
			Assert.assertEquals("Invalid MailTemplate deployment result", "text2",     template2.getProperty(StructrApp.key(MailTemplate.class, "text")));
			Assert.assertEquals("Invalid MailTemplate deployment result", false,       template2.getProperty(StructrApp.key(MailTemplate.class, "visibleToPublicUsers")));
			Assert.assertEquals("Invalid MailTemplate deployment result", true,        template2.getProperty(StructrApp.key(MailTemplate.class, "visibleToAuthenticatedUsers")));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test29Localizations() {

		// setup
		try (final Tx tx = app.tx()) {

			app.create(Localization.class,
				new NodeAttribute<>(StructrApp.key(Localization.class, "name"),                "localization1"),
				new NodeAttribute<>(StructrApp.key(Localization.class, "domain"),              "domain1"),
				new NodeAttribute<>(StructrApp.key(Localization.class, "locale"),              "de_DE"),
				new NodeAttribute<>(StructrApp.key(Localization.class, "localizedName"),       "localizedName1")
			);

			app.create(Localization.class,
				new NodeAttribute<>(StructrApp.key(Localization.class, "name"),                       "localization2"),
				new NodeAttribute<>(StructrApp.key(Localization.class, "domain"),                     "domain2"),
				new NodeAttribute<>(StructrApp.key(Localization.class, "locale"),                     "en"),
				new NodeAttribute<>(StructrApp.key(Localization.class, "localizedName"),              "localizedName2")
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

			final Localization localization1 = app.nodeQuery(Localization.class).and(Localization.name, "localization1").getFirst();
			final Localization localization2 = app.nodeQuery(Localization.class).and(Localization.name, "localization2").getFirst();

			Assert.assertNotNull("Invalid deployment result", localization1);
			Assert.assertNotNull("Invalid deployment result", localization2);

			Assert.assertEquals("Invalid Localization deployment result", "localization1",  localization1.getProperty(StructrApp.key(Localization.class, "name")));
			Assert.assertEquals("Invalid Localization deployment result", "domain1",        localization1.getProperty(StructrApp.key(Localization.class, "domain")));
			Assert.assertEquals("Invalid Localization deployment result", "de_DE",          localization1.getProperty(StructrApp.key(Localization.class, "locale")));
			Assert.assertEquals("Invalid Localization deployment result", "localizedName1", localization1.getProperty(StructrApp.key(Localization.class, "localizedName")));
			Assert.assertEquals("Invalid Localization deployment result", true,             localization1.getProperty(StructrApp.key(Localization.class, "visibleToPublicUsers")));
			Assert.assertEquals("Invalid Localization deployment result", true,             localization1.getProperty(StructrApp.key(Localization.class, "visibleToAuthenticatedUsers")));

			Assert.assertEquals("Invalid Localization deployment result", "localization2",  localization2.getProperty(StructrApp.key(Localization.class, "name")));
			Assert.assertEquals("Invalid Localization deployment result", "domain2",        localization2.getProperty(StructrApp.key(Localization.class, "domain")));
			Assert.assertEquals("Invalid Localization deployment result", "en",             localization2.getProperty(StructrApp.key(Localization.class, "locale")));
			Assert.assertEquals("Invalid Localization deployment result", "localizedName2", localization2.getProperty(StructrApp.key(Localization.class, "localizedName")));
			Assert.assertEquals("Invalid Localization deployment result", true,             localization2.getProperty(StructrApp.key(Localization.class, "visibleToPublicUsers")));
			Assert.assertEquals("Invalid Localization deployment result", true,             localization2.getProperty(StructrApp.key(Localization.class, "visibleToAuthenticatedUsers")));

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

	@Test
	public void test31RoundtripWithEmptyContentElements() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test31");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test31");

			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");
			final Div div2        = createElement(page, div1, "div", "");
			final Table table1    = createElement(page, div2, "table");
			final Thead thead     = createElement(page, table1, "thead");
			final Tbody tbody     = createElement(page, table1, "tbody");
			final Tr tr1          = createElement(page, thead, "tr");
			final Tr tr2          = createElement(page, tbody, "tr");
			final Td td11         = createElement(page, tr1, "td");
			final Content c1      = createContent(page, td11, "");
			final Td td12         = createElement(page, tr1, "td", "content12");
			final P p1            = createElement(page, td12, "p", "");
			final Ul ul           = createElement(page, p1, "ul");
			final Li li           = createElement(page, ul, "li", "");
			final Td td21         = createElement(page, tr2, "td", "content21");
			final Td td22         = createElement(page, tr2, "td", "content22");
			final Select select   = createElement(page, td11, "select");
			final Option option1  = createElement(page, select, "option", "");
			final Option option2  = createElement(page, select, "option", "value2");
			final Content c2      = createContent(page, div2, "");
			final Table table2    = createElement(page, div2, "table");

			// include visibility flags
			page.setProperty(AbstractNode.visibleToAuthenticatedUsers, true);
			c1.setProperty(AbstractNode.visibleToAuthenticatedUsers, true);
			c2.setProperty(AbstractNode.visibleToAuthenticatedUsers, true);

			// modify visibility to produce two consecutive deployment instruction comments
			td12.setProperty(AbstractNode.visibleToPublicUsers, true);
			table2.setProperty(AbstractNode.visibleToPublicUsers, true);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		compare(calculateHash(), true);
	}

	@Test
	public void test32RoundtripWithEmptyContentElements() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test32");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test32");

			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");
			final Div div2        = createElement(page, div1, "div", " ");
			final Ul ul           = createElement(page, div1, "ul");
			final Li li           = createElement(page, ul, "li", " ");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		compare(calculateHash(), true);
	}

	@Test
	public void test33SchemaMethods() {

		// setup
		try (final Tx tx = app.tx()) {

			app.create(SchemaMethod.class,
				new NodeAttribute<>(SchemaMethod.name,                        "method1"),
				new NodeAttribute<>(SchemaMethod.comment,                     "comment1"),
				new NodeAttribute<>(SchemaMethod.source,                      "source1"),
				new NodeAttribute<>(SchemaMethod.virtualFileName,             "virtualFileName1"),
				new NodeAttribute<>(SchemaMethod.visibleToPublicUsers,        true),
				new NodeAttribute<>(SchemaMethod.visibleToAuthenticatedUsers, false)

			);

			app.create(SchemaMethod.class,
				new NodeAttribute<>(SchemaMethod.name,                       "method2"),
				new NodeAttribute<>(SchemaMethod.comment,                    "comment2"),
				new NodeAttribute<>(SchemaMethod.source,                     "source2"),
				new NodeAttribute<>(SchemaMethod.virtualFileName,            "virtualFileName2"),
				new NodeAttribute<>(SchemaMethod.visibleToPublicUsers,        false),
				new NodeAttribute<>(SchemaMethod.visibleToAuthenticatedUsers, true)
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

			final SchemaMethod method1 = app.nodeQuery(SchemaMethod.class).and(SchemaMethod.name, "method1").getFirst();
			final SchemaMethod method2 = app.nodeQuery(SchemaMethod.class).and(SchemaMethod.name, "method2").getFirst();

			Assert.assertNotNull("Invalid deployment result", method1);
			Assert.assertNotNull("Invalid deployment result", method2);

			Assert.assertEquals("Invalid SchemaMethod deployment result", "method1",          method1.getProperty(SchemaMethod.name));
			Assert.assertEquals("Invalid SchemaMethod deployment result", "comment1",         method1.getProperty(SchemaMethod.comment));
			Assert.assertEquals("Invalid SchemaMethod deployment result", "source1",          method1.getProperty(SchemaMethod.source));
			Assert.assertEquals("Invalid SchemaMethod deployment result", "virtualFileName1", method1.getProperty(SchemaMethod.virtualFileName));
			Assert.assertEquals("Invalid SchemaMethod deployment result", true,               method1.getProperty(SchemaMethod.visibleToPublicUsers));
			Assert.assertEquals("Invalid SchemaMethod deployment result", false,              method1.getProperty(SchemaMethod.visibleToAuthenticatedUsers));

			Assert.assertEquals("Invalid SchemaMethod deployment result", "method2",          method2.getProperty(SchemaMethod.name));
			Assert.assertEquals("Invalid SchemaMethod deployment result", "comment2",         method2.getProperty(SchemaMethod.comment));
			Assert.assertEquals("Invalid SchemaMethod deployment result", "source2",          method2.getProperty(SchemaMethod.source));
			Assert.assertEquals("Invalid SchemaMethod deployment result", "virtualFileName2", method2.getProperty(SchemaMethod.virtualFileName));
			Assert.assertEquals("Invalid SchemaMethod deployment result", false,              method2.getProperty(SchemaMethod.visibleToPublicUsers));
			Assert.assertEquals("Invalid SchemaMethod deployment result", true,               method2.getProperty(SchemaMethod.visibleToAuthenticatedUsers));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}


	@Test
	public void test34WidgetWithTemplate() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page testPage = Page.createNewPage(securityContext, "WidgetTestPage");
			final Html html     = createElement(testPage, testPage, "html");
			final Head head     = createElement(testPage, html, "head");
			final Body body     = createElement(testPage, html, "body");
			final Div div       = createElement(testPage, body, "div");
			final Div div2      = createElement(testPage, body, "div");

			div.setProperty(AbstractNode.name, "WidgetTestPage-Div");
			div2.setProperty(AbstractNode.name, "WidgetTestPage-Div2");

			Widget widgetToImport = app.create(Widget.class,
					new NodeAttribute<>(StructrApp.key(Widget.class, "name"),"TestWidget"),
					new NodeAttribute<>(StructrApp.key(Widget.class, "source"),                      "<!-- @structr:content(text/html) --><structr:template>${{Structr.print(\"<div>Test</div>\");}}</structr:template>"),
					new NodeAttribute<>(StructrApp.key(Widget.class, "configuration"),               "{\"processDeploymentInfo\": true}"),
					new NodeAttribute<>(StructrApp.key(Widget.class, "visibleToPublicUsers"),        true),
					new NodeAttribute<>(StructrApp.key(Widget.class, "visibleToAuthenticatedUsers"), true)
			);

			Importer importer = new Importer(securityContext, widgetToImport.getProperty(new StringProperty("source")), null, null, true, true);

			importer.setIsDeployment(true);
			importer.setCommentHandler(new DeploymentCommentHandler());

			importer.parse(true);
			DOMNode template = importer.createComponentChildNodes(div, testPage);
			div.appendChild(template);

			makePublic(testPage, html,head, body, div, div2, template);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}


		// test
		try (final Tx tx = app.tx()) {

			Div div = (Div)app.nodeQuery().andName("WidgetTestPage-Div").getFirst();

			assertEquals(1, div.treeGetChildCount());

			Object obj = div.treeGetFirstChild();

			assertTrue(Template.class.isAssignableFrom(obj.getClass()));

			Template template = (Template)obj;

			assertEquals("${{Structr.print(\"<div>Test</div>\");}}", template.getTextContent());
			assertEquals("text/html", template.getContentType());

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

	}

	@Test
	public void test35WidgetWithSharedComponentCreation() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page testPage = Page.createNewPage(securityContext, "WidgetTestPage");
			final Html html     = createElement(testPage, testPage, "html");
			final Head head     = createElement(testPage, html, "head");
			final Body body     = createElement(testPage, html, "body");
			final Div div       = createElement(testPage, body, "div");
			final Div div2      = createElement(testPage, body, "div");

			div.setProperty(AbstractNode.name, "WidgetTestPage-Div");
			div2.setProperty(AbstractNode.name, "WidgetTestPage-Div2");

			Widget widgetToImport = app.create(Widget.class,
					new NodeAttribute<>(StructrApp.key(Widget.class, "name"), "TestWidget"),
					new NodeAttribute<>(StructrApp.key(Widget.class, "source"),
						"<structr:component src=\"TestComponent\">\n" +
						"	<div data-structr-meta-name=\"TestComponent\">\n" +
						"		Test123\n" +
						"	</div>\n" +
						"</structr:component>"),
					new NodeAttribute<>(StructrApp.key(Widget.class, "configuration"), ""),
					new NodeAttribute<>(StructrApp.key(Widget.class, "visibleToPublicUsers"), true),
					new NodeAttribute<>(StructrApp.key(Widget.class, "visibleToAuthenticatedUsers"), true)

			);

			Map<String,Object> paramMap = new HashMap<>();
			paramMap.put("widgetHostBaseUrl", "https://widgets.structr.org/structr/rest/widgets");
			paramMap.put("parentId", widgetToImport.getProperty(new StartNode<>("owner", PrincipalOwnsNode.class)));
			paramMap.put("source", widgetToImport.getProperty(new StringProperty("source")));
			paramMap.put("processDeploymentInfo", false);

			Widget.expandWidget(securityContext, testPage, div, baseUri, paramMap, false);
			Widget.expandWidget(securityContext, testPage, div, baseUri, paramMap, false);

			makePublic(testPage, html,head, body, div, div2);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}


		// test
		try (final Tx tx = app.tx()) {

			Div div = app.nodeQuery(Div.class).andName("WidgetTestPage-Div").getFirst();

			assertEquals(2, div.treeGetChildCount());

			Object obj = null;
			for(DOMNode n: div.getAllChildNodes()){
				obj = n;
				break;
			}

			assertTrue(Div.class.isAssignableFrom(obj.getClass()));

			Div clonedNode = (Div)obj;

			assertEquals(0, clonedNode.getChildNodes().getLength());
			assertEquals(3, app.nodeQuery(Div.class).andName("TestComponent").getResult().size());

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test36BuiltInTypesWithProperties() {

		// setup schema
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			Assert.assertNotNull("StructrSchema must return a valid schema object", schema);

			final JsonType pageType = schema.getType("Page");
			final JsonType fileType = schema.getType("File");
			Assert.assertNotNull("Type Page must exist in every schema", pageType);
			Assert.assertNotNull("Type File must exist in every schema", fileType);

			pageType.addIntegerProperty("displayPosition");
			pageType.addStringProperty("icon");

			fileType.addIntegerProperty("test1");
			fileType.addStringProperty("test2");

			// install schema
			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException | URISyntaxException fex) {
			fail("Unexpected exception.");
		}

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createSimplePage(securityContext, "page1");

			page.setProperty(StructrApp.key(Page.class, "displayPosition"), 12);
			page.setProperty(StructrApp.key(Page.class, "icon"),            "icon");

			final Folder folder = app.create(Folder.class, "files");
			folder.setProperty(StructrApp.key(Folder.class, "includeInFrontendExport"), true);

			// create test file with custom attributes
			app.create(File.class,
				new NodeAttribute<>(StructrApp.key(File.class, "name"),        "test.txt"),
				new NodeAttribute<>(StructrApp.key(File.class, "parent"),      folder),
				new NodeAttribute<>(StructrApp.key(File.class, "contentType"), "text/plain"),
				new NodeAttribute<>(StructrApp.key(File.class, "test1"),       123),
				new NodeAttribute<>(StructrApp.key(File.class, "test2"),       "testString")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		compare(calculateHash(), true);
	}

	@Test
	public void test37SharedComponentTemplate() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createSimplePage(securityContext, "page1");
			final Div div   = (Div)page.getElementsByTagName("div").item(0);

			try {

				final DOMNode newNode = (DOMNode) page.createTextNode("#template");
				newNode.unlockSystemPropertiesOnce();
				newNode.setProperties(newNode.getSecurityContext(), new PropertyMap(NodeInterface.type, Template.class.getSimpleName()));

				// append template
				div.appendChild(newNode);

				// create component from div
				createComponent(div);

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception.");
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		compare(calculateHash(), true);
	}

	@Test
	public void test38DynamicFileExport() {

		// setup
		try (final Tx tx = app.tx()) {

			app.create(SchemaNode.class,
				new NodeAttribute<>(SchemaNode.name, "ExtendedFile"),
				new NodeAttribute<>(SchemaNode.extendsClass, "org.structr.dynamic.File"),
				new NodeAttribute<>(new StringProperty("_test"), "String")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		final Class type       = StructrApp.getConfiguration().getNodeEntityClass("ExtendedFile");
		final PropertyKey test = StructrApp.key(type, "test");

		Assert.assertNotNull("Extended file type should exist", type);
		Assert.assertNotNull("Extended file property should exist", test);

		try (final Tx tx = app.tx()) {

			final NodeInterface node = FileHelper.createFile(securityContext, "test".getBytes("utf-8"), "text/plain", type, "test.txt");

			node.setProperty(StructrApp.key(File.class, "includeInFrontendExport"), true);
			node.setProperty(test, "test");

			tx.success();

		} catch (IOException | FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		compare(calculateHash(), true);
	}

	// ----- private methods -----
	private void compare(final String sourceHash, final boolean deleteTestDirectory) {
		compare(sourceHash, deleteTestDirectory, true);

	}
	private void compare(final String sourceHash, final boolean deleteTestDirectory, final boolean cleanDatabase) {

		doImportExportRoundtrip(deleteTestDirectory, cleanDatabase, null);

		final String roundtripHash = calculateHash();

			System.out.println("Expected: " + sourceHash);
			System.out.println("Actual:   " + roundtripHash);

		if (!sourceHash.equals(roundtripHash)) {

			System.out.println("Expected: " + sourceHash);
			System.out.println("Actual:   " + roundtripHash);

			fail("Invalid deployment roundtrip result");
		}
	}

	private void doImportExportRoundtrip(final boolean deleteTestDirectory) {
		doImportExportRoundtrip(deleteTestDirectory, true, null);
	}

	private void doImportExportRoundtrip(final boolean deleteTestDirectory, final boolean cleanDatabase, final Function callback) {

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
				firstImportParams.put("source", tmp.toString());

				// execute deploy command
				cmd.execute(firstImportParams);

			} else {

				fail("Unable to create temporary directory.");
			}

		} catch (FrameworkException fex) {

			logger.warn("", fex);

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

	private String calculateHash() {

		final StringBuilder buf = new StringBuilder();

		try (final Tx tx = app.tx()) {

			for (final Page page : app.nodeQuery(Page.class).sort(AbstractNode.name).getAsList()) {

				System.out.print("############################# ");
				calculateHash(page, buf, 0);
			}

			for (final Folder folder : app.nodeQuery(Folder.class).sort(AbstractNode.name).getAsList()) {

				if (DeployCommand.okToExport(folder) && folder.includeInFrontendExport()) {

					System.out.print("############################# ");
					calculateHash(folder, buf, 0);
				}
			}

			for (final File file : app.nodeQuery(File.class).sort(AbstractNode.name).getAsList()) {

				if (DeployCommand.okToExport(file) && file.includeInFrontendExport()) {

					System.out.print("############################# ");
					calculateHash(file, buf, 0);
				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}

		return buf.toString();//DigestUtils.md5Hex(buf.toString());
	}

	private void calculateHash(final NodeInterface start, final StringBuilder buf, final int depth) {

		buf.append(start.getType()).append("{");

		hash(start, buf);

		// indent
		for (int i=0; i<depth; i++) {
			System.out.print("    ");
		}

		System.out.println(start.getType() + ": " + start.getUuid().substring(0, 5));

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

		buf.append("}");
	}

	private void hash(final NodeInterface node, final StringBuilder buf) {

		// AbstractNode
		buf.append(valueOrEmpty(node, AbstractNode.type));
		buf.append(valueOrEmpty(node, AbstractNode.name));
		buf.append(valueOrEmpty(node, AbstractNode.visibleToPublicUsers));
		buf.append(valueOrEmpty(node, AbstractNode.visibleToAuthenticatedUsers));

		// include owner in content hash generation!
		final Principal owner = node.getOwnerNode();
		if (owner != null) {

			buf.append(valueOrEmpty(owner, AbstractNode.name));
		}

		// include grants in content hash generation!
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

		buf.append(valueOrEmpty(node, StructrApp.key(DOMElement.class, "data-structr-reload")));
		buf.append(valueOrEmpty(node, StructrApp.key(DOMElement.class, "data-structr-confirm")));
		buf.append(valueOrEmpty(node, StructrApp.key(DOMElement.class, "data-structr-action")));
		buf.append(valueOrEmpty(node, StructrApp.key(DOMElement.class, "data-structr-attributes")));
		buf.append(valueOrEmpty(node, StructrApp.key(DOMElement.class, "data-structr-attr")));
		buf.append(valueOrEmpty(node, StructrApp.key(DOMElement.class, "data-structr-name")));
		buf.append(valueOrEmpty(node, StructrApp.key(DOMElement.class, "data-structr-hide")));
		buf.append(valueOrEmpty(node, StructrApp.key(DOMElement.class, "data-structr-raw-value")));

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
	}

	private String valueOrEmpty(final GraphObject obj, final PropertyKey key) {

		final Object value = obj.getProperty(key);
		if (value != null) {

			return key.jsonName() + "=" + value.toString() + ";";
		}

		return "";
	}

	private <T extends Node> T createElement(final Page page, final DOMNode parent, final String tag, final String... content) {

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

	private Template createTemplate(final Page page, final DOMNode parent, final String content) throws FrameworkException {

		final Template template = StructrApp.getInstance().create(Template.class,
			new NodeAttribute<>(StructrApp.key(Template.class, "content"), content),
			new NodeAttribute<>(StructrApp.key(Template.class, "ownerDocument"), page)
		);

		if (parent != null) {
			parent.appendChild((DOMNode)template);
		}

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

	private <T> T createComponent(final DOMNode node) throws FrameworkException {
		return (T) new CreateComponentCommand().create(node);
	}

	private <T> T cloneComponent(final DOMNode node, final DOMNode parentNode) throws FrameworkException {
		return (T) new CloneComponentCommand().cloneComponent(node, parentNode);
	}

	private Folder getRootFolder(final Folder folder) {

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
