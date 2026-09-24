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
package org.structr.test.rest.resource;

import io.restassured.RestAssured;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SchemaViewTraitDefinition;
import org.structr.schema.SchemaService;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Ticket 776: the views a type inherits from its parents are materialized as SchemaView nodes with
 * isBuiltinView = true after every schema compilation (AbstractSchemaNodeTraitDefinition.createViewNodesForClass),
 * and nothing ever removes them again. Deleting the view on the parent type, or the parent type itself,
 * leaves an empty view node on every inheriting type - visible in the schema editor and surviving a
 * restart. The schema editor adds a second variant: it removes a view by writing the type's schemaViews
 * without it, which only detaches the node and leaves it in the graph without a type.
 *
 * <p>The tests describe the state the graph must be in afterwards, not how it gets there. Schema
 * compilation runs asynchronously after a change, so every assertion on view nodes polls for a while,
 * and a second schema change waits for the first compilation to finish: a reload requested while one is
 * running is dropped (SchemaService.schemaIsBeingReplaced), which is a limitation of the service, not
 * something these tests are about.
 */
public class SchemaViewInheritanceDeletionTest extends StructrRestTestBase {

	private static final String VIEW = "inheritedView";

	@Test
	public void testInheritedViewNodesAreRemovedWhenTheParentViewIsDeleted() {

		final String typeA = createEntity("/SchemaNode", "{ name: \"TypeA\" }");
		final String typeB = createEntity("/SchemaNode", "{ name: \"TypeB\", inheritedTraits: [ \"TypeA\" ] }");
		final String typeC = createEntity("/SchemaNode", "{ name: \"TypeC\", inheritedTraits: [ \"TypeB\" ] }");
		final String view  = createEntity("/SchemaView", "{ name: \"" + VIEW + "\", nonGraphProperties: \"id, type, createdDate\", schemaNode: \"" + typeA + "\" }");

		// the compilation materializes the inherited view on both subtypes
		assertEventually("the view defined on TypeA must be materialized on TypeB", () -> viewNode("TypeB", VIEW) != null);
		assertEventually("the view defined on TypeA must be materialized on TypeC", () -> viewNode("TypeC", VIEW) != null);

		final String nodeB = createEntity("/TypeB", "{}");

		// the view answers on the subtype before the deletion, so a 404 afterwards means it is gone, not that the URL is wrong
		assertEventually("the inherited view must be reachable on TypeB before the deletion", () -> RestAssured.given().when().get("/TypeB/" + nodeB + "/" + VIEW).getStatusCode() == 200);
		awaitSchemaCompilation();

		RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(200).when().delete("/SchemaView/" + view);

		// nothing named like the deleted view may remain on any type in the hierarchy
		assertEventually("deleting the view on TypeA must remove the materialized copy on TypeB", () -> viewNode("TypeB", VIEW) == null);
		assertEventually("deleting the view on TypeA must remove the materialized copy on TypeC", () -> viewNode("TypeC", VIEW) == null);
		assertEventually("the deleted view must not be reachable on TypeB anymore",              () -> RestAssured.given().when().get("/TypeB/" + nodeB + "/" + VIEW).getStatusCode() == 404);
	}

	@Test
	public void testInheritedViewNodesAreRemovedWhenTheParentTypeIsDeleted() {

		final String typeA = createEntity("/SchemaNode", "{ name: \"TypeA\" }");
		final String typeB = createEntity("/SchemaNode", "{ name: \"TypeB\", inheritedTraits: [ \"TypeA\" ] }");
		final String typeC = createEntity("/SchemaNode", "{ name: \"TypeC\", inheritedTraits: [ \"TypeB\" ] }");

		createEntity("/SchemaView", "{ name: \"" + VIEW + "\", nonGraphProperties: \"id, type, createdDate\", schemaNode: \"" + typeA + "\" }");

		assertEventually("the view defined on TypeA must be materialized on TypeC", () -> viewNode("TypeC", VIEW) != null);
		awaitSchemaCompilation();

		RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(200).when().delete("/SchemaNode/" + typeA);

		assertEventually("deleting TypeA must remove the view it contributed to TypeB", () -> viewNode("TypeB", VIEW) == null);
		assertEventually("deleting TypeA must remove the view it contributed to TypeC", () -> viewNode("TypeC", VIEW) == null);
	}

	/**
	 * The case the ticket leaves open: with A <- B <- C and the view defined on B, every copy on C looks
	 * exactly like a view inherited from A. Deleting B must still take the view away from C, while A, which
	 * never had it, stays untouched.
	 */
	@Test
	public void testInheritedViewNodesFollowTheHierarchyWhenTheMiddleTypeIsDeleted() {

		final String typeA = createEntity("/SchemaNode", "{ name: \"TypeA\" }");
		final String typeB = createEntity("/SchemaNode", "{ name: \"TypeB\", inheritedTraits: [ \"TypeA\" ] }");
		final String typeC = createEntity("/SchemaNode", "{ name: \"TypeC\", inheritedTraits: [ \"TypeB\" ] }");

		createEntity("/SchemaView", "{ name: \"" + VIEW + "\", nonGraphProperties: \"id, type, createdDate\", schemaNode: \"" + typeB + "\" }");

		assertEventually("the view defined on TypeB must be materialized on TypeC", () -> viewNode("TypeC", VIEW) != null);
		assertEquals("TypeA does not inherit from TypeB and must not get its view", null, viewNode("TypeA", VIEW));
		awaitSchemaCompilation();

		RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(200).when().delete("/SchemaNode/" + typeB);

		assertEventually("deleting TypeB must remove the view it contributed to TypeC", () -> viewNode("TypeC", VIEW) == null);
		assertEquals("TypeA must still be without the view",                            null, viewNode("TypeA", VIEW));
	}

	/**
	 * An inherited view that someone has filled with properties on the subtype carries data of its own.
	 * When the parent's view goes away, that data must not: the node stays and becomes an ordinary view
	 * of the subtype, which the schema editor then allows to delete. From then on the subtype is the
	 * origin of the view, so its own subtypes keep inheriting it.
	 */
	@Test
	public void testCustomizedInheritedViewBecomesAnOwnViewOfTheSubtype() {

		final String typeA = createEntity("/SchemaNode", "{ name: \"TypeA\" }");
		final String typeB = createEntity("/SchemaNode", "{ name: \"TypeB\", inheritedTraits: [ \"TypeA\" ] }");
		final String typeC = createEntity("/SchemaNode", "{ name: \"TypeC\", inheritedTraits: [ \"TypeB\" ] }");
		final String view  = createEntity("/SchemaView", "{ name: \"" + VIEW + "\", nonGraphProperties: \"id, type, createdDate\", schemaNode: \"" + typeA + "\" }");

		assertEventually("the view defined on TypeA must be materialized on TypeB", () -> viewNode("TypeB", VIEW) != null);
		assertEventually("the view defined on TypeA must be materialized on TypeC", () -> viewNode("TypeC", VIEW) != null);

		final String viewOnB = uuidOf(viewNode("TypeB", VIEW));
		final String nodeB   = createEntity("/TypeB", "{ name: \"b\" }");

		awaitSchemaCompilation();

		RestAssured.given().contentType("application/json; charset=UTF-8").body("{ nonGraphProperties: \"id, type, name\" }").expect().statusCode(200).when().put("/SchemaView/" + viewOnB);

		// the customization is compiled once the view on TypeB exposes the added attribute
		assertEventually("the customized view on TypeB must expose the added attribute", () -> "b".equals(RestAssured.given().when().get("/TypeB/" + nodeB + "/" + VIEW).jsonPath().getString("result.name")));
		awaitSchemaCompilation();

		RestAssured.given().contentType("application/json; charset=UTF-8").expect().statusCode(200).when().delete("/SchemaView/" + view);

		assertEventually("the surviving view is no longer inherited and must not be marked builtin", () -> Boolean.FALSE.equals(isBuiltin(viewNode("TypeB", VIEW))));

		final NodeInterface remaining = viewNode("TypeB", VIEW);

		assertNotNull("the customized copy on TypeB must survive the deletion of the parent's view", remaining);
		assertEquals("the surviving view must be the node that was customized, not a fresh copy", viewOnB, uuidOf(remaining));

		// TypeB owns the view now, and TypeC inherits it from TypeB
		final NodeInterface copyOnC = viewNode("TypeC", VIEW);

		assertNotNull("TypeC must keep the view it now inherits from TypeB", copyOnC);
		assertEquals("the copy on TypeC is still an inherited one", Boolean.TRUE, isBuiltin(copyOnC));
	}

	/**
	 * The schema editor removes a view by writing the type's schemaViews without it. That only detaches
	 * the node. A view that belongs to no type and names no static type has nothing left to describe.
	 */
	@Test
	public void testViewDetachedFromItsTypeIsDeleted() {

		final String typeA = createEntity("/SchemaNode", "{ name: \"TypeA\" }");
		final String view  = createEntity("/SchemaView", "{ name: \"" + VIEW + "\", nonGraphProperties: \"id, type, createdDate\", schemaNode: \"" + typeA + "\" }");

		assertEventually("the view must be attached to TypeA", () -> viewNode("TypeA", VIEW) != null);

		// the lookup that has to report the deletion must be able to find the node while it exists
		assertTrue("the view node must be found by its id before it is detached", nodeExists(view));
		awaitSchemaCompilation();

		final List<String> remainingViews = new LinkedList<>();

		for (final String uuid : viewUuidsOf("TypeA")) {

			if (!uuid.equals(view)) {

				remainingViews.add("\"" + uuid + "\"");
			}
		}

		RestAssured.given().contentType("application/json; charset=UTF-8").body("{ schemaViews: [ " + String.join(", ", remainingViews) + " ] }").expect().statusCode(200).when().put("/SchemaNode/" + typeA);

		assertEventually("a view detached from its type must be deleted, not left behind without a type", () -> nodeExists(view) == false);
	}

	// ----- private methods -----
	private NodeInterface viewNode(final String typeName, final String viewName) {

		final PropertyKey<NodeInterface> schemaNodeKey = Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY);

		try (final Tx tx = app.tx()) {

			for (final NodeInterface view : app.nodeQuery(StructrTraits.SCHEMA_VIEW).name(viewName).getAsList()) {

				final NodeInterface schemaNode = view.getProperty(schemaNodeKey);
				if (schemaNode != null && typeName.equals(schemaNode.getName())) {

					tx.success();

					return view;
				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}

		return null;
	}

	private List<String> viewUuidsOf(final String typeName) {

		final PropertyKey<NodeInterface> schemaNodeKey = Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.SCHEMA_NODE_PROPERTY);
		final List<String> uuids                       = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			for (final NodeInterface view : app.nodeQuery(StructrTraits.SCHEMA_VIEW).getAsList()) {

				final NodeInterface schemaNode = view.getProperty(schemaNodeKey);
				if (schemaNode != null && typeName.equals(schemaNode.getName())) {

					uuids.add(view.getUuid());
				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}

		return uuids;
	}

	private String uuidOf(final NodeInterface node) {

		assertNotNull("expected a view node", node);

		try (final Tx tx = app.tx()) {

			final String uuid = node.getUuid();

			tx.success();

			return uuid;

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}

		return null;
	}

	private Boolean isBuiltin(final NodeInterface view) {

		if (view == null) {

			return null;
		}

		try (final Tx tx = app.tx()) {

			final Boolean builtin = view.getProperty(Traits.of(StructrTraits.SCHEMA_VIEW).key(SchemaViewTraitDefinition.IS_BUILTIN_VIEW_PROPERTY));

			tx.success();

			return builtin;

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}

		return null;
	}

	private boolean nodeExists(final String uuid) {

		try (final Tx tx = app.tx()) {

			final boolean exists = app.getNodeById(uuid) != null;

			tx.success();

			return exists;

		} catch (FrameworkException fex) {

			fail("Unexpected exception: " + fex.getMessage());
		}

		return false;
	}

	/**
	 * Waits until no schema compilation is running. Called once the effect of the previous change is
	 * visible, so the compilation this waits for is the one that produced it, not one that has yet to start.
	 */
	private void awaitSchemaCompilation() {

		assertEventually("the schema compilation must finish", () -> !SchemaService.getSchemaIsBeingReplaced());
	}

	/**
	 * Schema changes are compiled asynchronously, so the graph reaches the expected state a little
	 * after the REST call returns. Twenty seconds covers a compilation on a loaded CI machine with the
	 * bolt driver several times over, and an assertion that never comes true fails after that time.
	 */
	private void assertEventually(final String message, final BooleanSupplier condition) {

		for (int attempt = 0; attempt < 200; attempt++) {

			if (condition.getAsBoolean()) {

				return;
			}

			try { Thread.sleep(100); } catch (InterruptedException ignored) {}
		}

		assertTrue(message, condition.getAsBoolean());
	}
}
