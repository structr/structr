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
/*
*  Copyright (C) 2010-2013 Axel Morgner
*
*  This file is part of Structr <http://structr.org>.
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
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.TestOne;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.app.Query;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;

//~--- classes ----------------------------------------------------------------

/**
 * Test paging
 *
 * All tests are executed in superuser context
 *
 *
 */
public class PagingTest extends StructrTest {

	private static final Logger logger = Logger.getLogger(PagingTest.class.getName());

	//~--- methods --------------------------------------------------------

	@Override
	public void test00DbAvailable() {

		super.test00DbAvailable();

	}


      public void test01FirstPage() {

		try {

			Class type                             = TestOne.class;
			int number                             = 43;

			// create nodes
			this.createTestNodes(type, number);

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(type).getResult();

				assertTrue(result.size() == number);

				PropertyKey sortKey = AbstractNode.name;
				boolean sortDesc    = false;
				int pageSize        = 10;
				int page            = 1;

				result = app.nodeQuery(type).includeDeletedAndHidden().sort(sortKey).order(sortDesc).page(page).pageSize(pageSize).getResult();

				logger.log(Level.INFO, "Raw result size: {0}, expected: {1}", new Object[] { result.getRawResultCount(), number });
				assertTrue(result.getRawResultCount() == number);
				logger.log(Level.INFO, "Result size: {0}, expected: {1}", new Object[] { result.size(), pageSize });
				assertTrue(result.size() == pageSize);
			}

              } catch (FrameworkException ex) {

		      ex.printStackTrace();

                      logger.log(Level.SEVERE, ex.toString());
                      fail("Unexpected exception");

              }

      }

	/**
	 * Test different pages and page sizes
	 */
	public void test01Paging() {

		try {

			boolean includeDeletedAndHidden = false;
			boolean publicOnly              = false;
			Class type                      = TestOne.class;
			int number                      = 89;    // no more than 89 to avoid sort order TestOne-10, TestOne-100 ...
			final int offset                = 10;
			final List<NodeInterface> nodes = this.createTestNodes(type, number);

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			try (final Tx tx = app.tx()) {


				int i                           = offset;
				for (NodeInterface node : nodes) {

					// System.out.println("Node ID: " + node.getNodeId());
					String _name = "TestOne-" + i;

					i++;

					node.setProperty(AbstractNode.name, _name);
				}
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(type).getResult();

				assertEquals(number, result.size());

				PropertyKey sortKey = AbstractNode.name;
				boolean sortDesc    = false;


				// test pages sizes from 0 to 10
				for (int ps=0; ps<10; ps++) {

					// test all pages
					for (int p=0; p<(number/Math.max(1,ps))+1; p++) {

						testPaging(type, ps, p, number, offset, includeDeletedAndHidden, publicOnly, sortKey, sortDesc);

					}
				}
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	protected void testPaging(final Class type, final int pageSize, final int page, final int number, final int offset, final boolean includeDeletedAndHidden, final boolean publicOnly, final PropertyKey sortKey, final boolean sortDesc) throws FrameworkException {

		final Query query = app.nodeQuery(type).sort(sortKey).order(sortDesc).page(page).pageSize(pageSize);

		if (includeDeletedAndHidden) {
			query.includeDeletedAndHidden();
		}

		final Result result = query.getResult();

		logger.log(Level.INFO, "Raw result size: {0}, expected: {1} (page size: {2}, page: {3})", new Object[] { result.getRawResultCount(), number, pageSize, page });
		assertTrue(result.getRawResultCount() == ((pageSize == 0 || page == 0) ? 0 : number));

		long expectedResultCount = (pageSize == 0 || page == 0)
					   ? 0
					   : Math.min(number, pageSize);

		int startIndex = (Math.max(page, 1) - 1) * pageSize;

		logger.log(Level.INFO, "Result size: {0}, expected: {1}, start index: {2}", new Object[] { result.size(), expectedResultCount, startIndex });
		assertTrue(result.size() == expectedResultCount);


		for (int j = 0; j < expectedResultCount; j++) {

			String expectedName = "TestOne-" + (offset + j + startIndex);
			String gotName      = result.get(j).getProperty(AbstractNode.name);

			System.out.println(expectedName + ", got: " + gotName);
			assertEquals(expectedName, gotName);

		}
	}
}
