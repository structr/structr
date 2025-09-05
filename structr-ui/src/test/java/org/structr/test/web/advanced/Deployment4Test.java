/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Security;
import org.structr.core.function.Functions;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.*;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.maintenance.DeployCommand;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.FileTraitDefinition;
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
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test41");

			final DOMElement body       = createElement(page, html, "body");
			final DOMElement div1        = createElement(page, body, "div");

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
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test42");

			final DOMElement body       = createElement(page, html, "body");
			final DOMElement div1        = createElement(page, body, "div");

			final Content content = createContent(page, div1, "my content text");
			content.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "myNamedConentElement");

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
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test43");

			final DOMElement body       = createElement(page, html, "body");
			final DOMElement div1        = createElement(page, body, "div");

			final Content content = createContent(page, div1, "my content text");
			content.setHidden(true);

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
			page.setHidden(true);

			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", testName);

			final DOMElement body       = createElement(page, html, "body");
			final DOMElement div1        = createElement(page, body, "div");

			createContent(page, div1, "my content text");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);


		try (final Tx tx = app.tx()) {

			final NodeInterface page = app.nodeQuery(StructrTraits.PAGE).key(Traits.of(StructrTraits.PAGE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), testName).getFirst();

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

			final Principal p1 = app.create(StructrTraits.USER, "user1").as(Principal.class);
			final Principal p2 = app.create(StructrTraits.USER, "user2").as(Principal.class);
			final Principal p3 = app.create(StructrTraits.USER, "user3").as(Principal.class);

			final NodeInterface test1 = FileHelper.createFile(securityContext, "test1".getBytes(), "text/plain", StructrTraits.FILE, "test1.txt", true);
			final NodeInterface test2 = FileHelper.createFile(securityContext, "test2".getBytes(), "text/plain", StructrTraits.FILE, "test2.txt", true);
			final NodeInterface test3 = FileHelper.createFile(securityContext, "test3".getBytes(), "text/plain", StructrTraits.FILE, "test3.txt", true);
			final NodeInterface test4 = FileHelper.createFile(securityContext, "test4".getBytes(), "text/plain", StructrTraits.FILE, "test4.txt", true);

			test1.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), p1);
			test1.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY),                     true);
			test1.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY),              true);
			test1.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true);
			test1.setProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.IS_TEMPLATE_PROPERTY),              true);
			test1.setProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.DONT_CACHE_PROPERTY),               false);

			test2.as(AccessControllable.class).grant(Permission.write, p1);
			test2.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY),                     false);
			test2.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY),              true);
			test2.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true);
			test2.setProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.IS_TEMPLATE_PROPERTY),              false);
			test2.setProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.DONT_CACHE_PROPERTY),               true);

			test3.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), p2);
			test3.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY),                     true);
			test3.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY),              false);
			test3.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true);
			test3.setProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.IS_TEMPLATE_PROPERTY),              true);
			test3.setProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.DONT_CACHE_PROPERTY),               false);

			test4.as(AccessControllable.class).grant(Permission.write, p2);
			test4.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY),                     false);
			test4.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY),              false);
			test4.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true);
			test4.setProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.IS_TEMPLATE_PROPERTY),              false);
			test4.setProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.DONT_CACHE_PROPERTY),               true);

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

			final Principal p1 = app.nodeQuery(StructrTraits.PRINCIPAL).name("user1").getFirst().as(Principal.class);
			final Principal p2 = app.nodeQuery(StructrTraits.PRINCIPAL).name("user2").getFirst().as(Principal.class);
			final Principal p3 = app.nodeQuery(StructrTraits.PRINCIPAL).name("user3").getFirst().as(Principal.class);

			for (final NodeInterface test : app.nodeQuery(StructrTraits.FILE).getResultStream()) {

				final AccessControllable ac = test.as(AccessControllable.class);

				// set wrong grantees, to be corrected by deployment import
				ac.grant(Permission.read,          p1);
				ac.grant(Permission.write,         p1);
				ac.grant(Permission.delete,        p1);
				ac.grant(Permission.accessControl, p1);

				ac.grant(Permission.read,          p2);
				ac.grant(Permission.write,         p2);
				ac.grant(Permission.delete,        p2);
				ac.grant(Permission.accessControl, p2);

				test.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY),                       p3);		// set wrong owner, to be corrected by deployment import
				test.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY),        true);
				test.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);
				test.setProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.IS_TEMPLATE_PROPERTY), true);
				test.setProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.DONT_CACHE_PROPERTY),  true);
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

			final Principal p1 = app.nodeQuery(StructrTraits.PRINCIPAL).name("user1").getFirst().as(Principal.class);
			final Principal p2 = app.nodeQuery(StructrTraits.PRINCIPAL).name("user2").getFirst().as(Principal.class);
			final NodeInterface test1   = app.nodeQuery(StructrTraits.FILE).name("test1.txt").getFirst();
			final NodeInterface test2   = app.nodeQuery(StructrTraits.FILE).name("test2.txt").getFirst();
			final NodeInterface test3   = app.nodeQuery(StructrTraits.FILE).name("test3.txt").getFirst();
			final NodeInterface test4   = app.nodeQuery(StructrTraits.FILE).name("test4.txt").getFirst();

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
				assertEquals("Owner is not set correctly by deployment import",         p1, test1.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY)));
				assertEquals("Visibility is not set correctly by deployment import",  true, (Object)test1.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY)));
				assertEquals("Visibility is not set correctly by deployment import",  true, (Object)test1.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY)));
				assertEquals("isTemplate is not set correctly by deployment import",  true, (Object)test1.getProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.IS_TEMPLATE_PROPERTY)));
				assertEquals("dontCache is not set correctly by deployment import",  false, (Object)test1.getProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.DONT_CACHE_PROPERTY)));
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

				assertEquals("Owner is not set correctly by deployment import",       (NodeInterface)null, test2.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY)));
				assertEquals("Visibility is not set correctly by deployment import", false, (Object)test2.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY)));
				assertEquals("Visibility is not set correctly by deployment import",  true, (Object)test2.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY)));
				assertEquals("isTemplate is not set correctly by deployment import", false, (Object)test2.getProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.IS_TEMPLATE_PROPERTY)));
				assertEquals("dontCache is not set correctly by deployment import",   true, (Object)test2.getProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.DONT_CACHE_PROPERTY)));
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

				assertEquals("Owner is not set correctly by deployment import",         p2, test3.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY)));
				assertEquals("Visibility is not set correctly by deployment import",  true, (Object)test3.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY)));
				assertEquals("Visibility is not set correctly by deployment import", false, (Object)test3.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY)));
				assertEquals("isTemplate is not set correctly by deployment import",  true, (Object)test3.getProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.IS_TEMPLATE_PROPERTY)));
				assertEquals("dontCache is not set correctly by deployment import",  false, (Object)test3.getProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.DONT_CACHE_PROPERTY)));
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

				assertEquals("Owner is not set correctly by deployment import",      (NodeInterface) null, test4.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY)));
				assertEquals("Visibility is not set correctly by deployment import", false, (Object)test4.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY)));
				assertEquals("Visibility is not set correctly by deployment import", false, (Object)test4.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY)));
				assertEquals("isTemplate is not set correctly by deployment import", false, (Object)test4.getProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.IS_TEMPLATE_PROPERTY)));
				assertEquals("dontCache is not set correctly by deployment import",   true, (Object)test4.getProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.DONT_CACHE_PROPERTY)));
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

			final Page page = Page.createNewPage(securityContext, "test47");
			final DOMElement html = createElement(page, page, "html");
			final DOMElement head = createElement(page, html, "head");
			createElement(page, head, "title", "test47");

			final DOMElement body       = createElement(page, html, "body");
			final DOMElement div1        = createElement(page, body, "div");
			final DOMElement div2        = createElement(page, div1, "div");
			final DOMElement div3        = createElement(page, div2, "div");
			final DOMElement div4        = createElement(page, div3, "div");
			final DOMElement div5        = createElement(page, div4, "div");
			final DOMElement div6        = createElement(page, div5, "div");

			div1.setIdAttribute("div1");
			div2.setIdAttribute("div2");
			div3.setIdAttribute("div3");
			div4.setIdAttribute("div4");
			div5.setIdAttribute("div5");
			div6.setIdAttribute("div6");

			final DOMNode comp1   = createComponent(div1);
			final DOMNode comp2   = createComponent(div3);

			comp1.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "xyz-component");
			comp2.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "abc-component");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// test
		compare(calculateHash(), true);
	}

	@Test
	public void test48SchemaGrants() {

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

			final Group testGroup1       = app.create(StructrTraits.GROUP, "Group1").as(Group.class);
			final Group testGroup2       = app.create(StructrTraits.GROUP, "Group2").as(Group.class);
			final Group testGroup3       = app.create(StructrTraits.GROUP, "Group3").as(Group.class);

			// create group hierarchy
			testGroup1.addMember(securityContext, testGroup2);
			testGroup2.addMember(securityContext, testGroup3);

			final NodeInterface user = app.create(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "user"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), "password")
			);

			testGroup3.addMember(securityContext, user.as(User.class));

			// create grant
			final NodeInterface projectNode = app.nodeQuery(StructrTraits.SCHEMA_NODE).name("Project").getFirst();
			final NodeInterface grant      = app.create(StructrTraits.SCHEMA_GRANT,
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key(SchemaGrantTraitDefinition.SCHEMA_NODE_PROPERTY),          projectNode),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key(SchemaGrantTraitDefinition.PRINCIPAL_PROPERTY),            testGroup1),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key(SchemaGrantTraitDefinition.ALLOW_READ_PROPERTY),           true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key(SchemaGrantTraitDefinition.ALLOW_WRITE_PROPERTY),          true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key(SchemaGrantTraitDefinition.ALLOW_DELETE_PROPERTY),         true),
				new NodeAttribute<>(Traits.of(StructrTraits.SCHEMA_GRANT).key(SchemaGrantTraitDefinition.ALLOW_ACCESS_CONTROL_PROPERTY), true)
			);

			// create 2 projects as superuser, no visibility flags etc.
			final String projectType = "Project";
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
				.headers(X_USER_HEADER, "user" , X_PASSWORD_HEADER, "password")

			.expect()
				.statusCode(200)

				.body("result", Matchers.hasSize(2))

			.when()
				.get("/Project");


		// test
		final String hash1 = calculateHash();

		// roundtrip
		doImportExportRoundtrip(true, (x) -> {

			// create group for schema grant to refer to
			try (final Tx tx = app.tx()) {

				final Group testGroup1       = app.create(StructrTraits.GROUP, "Group1").as(Group.class);
				final Group testGroup2       = app.create(StructrTraits.GROUP, "Group2").as(Group.class);
				final Group testGroup3       = app.create(StructrTraits.GROUP, "Group3").as(Group.class);

				// create group hierarchy
				testGroup1.addMember(securityContext, testGroup2);
				testGroup2.addMember(securityContext, testGroup3);

				app.create(StructrTraits.USER,
					new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "user"),
					new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), "password"),
					new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.GROUPS_PROPERTY), List.of(testGroup3))
				);

				tx.success();
			} catch (Throwable t) {
				t.printStackTrace();
			}

			return null;
		});

		// test
		final String hash2 = calculateHash();

		// create projects as well (database was cleared)
		final String projectType = "Project";

		try (final Tx tx = app.tx()) {

			app.create(projectType, "Project1");
			app.create(projectType, "Project2");

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		// test after roundtrip
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers(X_USER_HEADER, "user" , X_PASSWORD_HEADER, "password")

			.expect()
				.statusCode(200)

				.body("result", Matchers.hasSize(2))

			.when()
				.get("/Project");

		// test again but delete group hierarchy first
		try (final Tx tx = app.tx()) {

			final NodeInterface group2 = app.nodeQuery(StructrTraits.GROUP).name("Group2").getFirst();

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
				.headers(X_USER_HEADER, "user" , X_PASSWORD_HEADER, "password")

			.expect()
				.statusCode(200)

				.body("result", Matchers.hasSize(0))

			.when()
				.get("/Project");


		// test
		final String hash3 = calculateHash();

		// roundtrip
		doImportExportRoundtrip(true, (x) -> {

			// create group for schema grant to refer to
			try (final Tx tx = app.tx()) {

				final Group testGroup1       = app.create(StructrTraits.GROUP, "Group1").as(Group.class);
				final Group testGroup2       = app.create(StructrTraits.GROUP, "Group2").as(Group.class);
				final Group testGroup3       = app.create(StructrTraits.GROUP, "Group3").as(Group.class);

				// create group hierarchy
				testGroup1.addMember(securityContext, testGroup2);
				testGroup2.addMember(securityContext, testGroup3);

				app.create(StructrTraits.USER,
					new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "user"),
					new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), "password"),
					new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.GROUPS_PROPERTY), List.of(testGroup3))
				);

				tx.success();
			} catch (Throwable t) {
				t.printStackTrace();
			}

			return null;
		});

		// test after roundtrip
		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers(X_USER_HEADER, "user" , X_PASSWORD_HEADER, "password")

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

			final NodeInterface node = app.nodeQuery(StructrTraits.SCHEMA_NODE).name("Project").getFirst();

			assertTrue("Changelog disabled flag should be set after deployment roundtrip", node.getProperty(Traits.of(StructrTraits.SCHEMA_NODE).key(AbstractSchemaNodeTraitDefinition.CHANGELOG_DISABLED_PROPERTY)));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		final String type = "Project";

		// verify that the changelog flag is still disabled
		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.create(type, "test");

			node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "modified");

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

		final String anonClass = "Public";
		final String authClass = "Authenticated";
		final String bothClass = "Both";

		// setup 2 - schema grant
		try (final Tx tx = app.tx()) {

			app.create(anonClass, "anon1");
			app.create(anonClass, "anon2");

			app.create(authClass, "auth1");
			app.create(authClass, "auth2");

			app.create(bothClass, "both1");
			app.create(bothClass, "both2");

			app.create(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "user"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), "password")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		// allow REST access
		grant("Public",        UiAuthenticator.NON_AUTH_USER_GET | UiAuthenticator.AUTH_USER_GET,  true); // reset all other permissions
		grant("Authenticated", UiAuthenticator.NON_AUTH_USER_GET | UiAuthenticator.AUTH_USER_GET, false);
		grant("Both",          UiAuthenticator.NON_AUTH_USER_GET | UiAuthenticator.AUTH_USER_GET, false);

		// test before roundtrip
		RestAssured.given().expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Public");
		RestAssured.given().expect().statusCode(200).body("result", Matchers.hasSize(0)).when().get("/Authenticated");
		RestAssured.given().expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Both");

		RestAssured.given().header(X_USER_HEADER, "user").header(X_PASSWORD_HEADER, "password").expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Public");
		RestAssured.given().header(X_USER_HEADER, "user").header(X_PASSWORD_HEADER, "password").expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Authenticated");
		RestAssured.given().header(X_USER_HEADER, "user").header(X_PASSWORD_HEADER, "password").expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Both");

		// roundtrip and compare
		final String hash1 = calculateHash();
		doImportExportRoundtrip(true, null, false);
		final String hash2 = calculateHash();

		// test after roundtrip
		RestAssured.given().expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Public");
		RestAssured.given().expect().statusCode(200).body("result", Matchers.hasSize(0)).when().get("/Authenticated");
		RestAssured.given().expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Both");

		RestAssured.given().header(X_USER_HEADER, "user").header(X_PASSWORD_HEADER, "password").expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Public");
		RestAssured.given().header(X_USER_HEADER, "user").header(X_PASSWORD_HEADER, "password").expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Authenticated");
		RestAssured.given().header(X_USER_HEADER, "user").header(X_PASSWORD_HEADER, "password").expect().statusCode(200).body("result", Matchers.hasSize(2)).when().get("/Both");

		assertEquals("Invalid deployment roundtrip result", hash1, hash2);
	}

	// ----- private methods -----
	private boolean isAllowedByGrant(final NodeInterface entity, final Permission permission, final Principal user) {

		final Security security = entity.as(AccessControllable.class).getSecurityRelationship(user);
		if (security != null) {

			return security.isAllowed(permission);
		}

		return false;
	}
}
