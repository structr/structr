/**
 * Copyright (C) 2010-2016 Structr GmbH
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


import org.structr.core.property.PropertyKey;
import org.structr.common.error.FrameworkException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.GenericNode;
import org.structr.core.entity.relationship.NodeHasLocation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;

//~--- classes ----------------------------------------------------------------

/**
 * Test basic modification operations with graph objects (nodes, relationships)
 *
 * All tests are executed in superuser context
 *
 *
 */
public class ModifyGraphObjectsTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(ModifyGraphObjectsTest.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {
		super.test00DbAvailable();
	}
	
	/**
	 * Test the results of setProperty and getProperty of a node
	 */
	public void test01ModifyNode() {

		try {

			final PropertyMap props = new PropertyMap();
			final String type       = "UnknownTestType";
			final String name       = "GenericNode-name";
			
			NodeInterface node      = null;

			props.put(AbstractNode.type, type);
			props.put(AbstractNode.name, name);

			try (final Tx tx = app.tx()) {
				
				node = app.create(GenericNode.class, props);
				tx.success();
			}

			try (final Tx tx = app.tx()) {
				
				// Check defaults
				assertEquals(GenericNode.class.getSimpleName(), node.getProperty(AbstractNode.type));
				assertTrue(node.getProperty(AbstractNode.name).equals(name));
				assertTrue(!node.getProperty(AbstractNode.hidden));
				assertTrue(!node.getProperty(AbstractNode.deleted));
				assertTrue(!node.getProperty(AbstractNode.visibleToAuthenticatedUsers));
				assertTrue(!node.getProperty(AbstractNode.visibleToPublicUsers));
			}

			final String name2 = "GenericNode-name-äöüß";

			try (final Tx tx = app.tx()) {
				
				// Modify values
				node.setProperty(AbstractNode.name, name2);
				node.setProperty(AbstractNode.hidden, true);
				node.setProperty(AbstractNode.deleted, true);
				node.setProperty(AbstractNode.visibleToAuthenticatedUsers, true);
				node.setProperty(AbstractNode.visibleToPublicUsers, true);

				tx.success();
			}

			try (final Tx tx = app.tx()) {
				
				assertTrue(node.getProperty(AbstractNode.name).equals(name2));
				assertTrue(node.getProperty(AbstractNode.hidden));
				assertTrue(node.getProperty(AbstractNode.deleted));
				assertTrue(node.getProperty(AbstractNode.visibleToAuthenticatedUsers));
				assertTrue(node.getProperty(AbstractNode.visibleToPublicUsers));
			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	 /**
	 * Test the results of setProperty and getProperty of a relationship
	 */
	public void test02ModifyRelationship() {

		try {

			final NodeHasLocation rel = (createTestRelationships(NodeHasLocation.class, 1)).get(0);
			final PropertyKey key1         = new StringProperty("jghsdkhgshdhgsdjkfgh");
			final String val1              = "54354354546806849870";

			try (final Tx tx = app.tx()) {
				
				rel.setProperty(key1, val1);
				tx.success();
			}
			
			try (final Tx tx = app.tx()) {
				
				assertTrue("Expected relationship to have a value for key '" + key1.dbName() + "'", rel.getRelationship().hasProperty(key1.dbName()));

				assertEquals(val1, rel.getRelationship().getProperty(key1.dbName()));

				Object vrfy1 = rel.getProperty(key1);
				assertEquals(val1, vrfy1);
			}
			
			final String val2 = "öljkhöohü8osdfhoödhi";

			try (final Tx tx = app.tx()) {
				
				rel.setProperty(key1, val2);
				tx.success();
			}

			try (final Tx tx = app.tx()) {
				
				Object vrfy2 = rel.getProperty(key1);
				assertEquals(val2, vrfy2);
			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");
		}
	}
}
