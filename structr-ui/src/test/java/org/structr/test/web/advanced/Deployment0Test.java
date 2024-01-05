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
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.dom.*;
import org.structr.web.entity.html.*;
import org.structr.websocket.command.CreateComponentCommand;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.fail;

public class Deployment0Test extends DeploymentTestBase {

	private static final Logger logger = LoggerFactory.getLogger(Deployment0Test.class.getName());

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
	public void test02aVisibilities() {

		// setup
		try (final Tx tx = app.tx()) {

			// create page with visibility false/false
			final Page page       = Page.createNewPage(securityContext,   "test02");
			page.setProperty(Page.visibleToPublicUsers, false);
			page.setProperty(Page.visibleToAuthenticatedUsers, false);
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
	public void test02bVisibilities() {

		// setup
		try (final Tx tx = app.tx()) {

			// create page with visibility false/true
			final Page page       = Page.createNewPage(securityContext,   "test02");
			page.setProperty(Page.visibleToPublicUsers, false);
			page.setProperty(Page.visibleToAuthenticatedUsers, true);
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
	public void test02cVisibilities() {

		// setup
		try (final Tx tx = app.tx()) {

			// create page with visibility true/false
			final Page page       = Page.createNewPage(securityContext,   "test02");
			page.setProperty(Page.visibleToPublicUsers, true);
			page.setProperty(Page.visibleToAuthenticatedUsers, false);
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
	public void test02dVisibilities() {

		// setup
		try (final Tx tx = app.tx()) {

			// create page with visibility true/true
			final Page page       = Page.createNewPage(securityContext,   "test02");
			page.setProperty(Page.visibleToPublicUsers, true);
			page.setProperty(Page.visibleToAuthenticatedUsers, true);
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
}
