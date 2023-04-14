/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.hamcrest.Matchers;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.*;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.html.*;
import org.structr.websocket.command.CreateComponentCommand;
import org.testng.annotations.Test;

import java.lang.Object;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

public class Deployment5Test extends DeploymentTestBase {

	@Test
	public void test51SchemaGrantsRoundtrip() {

		/*
		 * This method verifies that schema-based permissions survive an export/import deployment
		 * roundtrip even if the UUID of the group changes. The test simulates the deployment of
		 * an application from one server to another with differen groups.
		 */

		// setup
		try (final Tx tx = app.tx()) {

			// Create a group with name "SchemaAccess" and allow access to all nodes of type "MailTemplate"
			final SchemaNode schemaNode = app.nodeQuery(SchemaNode.class).andName("MailTemplate").getFirst();
			final Group group           = app.create(Group.class, "SchemaAccess");
			final User user             = app.create(User.class, "tester");

			group.addMember(securityContext, user);

			// create schema grant object
			app.create(SchemaGrant.class,
				new NodeAttribute<>(SchemaGrant.schemaNode,  schemaNode),
				new NodeAttribute<>(SchemaGrant.principal,   group),
				new NodeAttribute<>(SchemaGrant.allowRead,   true),
				new NodeAttribute<>(SchemaGrant.allowWrite,  true),
				new NodeAttribute<>(SchemaGrant.allowDelete, true)
			);

			// create MailTemplate instances
			app.create(MailTemplate.class, "TEMPLATE1");
			app.create(MailTemplate.class, "TEMPLATE2");

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test1: verify that user is allowed to access MailTemplates
		try (final Tx tx = app.tx()) {

			final User user                   = app.nodeQuery(User.class).andName("tester").getFirst();
			final SecurityContext userContext = SecurityContext.getInstance(user, AccessMode.Backend);

			for (final MailTemplate template : app.nodeQuery(MailTemplate.class).getAsList()) {

				assertTrue("User should have read access to all mail templates", template.isGranted(Permission.read, userContext));
				assertTrue("User should have write access to all mail templates", template.isGranted(Permission.write, userContext));
				assertTrue("User should have delete access to all mail templates", template.isGranted(Permission.delete, userContext));
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// deployment export, clean database, create new group with same name but different ID, deployment import
		doImportExportRoundtrip(true, true, new Function() {

			@Override
			public Object apply(final Object o) {

				try (final Tx tx = app.tx()) {

					final Group group = app.create(Group.class, "SchemaAccess");
					final User user   = app.create(User.class, "tester");

					group.addMember(securityContext, user);

					tx.success();

				} catch (FrameworkException fex) {
					fex.printStackTrace();
					fail("Unexpected exception.");
				}

				return null;
			}
		});

		// test2: verify that new user is allowed to access MailTemplates
		try (final Tx tx = app.tx()) {

			final User user                   = app.nodeQuery(User.class).andName("tester").getFirst();
			final SecurityContext userContext = SecurityContext.getInstance(user, AccessMode.Backend);

			for (final MailTemplate template : app.nodeQuery(MailTemplate.class).getAsList()) {

				assertTrue("User should have read access to all mail templates", template.isGranted(Permission.read, userContext));
				assertTrue("User should have write access to all mail templates", template.isGranted(Permission.write, userContext));
				assertTrue("User should have delete access to all mail templates", template.isGranted(Permission.delete, userContext));
			}

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test52SpecialDOMNodeAttributes() {

		String uuid = null;

		// setup
		try (final Tx tx = app.tx()) {

			app.create(User.class,
				new NodeAttribute<>(AbstractNode.name, "admin"),
				new NodeAttribute<>(StructrApp.key(Principal.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(Principal.class, "isAdmin"), true)
			);

			final Group parent       = app.create(Group.class, "parent");
			final List<Group> groups = new LinkedList<>();

			for (int i=0; i<8; i++) {
				groups.add(app.create(Group.class, "group0" + i));
			}

			uuid = parent.getUuid();

			// add some members
			parent.addMember(securityContext, groups.get(1));
			parent.addMember(securityContext, groups.get(3));
			parent.addMember(securityContext, groups.get(4));
			parent.addMember(securityContext, groups.get(6));

			// create first page
			final Page page1 = Page.createNewPage(securityContext,   "test52_1");
			final Html html1 = createElement(page1, page1, "html");
			final Head head1 = createElement(page1, html1, "head");
			createElement(page1, head1, "title", "test52_1");

			final Body body1 =  createElement(page1, html1, "body");
			final Div div1   =  createElement(page1, body1, "div");
			final Select sel1 = createElement(page1, div1,  "select");
			final Option opt1 = createElement(page1, sel1,  "option", "${group.name}");

			sel1.setProperty(StructrApp.key(Select.class, "_html_multiple"), "multiple");

			// repeater config
			opt1.setProperty(StructrApp.key(DOMElement.class, "functionQuery"), "find('Group', sort('name'))");
			opt1.setProperty(StructrApp.key(DOMElement.class, "dataKey"),       "group");

			// special keys for Option element
			opt1.setProperty(StructrApp.key(Option.class, "selectedValues"), "current.members");
			opt1.setProperty(StructrApp.key(Option.class, "_html_value"),    "${group.id}");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		// check HTML result before roundtrip
		RestAssured
			.given()
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.header("x-user", "admin")
			.header("x-password", "admin")
			.expect()
			.body("html.body.div.select.option[0]",            Matchers.equalTo("group00"))
			.body("html.body.div.select.option[1]",            Matchers.equalTo("group01"))
			.body("html.body.div.select.option[1].@selected",   Matchers.equalTo("selected"))
			.body("html.body.div.select.option[2]",            Matchers.equalTo("group02"))
			.body("html.body.div.select.option[3]",            Matchers.equalTo("group03"))
			.body("html.body.div.select.option[3].@selected",   Matchers.equalTo("selected"))
			.body("html.body.div.select.option[4]",            Matchers.equalTo("group04"))
			.body("html.body.div.select.option[4].@selected",   Matchers.equalTo("selected"))
			.body("html.body.div.select.option[5]",            Matchers.equalTo("group05"))
			.body("html.body.div.select.option[6]",            Matchers.equalTo("group06"))
			.body("html.body.div.select.option[6].@selected",   Matchers.equalTo("selected"))
			.body("html.body.div.select.option[7]",            Matchers.equalTo("group07"))
			.body("html.body.div.select.option[8]",            Matchers.equalTo("parent"))
			.statusCode(200)
			.when()
			.get("/test52_1/" + uuid);

		// test roundtrip
		compare(calculateHash(), true);

		// user must be created again...
		try (final Tx tx = app.tx()) {

			app.create(User.class,
				new NodeAttribute<>(AbstractNode.name, "admin"),
				new NodeAttribute<>(StructrApp.key(Principal.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(Principal.class, "isAdmin"), true)
			);

			final Group parent       = app.create(Group.class, "parent");
			final List<Group> groups = new LinkedList<>();

			for (int i=0; i<8; i++) {
				groups.add(app.create(Group.class, "group0" + i));
			}

			uuid = parent.getUuid();

			// add some members
			parent.addMember(securityContext, groups.get(1));
			parent.addMember(securityContext, groups.get(3));
			parent.addMember(securityContext, groups.get(4));
			parent.addMember(securityContext, groups.get(6));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// wait for transaction to settle
		try { Thread.sleep(1000); } catch (Throwable t) {}

		RestAssured.basePath = "/";

		// check HTML result after roundtrip
		RestAssured
			.given()
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
			.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
			.header("x-user", "admin")
			.header("x-password", "admin")
			.expect()
			.body("html.body.div.select.option[0]",            Matchers.equalTo("group00"))
			.body("html.body.div.select.option[1]",            Matchers.equalTo("group01"))
			.body("html.body.div.select.option[1].@selected",   Matchers.equalTo("selected"))
			.body("html.body.div.select.option[2]",            Matchers.equalTo("group02"))
			.body("html.body.div.select.option[3]",            Matchers.equalTo("group03"))
			.body("html.body.div.select.option[3].@selected",   Matchers.equalTo("selected"))
			.body("html.body.div.select.option[4]",            Matchers.equalTo("group04"))
			.body("html.body.div.select.option[4].@selected",   Matchers.equalTo("selected"))
			.body("html.body.div.select.option[5]",            Matchers.equalTo("group05"))
			.body("html.body.div.select.option[6]",            Matchers.equalTo("group06"))
			.body("html.body.div.select.option[6].@selected",   Matchers.equalTo("selected"))
			.body("html.body.div.select.option[7]",            Matchers.equalTo("group07"))
			.body("html.body.div.select.option[8]",            Matchers.equalTo("parent"))
			.statusCode(200)
			.when()
			.get("/test52_1/" + uuid);

	}

	@Test
	public void test53CircularNestedSharedComponents() {

		// setup
		try (final Tx tx = app.tx()) {

			app.create(User.class,
					new NodeAttribute<>(StructrApp.key(Principal.class,     "name"), "admin"),
					new NodeAttribute<>(StructrApp.key(Principal.class, "password"), "admin"),
					new NodeAttribute<>(StructrApp.key(Principal.class,  "isAdmin"),    true)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// setup
		try (final Tx tx = app.tx()) {

			final Page shadowPage = CreateComponentCommand.getOrCreateHiddenDocument();

			final Page page = Page.createNewPage(securityContext, "test52");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test52");

			final Body body       = createElement(page, html, "body");

			final Div div1        = createElement(page, body, "div");
			final Div div2        = createElement(page, body, "div");

			final DOMNode comp1   = createComponent(div1);
			final DOMNode comp2   = createComponent(div2);

			// remove both divs from body (to later add the shared components)
			body.removeChild(div1);
			body.removeChild(div2);

			comp1.setProperty(AbstractNode.name, "shared-component-one");
			comp1.setProperty(StructrApp.key(DOMNode.class, "hideConditions"), "{ return $.requestStore['SC1_render_count'] > 3; }");

			comp2.setProperty(AbstractNode.name, "shared-component-two");
			comp2.setProperty(StructrApp.key(DOMNode.class, "hideConditions"), "{ return $.requestStore['SCS_render_count'] > 3; }");

			createContent(shadowPage, comp1, "shared-component-one\n" +
					"${{\n" +
					"\n" +
					"\tlet cnt2 = $.requestStore['SC1_render_count'] || 0;\n" +
					"\t\n" +
					"\t$.requestStore['SC1_render_count'] = cnt2 + 1;\n" +
					"\n" +
					"}}");

			createContent(shadowPage, comp2, "shared-component-two\n" +
					"${{\n" +
					"\n" +
					"\tlet cnt2 = $.requestStore['SC2_render_count'] || 0;\n" +
					"\t\n" +
					"\t$.requestStore['SC2_render_count'] = cnt2 + 1;\n" +
					"\n" +
					"}}");

			// insert shared-component-one in shared-component-two (AND vice versa)
			cloneComponent(comp1, comp2);
			cloneComponent(comp2, comp1);

			// insert shared components into page
			cloneComponent(comp1, body);
			cloneComponent(comp2, body);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test54DelayedRenderingDeploymentRoundtrip() {

		// setup
		try (final Tx tx = app.tx()) {

			app.create(User.class,
					new NodeAttribute<>(StructrApp.key(Principal.class,     "name"), "admin"),
					new NodeAttribute<>(StructrApp.key(Principal.class, "password"), "admin"),
					new NodeAttribute<>(StructrApp.key(Principal.class,  "isAdmin"),    true)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// setup
		try (final Tx tx = app.tx()) {

			// create page with visibility false/true
			final Page page       = Page.createNewPage(securityContext,   "test54");
			final Html html       = createElement(page, page, "html");
			final Head head       = createElement(page, html, "head");
			final DOMNode title   = createElement(page, head, "title", "test54");
			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, head, "div");
			final Div div11       = createElement(page, div1, "div");

			// this one will be set to delayed rendering
			final Div testDiv     = createElement(page, div1, "div");
			final Div div111      = createElement(page, div11, "div", "content 1");
			final Div div121      = createElement(page, div11, "div", "content 2");

			testDiv.setProperty(StructrApp.key(DOMElement.class, "data-structr-rendering-mode"), "visible");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}
}
