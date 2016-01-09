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
import org.structr.core.property.PropertyMap;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.TestOne;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Principal;
import org.structr.core.entity.TestUser;
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
			Class type                      = TestOne.class;
			int number                      = 20;    // no more than 89 to avoid sort order TestOne-10, TestOne-100 ...
			final List<NodeInterface> nodes = this.createTestNodes(type, number);
			final int offset                = 10;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			try (final Tx tx = app.tx()) {

				int i = offset;
				for (NodeInterface node : nodes) {

					// System.out.println("Node ID: " + node.getNodeId());
					String _name = "TestOne-" + i;

					i++;

					node.setProperty(AbstractNode.name, _name);
				}
				tx.success();
			}

			try (final Tx tx = app.tx()) {
				List<NodeInterface> result = app.get(type);

				assertTrue(result.size() == number);

				PropertyKey sortKey = AbstractNode.name;
				boolean sortDesc    = false;
				int pageSize        = 2;
				int page            = 1;

				testPaging(type, pageSize, page, number, offset, includeDeletedAndHidden, publicOnly, sortKey, sortDesc);

				PropertyMap props = new PropertyMap();

				props.put(sortKey, "TestOne-09");
				this.createTestNode(type, props);

				tx.success();
			}


			try (final Tx tx = app.tx()) {

				PropertyKey sortKey = AbstractNode.name;
				boolean sortDesc    = false;
				int pageSize        = 2;
				int page            = 1;

				testPaging(type, pageSize, page + 1, number + 1, offset - 1, includeDeletedAndHidden, publicOnly, sortKey, sortDesc);
				System.out.println("paging test finished");

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test03NegativeOffsetPaging() {

		try {

			Class type                      = TestOne.class;
			int number                      = 8;
			final List<NodeInterface> nodes = this.createTestNodes(type, number);
			final int offset                = 0;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			try (final Tx tx = app.tx()) {

				int i = offset;
				for (NodeInterface node : nodes) {

					// System.out.println("Node ID: " + node.getNodeId());
					String _name = "TestOne-" + i;

					i++;

					node.setProperty(AbstractNode.name, _name);
				}
				tx.success();
			}

			try (final Tx tx = app.tx()) {
				List<NodeInterface> result = app.get(type);

				assertEquals(number, result.size());

				PropertyKey sortKey = AbstractNode.name;
				boolean sortDesc    = false;
				int pageSize        = 2;
				int page            = 1;

				result = app.nodeQuery(type).sort(sortKey).order(sortDesc).pageSize(pageSize).page(page).getAsList();

				assertEquals(2, result.size());
				assertEquals("TestOne-0", result.get(0).getProperty(AbstractNode.name));
				assertEquals("TestOne-1", result.get(1).getProperty(AbstractNode.name));

				page = -1;

				result = app.nodeQuery(type).sort(AbstractNode.name).pageSize(pageSize).page(page).getAsList();

				assertEquals(2, result.size());
				assertEquals("TestOne-6", result.get(0).getProperty(AbstractNode.name));
				assertEquals("TestOne-7", result.get(1).getProperty(AbstractNode.name));

				page = -2;

				result = app.nodeQuery(type).sort(sortKey).order(sortDesc).pageSize(pageSize).page(page).getAsList();

				assertEquals(2, result.size());
				assertEquals("TestOne-4", result.get(0).getProperty(AbstractNode.name));
				assertEquals("TestOne-5", result.get(1).getProperty(AbstractNode.name));

				page = -3;

				result = app.nodeQuery(type).sort(sortKey).order(sortDesc).pageSize(pageSize).page(page).getAsList();

				assertEquals(2, result.size());
				assertEquals("TestOne-2", result.get(0).getProperty(AbstractNode.name));
				assertEquals("TestOne-3", result.get(1).getProperty(AbstractNode.name));

				page = -4;

				result = app.nodeQuery(type).sort(sortKey).order(sortDesc).pageSize(pageSize).page(page).getAsList();

				assertEquals(2, result.size());
				assertEquals("TestOne-0", result.get(0).getProperty(AbstractNode.name));
				assertEquals("TestOne-1", result.get(1).getProperty(AbstractNode.name));

				// now with offsetId

				page = 1;


				result = app.nodeQuery(type).sort(sortKey).order(sortDesc).pageSize(pageSize).page(page).offsetId(nodes.get(3).getUuid()).getAsList();

				assertEquals(2, result.size());
				assertEquals("TestOne-3", result.get(0).getProperty(AbstractNode.name));
				assertEquals("TestOne-4", result.get(1).getProperty(AbstractNode.name));

				page = -1;

				result = app.nodeQuery(type).sort(sortKey).order(sortDesc).pageSize(pageSize).page(page).offsetId(nodes.get(5).getUuid()).getAsList();

				assertEquals(2, result.size());
				assertEquals("TestOne-3", result.get(0).getProperty(AbstractNode.name));
				assertEquals("TestOne-4", result.get(1).getProperty(AbstractNode.name));
			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test04OffsetPagingWithLargePageSize() {

		try {

			Class type                      = TestOne.class;
			int number                      = 10;
			final List<NodeInterface> nodes = this.createTestNodes(type, number);
			final int offset                = 0;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			try (final Tx tx = app.tx()) {

				int i = offset;
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
				int pageSize        = 10;
				int page            = 1;


				// now with offsetId

				result = app.nodeQuery(type).sort(sortKey).order(sortDesc).pageSize(pageSize).page(page).offsetId(nodes.get(3).getUuid()).getResult();



				assertEquals(7, result.size());
				assertEquals("TestOne-3", result.get(0).getProperty(AbstractNode.name));
				assertEquals("TestOne-4", result.get(1).getProperty(AbstractNode.name));
				assertEquals("TestOne-5", result.get(2).getProperty(AbstractNode.name));
				assertEquals("TestOne-6", result.get(3).getProperty(AbstractNode.name));
				assertEquals("TestOne-7", result.get(4).getProperty(AbstractNode.name));
				assertEquals("TestOne-8", result.get(5).getProperty(AbstractNode.name));
				assertEquals("TestOne-9", result.get(6).getProperty(AbstractNode.name));

				tx.success();
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test05UnkownOffsetId() {

		try {

			Class type                      = TestOne.class;
			int number                      = 8;
			final List<NodeInterface> nodes = this.createTestNodes(type, number);
			final int offset                = 0;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			try (final Tx tx = app.tx()) {

				int i = offset;
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
				int pageSize        = 2;
				int page            = -5;

				result = app.nodeQuery(type).sort(sortKey).order(sortDesc).pageSize(pageSize).page(page).getResult();

				assertEquals(2, result.size());
				assertEquals("TestOne-0", result.get(0).getProperty(AbstractNode.name));
				assertEquals("TestOne-1", result.get(1).getProperty(AbstractNode.name));

				// unknown offsetId

				page = 1;

				try {
					app.nodeQuery(type).sort(sortKey).order(sortDesc).pageSize(pageSize).page(page).offsetId("000000000000000000000").getResult();

					fail("Should have failed with a FrameworkException with 'id not found' token");

				} catch (FrameworkException fex) {
					logger.log(Level.INFO, "Exception logged", fex);
				}
			}


		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}

	}

	public void test06PagingVisibility() {

		Principal tester1 = null;
		Principal tester2 = null;

		try (final Tx tx = app.tx()) {

			// create non-admin user
			tester1 = app.create(TestUser.class, "tester1");
			tester2 = app.create(TestUser.class, "tester2");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try {

			final SecurityContext tester1Context     = SecurityContext.getInstance(tester1, AccessMode.Backend);
			final SecurityContext tester2Context     = SecurityContext.getInstance(tester2, AccessMode.Backend);
			final App tester1App                     = StructrApp.getInstance(tester1Context);
			final App tester2App                     = StructrApp.getInstance(tester2Context);
			final Class type                         = TestOne.class;
			final int number                         = 1000;
			final List<NodeInterface> allNodes       = this.createTestNodes(type, number);
			final List<NodeInterface> tester1Nodes   = new LinkedList<>();
			final List<NodeInterface> tester2Nodes   = new LinkedList<>();
			final int offset                         = 0;

			try (final Tx tx = app.tx()) {

				int i = offset;
				for (NodeInterface node : allNodes) {

					// System.out.println("Node ID: " + node.getNodeId());
					String _name = "TestOne-" + StringUtils.leftPad(Integer.toString(i), 5, "0");

					final double rand = Math.random();

					if (rand < 0.3) {

						node.setProperty(NodeInterface.owner, tester1);
						tester1Nodes.add(node);

					} else if (rand < 0.6) {

						node.setProperty(NodeInterface.owner, tester2);
						tester2Nodes.add(node);
					}

					i++;

					node.setProperty(AbstractNode.name, _name);
				}

				tx.success();
			}

			final int tester1NodeCount   = tester1Nodes.size();
			final int tester2NodeCount   = tester2Nodes.size();

			try (final Tx tx = app.tx()) {

				final PropertyKey sortKey = AbstractNode.name;
				final boolean sortDesc    = false;
				final int pageSize        = 10;
				final int page            = 22;
				final Result result       = tester1App.nodeQuery(type).sort(sortKey).order(sortDesc).pageSize(pageSize).page(page).getResult();

				assertEquals("Invalid paging result count with non-superuser security context", tester1NodeCount, (int)result.getRawResultCount());

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				final PropertyKey sortKey = AbstractNode.name;
				final boolean sortDesc    = false;
				final int pageSize        = 10;
				final int page            = 22;
				final Result result       = tester2App.nodeQuery(type).sort(sortKey).order(sortDesc).pageSize(pageSize).page(page).getResult();

				assertEquals("Invalid paging result count with non-superuser security context", tester2NodeCount, (int)result.getRawResultCount());

				tx.success();
			}


		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}
	}

	public void test07PagingOverflow() {

		try {

			final Class type = TestOne.class;

			// create 20 nodes
			createTestNodes(type, 20);

			try (final Tx tx = app.tx()) {

				// request a page beyond the number of existing elements
				app.nodeQuery(type).pageSize(10).page(100).getAsList();

				tx.success();
			}

		} catch (Throwable t) {
			fail("Requesting a page beyond the number of existing elements should not throw an exception.");
		}
	}

	public void testPagingWithHiddenOrDeletedElements() {

		try {

			// create 20 nodes
			createTestNodes(TestOne.class, 10);

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final List<TestOne> testOnes = app.nodeQuery(TestOne.class).getAsList();

			final TestOne test1 = testOnes.get(3);
			final TestOne test2 = testOnes.get(4);
			final TestOne test3 = testOnes.get(7);

			test1.setProperty(AbstractNode.hidden, true);

			test2.setProperty(AbstractNode.deleted, true);

			test3.setProperty(AbstractNode.hidden, true);
			test3.setProperty(AbstractNode.deleted, true);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final Result<TestOne> result = app.nodeQuery(TestOne.class).getResult();

			assertEquals("Result count should not include deleted or hidden nodes", 7, (int)result.getRawResultCount());
			assertEquals("Actual result size should be equal to result count", 7, (int)result.getResults().size());


			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}


	}
}
