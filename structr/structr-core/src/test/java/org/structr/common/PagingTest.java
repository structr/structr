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
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.TestOne;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
public class PagingTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(PagingTest.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {

		super.test00DbAvailable();

	}

//
//      public void test01FirstPage() {
//
//              try {
//
//                      boolean includeDeletedAndHidden        = true;
//                      boolean publicOnly                     = false;
//                      String type                            = TestOne.class.getSimpleName();
//                      int number                             = 43;
//                      List<AbstractNode> nodes               = this.createTestNodes(type, number);
//                      List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
//
//                      searchAttributes.add(Search.andType(type));
//
//                      Result result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes);
//
//                      assertTrue(result.size() == number);
//
//                      String sortKey   = "name";
//                      boolean sortDesc = false;
//                      int pageSize     = 10;
//                      int page         = 1;
//
//                      result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page);
//
//                      logger.log(Level.INFO, "Raw result size: {0}, expected: {1}", new Object[] { result.getRawResultCount(), number });
//                      assertTrue(result.getRawResultCount() == number);
//                      logger.log(Level.INFO, "Result size: {0}, expected: {1}", new Object[] { result.size(), pageSize });
//                      assertTrue(result.size() == pageSize);
//
//              } catch (FrameworkException ex) {
//
//                      logger.log(Level.SEVERE, ex.toString());
//                      fail("Unexpected exception");
//
//              }
//
//      }

	/**
	 * Test different pages and page sizes
	 */
	public void test01Paging() {

		try {

			boolean includeDeletedAndHidden = false;
			boolean publicOnly              = false;
			String type                     = TestOne.class.getSimpleName();
			int number                      = 89;    // no more than 89 to avoid sort order TestOne-10, TestOne-100 ...
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

			Result result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes);

			assertTrue(result.size() == number);

			String sortKey   = AbstractNode.Key.name.name();
			boolean sortDesc = false;
			
			
			// test pages sizes from 0 to 10
			for (int ps=0; ps<10; ps++) {
				
				// test all pages
				for (int p=0; p<(number/Math.max(1,ps))+1; p++) {
			
					testPaging(ps, p, number, offset, includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc);
				
				}
			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	private void testPaging(final int pageSize, final int page, final int number, final int offset, final boolean includeDeletedAndHidden, final boolean publicOnly,
				final List<SearchAttribute> searchAttributes, final String sortKey, final boolean sortDesc)
		throws FrameworkException {

		Result result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page);

//              for (GraphObject obj : result.getResults()) {
//                      
//                      System.out.println(obj.getStringProperty(AbstractNode.Key.name));
//                      
//              }
		logger.log(Level.INFO, "Raw result size: {0}, expected: {1} (page size: {2}, page: {3})", new Object[] { result.getRawResultCount(), number, pageSize, page });
		assertTrue(result.getRawResultCount() == number);
		
		long expectedResultCount =  pageSize==0 ? result.getRawResultCount(): Math.min(number, pageSize);
		
		logger.log(Level.INFO, "Result size: {0}, expected: {1}", new Object[] { result.size(), expectedResultCount});
		assertTrue(result.size() == expectedResultCount);

		for (int j = (Math.max(page,1) - 1) * pageSize; j < expectedResultCount; j++) {

			String expectedName = "TestOne-" + (offset + j);
			String gotName      = result.get(j).getStringProperty(AbstractNode.Key.name);

			System.out.println(expectedName + ", got: " + gotName);
			assertTrue(gotName.equals(expectedName));

		}

	}

}
