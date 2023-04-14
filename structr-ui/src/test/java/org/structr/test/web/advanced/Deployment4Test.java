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
import org.structr.api.config.Settings;
import org.structr.api.schema.JsonSchema;
import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.*;
import org.structr.core.function.Functions;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.StringProperty;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.html.Body;
import org.structr.web.entity.html.Div;
import org.structr.web.entity.html.Head;
import org.structr.web.entity.html.Html;
import org.structr.web.maintenance.DeployCommand;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.*;

public class Deployment4Test extends DeploymentTestBase {

	@Test
	public void test41CustomAttributes() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test41");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test41");

			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");

			div1.setProperty(new StringProperty("_custom_html_aria-expanded"), "true");
			div1.setProperty(new StringProperty("_custom_html_aria-controls"), "#test");
			div1.setProperty(new StringProperty("_custom_html_data-target"),   "#target");
			div1.setProperty(new StringProperty("_custom_html_data-node-id"),  "1233");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test42NamedContentElement() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test42");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test42");

			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");

			final Content content = createContent(page, div1, "my content text");
			content.setProperty(AbstractNode.name, "myNamedConentElement");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test43HiddenContentElement() {

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   "test43");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test43");

			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");

			final Content content = createContent(page, div1, "my content text");
			content.setProperty(AbstractNode.hidden, true);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test44HiddenPage() {

		final String testName = "test44";

		// setup
		try (final Tx tx = app.tx()) {

			final Page page = Page.createNewPage(securityContext,   testName);
			page.setProperty(AbstractNode.hidden, true);

			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", testName);

			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");

			createContent(page, div1, "my content text");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);


		try (final Tx tx = app.tx()) {

			final Page page = app.nodeQuery(Page.class).and(Page.name, testName).getFirst();

			assertTrue("Expected page to have the hidden flag!", page.isHidden());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void test45Schema() {

		final String testName = "test45";

		// setup
		try (final Tx tx = app.tx()) {

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);

	}

	@Test
	public void test46FileAttributes() {

		// setup
		try (final Tx tx = app.tx()) {

			final Principal p1 = app.create(Principal.class, "user1");
			final Principal p2 = app.create(Principal.class, "user2");
			final Principal p3 = app.create(Principal.class, "user3");

			final File test1 = FileHelper.createFile(securityContext, "test1".getBytes(), "text/plain", File.class, "test1.txt", true);
			final File test2 = FileHelper.createFile(securityContext, "test2".getBytes(), "text/plain", File.class, "test2.txt", true);
			final File test3 = FileHelper.createFile(securityContext, "test3".getBytes(), "text/plain", File.class, "test3.txt", true);
			final File test4 = FileHelper.createFile(securityContext, "test4".getBytes(), "text/plain", File.class, "test4.txt", true);

			test1.setProperty(NodeInterface.owner, p1);
			test1.setProperty(AbstractNode.visibleToPublicUsers,                     true);
			test1.setProperty(AbstractNode.visibleToAuthenticatedUsers,              true);
			test1.setProperty(StructrApp.key(File.class, "includeInFrontendExport"), true);
			test1.setProperty(StructrApp.key(File.class, "isTemplate"),              true);
			test1.setProperty(StructrApp.key(File.class, "dontCache"),               false);

			test2.grant(Permission.write, p1);
			test2.setProperty(AbstractNode.visibleToPublicUsers,                     false);
			test2.setProperty(AbstractNode.visibleToAuthenticatedUsers,              true);
			test2.setProperty(StructrApp.key(File.class, "includeInFrontendExport"), true);
			test2.setProperty(StructrApp.key(File.class, "isTemplate"),              false);
			test2.setProperty(StructrApp.key(File.class, "dontCache"),               true);

			test3.setProperty(NodeInterface.owner, p2);
			test3.setProperty(AbstractNode.visibleToPublicUsers,                     true);
			test3.setProperty(AbstractNode.visibleToAuthenticatedUsers,              false);
			test3.setProperty(StructrApp.key(File.class, "includeInFrontendExport"), true);
			test3.setProperty(StructrApp.key(File.class, "isTemplate"),              true);
			test3.setProperty(StructrApp.key(File.class, "dontCache"),               false);

			test4.grant(Permission.write, p2);
			test4.setProperty(AbstractNode.visibleToPublicUsers,                     false);
			test4.setProperty(AbstractNode.visibleToAuthenticatedUsers,              false);
			test4.setProperty(StructrApp.key(File.class, "includeInFrontendExport"), true);
			test4.setProperty(StructrApp.key(File.class, "isTemplate"),              false);
			test4.setProperty(StructrApp.key(File.class, "dontCache"),               true);

			tx.success();

		} catch (FrameworkException|IOException fex) {
			fail("Unexpected exception.");
		}

		final Path tmp          = Paths.get("/tmp/structr-deployment-test" + System.currentTimeMillis() + System.nanoTime());
		final DeployCommand cmd = app.command(DeployCommand.class);

		// export data
		try (final Tx tx = app.tx()) {

				// export to temp directory
				final Map<String, Object> firstExportParams = new HashMap<>();
				firstExportParams.put("mode", "export");
				firstExportParams.put("target", tmp.toString());

				// execute deploy command
				cmd.execute(firstExportParams);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// modify permissions of files that were exported
		try (final Tx tx = app.tx()) {

			final Principal p1 = app.nodeQuery(Principal.class).andName("user1").getFirst();
			final Principal p2 = app.nodeQuery(Principal.class).andName("user2").getFirst();
			final Principal p3 = app.nodeQuery(Principal.class).andName("user3").getFirst();

			for (final File test : app.nodeQuery(File.class).getResultStream()) {

				// set wrong grantees, to be corrected by deployment import
				test.grant(Permission.read,          p1);
				test.grant(Permission.write,         p1);
				test.grant(Permission.delete,        p1);
				test.grant(Permission.accessControl, p1);

				test.grant(Permission.read,          p2);
				test.grant(Permission.write,         p2);
				test.grant(Permission.delete,        p2);
				test.grant(Permission.accessControl, p2);

				test.setProperty(AbstractNode.owner,                       p3);		// set wrong owner, to be corrected by deployment import
				test.setProperty(AbstractNode.visibleToPublicUsers,        true);
				test.setProperty(AbstractNode.visibleToAuthenticatedUsers, true);
				test.setProperty(StructrApp.key(File.class, "isTemplate"), true);
				test.setProperty(StructrApp.key(File.class, "dontCache"),  true);
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// do import
		try (final Tx tx = app.tx()) {

				final Map<String, Object> firstImportParams = new HashMap<>();
				firstImportParams.put("mode", "import");
				firstImportParams.put("source", tmp.toString());

				// execute deploy command
				cmd.execute(firstImportParams);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// check files
		try (final Tx tx = app.tx()) {

			final Principal p1 = app.nodeQuery(Principal.class).andName("user1").getFirst();
			final Principal p2 = app.nodeQuery(Principal.class).andName("user2").getFirst();
			final File test1   = app.nodeQuery(File.class).andName("test1.txt").getFirst();
			final File test2   = app.nodeQuery(File.class).andName("test2.txt").getFirst();
			final File test3   = app.nodeQuery(File.class).andName("test3.txt").getFirst();
			final File test4   = app.nodeQuery(File.class).andName("test4.txt").getFirst();

			// test1
			{
				// test permissions
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test1, Permission.read,          p1));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test1, Permission.write,         p1));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test1, Permission.delete,        p1));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test1, Permission.accessControl, p1));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test1, Permission.read,          p2));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test1, Permission.write,         p2));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test1, Permission.delete,        p2));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test1, Permission.accessControl, p2));

				// test properties
				assertEquals("Owner is not set correctly by deployment import",         p1, test1.getProperty(AbstractNode.owner));
				assertEquals("Visibility is not set correctly by deployment import",  true, (Object)test1.getProperty(AbstractNode.visibleToPublicUsers));
				assertEquals("Visibility is not set correctly by deployment import",  true, (Object)test1.getProperty(AbstractNode.visibleToAuthenticatedUsers));
				assertEquals("isTemplate is not set correctly by deployment import",  true, (Object)test1.getProperty(StructrApp.key(File.class, "isTemplate")));
				assertEquals("dontCache is not set correctly by deployment import",  false, (Object)test1.getProperty(StructrApp.key(File.class, "dontCache")));
			}

			{
				// test permissions
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test2, Permission.read,          p1));
				assertTrue("Permissions are not restored by deployment import",  isAllowedByGrant(test2, Permission.write,         p1)); // true
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test2, Permission.delete,        p1));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test2, Permission.accessControl, p1));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test2, Permission.read,          p2));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test2, Permission.write,         p2));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test2, Permission.delete,        p2));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test2, Permission.accessControl, p2));

				assertEquals("Owner is not set correctly by deployment import",       null, test2.getProperty(AbstractNode.owner));
				assertEquals("Visibility is not set correctly by deployment import", false, (Object)test2.getProperty(AbstractNode.visibleToPublicUsers));
				assertEquals("Visibility is not set correctly by deployment import",  true, (Object)test2.getProperty(AbstractNode.visibleToAuthenticatedUsers));
				assertEquals("isTemplate is not set correctly by deployment import", false, (Object)test2.getProperty(StructrApp.key(File.class, "isTemplate")));
				assertEquals("dontCache is not set correctly by deployment import",   true, (Object)test2.getProperty(StructrApp.key(File.class, "dontCache")));
			}

			{
				// test permissions
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test3, Permission.read,          p1));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test3, Permission.write,         p1));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test3, Permission.delete,        p1));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test3, Permission.accessControl, p1));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test3, Permission.read,          p2));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test3, Permission.write,         p2));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test3, Permission.delete,        p2));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test3, Permission.accessControl, p2));

				assertEquals("Owner is not set correctly by deployment import",         p2, test3.getProperty(AbstractNode.owner));
				assertEquals("Visibility is not set correctly by deployment import",  true, (Object)test3.getProperty(AbstractNode.visibleToPublicUsers));
				assertEquals("Visibility is not set correctly by deployment import", false, (Object)test3.getProperty(AbstractNode.visibleToAuthenticatedUsers));
				assertEquals("isTemplate is not set correctly by deployment import",  true, (Object)test3.getProperty(StructrApp.key(File.class, "isTemplate")));
				assertEquals("dontCache is not set correctly by deployment import",  false, (Object)test3.getProperty(StructrApp.key(File.class, "dontCache")));
			}

			{
				// test permissions
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test4, Permission.read,          p1));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test4, Permission.write,         p1));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test4, Permission.delete,        p1));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test4, Permission.accessControl, p1));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test4, Permission.read,          p2));
				assertTrue("Permissions are not restored by deployment import",  isAllowedByGrant(test4, Permission.write,         p2));	// true
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test4, Permission.delete,        p2));
				assertFalse("Permissions are not restored by deployment import", isAllowedByGrant(test4, Permission.accessControl, p2));

				assertEquals("Owner is not set correctly by deployment import",       null, test4.getProperty(AbstractNode.owner));
				assertEquals("Visibility is not set correctly by deployment import", false, (Object)test4.getProperty(AbstractNode.visibleToPublicUsers));
				assertEquals("Visibility is not set correctly by deployment import", false, (Object)test4.getProperty(AbstractNode.visibleToAuthenticatedUsers));
				assertEquals("isTemplate is not set correctly by deployment import", false, (Object)test4.getProperty(StructrApp.key(File.class, "isTemplate")));
				assertEquals("dontCache is not set correctly by deployment import",   true, (Object)test4.getProperty(StructrApp.key(File.class, "dontCache")));
			}

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try {
			// clean directories
			Files.walkFileTree(tmp, new DeletingFileVisitor());
			Files.delete(tmp);

		} catch (IOException ioex) {}

	}

	@Test
	public void test47NestedSharedComponents() {


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

			final Page page = Page.createNewPage(securityContext, "test47");
			final Html html = createElement(page, page, "html");
			final Head head = createElement(page, html, "head");
			createElement(page, head, "title", "test47");

			final Body body       = createElement(page, html, "body");
			final Div div1        = createElement(page, body, "div");
			final Div div2        = createElement(page, div1, "div");
			final Div div3        = createElement(page, div2, "div");
			final Div div4        = createElement(page, div3, "div");
			final Div div5        = createElement(page, div4, "div");
			final Div div6        = createElement(page, div5, "div");

			div1.setIdAttribute("div1", true);
			div2.setIdAttribute("div2", true);
			div3.setIdAttribute("div3", true);
			div4.setIdAttribute("div4", true);
			div5.setIdAttribute("div5", true);
			div6.setIdAttribute("div6", true);

			final DOMNode comp1   = createComponent(div1);
			final DOMNode comp2   = createComponent(div3);

			comp1.setProperty(AbstractNode.name, "xyz-component");
			comp2.setProperty(AbstractNode.name, "abc-component");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test48SchemaGrants() {

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

		// setup 1 - schema type
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			// add test type
			schema.addType("Project");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// setup 2 - schema grant
		try (final Tx tx = app.tx()) {

			final Group testGroup1       = app.create(Group.class, "Group1");
			final Group testGroup2       = app.create(Group.class, "Group2");
			final Group testGroup3       = app.create(Group.class, "Group3");

			// create group hierarchy
			testGroup1.addMember(securityContext, testGroup2);
			testGroup2.addMember(securityContext, testGroup3);

			final User user = app.create(User.class,
				new NodeAttribute<>(AbstractNode.name, "user"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "password")
			);

			testGroup3.addMember(securityContext, user);

			// create grant
			final SchemaNode projectNode = app.nodeQuery(SchemaNode.class).andName("Project").getFirst();
			final SchemaGrant grant      = app.create(SchemaGrant.class,
				new NodeAttribute<>(SchemaGrant.schemaNode,          projectNode),
				new NodeAttribute<>(SchemaGrant.principal,           testGroup1),
				new NodeAttribute<>(SchemaGrant.allowRead,           true),
				new NodeAttribute<>(SchemaGrant.allowWrite,          true),
				new NodeAttribute<>(SchemaGrant.allowDelete,         true),
				new NodeAttribute<>(SchemaGrant.allowAccessControl,  true)
			);

			// create 2 projects as superuser, no visibility flags etc.
			final Class projectType = StructrApp.getConfiguration().getNodeEntityClass("Project");
			app.create(projectType, "Project1");
			app.create(projectType, "Project2");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// allow REST access
		grant("Project", UiAuthenticator.AUTH_USER_GET, true);

		// test before roundtrip
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers("X-User", "user" , "X-Password", "password")

			.expect()
				.statusCode(200)

				.body("result", Matchers.hasSize(2))

			.when()
				.get("/Project");


		// test
		final String hash1 = calculateHash();

		// roundtrip
		doImportExportRoundtrip(true, false, null);

		// test
		final String hash2 = calculateHash();

		// test after roundtrip
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers("X-User", "user" , "X-Password", "password")

			.expect()
				.statusCode(200)

				.body("result", Matchers.hasSize(2))

			.when()
				.get("/Project");

		// test again but delete group hierarchy first
		try (final Tx tx = app.tx()) {

			final Group group2 = app.nodeQuery(Group.class).andName("Group2").getFirst();

			// Group2 connects the schema grant group (Group1) with the user in Group3,
			// so we expect the user to not see any Projects after this
			app.delete(group2);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test before roundtrip
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers("X-User", "user" , "X-Password", "password")

			.expect()
				.statusCode(200)

				.body("result", Matchers.hasSize(0))

			.when()
				.get("/Project");


		// test
		final String hash3 = calculateHash();

		// roundtrip
		doImportExportRoundtrip(true, false, null);

		// test after roundtrip
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers("X-User", "user" , "X-Password", "password")

			.expect()
				.statusCode(200)

				.body("result", Matchers.hasSize(0))

			.when()
				.get("/Project");

		assertEquals("Invalid deployment roundtrip result", hash1, hash2);
		assertEquals("Invalid deployment roundtrip result", hash2, hash3);
	}

	@Test
	public void test49ChangelogDisabled() {

		Settings.ChangelogEnabled.setValue(true);

		// setup 1 - schema type
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			// add test type
			schema.addType("Project").setIsChangelogDisabled();

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test roundtrip
		compare(calculateHash(), true);

		// verify that the changelog flag is still disabled
		try (final Tx tx = app.tx()) {

			final SchemaNode node = app.nodeQuery(SchemaNode.class).andName("Project").getFirst();

			assertTrue("Changelog disabled flag should be set after deployment roundtrip", node.getProperty(AbstractSchemaNode.changelogDisabled));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		final Class type = StructrApp.getConfiguration().getNodeEntityClass("Project");

		// verify that the changelog flag is still disabled
		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.create(type, "test");

			node.setProperty(AbstractNode.name, "modified");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// verify that no changelog file was written!
		try (final Tx tx = app.tx()) {

			final GraphObject node = app.nodeQuery(type).getFirst();
			final List changelog   = (List)Functions.get("changelog").apply(new ActionContext(securityContext), null, new Object[] { node });

			assertEquals("Changelog was created despite being disabled for the test type", 0, changelog.size());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

	}

	@Test
	public void test50SchemaBasedVisibilityFlags() {

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

		// setup 1 - schema type
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			// add test type
			schema.addType("Public").setVisibleForPublicUsers();
			schema.addType("Authenticated").setVisibleForAuthenticatedUsers();
			schema.addType("Both").setVisibleForAuthenticatedUsers().setVisibleForPublicUsers();

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		final Class anonClass = StructrApp.getConfiguration().getNodeEntityClass("Public");
		final Class authClass = StructrApp.getConfiguration().getNodeEntityClass("Authenticated");
		final Class bothClass = StructrApp.getConfiguration().getNodeEntityClass("Both");

		// setup 2 - schema grant
		try (final Tx tx = app.tx()) {

			app.create(anonClass, "anon1");
			app.create(anonClass, "anon2");

			app.create(authClass, "auth1");
			app.create(authClass, "auth2");

			app.create(bothClass, "both1");
			app.create(bothClass, "both2");

			app.create(User.class,
				new NodeAttribute<>(AbstractNode.name, "user"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "password")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// allow REST access
		grant("Public",        UiAuthenticator.NON_AUTH_USER_GET | UiAuthenticator.AUTH_USER_GET,  true); // reset all other grants
		grant("Authenticated", UiAuthenticator.NON_AUTH_USER_GET | UiAuthenticator.AUTH_USER_GET, false);
		grant("Both",          UiAuthenticator.NON_AUTH_USER_GET | UiAuthenticator.AUTH_USER_GET, false);

		// test before roundtrip
		RestAssured.given().expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Public");
		RestAssured.given().expect().statusCode(200).body("result", Matchers.hasSize(0)).when().get("/Authenticated");
		RestAssured.given().expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Both");

		RestAssured.given().header("X-User", "user").header("X-Password", "password").expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Public");
		RestAssured.given().header("X-User", "user").header("X-Password", "password").expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Authenticated");
		RestAssured.given().header("X-User", "user").header("X-Password", "password").expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Both");

		// roundtrip and compare
		final String hash1 = calculateHash();
		doImportExportRoundtrip(true, false, null);
		final String hash2 = calculateHash();

		// test after roundtrip
		RestAssured.given().expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Public");
		RestAssured.given().expect().statusCode(200).body("result", Matchers.hasSize(0)).when().get("/Authenticated");
		RestAssured.given().expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Both");

		RestAssured.given().header("X-User", "user").header("X-Password", "password").expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Public");
		RestAssured.given().header("X-User", "user").header("X-Password", "password").expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Authenticated");
		RestAssured.given().header("X-User", "user").header("X-Password", "password").expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Both");

		assertEquals("Invalid deployment roundtrip result", hash1, hash2);
	}

	// ----- private methods -----
	private boolean isAllowedByGrant(final AccessControllable entity, final Permission permission, final Principal user) {

		final Security security = entity.getSecurityRelationship(user);
		if (security != null) {

			return security.isAllowed(permission);
		}

		return false;
	}
}
