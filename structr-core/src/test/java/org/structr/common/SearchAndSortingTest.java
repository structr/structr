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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.commons.lang3.StringUtils;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Principal;
import org.structr.core.entity.SixOneManyToMany;
import org.structr.core.entity.TestOne;
import org.structr.core.entity.TestSeven;
import org.structr.core.entity.TestSix;
import org.structr.core.entity.TestUser;
import org.structr.core.entity.relationship.NodeHasLocation;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;

/**
 */
public class SearchAndSortingTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(SearchAndSortingTest.class.getName());

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
				assertEquals(1006, result.size());

				result = app.nodeQuery(NodeInterface.class).getResult();

				long t2 = System.currentTimeMillis();
				logger.info("Query with exact type took {} ms", t2-t1);
				assertEquals(1006, result.size());

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

	@Test
	public void test01SortByName() {

		try  {

			Class type                      = TestOne.class;
			int number                      = 4; // no more than 89 to avoid sort order TestOne-10, TestOne-100 ...
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

				Result result = app.nodeQuery(type).getResult();

				assertEquals(number, result.size());

				PropertyKey sortKey = AbstractNode.name;
				boolean sortDesc    = false;
				int pageSize        = 10;
				int page            = 1;

				result = app.nodeQuery(type).sort(sortKey).order(sortDesc).page(page).pageSize(pageSize).getResult();

				logger.info("Raw result size: {}, expected: {}", new Object[] { result.getRawResultCount(), number });
				assertEquals(number, (int) result.getRawResultCount());
				logger.info("Result size: {}, expected: {}", new Object[] { result.size(), Math.min(number, pageSize) });
				assertEquals(Math.min(number, pageSize), result.size());

				for (int j = 0; j < Math.min(result.size(), pageSize); j++) {

					String expectedName = "TestOne-" + (offset + j);
					String gotName     = result.get(j).getProperty(AbstractNode.name);

					System.out.println(expectedName + ", got: " + gotName);
					assertEquals(expectedName, gotName);

				}

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test02SortByNameDesc() {

		try {

			Class type                      = TestOne.class;
			int number                      = 43;
			final List<NodeInterface> nodes = this.createTestNodes(type, number);
			final int offset                = 10;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			try (final Tx tx = app.tx()) {

				int i = offset;
				String name;

				for (NodeInterface node : nodes) {

					name = Integer.toString(i);

					i++;

					node.setProperty(AbstractNode.name, name);

				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(type).getResult();

				assertEquals(number, result.size());

				PropertyKey sortKey = AbstractNode.name;
				boolean sortDesc    = true;
				int pageSize        = 10;
				int page            = 1;

				result = app.nodeQuery(type).sort(sortKey).order(sortDesc).page(page).pageSize(pageSize).getResult();

				logger.info("Raw result size: {}, expected: {}", new Object[] { result.getRawResultCount(), number });
				assertTrue(result.getRawResultCount() == number);
				logger.info("Result size: {}, expected: {}", new Object[] { result.size(), Math.min(number, pageSize) });
				assertTrue(result.size() == Math.min(number, pageSize));

				for (int j = 0; j < Math.min(result.size(), pageSize); j++) {

					int expectedNumber = number + offset - 1 - j;
					String gotName     = result.get(j).getProperty(AbstractNode.name);

					System.out.println(expectedNumber + ", got: " + gotName);
					assertEquals(Integer.toString(expectedNumber), gotName);

				}

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test03SortByDate() {

		try {

			Class type                      = TestOne.class;
			int number                      = 97;
			final List<NodeInterface> nodes = this.createTestNodes(type, number);
			final int offset                = 10;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			try (final Tx tx = app.tx()) {

				int i = offset;
				String name;

				for (NodeInterface node : nodes) {

					name = Integer.toString(i);

					i++;

					node.setProperty(AbstractNode.name, "TestOne-" + name);

					node.setProperty(TestOne.aDate, new Date());

					// slow down execution speed to make sure distinct changes fall in different milliseconds
					try { Thread.sleep(2); } catch (Throwable t) {}

				}
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(type).getResult();

				assertEquals(number, result.size());

				PropertyKey sortKey = TestOne.aDate;
				boolean sortDesc    = false;
				int pageSize        = 10;
				int page            = 1;

				result = app.nodeQuery(type).sort(sortKey).order(sortDesc).page(page).pageSize(pageSize).getResult();

				logger.info("Raw result size: {}, expected: {}", new Object[] { result.getRawResultCount(), number });
				assertTrue(result.getRawResultCount() == number);
				logger.info("Result size: {}, expected: {}", new Object[] { result.size(), pageSize });
				assertTrue(result.size() == Math.min(number, pageSize));

				for (int j = 0; j < Math.min(result.size(), pageSize); j++) {

					String expectedName = "TestOne-" + (offset + j);
					String gotName     = result.get(j).getProperty(AbstractNode.name);

					System.out.println(expectedName + ", got: " + gotName);
					assertEquals(expectedName, gotName);

				}

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test04SortByDateDesc() {

		try {

			Class type                      = TestOne.class;
			int number                      = 131;
			final List<NodeInterface> nodes = this.createTestNodes(type, number);
			final int offset                = 10;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			try (final Tx tx = app.tx()) {

				int i = offset;
				String name;

				for (NodeInterface node : nodes) {

					name = Integer.toString(i);

					i++;

					node.setProperty(AbstractNode.name, name);

					// slow down execution speed to make sure distinct changes fall in different milliseconds
					try { Thread.sleep(2); } catch (Throwable t) {}
				}
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(type).getResult();

				assertEquals(number, result.size());

				PropertyKey sortKey = AbstractNode.lastModifiedDate;
				boolean sortDesc    = true;
				int pageSize        = 10;
				int page            = 1;

				result = app.nodeQuery(type).sort(sortKey).order(sortDesc).page(page).pageSize(pageSize).getResult();

				logger.info("Raw result size: {}, expected: {}", new Object[] { result.getRawResultCount(), number });
				assertTrue(result.getRawResultCount() == number);
				logger.info("Result size: {}, expected: {}", new Object[] { result.size(), pageSize });
				assertTrue(result.size() == Math.min(number, pageSize));

				for (int j = 0; j < pageSize; j++) {

					int expectedNumber = number + offset - 1 - j;
					String gotName     = result.get(j).getProperty(AbstractNode.name);

					System.out.println(expectedNumber + ", got: " + gotName);
					assertEquals(Integer.toString(expectedNumber), gotName);

				}

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test05SortByInt() {

		try {

			Class type                      = TestOne.class;
			int number                      = 61;
			final List<NodeInterface> nodes = this.createTestNodes(type, number);
			final PropertyKey key           = TestOne.anInt;
			final int offset                = 10;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			try (final Tx tx = app.tx()) {

				int i = offset;

				for (NodeInterface node : nodes) {

					node.setProperty(AbstractNode.name, Integer.toString(i));
					node.setProperty(key, i);

					i++;
				}
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(type).getResult();

				assertEquals(number, result.size());

				PropertyKey sortKey = key;
				boolean sortDesc    = false;
				int pageSize        = 5;
				int page            = 1;

				result = app.nodeQuery(type).sort(sortKey).order(sortDesc).page(page).pageSize(pageSize).getResult();

				logger.info("Raw result size: {}, expected: {}", new Object[] { result.getRawResultCount(), number });
				assertTrue(result.getRawResultCount() == number);
				logger.info("Result size: {}, expected: {}", new Object[] { result.size(), pageSize });
				assertTrue(result.size() == Math.min(number, pageSize));

				for (int j = 0; j < pageSize; j++) {

					int expectedNumber = offset + j;
					int gotNumber      = (Integer) result.get(j).getProperty(key);

					System.out.println("expected: " + expectedNumber + ", got: " + gotNumber);
					assertEquals(expectedNumber, gotNumber);

				}

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test06SortByDateWitNullValues() {

		try {

			Class type                      = TestOne.class;
			int number                      = 20;
			final List<NodeInterface> nodes = this.createTestNodes(type, number);
			final int offset                = 10;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			int i = offset;
			String name;

			try (final Tx tx = app.tx()) {

				for (NodeInterface node : nodes) {

					name = Integer.toString(i);

					i++;

					node.setProperty(AbstractNode.name, "TestOne-" + name);

					if ((i % 2) != 0) {
						node.setProperty(TestOne.aDate, new Date());
						System.out.println("TestOne-" + name + ": indexed with date");
					} else {
						node.setProperty(TestOne.aDate, null);
						System.out.println("TestOne-" + name + ": null date");
					}

					// slow down execution speed to make sure distinct changes fall in different milliseconds
					try { Thread.sleep(2); } catch (Throwable t) {}

				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(type).getResult();

				assertEquals(number, result.size());

				PropertyKey sortKey = TestOne.aDate;
				boolean sortDesc    = true;
				int pageSize        = 10;
				int page            = 1;

				result = app.nodeQuery(type).sort(sortKey).order(sortDesc).page(page).pageSize(pageSize).getResult();

				logger.info("Raw result size: {}, expected: {}", new Object[] { result.getRawResultCount(), number });
				assertTrue(result.getRawResultCount() == number);
				logger.info("Result size: {}, expected: {}", new Object[] { result.size(), pageSize });
				assertTrue(result.size() == Math.min(number, pageSize));

				for (int j = 0; j < Math.min(result.size(), pageSize); j++) {

					String expectedName = "TestOne-" + (30 - (j+1)*2);
					String gotName     = result.get(j).getProperty(AbstractNode.name);

					System.out.println(j + ": " +  expectedName + ", got: " + gotName);
					assertEquals(expectedName, gotName);

				}

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test07SortByIntWithNullValues() {

		try {

			final List<TestOne> nodes = this.createTestNodes(TestOne.class, 10);
			try (final Tx tx = app.tx()) {

				int i = 0;

				for (NodeInterface node : nodes) {

					node.setProperty(AbstractNode.name, Long.toString(i));
					if (i < 7) {
						node.setProperty(TestOne.anInt, i);
					}

					i++;
				}
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				boolean sortDesc    = false;

				final List<TestOne> result = app.nodeQuery(TestOne.class).sort(TestOne.anInt).order(sortDesc).getAsList();

				// check that the sorting is stable, i.e. the position of nodes
				// with equal values (and null) is not modified by sorting

				final Iterator<TestOne> nameIterator = result.iterator();
				while (nameIterator.hasNext()) {

					// nulls first
					assertEquals("Invalid sort result with mixed values (null vs. int)", "7", nameIterator.next().getProperty(TestOne.name));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "8", nameIterator.next().getProperty(TestOne.name));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "9", nameIterator.next().getProperty(TestOne.name));

					// other values after that
					assertEquals("Invalid sort result with mixed values (null vs. int)", "0", nameIterator.next().getProperty(TestOne.name));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "1", nameIterator.next().getProperty(TestOne.name));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "2", nameIterator.next().getProperty(TestOne.name));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "3", nameIterator.next().getProperty(TestOne.name));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "4", nameIterator.next().getProperty(TestOne.name));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "5", nameIterator.next().getProperty(TestOne.name));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "6", nameIterator.next().getProperty(TestOne.name));
				}


				// check that the sorting is "nulls first" as documented
				final Iterator<TestOne> intIterator = result.iterator();
				while (intIterator.hasNext()) {

					// nulls first
					assertEquals("Invalid sort result with mixed values (null vs. int)", null, intIterator.next().getProperty(TestOne.anInt));
					assertEquals("Invalid sort result with mixed values (null vs. int)", null, intIterator.next().getProperty(TestOne.anInt));
					assertEquals("Invalid sort result with mixed values (null vs. int)", null, intIterator.next().getProperty(TestOne.anInt));

					// other values after that
					assertEquals("Invalid sort result with mixed values (null vs. int)", 0L, (long)intIterator.next().getProperty(TestOne.anInt));
					assertEquals("Invalid sort result with mixed values (null vs. int)", 1L, (long)intIterator.next().getProperty(TestOne.anInt));
					assertEquals("Invalid sort result with mixed values (null vs. int)", 2L, (long)intIterator.next().getProperty(TestOne.anInt));
					assertEquals("Invalid sort result with mixed values (null vs. int)", 3L, (long)intIterator.next().getProperty(TestOne.anInt));
					assertEquals("Invalid sort result with mixed values (null vs. int)", 4L, (long)intIterator.next().getProperty(TestOne.anInt));
					assertEquals("Invalid sort result with mixed values (null vs. int)", 5L, (long)intIterator.next().getProperty(TestOne.anInt));
					assertEquals("Invalid sort result with mixed values (null vs. int)", 6L, (long)intIterator.next().getProperty(TestOne.anInt));
				}

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test01SearchSingleNodeByName() {

		try {

			PropertyMap props     = new PropertyMap();
			final PropertyKey key = AbstractNode.name;
			final String name     = "89w3hkl sdfghsdkljth";

			props.put(key, name);

			final AbstractNode node = createTestNode(TestOne.class, props);

			Result result = null;

			try (final Tx tx = app.tx()) {

				result = app.nodeQuery(TestOne.class).andName(name).includeDeletedAndHidden().getResult();

				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(node));
			}

			// Change name attribute and search again
			final String name2 = "klppptzoehi gösoiu tzüw0e9hg";

			try (final Tx tx = app.tx()) {

				node.setProperty(key, name2);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				result = app.nodeQuery(TestOne.class).andName(name2).includeDeletedAndHidden().getResult();

				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(node));
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test02SearchSingleNodeByDate() {

		try {

			PropertyMap props = new PropertyMap();
			PropertyKey key   = TestOne.aDate;
			Date date         = new Date();
			Class type        = TestOne.class;

			props.put(key, date);

			AbstractNode node = createTestNode(type, props);

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(type).and(key, date).includeDeletedAndHidden().getResult();

				assertEquals(1, result.size());
				assertTrue(result.get(0).equals(node));
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test03SearchRelationship() {

		try {

			final NodeHasLocation rel = createTestRelationships(NodeHasLocation.class, 1).get(0);
			final PropertyKey key1    = new StringProperty("jghsdkhgshdhgsdjkfgh").indexed();
			final Class type          = NodeHasLocation.class;
			final String val1         = "54354354546806849870";

			final Result<RelationshipInterface> result;

			try (final Tx tx = app.tx()) {

				rel.setProperty(key1, val1);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				assertTrue(rel.getProperty(key1).equals(val1));

				result = app.relationshipQuery(type).and(key1, val1).getResult();

				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(rel));
			}

			final String val2 = "ölllldjöoa8w4rasf";

			try (final Tx tx = app.tx()) {

				rel.setProperty(key1, val2);
				tx.success();
			}

			assertTrue(result.size() == 1);
			assertTrue(result.get(0).equals(rel));

		} catch (FrameworkException ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test04SearchByLocation() {

		try {

			final PropertyMap props = new PropertyMap();
			final PropertyKey lat   = TestSeven.latitude;
			final PropertyKey lon   = TestSeven.longitude;
			final Class type        = TestSeven.class;

			props.put(lat, 50.12284d);
			props.put(lon, 8.73923d);
			props.put(AbstractNode.name, "TestSeven-0");

			AbstractNode node = createTestNode(type, props);

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(type).location("Hanauer Landstraße", "200", "60314", "Frankfurt", "Germany", 10.0).includeDeletedAndHidden().getResult();

				assertEquals(1, result.size());
				assertTrue(result.get(0).equals(node));
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test05SpatialRollback() {

		try {

			final Class type        = TestSeven.class;
			final PropertyMap props = new PropertyMap();
			final PropertyKey lat   = TestSeven.latitude;
			final PropertyKey lon   = TestSeven.longitude;

			props.put(AbstractNode.type, type.getSimpleName());
			props.put(lat, 50.12284d);
			props.put(lon, 8.73923d);
			props.put(AbstractNode.name, "TestSeven-0");

			try (final Tx tx = app.tx()) {

				// this will work
				TestSeven node = app.create(TestSeven.class, props);

				props.remove(AbstractNode.name);
				props.put(lat, 50.12285d);
				props.put(lon, 8.73924d);

				// this will fail
				TestSeven node2 = app.create(TestSeven.class, props);

				// adding another
				TestSeven node3 = app.create(TestSeven.class, props);

				tx.success();
			}

			fail("Expected a FrameworkException (name must_not_be_empty)");

		} catch (FrameworkException nfe) {
			logger.warn("", nfe);

		}

	}

	@Test
	public void test06DistanceSearchOnEmptyDB() {

		try (final Tx tx = app.tx()) {

			Result result = app.nodeQuery(TestOne.class).location("Hanauer Landstraße", "200", "60314", "Frankfurt", "Germany", 10.0).includeDeletedAndHidden().getResult();

			assertEquals(0, result.size());

		} catch (FrameworkException ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test07SearchByStaticMethod01() {

		try {

			PropertyMap props     = new PropertyMap();
			final PropertyKey key = AbstractNode.name;
			final String name     = "89w3hkl sdfghsdkljth";

			props.put(key, name);

			final AbstractNode node = createTestNode(TestOne.class, props);

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(TestOne.class).andName(name).includeDeletedAndHidden().getResult();

				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(node));
			}


		} catch (FrameworkException ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}
	}

	@Test
	public void test08SearchByStaticMethod02() {

		try {

			PropertyMap props     = new PropertyMap();
			final PropertyKey key = AbstractNode.name;
			final String name     = "89w3hkl sdfghsdkljth";

			props.put(key, name);

			final AbstractNode node = createTestNode(TestOne.class, props);

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(TestOne.class).andName(name).includeDeletedAndHidden().getResult();

				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(node));
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}
	}

	@Test
	public void test08SearchByStaticMethodWithNullSearchValue01() {

		try {

			PropertyMap props     = new PropertyMap();
			final PropertyKey key = AbstractNode.name;
			final String name     = "abc";

			props.put(key, name);

			createTestNode(TestOne.class, props);

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(TestOne.class).andName(null).includeDeletedAndHidden().getResult();

				assertTrue(result.isEmpty());
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test09SearchByEmptyStringField() {

		try {

			PropertyMap props = new PropertyMap();
			AbstractNode node = createTestNode(TestOne.class, props);

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(TestOne.class).and(TestOne.aString, null).includeDeletedAndHidden().getResult();

				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(node));
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test10SearchByEmptyDateField() {

		try {

			PropertyMap props     = new PropertyMap();
			AbstractNode node = createTestNode(TestOne.class, props);

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(TestOne.class).and(TestOne.aDate, null).includeDeletedAndHidden().getResult();

				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(node));
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test11SearchByEmptyIntField() {

		try {

			PropertyMap props     = new PropertyMap();
			AbstractNode node = createTestNode(TestOne.class, props);

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(TestOne.class).and(TestOne.anInt, null).includeDeletedAndHidden().getResult();

				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(node));
			}

		} catch (Throwable ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test12SearchByEmptyLongField() {

		try {

			PropertyMap props     = new PropertyMap();
			AbstractNode node = createTestNode(TestOne.class, props);

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(TestOne.class).and(TestOne.aLong, null).includeDeletedAndHidden().getResult();

				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(node));
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test13SearchByEmptyDoubleField() {

		try {

			PropertyMap props = new PropertyMap();
			AbstractNode node = createTestNode(TestOne.class, props);

			try (final Tx tx = app.tx()) {

				Result result = app.nodeQuery(TestOne.class).and(TestOne.aDouble, null).includeDeletedAndHidden().getResult();
				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(node));

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
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

				logger.info("Raw result size: {}, expected: {}", new Object[] { result.getRawResultCount(), number });
				assertTrue(result.getRawResultCount() == number);
				logger.info("Result size: {}, expected: {}", new Object[] { result.size(), pageSize });
				assertTrue(result.size() == pageSize);
			}

              } catch (FrameworkException ex) {

		      logger.warn("", ex);

                      logger.error(ex.toString());
                      fail("Unexpected exception");

              }

      }

	/**
	 * Test different pages and page sizes
	 */
	@Test
	public void test02Paging() {

		try {

			boolean includeDeletedAndHidden = false;
			boolean publicOnly              = false;
			Class type                      = TestOne.class;
			int number                      = 89;    // no more than 89 to avoid sort order TestOne-10, TestOne-100 ...
			final int offset                = 10;
			final List<NodeInterface> nodes = this.createTestNodes(type, number);

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


				// test pages sizes from 0 to 10
				for (int ps=0; ps<10; ps++) {

					// test all pages
					for (int p=0; p<(number/Math.max(1,ps))+1; p++) {

						testPaging(type, ps, p, number, offset, includeDeletedAndHidden, publicOnly, sortKey, sortDesc);

					}
				}
			}

		} catch (FrameworkException ex) {

			logger.warn("", ex);

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test01ParallelInstantiationPerformance() {

		try {

			for (int i=0; i<10; i++) {

				System.out.println("Creating nodes..");
				createTestNodes(TestOne.class, 100);
			}

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			System.out.println("Loading nodes..");
			final List<TestOne> testOnes = app.nodeQuery(TestOne.class).getAsList();


			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

	}

	@Test
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

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
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

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
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

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
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
					logger.info("Exception logged", fex);
				}
			}


		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test06PagingVisibility() {

		Principal tester1 = null;
		Principal tester2 = null;

		try (final Tx tx = app.tx()) {

			// create non-admin user
			tester1 = app.create(TestUser.class, "tester1");
			tester2 = app.create(TestUser.class, "tester2");

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
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

			logger.error(ex.toString());
			fail("Unexpected exception");

		}
	}

	@Test
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

	@Test
	public void test08PagingWithHiddenOrDeletedElements() {

		try {

			// create 10 nodes
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

			final Result<TestOne> result = app.nodeQuery(TestOne.class).includeDeletedAndHidden(false).getResult();

			assertEquals("Result count should not include deleted or hidden nodes", 7, (int)result.getRawResultCount());
			assertEquals("Actual result size should be equal to result count", 7, (int)result.getResults().size());


			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}


	}

	// ----- private methods -----
	private void testPaging(final Class type, final int pageSize, final int page, final int number, final int offset, final boolean includeDeletedAndHidden, final boolean publicOnly, final PropertyKey sortKey, final boolean sortDesc) throws FrameworkException {

		final Query query = app.nodeQuery(type).sort(sortKey).order(sortDesc).page(page).pageSize(pageSize);

		if (includeDeletedAndHidden) {
			query.includeDeletedAndHidden();
		}

		final Result result = query.getResult();

		logger.info("===================================================\nRaw result size: {}, expected: {} (page size: {}, page: {})", new Object[] { result.getRawResultCount(), number, pageSize, page });
		assertTrue(result.getRawResultCount() == ((pageSize == 0 || page == 0) ? 0 : number));

		long expectedResultCount = (pageSize == 0 || page == 0)
					   ? 0
					   : Math.min(number, pageSize);

		int startIndex = (Math.max(page, 1) - 1) * pageSize;

		logger.info("Result size: {}, expected: {}, start index: {}", new Object[] { result.size(), expectedResultCount, startIndex });
		assertTrue(result.size() == expectedResultCount);


		for (int j = 0; j < expectedResultCount; j++) {

			String expectedName = "TestOne-" + (offset + j + startIndex);
			String gotName      = result.get(j).getProperty(AbstractNode.name);

			System.out.println(expectedName + ", got: " + gotName);
			assertEquals(expectedName, gotName);

		}
	}
}
