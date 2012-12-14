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

import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.TestOne;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Test paging
 *
 * All tests are executed in superuser context
 *
 * @author Axel Morgner
 */
public class AdvancedPagingTest extends PagingTest {

	private static final Logger logger = Logger.getLogger(AdvancedPagingTest.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {

		super.test00DbAvailable();

	}

	@Override
	public void test01Paging() {}

	public void test02PagingAndCreate() {

		try {

			boolean includeDeletedAndHidden = false;
			boolean publicOnly              = false;
			String type                     = TestOne.class.getSimpleName();
			int number                      = 20;    // no more than 89 to avoid sort order TestOne-10, TestOne-100 ...
			List<AbstractNode> nodes        = this.createTestNodes(type, number);
			int offset                      = 10;
			int i                           = offset;
			String name;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			for (AbstractNode node : nodes) {

				// System.out.println("Node ID: " + node.getNodeId());
				name = "TestOne-" + i;

				i++;

				node.setName(name);
			}

			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

			searchAttributes.add(Search.andExactTypeAndSubtypes(type));

			Result result = searchNodeCommand.execute(searchAttributes);

			assertTrue(result.size() == number);

			PropertyKey sortKey = AbstractNode.name;
			boolean sortDesc    = false;
			int pageSize        = 2;
			int page            = 1;

			testPaging(pageSize, page, number, offset, includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc);

			PropertyMap props = new PropertyMap();

			props.put(sortKey, "TestOne-09");
			this.createTestNode(type, props);
			testPaging(pageSize, page + 1, number + 1, offset - 1, includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc);
			System.out.println("paging test finished");

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test03NegativeOffsetPaging() {

		try {

			boolean includeDeletedAndHidden = false;
			boolean publicOnly              = false;
			String type                     = TestOne.class.getSimpleName();
			int number                      = 8;
			List<AbstractNode> nodes        = this.createTestNodes(type, number);
			int offset                      = 0;
			int i                           = offset;
			String name;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			for (AbstractNode node : nodes) {

				// System.out.println("Node ID: " + node.getNodeId());
				name = "TestOne-" + i;

				i++;

				node.setName(name);
			}

			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

			searchAttributes.add(Search.andExactTypeAndSubtypes(type));

			Result result = searchNodeCommand.execute(searchAttributes);

			assertTrue(result.size() == number);

			PropertyKey sortKey = AbstractNode.name;
			boolean sortDesc    = false;
			int pageSize        = 2;
			int page            = 1;

			result = searchNodeCommand.execute(includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page);

			assertTrue(result.size() == 2);
			
			assertEquals("TestOne-0", result.get(0).getProperty(AbstractNode.name));
			assertEquals("TestOne-1", result.get(1).getProperty(AbstractNode.name));
			
			page = -1;
			
			result = searchNodeCommand.execute(includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page);

			assertTrue(result.size() == 2);
			assertEquals("TestOne-6", result.get(0).getProperty(AbstractNode.name));
			assertEquals("TestOne-7", result.get(1).getProperty(AbstractNode.name));
			
			page = -2;
			
			result = searchNodeCommand.execute(includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page);

			assertTrue(result.size() == 2);
			assertEquals("TestOne-4", result.get(0).getProperty(AbstractNode.name));
			assertEquals("TestOne-5", result.get(1).getProperty(AbstractNode.name));

			page = -3;
			
			result = searchNodeCommand.execute(includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page);

			assertTrue(result.size() == 2);
			assertEquals("TestOne-2", result.get(0).getProperty(AbstractNode.name));
			assertEquals("TestOne-3", result.get(1).getProperty(AbstractNode.name));

			page = -4;
			
			result = searchNodeCommand.execute(includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page);

			assertTrue(result.size() == 2);
			assertEquals("TestOne-0", result.get(0).getProperty(AbstractNode.name));
			assertEquals("TestOne-1", result.get(1).getProperty(AbstractNode.name));

			// now with offsetId
			
			page = 1;
			
			result = searchNodeCommand.execute(includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page, nodes.get(3).getUuid());

			assertTrue(result.size() == 2);
			assertEquals("TestOne-3", result.get(0).getProperty(AbstractNode.name));
			assertEquals("TestOne-4", result.get(1).getProperty(AbstractNode.name));

			page = -1;
			
			result = searchNodeCommand.execute(includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page, nodes.get(3).getUuid());

			assertTrue(result.size() == 2);
			assertEquals("TestOne-1", result.get(0).getProperty(AbstractNode.name));
			assertEquals("TestOne-2", result.get(1).getProperty(AbstractNode.name));
			
		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}
	
	public void test04OffsetPagingWithLargePageSize() {

		try {

			boolean includeDeletedAndHidden = false;
			boolean publicOnly              = false;
			String type                     = TestOne.class.getSimpleName();
			int number                      = 10;
			List<AbstractNode> nodes        = this.createTestNodes(type, number);
			int offset                      = 0;
			int i                           = offset;
			String name;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			for (AbstractNode node : nodes) {

				// System.out.println("Node ID: " + node.getNodeId());
				name = "TestOne-" + i;

				i++;

				node.setName(name);
			}

			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

			searchAttributes.add(Search.andExactTypeAndSubtypes(type));

			Result result = searchNodeCommand.execute(includeDeletedAndHidden, publicOnly, searchAttributes);

			assertTrue(result.size() == number);

			PropertyKey sortKey = AbstractNode.name;
			boolean sortDesc    = false;
			int pageSize        = 10;
			int page            = 1;

			// now with offsetId
			
			result = searchNodeCommand.execute(includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page, nodes.get(3).getUuid());

			assertTrue(result.size() == 7);
			assertEquals("TestOne-3", result.get(0).getProperty(AbstractNode.name));
			assertEquals("TestOne-4", result.get(1).getProperty(AbstractNode.name));
			assertEquals("TestOne-5", result.get(2).getProperty(AbstractNode.name));
			assertEquals("TestOne-6", result.get(3).getProperty(AbstractNode.name));
			assertEquals("TestOne-7", result.get(4).getProperty(AbstractNode.name));
			assertEquals("TestOne-8", result.get(5).getProperty(AbstractNode.name));
			assertEquals("TestOne-9", result.get(6).getProperty(AbstractNode.name));
			
		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test05UnkownOffsetId() {

		try {

			boolean includeDeletedAndHidden = false;
			boolean publicOnly              = false;
			String type                     = TestOne.class.getSimpleName();
			int number                      = 8;
			List<AbstractNode> nodes        = this.createTestNodes(type, number);
			int offset                      = 0;
			int i                           = offset;
			String name;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			for (AbstractNode node : nodes) {

				// System.out.println("Node ID: " + node.getNodeId());
				name = "TestOne-" + i;

				i++;

				node.setName(name);
			}

			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

			searchAttributes.add(Search.andExactTypeAndSubtypes(type));

			Result result = searchNodeCommand.execute(includeDeletedAndHidden, publicOnly, searchAttributes);

			assertTrue(result.size() == number);

			PropertyKey sortKey = AbstractNode.name;
			boolean sortDesc    = false;
			int pageSize        = 2;
			int page            = -5;
			
			result = searchNodeCommand.execute(includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page);

			assertTrue(result.size() == 2);
			assertEquals("TestOne-0", result.get(0).getProperty(AbstractNode.name));
			assertEquals("TestOne-1", result.get(1).getProperty(AbstractNode.name));

			// unknown offsetId
			
			page = 1;
			
			try {
				searchNodeCommand.execute(includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page, "00000000000000000000");
				
				fail("Should have failed with a FrameworkException with 'id not found' token");
				
			} catch (FrameworkException fex) {
				logger.log(Level.INFO, "Exception logged", fex);
			}

			
		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

}
