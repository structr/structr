/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
				testPaging(type, pageSize, page + 1, number + 1, offset - 1, includeDeletedAndHidden, publicOnly, sortKey, sortDesc);
				System.out.println("paging test finished");
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

		Principal user = null;

		try (final Tx tx = app.tx()) {

			// create non-admin user
			user = app.create(TestUser.class, "tester");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		try {

			final SecurityContext userContext   = SecurityContext.getInstance(user, AccessMode.Backend);
			final App userApp                   = StructrApp.getInstance(userContext);
			final Class type                    = TestOne.class;
			final int number                    = 1000;
			final List<NodeInterface> allNodes  = this.createTestNodes(type, number);
			final List<NodeInterface> userNodes = new LinkedList<>();
			final int offset                    = 0;

			try (final Tx tx = app.tx()) {

				int i = offset;
				for (NodeInterface node : allNodes) {

					// System.out.println("Node ID: " + node.getNodeId());
					String _name = "TestOne-" + StringUtils.leftPad(Integer.toString(i), 5, "0");

					if (Math.random() < 0.5) {

						node.setProperty(NodeInterface.owner, user);
						userNodes.add(node);
					}

					i++;

					node.setProperty(AbstractNode.name, _name);
				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				final PropertyKey sortKey = AbstractNode.name;
				final boolean sortDesc    = false;
				final int pageSize        = 10;
				final int page            = 22;
				final Result result       = userApp.nodeQuery(type).sort(sortKey).order(sortDesc).pageSize(pageSize).page(page).getResult();

				assertEquals("Invalid paging result count with non-superuser security context", userNodes.size(), (int)result.getRawResultCount());

				for (int i=0; i<pageSize; i++) {

					final NodeInterface expected = userNodes.get((pageSize * (page-1))+i);
					final NodeInterface actual   = (NodeInterface)result.get(i);

					assertEquals("Invalid paging result with non-superuser security context", expected, actual);
				}

				tx.success();
			}


		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, ex.toString());
			fail("Unexpected exception");

		}


	}
}
