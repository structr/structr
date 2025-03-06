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

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.hamcrest.Matchers;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Group;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.SchemaGrantTraitDefinition;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.websocket.command.CreateComponentCommand;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import static org.testng.AssertJUnit.*;

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

			// Create a group with name "SchemaAccess" and allow access to all nodes of type StructrTraits.MAIL_TEMPLATE
			final NodeInterface group = app.create(StructrTraits.GROUP, "SchemaAccess");
			final NodeInterface user  = app.create(StructrTraits.USER, "tester");

			group.as(Group.class).addMember(securityContext, user.as(User.class));

			// create schema grant object
			app.create(StructrTraits.SCHEMA_GRANT,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key(SchemaGrantTraitDefinition.STATIC_SCHEMA_NODE_NAME_PROPERTY), StructrTraits.MAIL_TEMPLATE),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key("principal"),            group),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key("allowRead"),            true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key("allowWrite"),           true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key("allowDelete"),          true)
			);

			// create MailTemplate instances
			app.create(StructrTraits.MAIL_TEMPLATE, "TEMPLATE1");
			app.create(StructrTraits.MAIL_TEMPLATE, "TEMPLATE2");

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// test1: verify that user is allowed to access MailTemplates
		try (final Tx tx = app.tx()) {

			final NodeInterface user          = app.nodeQuery(StructrTraits.USER).andName("tester").getFirst();
			final SecurityContext userContext = SecurityContext.getInstance(user.as(User.class), AccessMode.Backend);

			for (final NodeInterface template : app.nodeQuery(StructrTraits.MAIL_TEMPLATE).getAsList()) {

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

					final NodeInterface group = app.create(StructrTraits.GROUP, "SchemaAccess");
					final NodeInterface user   = app.create(StructrTraits.USER, "tester");

					group.as(Group.class).addMember(securityContext, user.as(User.class));

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

			final NodeInterface user          = app.nodeQuery(StructrTraits.USER).andName("tester").getFirst();
			final SecurityContext userContext = SecurityContext.getInstance(user.as(User.class), AccessMode.Backend);

			for (final NodeInterface template : app.nodeQuery(StructrTraits.MAIL_TEMPLATE).getAsList()) {

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

			createAdminUser();

			final Group parent               = app.create(StructrTraits.GROUP, "parent").as(Group.class);
			final List<NodeInterface> groups = new LinkedList<>();

			for (int i=0; i<8; i++) {
				groups.add(app.create(StructrTraits.GROUP, "group0" + i));
			}

			uuid = parent.getUuid();

			// add some members
			parent.addMember(securityContext, groups.get(1).as(Group.class));
			parent.addMember(securityContext, groups.get(3).as(Group.class));
			parent.addMember(securityContext, groups.get(4).as(Group.class));
			parent.addMember(securityContext, groups.get(6).as(Group.class));

			// create first page
			final Page page1 = Page.createNewPage(securityContext,   "test52_1");
			final DOMElement html1 = createElement(page1, page1, "html");
			final DOMElement head1 = createElement(page1, html1, "head");
			createElement(page1, head1, "title", "test52_1");

			final DOMElement body1 =  createElement(page1, html1, "body");
			final DOMElement div1   =  createElement(page1, body1, "div");
			final DOMElement sel1 = createElement(page1, div1,  "select");
			final DOMElement opt1 = createElement(page1, sel1,  "option", "${group.name}");

			sel1.setProperty(Traits.of("Select").key("_html_multiple"), "multiple");

			// repeater config
			opt1.setProperty(Traits.of(StructrTraits.DOM_ELEMENT).key("functionQuery"), "find('Group', sort('name'))");
			opt1.setProperty(Traits.of(StructrTraits.DOM_ELEMENT).key("dataKey"),       "group");

			// special keys for Option element
			opt1.setProperty(Traits.of("Option").key("selectedValues"), "current.members");
			opt1.setProperty(Traits.of("Option").key("_html_value"),    "${group.id}");

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
			.header(X_USER_HEADER, ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
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

			createAdminUser();

			final Group parent               = app.create(StructrTraits.GROUP, "parent").as(Group.class);
			final List<NodeInterface> groups = new LinkedList<>();

			for (int i=0; i<8; i++) {
				groups.add(app.create(StructrTraits.GROUP, "group0" + i));
			}

			uuid = parent.getUuid();

			// add some members
			parent.addMember(securityContext, groups.get(1).as(Group.class));
			parent.addMember(securityContext, groups.get(3).as(Group.class));
			parent.addMember(securityContext, groups.get(4).as(Group.class));
			parent.addMember(securityContext, groups.get(6).as(Group.class));

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
			.header(X_USER_HEADER, ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
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

			createAdminUser();

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// setup
		try (final Tx tx = app.tx()) {

			final Page shadowPage = CreateComponentCommand.getOrCreateHiddenDocument();

			final Page page = Page.createNewPage(securityContext, "test52");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test52");

			final DOMElement body       = createElement(page, html, "body");

			final DOMElement div1        = createElement(page, body, "div");
			final DOMElement div2        = createElement(page, body, "div");

			final DOMNode comp1   = createComponent(div1);
			final DOMNode comp2   = createComponent(div2);

			// remove both divs from body (to later add the shared components)
			body.removeChild(div1);
			body.removeChild(div2);

			comp1.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "shared-component-one");
			comp1.setProperty(Traits.of(StructrTraits.DOM_NODE).key("hideConditions"), "{ return $.requestStore['SC1_render_count'] > 3; }");

			comp2.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "shared-component-two");
			comp2.setProperty(Traits.of(StructrTraits.DOM_NODE).key("hideConditions"), "{ return $.requestStore['SCS_render_count'] > 3; }");

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

			createAdminUser();

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// setup
		try (final Tx tx = app.tx()) {

			// create page with visibility false/true
			final Page page         = Page.createNewPage(securityContext,   "test54");
			final DOMElement html   = createElement(page, page, "html");
			final DOMElement head   = createElement(page, html, "head");
			final DOMNode title     = createElement(page, head, "title", "test54");
			final DOMElement body   = createElement(page, html, "body");
			final DOMElement div1   = createElement(page, head, "div");
			final DOMElement div11  = createElement(page, div1, "div");

			// this one will be set to delayed rendering
			final DOMElement testDiv = createElement(page, div1, "div");
			final DOMElement div111  = createElement(page, div11, "div", "content 1");
			final DOMElement div121  = createElement(page, div11, "div", "content 2");

			testDiv.setProperty(Traits.of(StructrTraits.DOM_ELEMENT).key("data-structr-rendering-mode"), "visible");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	/* disabled, no support for pure Java methods any more
	public void test55JavaSchemaMethods() {

		// setup
		try (final Tx tx = app.tx()) {

			final JsonSchema sourceSchema = StructrSchema.createFromDatabase(app);

			// a customer
			final JsonType customer = sourceSchema.addType("Customer");

			customer.addMethod("test", "System.out.println(parameters); return null;").setCodeType("java");

			StructrSchema.replaceDatabaseSchema(app, sourceSchema);

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test
		doImportExportRoundtrip(true);

		// check
		try (final Tx tx = app.tx()) {

			final NodeInterface method1 = app.nodeQuery(StructrTraits.SCHEMA_METHOD).and(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "test").getFirst();

			assertNotNull("Invalid deployment result", method1);

			assertEquals("Invalid SchemaMethod deployment result", "test",                                         method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));
			assertEquals("Invalid SchemaMethod deployment result", "System.out.println(parameters); return null;", method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.SOURCE_PROPERTY)));
			assertEquals("Invalid SchemaMethod deployment result", "java",                                         method1.getProperty(Traits.of(StructrTraits.SCHEMA_METHOD).key(SchemaMethodTraitDefinition.CODE_TYPE_PROPERTY)));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}
	*/

	@Test
	public void test56RenamedFileAndFolderInNewerVersionOfDeploymentExport() {

		/**
		 * This method tests/ensures that the state of files/folders after a deployment import reprensents what was actually in the export data
		 *
		 * We first create v2 and export it, then we artificially go back to v1 and import the v2 export.
		 * The file/folder should then be identical to the one imported
		 */

		final String v1FolderName = "rezources";
		final String v2FolderName = "resources";

		final String v1FileName   = "app.mun.js";
		final String v2FileName   = "app.min.js";

		// setup
		try (final Tx tx = app.tx()) {

			final String folderPath        = "/" + v2FolderName + "/js/";
			final NodeInterface folder     = FileHelper.createFolderPath(securityContext, folderPath);
			final NodeInterface file       = FileHelper.createFile(securityContext, "/* app.min.js */".getBytes("utf-8"), "text/javascript", StructrTraits.FILE, v2FileName, true);
			final NodeInterface rootFolder = getRootFolder(folder.as(Folder.class));

			assertNotNull("Root folder should not be null", rootFolder);

			// root folder needs to have "includeInFrontendExport" set
			rootFolder.setProperty(Traits.of(StructrTraits.FOLDER).key("includeInFrontendExport"), true);

			file.setProperty(Traits.of(StructrTraits.FILE).key("parent"), folder);

			tx.success();

		} catch (IOException | FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test, don't clean the database but change folder+file name back to v1
		doImportExportRoundtrip(true, false, t -> {

			try (final Tx tx = app.tx()) {

				final NodeInterface folder = app.nodeQuery(StructrTraits.FOLDER).andName(v2FolderName).getFirst();
				folder.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), v1FolderName);

				final NodeInterface file = app.nodeQuery(StructrTraits.FILE).andName(v2FileName).getFirst();
				file.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), v1FileName);

				tx.success();

			} catch (FrameworkException fex) {

				logger.warn("", fex);
				fail("Unexpected exception.");
			}

			try (final Tx tx = app.tx()) {

				final NodeInterface folder = app.nodeQuery(StructrTraits.FOLDER).andName(v1FolderName).getFirst();
				final NodeInterface file = app.nodeQuery(StructrTraits.FILE).andName(v1FileName).getFirst();

				assertNotNull("Folder rename did not work", folder);
				assertNotNull("File rename did not work", file);

				tx.success();

			} catch (FrameworkException fex) {

				logger.warn("", fex);
				fail("Unexpected exception.");
			}

			return null;
		});

		// check that the correct file/folder name is set
		try (final Tx tx = app.tx()) {

			final NodeInterface folder = app.nodeQuery(StructrTraits.FOLDER).andName(v2FolderName).getFirst();

			assertNotNull("Invalid deployment result", folder);

			final NodeInterface file = app.nodeQuery(StructrTraits.FILE).andName(v2FileName).getFirst();

			assertNotNull("Invalid deployment result", file);

			assertEquals("Deployment import does not restore parent attribute correctly", folder, file.as(File.class).getParent().getParent());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

	}
}
