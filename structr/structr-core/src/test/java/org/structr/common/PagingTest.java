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


import java.util.LinkedList;
import java.util.List;
import org.structr.common.error.FrameworkException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.TestOne;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchAttribute;

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
	
	public void test01Paging() {

		try {
			
			boolean includeDeletedAndHidden = true;
			boolean publicOnly = false;
			
			String type = TestOne.class.getSimpleName();
			int number = 43;
			
			List<AbstractNode> nodes = this.createTestNodes(type, number);
			
			List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();
			
			searchAttributes.add(Search.andType(type));
						
			Result result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes);
			
			assertTrue(result.size() == number);
			
			String sortKey = "name";
			boolean sortDesc = false;
			int pageSize = 10;
			int page = 1;
			
			
			result = (Result) searchNodeCommand.execute(null, includeDeletedAndHidden, publicOnly, searchAttributes, sortKey, sortDesc, pageSize, page);
			
			logger.log(Level.INFO, "Raw result size: {0}, expected: {1}", new Object[]{ result.getRawResultCount(), number});
			assertTrue(result.getRawResultCount() == number);


			logger.log(Level.INFO, "Result size: {0}, expected: {1}", new Object[]{result.size(), pageSize});
			assertTrue(result.size() == pageSize);

			
			
			
		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

}
