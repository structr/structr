/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.common;


import org.structr.core.property.PropertyKey;
import org.structr.common.error.FrameworkException;

//~--- JDK imports ------------------------------------------------------------


import java.util.logging.Level;
import java.util.logging.Logger;
import static junit.framework.Assert.assertTrue;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.GenericNode;
import org.structr.core.entity.relationship.LocationRelationship;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;

//~--- classes ----------------------------------------------------------------

/**
 * Test basic modification operations with graph objects (nodes, relationships)
 *
 * All tests are executed in superuser context
 *
 * @author Axel Morgner
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

			props.put(AbstractNode.type, type);
			props.put(AbstractNode.name, name);

			app.beginTx();
			final NodeInterface node = app.create(GenericNode.class, props);
			app.commitTx();

			// Check defaults
			assertEquals(GenericNode.class.getSimpleName(), node.getProperty(AbstractNode.type));
			assertTrue(node.getProperty(AbstractNode.name).equals(name));
			assertTrue(!node.getProperty(AbstractNode.hidden));
			assertTrue(!node.getProperty(AbstractNode.deleted));
			assertTrue(!node.getProperty(AbstractNode.visibleToAuthenticatedUsers));
			assertTrue(!node.getProperty(AbstractNode.visibleToPublicUsers));

			final String name2 = "GenericNode-name-äöüß";

			app.beginTx();
			
			// Modify values
			node.setProperty(AbstractNode.name, name2);
			node.setProperty(AbstractNode.hidden, true);
			node.setProperty(AbstractNode.deleted, true);
			node.setProperty(AbstractNode.visibleToAuthenticatedUsers, true);
			node.setProperty(AbstractNode.visibleToPublicUsers, true);
			
			app.commitTx();

			assertTrue(node.getProperty(AbstractNode.name).equals(name2));
			assertTrue(node.getProperty(AbstractNode.hidden));
			assertTrue(node.getProperty(AbstractNode.deleted));
			assertTrue(node.getProperty(AbstractNode.visibleToAuthenticatedUsers));
			assertTrue(node.getProperty(AbstractNode.visibleToPublicUsers));

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

			final LocationRelationship rel = (createTestRelationships(LocationRelationship.class, 1)).get(0);
			final PropertyKey key1         = new StringProperty("jghsdkhgshdhgsdjkfgh");
			final String val1              = "54354354546806849870";

			app.beginTx();
			rel.setProperty(key1, val1);
			app.commitTx();
			
			assertTrue("Expected relationship to have a value for key '" + key1.dbName() + "'", rel.getRelationship().hasProperty(key1.dbName()));
			
			assertEquals(val1, rel.getRelationship().getProperty(key1.dbName()));
			
			Object vrfy1 = rel.getProperty(key1);
			assertEquals(val1, vrfy1);
			
			final String val2 = "öljkhöohü8osdfhoödhi";

			app.beginTx();
			rel.setProperty(key1, val2);
			app.commitTx();
			
			Object vrfy2 = rel.getProperty(key1);
			assertEquals(val2, vrfy2);
			

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

}
