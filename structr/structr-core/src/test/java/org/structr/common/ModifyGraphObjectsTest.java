/*
 *  Copyright (C) 2010-2012 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.common;


import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.StructrTransaction;

//~--- JDK imports ------------------------------------------------------------


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.entity.AbstractRelationship;

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

			AbstractNode node;
			final Map<String, Object> props = new HashMap<String, Object>();
			String type                     = "UnknownTestType";
			String name                     = "GenericNode-name";

			props.put(AbstractNode.Key.type.name(), type);
			props.put(AbstractNode.Key.name.name(), name);

			node = (AbstractNode) transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					// Create node with a type which has no entity class => should result in a node of type 'GenericNode'
					return (AbstractNode) createNodeCommand.execute(props);
				}

			});

			// Check defaults
			assertTrue(node.getStringProperty(AbstractNode.Key.type).equals(type));
			assertTrue(node.getStringProperty(AbstractNode.Key.name).equals(name));
			assertTrue(!node.getBooleanProperty(AbstractNode.Key.hidden));
			assertTrue(!node.getBooleanProperty(AbstractNode.Key.deleted));
			assertTrue(!node.getBooleanProperty(AbstractNode.Key.visibleToAuthenticatedUsers));
			assertTrue(!node.getBooleanProperty(AbstractNode.Key.visibleToPublicUsers));

			name = "GenericNode-name-äöüß";

			// Modify values
			node.setProperty(AbstractNode.Key.name, name);
			assertTrue(node.getStringProperty(AbstractNode.Key.name).equals(name));
			node.setProperty(AbstractNode.Key.hidden, true);
			assertTrue(node.getBooleanProperty(AbstractNode.Key.hidden));
			node.setProperty(AbstractNode.Key.deleted, true);
			assertTrue(node.getBooleanProperty(AbstractNode.Key.deleted));
			node.setProperty(AbstractNode.Key.visibleToAuthenticatedUsers, true);
			assertTrue(node.getBooleanProperty(AbstractNode.Key.visibleToAuthenticatedUsers));
			node.setProperty(AbstractNode.Key.visibleToPublicUsers, true);
			assertTrue(node.getBooleanProperty(AbstractNode.Key.visibleToPublicUsers));

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

			AbstractRelationship rel = ((List<AbstractRelationship>) createTestRelationships(RelType.UNDEFINED, 1)).get(0);

			String key1 = "jghsdkhgshdhgsdjkfgh";
			String val1 = "54354354546806849870";

			// Modify values
			rel.setProperty(key1, val1);
			assertTrue(rel.getStringProperty(key1).equals(val1));
			
			val1 = "öljkhöohü8osdfhoödhi";
			rel.setProperty(key1, val1);
			assertTrue(rel.getStringProperty(key1).equals(val1));
			

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

}
