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

import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.structr.api.util.ResultStream;
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
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.SchemaGrantTraitDefinition;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.*;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.FileTraitDefinition;
import org.structr.web.traits.definitions.PagePathParameterTraitDefinition;
import org.structr.web.traits.definitions.PagePathTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMElementTraitDefinition;
import org.structr.web.traits.definitions.dom.DOMNodeTraitDefinition;
import org.structr.web.traits.definitions.html.Option;
import org.structr.web.traits.definitions.html.Select;
import org.structr.websocket.command.CreateComponentCommand;
import org.structr.websocket.command.RemoveCommand;
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
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key(SchemaGrantTraitDefinition.PRINCIPAL_PROPERTY),               group),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key(SchemaGrantTraitDefinition.ALLOW_READ_PROPERTY),              true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key(SchemaGrantTraitDefinition.ALLOW_WRITE_PROPERTY),             true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key(SchemaGrantTraitDefinition.ALLOW_DELETE_PROPERTY),            true)
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

			final NodeInterface user          = app.nodeQuery(StructrTraits.USER).name("tester").getFirst();
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
		doImportExportRoundtrip(true, new Function() {

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

			final NodeInterface user          = app.nodeQuery(StructrTraits.USER).name("tester").getFirst();
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
			final DOMElement div1  =  createElement(page1, body1, "div");
			final DOMElement sel1  = createElement(page1, div1,  "select");
			final DOMElement opt1  = createElement(page1, sel1,  "option", "${group.name}");

			sel1.setProperty(Traits.of(StructrTraits.SELECT).key(Select.MULTIPLE_PROPERTY), "multiple");

			// repeater config
			opt1.setProperty(Traits.of(StructrTraits.DOM_ELEMENT).key(DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY), "find('Group', sort('name'))");
			opt1.setProperty(Traits.of(StructrTraits.DOM_ELEMENT).key(DOMNodeTraitDefinition.DATA_KEY_PROPERTY),       "group");

			// special keys for Option element
			opt1.setProperty(Traits.of(StructrTraits.OPTION).key(Option.SELECTEDVALUES_PROPERTY), "current.members");
			opt1.setProperty(Traits.of(StructrTraits.OPTION).key(Option.VALUE_PROPERTY),          "${group.id}");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		// check HTML result before roundtrip
		RestAssured
			.given()
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
			comp1.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY), "{ return $.requestStore['SC1_render_count'] > 3; }");

			comp2.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "shared-component-two");
			comp2.setProperty(Traits.of(StructrTraits.DOM_NODE).key(DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY), "{ return $.requestStore['SCS_render_count'] > 3; }");

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

			testDiv.setProperty(Traits.of(StructrTraits.DOM_ELEMENT).key(DOMElementTraitDefinition.DATA_STRUCTR_RENDERING_MODE_PROPERTY), "visible");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

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
			rootFolder.setProperty(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true);

			file.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder);

			tx.success();

		} catch (IOException | FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception.");
		}

		// test, don't clean the database but change folder+file name back to v1
		doImportExportRoundtrip(true, t -> {

			try (final Tx tx = app.tx()) {

				final NodeInterface folder = app.nodeQuery(StructrTraits.FOLDER).name(v2FolderName).getFirst();
				folder.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), v1FolderName);

				final NodeInterface file = app.nodeQuery(StructrTraits.FILE).name(v2FileName).getFirst();
				file.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), v1FileName);

				tx.success();

			} catch (FrameworkException fex) {

				logger.warn("", fex);
				fail("Unexpected exception.");
			}

			try (final Tx tx = app.tx()) {

				final NodeInterface folder = app.nodeQuery(StructrTraits.FOLDER).name(v1FolderName).getFirst();
				final NodeInterface file = app.nodeQuery(StructrTraits.FILE).name(v1FileName).getFirst();

				assertNotNull("Folder rename did not work", folder);
				assertNotNull("File rename did not work", file);

				tx.success();

			} catch (FrameworkException fex) {

				logger.warn("", fex);
				fail("Unexpected exception.");
			}

			return null;
		}, false);

		// check that the correct file/folder name is set
		try (final Tx tx = app.tx()) {

			final NodeInterface folder = app.nodeQuery(StructrTraits.FOLDER).name(v2FolderName).getFirst();

			assertNotNull("Invalid deployment result", folder);

			final NodeInterface file = app.nodeQuery(StructrTraits.FILE).name(v2FileName).getFirst();

			assertNotNull("Invalid deployment result", file);

			assertEquals("Deployment import does not restore parent attribute correctly", folder, file.as(File.class).getParent().getParent());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

	}

	@Test
	public void test57DynamicFileExecution() {

		final String dynamicFileCode = "${{ $.log('!!!!!!!!!!!!!!!!!!!!!!!!!! This should no be run during deployment !!!!!!!!!!!!!!!!!!!!!!!!!!! '); }}";

		// setup
		try (final Tx tx = app.tx()) {

			final NodeInterface test1 = FileHelper.createFile(securityContext, dynamicFileCode.getBytes(), "text/plain", StructrTraits.FILE, "test1.txt", true);

			test1.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY),                     true);
			test1.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY),              true);
			test1.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true);
			test1.setProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.IS_TEMPLATE_PROPERTY),              true);
			test1.setProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.DONT_CACHE_PROPERTY),               false);

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		final String hash1 = calculateHash();
		doImportExportRoundtrip(true, null, false);
		final String hash2 = calculateHash();

		assertEquals("Invalid deployment roundtrip result for dynamic file", hash1, hash2);
	}

	@Test
	public void test58PagePathExport() {

		// create page and path
		try (final Tx tx = app.tx()) {

			// create page
			final Page page = Page.createSimplePage(securityContext, "test058");

			// create path
			final NodeInterface path = app.create(StructrTraits.PAGE_PATH,
				new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(PagePathTraitDefinition.PAGE_PROPERTY), page),
				new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "/test058/{test}/static/{test2}")
			);

			// create one parameter with all values
			app.create(StructrTraits.PAGE_PATH_PARAMETER,
				new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY),          path),
				new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),              "test1"),
				new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.VALUE_TYPE_PROPERTY),    "String"),
				new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.DEFAULT_VALUE_PROPERTY), "TEST"),
				new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.IS_OPTIONAL_PROPERTY),   true)
			);

			// create one parameter with only required values
			app.create(StructrTraits.PAGE_PATH_PARAMETER,
				new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(PagePathParameterTraitDefinition.PATH_PROPERTY), path),
				new NodeAttribute<>(Traits.of(StructrTraits.PAGE_PATH_PARAMETER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "test2")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		final String hash1 = calculateHash();
		doImportExportRoundtrip(true, null);
		final String hash2 = calculateHash();

		assertEquals("Invalid deployment roundtrip result for dynamic file", hash1, hash2);
	}

	@Test
	public void test59NewlinesInRepeaterCode() {

		final String multilineQuery =
			"""
			{
				[
					{ name: 'Test1' },
					{ name: 'Test2' },
					{ name: 'Test3' },
					{ name: 'Test4' }
				];
			}
			""";

		// setup
		try (final Tx tx = app.tx()) {

			createAdminUser();

			// create first page
			final Page page1 = Page.createNewPage(securityContext,   "test59");
			final DOMElement html1 = createElement(page1, page1, "html");
			final DOMElement head1 = createElement(page1, html1, "head");
			createElement(page1, head1, "title", "test59");

			final DOMElement body1 = createElement(page1, html1, "body");
			final DOMElement div1  = createElement(page1, body1, "div");
			final Content content  = createContent(page1, div1, "${group.name}");

			div1.setProperty(Traits.of(StructrTraits.DOM_ELEMENT).key(DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY), multilineQuery);
			div1.setProperty(Traits.of(StructrTraits.DOM_ELEMENT).key(DOMNodeTraitDefinition.DATA_KEY_PROPERTY), "group");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		// check HTML result before roundtrip
		RestAssured
			.given()
			.header(X_USER_HEADER, ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
			.body("html.body.div[0]", Matchers.equalTo("Test1"))
			.body("html.body.div[1]", Matchers.equalTo("Test2"))
			.body("html.body.div[2]", Matchers.equalTo("Test3"))
			.body("html.body.div[3]", Matchers.equalTo("Test4"))
			.statusCode(200)
			.when()
			.get("/test59");

		// test roundtrip
		compare(calculateHash(), true);

		// user must be created again
		createAdminUser();

		// wait for transaction to settle
		try { Thread.sleep(1000); } catch (Throwable t) {}

		RestAssured.basePath = "/";

		// check HTML result after roundtrip
		RestAssured
			.given()
			.header(X_USER_HEADER, ADMIN_USERNAME)
			.header(X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
			.body("html.body.div[0]", Matchers.equalTo("Test1"))
			.body("html.body.div[1]", Matchers.equalTo("Test2"))
			.body("html.body.div[2]", Matchers.equalTo("Test3"))
			.body("html.body.div[3]", Matchers.equalTo("Test4"))
			.statusCode(200)
			.when()
			.get("/test59");

		// check that the function query property value is identical
		try (final Tx tx = app.tx()) {

			final DOMElement div = app.nodeQuery("Div").getFirst().as(DOMElement.class);

			assertEquals("Invalid deployment roundtrip result: function query has changed!", multilineQuery, div.getFunctionQuery());

			System.out.println(div.getFunctionQuery());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

	}

	@Test
	public void test60PreventDeploymentExportOfNestedTemplatesInTrash() {

		String template2UUID = null;
		String template3UUID = null;

		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test60");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test05");

			final DOMElement body = createElement(page, html, "body");
			final DOMElement div1  = createElement(page, body, "div");

			final Template template1 = createTemplate(page, div1, "template1");
			final Template template2 = createTemplate(page, template1, "template2");
			final Template template3 = createTemplate(page, template2, "template3");

			template2UUID = template2.getUuid();
			template3UUID = template3.getUuid();

			// remove pageId from node and all children ("move to trash")
			template2.getParent().removeChild(template2);
			RemoveCommand.recursivelyRemoveNodesFromPage(template2, securityContext);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		compare(calculateHash(), true);

		try (final Tx tx = app.tx()) {

			// make sure that the templates from the trash were not exported/imported

			assertNotNull(template2UUID);
			assertNotNull(template3UUID);

			final NodeInterface n1 = app.getNodeById(template2UUID);
			assertEquals("Template node should not be exported if it is in the trash", null, n1);

			final NodeInterface n2 = app.getNodeById(template3UUID);
			assertEquals("Template node should not be exported if it is in the trash as a child of another node", null, n2);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test61ContentElementHasNoUselessTextAttributes() {

		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test42");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test42");

			final DOMElement body = createElement(page, html, "body");
			final DOMElement div1 = createElement(page, body, "div");
			final DOMElement div2 = createElement(page, body, "div");
			final DOMElement div3 = createElement(page, body, "div");

			createContent(page,  div1, "my content text");
			createComment(page,  div2, "my comment text");
			createTemplate(page, div3, "my template text");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// run deployment
		compare(calculateHash(), true);

		try (final Tx tx = app.tx()) {

			try (final ResultStream<NodeInterface> results = app.nodeQuery(StructrTraits.CONTENT).getResultStream()) {

				for (NodeInterface content : results) {

					assertFalse("After a deployment, no Content node should have an '_html_#text' attribute!", content.getPropertyContainer().hasProperty("_html_#text"));
				}
			}

			try (final ResultStream<NodeInterface> results = app.nodeQuery(StructrTraits.COMMENT).getResultStream()) {

				for (NodeInterface comment : results) {

					assertFalse("After a deployment, no Comment node should have an '_html_#comment' attribute!", comment.getPropertyContainer().hasProperty("_html_#comment"));
				}
			}

			try (final ResultStream<NodeInterface> results = app.nodeQuery(StructrTraits.TEMPLATE).getResultStream()) {

				for (NodeInterface template : results) {

					assertFalse("After a deployment, no Template node should have an '_html_src' attribute!", template.getPropertyContainer().hasProperty("_html_src"));
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}
}