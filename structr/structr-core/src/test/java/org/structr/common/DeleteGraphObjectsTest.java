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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Test basic delete operations with graph objects (nodes, relationships)
 *
 * All tests are executed in superuser context
 *
 * @author Axel Morgner
 */
public class DeleteGraphObjectsTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(DeleteGraphObjectsTest.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {

		super.test00DbAvailable();

	}

	/**
	 * Test successful deletion of a node.
	 *
	 * The node shouldn't be found afterwards.
	 * Creation and deletion are executed in two different transactions.
	 *
	 */
	public void test01DeleteNode() {

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

			assertTrue(node != null);

			final String nodeId = node.getUuid();

			transactionCommand.execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					return deleteNodeCommand.execute(nodeId);

				}

			});

			try {

				// Node should not be found after deletion
				AbstractNode nodeAfterDelete = (AbstractNode) findNodeCommand.execute(nodeId);

				fail("Should have raised a not found exception");
			} catch (FrameworkException fe) {}

			// Old node object should be non-accessible
			// node.getUuid();

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

}
