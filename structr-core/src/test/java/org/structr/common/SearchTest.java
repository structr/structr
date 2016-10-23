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

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SixOneManyToMany;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestSix;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;

//~--- classes ----------------------------------------------------------------

/**
 * Test search
 *
 * All tests are executed in superuser context
 *
 *
 */
public class SearchTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(SearchTest.class.getName());

	@Test
	public void test01SeachByName() {

		try  {

			Class type                      = TestOne.class;
			int number                      = 4;
			final List<NodeInterface> nodes = this.createTestNodes(type, number);
			final int offset                = 10;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			try (final Tx tx = app.tx()) {

				int i = offset;
				String name;

				for (NodeInterface node : nodes) {

					//System.out.println("Node ID: " + node.getNodeId());

					name = "TestOne-" + i;

					i++;

					node.setProperty(AbstractNode.name, name);

				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				Result<TestOne> result = app.nodeQuery(type).getResult();

				assertEquals(4, result.size());

				for (NodeInterface node : result.getResults()) {
					System.out.println(node);
				}

				result = app.nodeQuery(type).andName("TestOne-12").getResult();

				assertEquals(1, result.size());

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test02SeachByNameProperty() {

		try  {

			Class type                      = TestOne.class;
			int number                      = 4;
			final List<NodeInterface> nodes = this.createTestNodes(type, number);
			final int offset                = 10;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			try (final Tx tx = app.tx()) {

				int i = offset;
				String name;

				for (NodeInterface node : nodes) {

					//System.out.println("Node ID: " + node.getNodeId());

					name = "TestOne-" + i;

					i++;

					node.setProperty(AbstractNode.name, name);

				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(type).and(TestOne.name, "TestOne-13").getResult();

				assertEquals(1, result.size());

				tx.success();

			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test03SeachByNamePropertyLooseLowercase() {

		try  {

			Class type                      = TestOne.class;
			int number                      = 4;
			final List<NodeInterface> nodes = this.createTestNodes(type, number);
			final int offset                = 10;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			try (final Tx tx = app.tx()) {

				int i = offset;
				String name;

				for (NodeInterface node : nodes) {

					//System.out.println("Node ID: " + node.getNodeId());

					name = "TestOne-" + i;

					i++;

					node.setProperty(AbstractNode.name, name);

				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(type).and(TestOne.name, "testone", false).getResult();

				assertEquals(4, result.size());

				tx.success();

			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test04SeachByNamePropertyLooseUppercase() {

		try  {

			Class type                      = TestOne.class;
			int number                      = 4;
			final List<NodeInterface> nodes = this.createTestNodes(type, number);
			final int offset                = 10;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			try (final Tx tx = app.tx()) {

				int i = offset;
				String name;

				for (NodeInterface node : nodes) {

					//System.out.println("Node ID: " + node.getNodeId());

					name = "TestOne-" + i;

					i++;

					node.setProperty(AbstractNode.name, name);

				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(type).and(TestOne.name, "TestOne", false).getResult();

				assertEquals(4, result.size());

				tx.success();

			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test05SeachByDefaultValue() {

		try  {

			Class type                      = TestOne.class;
			int number                      = 4;
			final List<NodeInterface> nodes = this.createTestNodes(type, number);
			final int offset                = 10;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			try (final Tx tx = app.tx()) {

				int i = offset;
				String name;

				for (NodeInterface node : nodes) {

					//System.out.println("Node ID: " + node.getNodeId());

					name = "TestOne-" + i;

					i++;

					node.setProperty(AbstractNode.name, name);

				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(type).and(TestOne.stringWithDefault, "default value", false).getResult();

				assertEquals(4, result.size());

				tx.success();

			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test06InexactSearchWithHyphen() {

		final Map<String, Integer> testResults = new LinkedHashMap<>();

		testResults.put("ABCDE12345",  1);
		testResults.put("ABCDE#1234",  1);
		testResults.put("ABCDE+2345",  1);
		testResults.put("ABCDE-2345",  1);
		testResults.put("ABCDE!2345",  1);
		testResults.put("ABCDE(2345",  1);
		testResults.put("ABCDE)2345",  1);
		testResults.put("ABCDE:2345",  1);
		testResults.put("ABCDE^2345",  1);
		testResults.put("ABCDE[2345",  1);
		testResults.put("ABCDE]2345",  1);
		testResults.put("ABCDE\"2345", 1);
		testResults.put("ABCDE{2345",  1);
		testResults.put("ABCDE}2345",  1);
		testResults.put("ABCDE~2345",  1);
		testResults.put("ABCDE*2345", 21);
		testResults.put("ABCDE?2345", 21);
		testResults.put("ABCDE|2345",  1);
		testResults.put("ABCDE&2345",  1);
		testResults.put("ABCDE;2345",  1);
		testResults.put("ABCDE/2345",  1);

		try  {

			try (final Tx tx = app.tx()) {

				for (final String source : testResults.keySet()) {
					app.create(TestOne.class, source);
				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				for (final String source : testResults.keySet()) {

					final String query         = source.substring(0, 6);
					final int count            = testResults.get(source);
					final List<TestOne> result = app.nodeQuery(TestOne.class).and(AbstractNode.name, query, false).getAsList();

					assertEquals("Unexpected query result for special char query " + query, count, result.size());
				}

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test07NodeQueryByType() {

		try  {

			Class type                      = TestOne.class;
			int number                      = 1000;
			final List<NodeInterface> nodes = this.createTestNodes(type, number);

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			try (final Tx tx = app.tx()) {

				long t0 = System.currentTimeMillis();

				Result<? extends GraphObject> result = app.nodeQuery(NodeInterface.class).getResult();

				long t1 = System.currentTimeMillis();
				logger.info("Query with inexact type took {} ms", t1-t0);
				assertEquals(1012, result.size());

				result = app.nodeQuery(NodeInterface.class).getResult();

				long t2 = System.currentTimeMillis();
				logger.info("Query with exact type took {} ms", t2-t1);
				assertEquals(1012, result.size());

				// TODO: Implement app.nodeQuery() to return all nodes in the system as an alternative to the (slow) app.nodeQuery(NodeInterface.class)
//				result = app.nodeQuery().getResult();
//
//				long t3 = System.currentTimeMillis();
//				logger.info("Query without type took {} ms", t3-t2);
//				assertEquals(1012, result.size());

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}
	}

	@Test
	public void test08InexactSearch() {

		try  {

			final Date date    = new Date();
			final TestOne test = createTestNode(TestOne.class,
				new NodeAttribute(TestOne.name, "TestOne"),
				new NodeAttribute(TestOne.aBoolean, true),
				new NodeAttribute(TestOne.aDate, date),
				new NodeAttribute(TestOne.aDouble, 1.234),
				new NodeAttribute(TestOne.aLong, 12345L),
				new NodeAttribute(TestOne.anEnum, TestOne.Status.One),
				new NodeAttribute(TestOne.anInt, 123)
			);

			try (final Tx tx = app.tx()) {

				assertEquals("Invalid inexact search result for type String",  test, app.nodeQuery(TestOne.class).and(TestOne.name,    "TestOne",           false).getFirst());
				assertEquals("Invalid inexact search result for type Boolean", test, app.nodeQuery(TestOne.class).and(TestOne.aBoolean, true,               false).getFirst());
				assertEquals("Invalid inexact search result for type Date",    test, app.nodeQuery(TestOne.class).and(TestOne.aDate,    date,               false).getFirst());
				assertEquals("Invalid inexact search result for type Double",  test, app.nodeQuery(TestOne.class).and(TestOne.aDouble,  1.234,              false).getFirst());
				assertEquals("Invalid inexact search result for type Long",    test, app.nodeQuery(TestOne.class).and(TestOne.aLong,    12345L,             false).getFirst());
				assertEquals("Invalid inexact search result for type String",  test, app.nodeQuery(TestOne.class).and(TestOne.anEnum,   TestOne.Status.One, false).getFirst());
				assertEquals("Invalid inexact search result for type Enum",    test, app.nodeQuery(TestOne.class).and(TestOne.anInt,    123,                false).getFirst());

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}
	}

	@Test
	public void test09SearchBySourceAndTargetId() {

		try  {

			final TestOne test1       = createTestNode(TestOne.class);
			final List<TestSix> tests = createTestNodes(TestSix.class, 5);

			try (final Tx tx = app.tx()) {

				test1.setProperty(TestOne.manyToManyTestSixs, tests);

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				final List<SixOneManyToMany> result1 = app.relationshipQuery(SixOneManyToMany.class).and(SixOneManyToMany.sourceId, tests.get(0).getUuid()).getAsList();

				assertEquals("Invalid sourceId query result", 1, result1.size());

				tx.success();

			}


			try (final Tx tx = app.tx()) {

				final List<SixOneManyToMany> result1 = app.relationshipQuery(SixOneManyToMany.class).and(SixOneManyToMany.targetId, test1.getUuid()).getAsList();

				assertEquals("Invalid targetId query result", 5, result1.size());

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}
}
