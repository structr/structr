/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.common;

import org.junit.Assert;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.ResourceAccess;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.entity.SchemaRelationshipNode.Direction;
import org.structr.core.entity.SchemaRelationshipNode.Propagation;
import org.structr.core.entity.TestUser;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;

//~--- classes ----------------------------------------------------------------

/**
 * Test access control with different permission levels.
 *
 *
 */
public class PermissionResolutionTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(PermissionResolutionTest.class.getName());

	@Test
	public void test01SimplePermissionResolution() {

		SchemaRelationshipNode rel = null;
		PropertyKey key            = null;
		TestUser user1             = null;
		Class type1                = null;
		Class type2                = null;

		try (final Tx tx = app.tx()) {

			// create a test user
			user1 = app.create(TestUser.class, "user1");

			// create schema setup with permission propagation
			final SchemaNode t1 = app.create(SchemaNode.class, "Type1");
			final SchemaNode t2 = app.create(SchemaNode.class, "Type2");

			rel = app.create(SchemaRelationshipNode.class,
				new NodeAttribute<>(SchemaRelationshipNode.sourceNode, t1),
				new NodeAttribute<>(SchemaRelationshipNode.targetNode, t2),
				new NodeAttribute<>(SchemaRelationshipNode.relationshipType, "RELATED"),
				new NodeAttribute<>(SchemaRelationshipNode.sourceMultiplicity, "1"),
				new NodeAttribute<>(SchemaRelationshipNode.targetMultiplicity, "1"),
				new NodeAttribute<>(SchemaRelationshipNode.sourceJsonName, "source"),
				new NodeAttribute<>(SchemaRelationshipNode.targetJsonName, "target")
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		Assert.assertNotNull("User should have been created", user1);

		// create and link objects, make object of type 1 visible,
		// expect object of type 2 to be visible as well
		try (final Tx tx = app.tx()) {

			type1 = StructrApp.getConfiguration().getNodeEntityClass("Type1");
			type2 = StructrApp.getConfiguration().getNodeEntityClass("Type2");
			key   = StructrApp.getConfiguration().getPropertyKeyForJSONName(type1, "target");

			Assert.assertNotNull("Node type Type1 should exist.", type1);
			Assert.assertNotNull("Node type Type2 should exist.", type2);
			Assert.assertNotNull("Property key \"target\" should exist.", key);

			final NodeInterface instance1 = app.create(type1, "instance1OfType1");
			final NodeInterface instance2 = app.create(type2, "instance1OfType2");

			Assert.assertNotNull("Instance of type Type1 should exist", instance1);
			Assert.assertNotNull("Instance of type Type2 should exist", instance2);

			instance1.setProperty(key, instance2);

			// make instance1 visible to user1
			instance1.grant(Permission.read, user1);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// check access for user1 on instance1
		final App userApp = StructrApp.getInstance(SecurityContext.getInstance(user1, AccessMode.Backend));
		try (final Tx tx = userApp.tx()) {

			Assert.assertNotNull("User1 should be able to find instance of type Type1", userApp.nodeQuery(type1).getFirst());
			Assert.assertNull("User1 should NOT be able to find instance of type Type2", userApp.nodeQuery(type2).getFirst());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// enable permission resolution for Type2->Type1 (should NOT make object visible
		// because the resolution direction is wrong.
		try (final Tx tx = app.tx()) {

			rel.setProperty(SchemaRelationshipNode.permissionPropagation, Direction.In);
			rel.setProperty(SchemaRelationshipNode.readPropagation, Propagation.Add);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// check access for user1 on instance1
		try (final Tx tx = userApp.tx()) {

			Assert.assertNotNull("User1 should be able to find instance of type Type1", userApp.nodeQuery(type1).getFirst());
			Assert.assertNull("User1 should NOT be able to find instance of type Type2", userApp.nodeQuery(type2).getFirst());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// enable permission resolution for Type1->Type2 (should make object visible
		// because the resolution direction is correct
		try (final Tx tx = app.tx()) {

			rel.setProperty(SchemaRelationshipNode.permissionPropagation, Direction.Out);
			rel.setProperty(SchemaRelationshipNode.readPropagation, Propagation.Add);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// check access for user1 on instance1
		try (final Tx tx = userApp.tx()) {

			Assert.assertNotNull("User1 should be able to find instance of type Type1", userApp.nodeQuery(type1).getFirst());
			Assert.assertNotNull("User1 should be able to find instance of type Type2", userApp.nodeQuery(type2).getFirst());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// enable permission resolution for both directions, should make object visible
		// because both resolution directions are enabled
		try (final Tx tx = app.tx()) {

			rel.setProperty(SchemaRelationshipNode.permissionPropagation, Direction.Both);
			rel.setProperty(SchemaRelationshipNode.readPropagation, Propagation.Add);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// check access for user1 on instance1
		try (final Tx tx = userApp.tx()) {

			Assert.assertNotNull("User1 should be able to find instance of type Type1", userApp.nodeQuery(type1).getFirst());
			Assert.assertNotNull("User1 should be able to find instance of type Type2", userApp.nodeQuery(type2).getFirst());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// disable permission resolution for both directions, should make
		// object invisible again.
		try (final Tx tx = app.tx()) {

			rel.setProperty(SchemaRelationshipNode.permissionPropagation, Direction.None);
			rel.setProperty(SchemaRelationshipNode.readPropagation, Propagation.Add);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// check access for user1 on instance1
		try (final Tx tx = userApp.tx()) {

			Assert.assertNotNull("User1 should be able to find instance of type Type1", userApp.nodeQuery(type1).getFirst());
			Assert.assertNull("User1 should NOT be able to find instance of type Type2", userApp.nodeQuery(type2).getFirst());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	// ----- private methods -----
	public static void clearResourceAccess() {

		final App app = StructrApp.getInstance();

		try (final Tx tx = app.tx()) {

			for (final ResourceAccess access : app.nodeQuery(ResourceAccess.class).getAsList()) {
				app.delete(access);
			}

			tx.success();

		} catch (Throwable t) {

			logger.warn("Unable to clear resource access grants", t);
		}
	}
}
