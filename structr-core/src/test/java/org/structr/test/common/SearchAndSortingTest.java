/**
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.test.common;

import java.util.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseFeature;
import org.structr.api.config.Settings;
import org.structr.api.search.ComparisonQuery;
import org.structr.api.search.Occurrence;
import org.structr.api.util.ResultStream;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.GenericRelationship;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.ComparisonSearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchAttributeGroup;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.schema.action.ActionContext;
import org.structr.test.core.entity.SixOneManyToMany;
import org.structr.test.core.entity.TestOne;
import org.structr.test.core.entity.TestSeven;
import org.structr.test.core.entity.TestSix;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;
import org.testng.annotations.Test;

/**
 */
public class SearchAndSortingTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(SearchAndSortingTest.class.getName());

	@Test
	public void test01SearchByName() {

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

					name = "TestOne-" + i;

					i++;

					node.setProperty(AbstractNode.name, name);

				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<TestOne> result = app.nodeQuery(type).getAsList();

				assertEquals(4, result.size());

				for (NodeInterface node : result) {
					System.out.println(node);
				}

				result = app.nodeQuery(type).andName("TestOne-12").getAsList();

				assertEquals(1, result.size());

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test02SearchByNameProperty() {

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

				List<GraphObject> result = app.nodeQuery(type).and(TestOne.name, "TestOne-13").getAsList();

				assertEquals(1, result.size());

				tx.success();

			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test03SearchByNamePropertyLooseLowercase() {

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

				List<GraphObject> result = app.nodeQuery(type).and(TestOne.name, "testone", false).getAsList();

				assertEquals(4, result.size());

				tx.success();

			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test04SearchByNamePropertyLooseUppercase() {

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

				List<GraphObject> result = app.nodeQuery(type).and(TestOne.name, "TestOne", false).getAsList();

				assertEquals(4, result.size());

				tx.success();

			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test05SearchByDefaultValue() {

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

				List<GraphObject> result = app.nodeQuery(type).and(TestOne.stringWithDefault, "default value", false).getAsList();

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
		testResults.put("ABCDE*2345",  1);
		testResults.put("ABCDE?2345",  1);
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
			int countBefore                 = 0;
			int numberToCreate              = 1000;

			try (final Tx tx = app.tx()) {

				long t0 = System.currentTimeMillis();

				List<? extends GraphObject> result = app.nodeQuery(NodeInterface.class).getAsList();

				countBefore = result.size();

				tx.success();

			} catch (FrameworkException ex) {
				logger.error(ex.toString());
				fail("Unexpected exception");
			}

			final int expectedNumber        = countBefore + numberToCreate;
			final List<NodeInterface> nodes = this.createTestNodes(type, numberToCreate);

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			try (final Tx tx = app.tx()) {

				long t0 = System.currentTimeMillis();

				List<? extends GraphObject> result = app.nodeQuery(NodeInterface.class).getAsList();

				long t1 = System.currentTimeMillis();
				logger.info("Query with inexact type took {} ms", t1-t0);
				assertEquals(expectedNumber, result.size());

				result = app.nodeQuery(NodeInterface.class).getAsList();

				long t2 = System.currentTimeMillis();
				logger.info("Query with exact type took {} ms", t2-t1);
				assertEquals(expectedNumber, result.size());

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

				List<GraphObject> result = app.nodeQuery(type).getAsList();

				assertEquals(number, result.size());

				PropertyKey sortKey = AbstractNode.name;
				boolean sortDesc    = false;
				int pageSize        = 10;
				int page            = 1;

				result = app.nodeQuery(type).sort(sortKey, sortDesc).page(page).pageSize(pageSize).getAsList();

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

				List<GraphObject> result = app.nodeQuery(type).getAsList();

				assertEquals(number, result.size());

				PropertyKey sortKey = AbstractNode.name;
				boolean sortDesc    = true;
				int pageSize        = 10;
				int page            = 1;

				result = app.nodeQuery(type).sort(sortKey, sortDesc).page(page).pageSize(pageSize).getAsList();

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

				List<GraphObject> result = app.nodeQuery(type).getAsList();

				assertEquals(number, result.size());

				PropertyKey sortKey = TestOne.aDate;
				boolean sortDesc    = false;
				int pageSize        = 10;
				int page            = 1;

				result = app.nodeQuery(type).sort(sortKey, sortDesc).page(page).pageSize(pageSize).getAsList();

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

				List<GraphObject> result = app.nodeQuery(type).getAsList();

				assertEquals(number, result.size());

				PropertyKey sortKey = AbstractNode.lastModifiedDate;
				boolean sortDesc    = true;
				int pageSize        = 10;
				int page            = 1;

				result = app.nodeQuery(type).sort(sortKey, sortDesc).page(page).pageSize(pageSize).getAsList();

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

				List<GraphObject> result = app.nodeQuery(type).getAsList();

				assertEquals(number, result.size());

				PropertyKey sortKey = key;
				boolean sortDesc    = false;
				int pageSize        = 5;
				int page            = 1;

				result = app.nodeQuery(type).sort(sortKey, sortDesc).page(page).pageSize(pageSize).getAsList();

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

				List<GraphObject> result = app.nodeQuery(type).getAsList();

				assertEquals(number, result.size());

				PropertyKey sortKey = TestOne.aDate;
				boolean sortDesc    = true;
				int pageSize        = 10;
				int page            = 2;

				Settings.CypherDebugLogging.setValue(true);
				result = app.nodeQuery(type).sort(sortKey, sortDesc).page(page).pageSize(pageSize).getAsList();
				Settings.CypherDebugLogging.setValue(false);

				logger.info("Result size: {}, expected: {}", new Object[] { result.size(), pageSize });
				assertTrue(result.size() == Math.min(number, pageSize));

				result.stream().forEach(r -> System.out.println(r.getProperty(AbstractNode.name)));

				for (int j = 0; j < Math.min(result.size(), pageSize); j++) {

					String expectedName = "TestOne-" + (30 - (j+1)*2);
					String gotName     = result.get(j).getProperty(AbstractNode.name);

					System.out.println(j + ": " +  expectedName + ", got: " + gotName);
					assertEquals(expectedName, gotName);

				}

				// allow visual inspection of test results
				final List<AbstractNode> list = app.nodeQuery(type).sort(sortKey, sortDesc).getAsList();
				list.stream().forEach(n -> System.out.println(n.getName() + ": " + n.getProperty(TestOne.aDate)));

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

				final List<TestOne> result = app.nodeQuery(TestOne.class).sort(TestOne.anInt, sortDesc).sort(AbstractNode.name).getAsList();

				// check that the sorting is stable, i.e. the position of nodes
				// with equal values (and null) is not modified by sorting

				final Iterator<TestOne> nameIterator = result.iterator();
				while (nameIterator.hasNext()) {

					// values first
					assertEquals("Invalid sort result with mixed values (null vs. int)", "0", nameIterator.next().getProperty(TestOne.name));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "1", nameIterator.next().getProperty(TestOne.name));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "2", nameIterator.next().getProperty(TestOne.name));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "3", nameIterator.next().getProperty(TestOne.name));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "4", nameIterator.next().getProperty(TestOne.name));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "5", nameIterator.next().getProperty(TestOne.name));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "6", nameIterator.next().getProperty(TestOne.name));

					// nulls after that
					assertEquals("Invalid sort result with mixed values (null vs. int)", "7", nameIterator.next().getProperty(TestOne.name));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "8", nameIterator.next().getProperty(TestOne.name));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "9", nameIterator.next().getProperty(TestOne.name));
				}


				// check that the sorting is "nulls last" as documented
				final Iterator<TestOne> intIterator = result.iterator();
				while (intIterator.hasNext()) {

					// values first
					assertEquals("Invalid sort result with mixed values (null vs. int)", 0L, (long)intIterator.next().getProperty(TestOne.anInt));
					assertEquals("Invalid sort result with mixed values (null vs. int)", 1L, (long)intIterator.next().getProperty(TestOne.anInt));
					assertEquals("Invalid sort result with mixed values (null vs. int)", 2L, (long)intIterator.next().getProperty(TestOne.anInt));
					assertEquals("Invalid sort result with mixed values (null vs. int)", 3L, (long)intIterator.next().getProperty(TestOne.anInt));
					assertEquals("Invalid sort result with mixed values (null vs. int)", 4L, (long)intIterator.next().getProperty(TestOne.anInt));
					assertEquals("Invalid sort result with mixed values (null vs. int)", 5L, (long)intIterator.next().getProperty(TestOne.anInt));
					assertEquals("Invalid sort result with mixed values (null vs. int)", 6L, (long)intIterator.next().getProperty(TestOne.anInt));

					// nulls after that
					assertEquals("Invalid sort result with mixed values (null vs. int)", null, intIterator.next().getProperty(TestOne.anInt));
					assertEquals("Invalid sort result with mixed values (null vs. int)", null, intIterator.next().getProperty(TestOne.anInt));
					assertEquals("Invalid sort result with mixed values (null vs. int)", null, intIterator.next().getProperty(TestOne.anInt));
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

			List<TestOne> result = null;

			try (final Tx tx = app.tx()) {

				result = app.nodeQuery(TestOne.class).andName(name).includeHidden().getAsList();

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

				result = app.nodeQuery(TestOne.class).andName(name2).includeHidden().getAsList();

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

			NodeInterface node = createTestNode(type, props);

			try (final Tx tx = app.tx()) {

				List<GraphObject> result = app.nodeQuery(type).and(key, date).includeHidden().getAsList();

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

			final GenericRelationship rel = createTestRelationships(GenericRelationship.class, 1).get(0);
			final PropertyKey key1        = new StringProperty("jghsdkhgshdhgsdjkfgh").indexed();
			final Class type              = GenericRelationship.class;
			final String val1             = "54354354546806849870";

			final List<RelationshipInterface> result;

			try (final Tx tx = app.tx()) {

				rel.setProperty(key1, val1);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				assertTrue(rel.getProperty(key1).equals(val1));

				result = app.relationshipQuery(type).and(key1, val1).getAsList();

				assertEquals(1, result.size());
				assertEquals(rel, result.get(0));
			}

			final String val2 = "ölllldjöoa8w4rasf";

			try (final Tx tx = app.tx()) {

				rel.setProperty(key1, val2);
				tx.success();
			}

			assertEquals(1, result.size());
			assertEquals(rel, result.get(0));

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

			NodeInterface node = createTestNode(type, props);

			try (final Tx tx = app.tx()) {

				List<GraphObject> result = app.nodeQuery(type).location("Hanauer Landstraße", "200", "60314", "Frankfurt", "Germany", 10.0).includeHidden().getAsList();

				assertEquals(1, result.size());
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

			List<TestOne> result = app.nodeQuery(TestOne.class).location("Hanauer Landstraße", "200", "60314", "Frankfurt", "Germany", 10.0).includeHidden().getAsList();

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

				List<TestOne> result = app.nodeQuery(TestOne.class).andName(name).includeHidden().getAsList();

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

				List<TestOne> result = app.nodeQuery(TestOne.class).andName(name).includeHidden().getAsList();

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

				List<TestOne> result = app.nodeQuery(TestOne.class).andName(null).includeHidden().getAsList();

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

				List<TestOne> result = app.nodeQuery(TestOne.class).and(TestOne.aString, null).includeHidden().getAsList();

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

				List<TestOne> result = app.nodeQuery(TestOne.class).and(TestOne.aDate, null).includeHidden().getAsList();

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

				List<TestOne> result = app.nodeQuery(TestOne.class).and(TestOne.anInt, null).includeHidden().getAsList();

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

				List<TestOne> result = app.nodeQuery(TestOne.class).and(TestOne.aLong, null).includeHidden().getAsList();

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

				List<TestOne> result = app.nodeQuery(TestOne.class).and(TestOne.aDouble, null).includeHidden().getAsList();
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

				List<GraphObject> result = app.nodeQuery(type).getAsList();

				assertTrue(result.size() == number);

				PropertyKey sortKey = AbstractNode.name;
				boolean sortDesc    = false;
				int pageSize        = 10;
				int page            = 1;

				result = app.nodeQuery(type).includeHidden().sort(sortKey, sortDesc).page(page).pageSize(pageSize).getAsList();

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

			boolean includeHidden           = false;
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

				List<GraphObject> result = app.nodeQuery(type).getAsList();

				assertEquals(number, result.size());

				PropertyKey sortKey = AbstractNode.name;
				boolean sortDesc    = false;


				// test pages sizes from 0 to 10
				for (int ps=0; ps<10; ps++) {

					// test all pages
					for (int p=0; p<(number/Math.max(1,ps))+1; p++) {

						testPaging(type, ps, p, number, offset, includeHidden, sortKey, sortDesc);

					}
				}
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();

			logger.error(ex.toString());
			fail("Unexpected exception");

		}

	}

	@Test
	public void test02PagingAndCreate() {

		try {

			boolean includeHidden           = false;
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

				final List<NodeInterface> result = app.nodeQuery(type).getAsList();

				assertTrue(result.size() == number);

				PropertyKey sortKey = AbstractNode.name;
				boolean sortDesc    = false;
				int pageSize        = 2;
				int page            = 1;

				testPaging(type, pageSize, page, number, offset, includeHidden, sortKey, sortDesc);

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

				testPaging(type, pageSize, page + 1, number + 1, offset - 1, includeHidden, sortKey, sortDesc);
				System.out.println("paging test finished");

				tx.success();
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
			tester1 = app.create(Principal.class, "tester1");
			tester2 = app.create(Principal.class, "tester2");

			tx.success();

		} catch (FrameworkException fex) {

			logger.warn("", fex);
			fail("Unexpected exception");
		}

		try {

			final SecurityContext tester1Context     = SecurityContext.getInstance(tester1, AccessMode.Backend);
			final SecurityContext tester2Context     = SecurityContext.getInstance(tester2, AccessMode.Backend);
			final int softLimit                      = Settings.ResultCountSoftLimit.getValue();
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

				final PropertyKey sortKey        = AbstractNode.name;
				final boolean sortDesc           = false;
				final int pageSize               = 10;
				final int page                   = 22;
				final ResultStream<GraphObject> result = tester1App.nodeQuery(type).sort(sortKey, sortDesc).pageSize(pageSize).page(page).getResultStream();

				assertEquals("Invalid paging result count with non-superuser security context", tester1NodeCount, result.calculateTotalResultCount(null, softLimit));

				result.close();

				tx.success();

			} catch (Exception ex) {
				fail("Unexpected exception");
			}

			try (final Tx tx = app.tx()) {

				final PropertyKey sortKey        = AbstractNode.name;
				final boolean sortDesc           = false;
				final int pageSize               = 10;
				final int page                   = 22;
				final ResultStream<GraphObject> result = tester2App.nodeQuery(type).sort(sortKey, sortDesc).pageSize(pageSize).page(page).getResultStream();

				assertEquals("Invalid paging result count with non-superuser security context", tester2NodeCount, result.calculateTotalResultCount(null, softLimit));

				result.close();

				tx.success();

			} catch (Exception ex) {
				fail("Unexpected exception");
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

			test3.setProperty(AbstractNode.hidden, true);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final List<TestOne> result = app.nodeQuery(TestOne.class).includeHidden(false).getAsList();

			assertEquals("Actual result size should be equal to result count", 8, (int)result.size());


			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testManyToManyReverseNodeSearch() {

		final Class<Group> groupType                     = StructrApp.getConfiguration().getNodeEntityClass("Group");
		final PropertyKey<Iterable<Principal>> groupsKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(groupType, "groups");
		final List<Group> groups                         = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			for (int i=0; i<5; i++) {

				groups.add(createTestNode(groupType, "Group" + i));
			}

			final Group group1 = groups.get(0);
			final Group group2 = groups.get(1);
			final Group group3 = groups.get(2);
			final Group group4 = groups.get(3);
			final Group group5 = groups.get(4);

			group1.addMember(securityContext, group2);
			group2.addMember(securityContext, group3);
			group2.addMember(securityContext, group4);
			group3.addMember(securityContext, group5);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			// search for a group with empty list of parents
			final List<Group> result1 = app.nodeQuery(Group.class).and(groupsKey, new LinkedList<>()).getAsList();
			assertEquals("Invalid search result", 1, result1.size());

			// search for a group with group2 as a parent
			final List<Group> result2 = app.nodeQuery(Group.class).and(groupsKey, Arrays.asList(groups.get(1))).getAsList();
			assertEquals("Invalid search result", 2, result2.size());

			// search for a group with group2 as a parent and a given name
			final List<Group> result3 = app.nodeQuery(Group.class).andName("Group3").and(groupsKey, Arrays.asList(groups.get(1))).getAsList();
			assertEquals("Invalid search result", 1, result3.size());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testSortFunctionForGraphObjectMaps() {

		// don't run tests that depend on Cypher being available in the backend
		if (Services.getInstance().getDatabaseService().supportsFeature(DatabaseFeature.QueryLanguage, "application/x-cypher-query")) {

			final Class<Group> groupType      = StructrApp.getConfiguration().getNodeEntityClass("Group");
			final PropertyKey<String> nameKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(groupType, "name");

			try (final Tx tx = app.tx()) {

				createTestNode(groupType, "zzz");
				createTestNode(groupType, "aaa");
				createTestNode(groupType, "ttt");
				createTestNode(groupType, "xxx");
				createTestNode(groupType, "bbb");

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			try (final Tx tx = app.tx()) {

				final List<GraphObject> list = (List<GraphObject>)Scripting.evaluate(new ActionContext(securityContext), null, "${sort(cypher('MATCH (n:Group:" + randomTenantId + ") RETURN { id: n.id, type: n.type, name: n.name }'), 'name')}", "test");

				assertEquals("Invalid sort() result", "aaa", list.get(0).getProperty(nameKey));
				assertEquals("Invalid sort() result", "bbb", list.get(1).getProperty(nameKey));
				assertEquals("Invalid sort() result", "ttt", list.get(2).getProperty(nameKey));
				assertEquals("Invalid sort() result", "xxx", list.get(3).getProperty(nameKey));
				assertEquals("Invalid sort() result", "zzz", list.get(4).getProperty(nameKey));

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				System.out.println(fex.getMessage());
				fail("Unexpected exception.");
			}
		}
	}

	@Test
	public void testNestedSearchGroups() {

		final Class<Group> groupType      = StructrApp.getConfiguration().getNodeEntityClass("Group");
		final PropertyKey<String> nameKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(groupType, "name");

		try (final Tx tx = app.tx()) {

			createTestNode(groupType, "ttt");
			createTestNode(groupType, "aaa");
			createTestNode(groupType, "bbb");
			createTestNode(groupType, "xxx");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			List<Group> list = app.nodeQuery(Group.class)
					.or()
						.andName("aaa")
						.not().andName("bbb").parent()
					.or()
						.andName("ttt").parent()
					.or()
						.andName("xxx").parent()
					.or()
						.andName("bbb").parent()
					.and()
						.and().parent()
						.and().parent()
						.and().parent()
					.sort(AbstractNode.name, false)
					.getAsList();

			assertEquals("Invalid sort() result", "aaa", list.get(0).getProperty(nameKey));
			assertEquals("Invalid sort() result", "bbb", list.get(1).getProperty(nameKey));
			assertEquals("Invalid sort() result", "ttt", list.get(2).getProperty(nameKey));
			assertEquals("Invalid sort() result", "xxx", list.get(3).getProperty(nameKey));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			System.out.println(fex.getMessage());
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testComparisonSearchAttributes() {

		final Class<Group> groupType          = StructrApp.getConfiguration().getNodeEntityClass("Group");
		final PropertyKey<String> nameKey     = StructrApp.getConfiguration().getPropertyKeyForJSONName(groupType, "name");
		final PropertyKey<Principal> ownerKey = StructrApp.getConfiguration().getPropertyKeyForJSONName(groupType, "owner");

		try (final Tx tx = app.tx()) {

			createTestNode(groupType, "a");
			createTestNode(groupType, "b");
			createTestNode(groupType, "c");
			createTestNode(groupType, "d");
			createTestNode(groupType, "e");

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final List<SearchAttribute> attributes       = new ArrayList<>();
			final SearchAttributeGroup rootGroup         = new SearchAttributeGroup(Occurrence.REQUIRED);
			final SearchAttributeGroup mainMatchingGroup = new SearchAttributeGroup(Occurrence.REQUIRED);

			mainMatchingGroup.add(new ComparisonSearchAttribute(nameKey, ComparisonQuery.Operation.equal, "a", Occurrence.OPTIONAL));
			mainMatchingGroup.add(new ComparisonSearchAttribute(nameKey, ComparisonQuery.Operation.equal, "b", Occurrence.OPTIONAL));
			mainMatchingGroup.add(new ComparisonSearchAttribute(nameKey, ComparisonQuery.Operation.equal, "c", Occurrence.OPTIONAL));
			mainMatchingGroup.add(new ComparisonSearchAttribute(nameKey, ComparisonQuery.Operation.equal, "d", Occurrence.OPTIONAL));

			final SearchAttributeGroup secondaryMatchingGroup = new SearchAttributeGroup(Occurrence.REQUIRED);

			secondaryMatchingGroup.add(new ComparisonSearchAttribute(ownerKey, ComparisonQuery.Operation.isNull, null, Occurrence.REQUIRED));
			secondaryMatchingGroup.add(new ComparisonSearchAttribute(ownerKey, ComparisonQuery.Operation.isNotNull, null, Occurrence.FORBIDDEN));

			// Test Greater/Less with ASCII chars
			secondaryMatchingGroup.add(new ComparisonSearchAttribute(nameKey, ComparisonQuery.Operation.greater, "_", Occurrence.REQUIRED));
			secondaryMatchingGroup.add(new ComparisonSearchAttribute(nameKey, ComparisonQuery.Operation.greaterOrEqual, "a", Occurrence.REQUIRED));
			secondaryMatchingGroup.add(new ComparisonSearchAttribute(nameKey, ComparisonQuery.Operation.lessOrEqual, "d", Occurrence.REQUIRED));
			secondaryMatchingGroup.add(new ComparisonSearchAttribute(nameKey, ComparisonQuery.Operation.less, "e", Occurrence.REQUIRED));
			secondaryMatchingGroup.add(new ComparisonSearchAttribute(nameKey, ComparisonQuery.Operation.notEqual, "b", Occurrence.REQUIRED));

			rootGroup.add(mainMatchingGroup);
			rootGroup.add(secondaryMatchingGroup);
			attributes.add(rootGroup);

			final List<Group> list = app.nodeQuery(Group.class)
					.attributes(attributes)
					.sort(AbstractNode.name, false)
					.getAsList();

			assertEquals("Invalid sort() result", "a", list.get(0).getProperty(nameKey));
			assertEquals("Invalid sort() result", "c", list.get(1).getProperty(nameKey));
			assertEquals("Invalid sort() result", "d", list.get(2).getProperty(nameKey));
			assertEquals("Too many query results", 3, list.size());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			System.out.println(fex.getMessage());
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testSortByMultipleKeys() {

		try (final Tx tx = app.tx()) {

			app.create(TestOne.class, new NodeAttribute<>(AbstractNode.name, "name7"), new NodeAttribute<>(TestOne.anInt, 3), new NodeAttribute<>(TestOne.aLong, 20L));
			app.create(TestOne.class, new NodeAttribute<>(AbstractNode.name, "name5"), new NodeAttribute<>(TestOne.anInt, 2), new NodeAttribute<>(TestOne.aLong, 20L));
			app.create(TestOne.class, new NodeAttribute<>(AbstractNode.name, "name2"), new NodeAttribute<>(TestOne.anInt, 1), new NodeAttribute<>(TestOne.aLong, 20L));
			app.create(TestOne.class, new NodeAttribute<>(AbstractNode.name, "name1"), new NodeAttribute<>(TestOne.anInt, 3), new NodeAttribute<>(TestOne.aLong, 20L));
			app.create(TestOne.class, new NodeAttribute<>(AbstractNode.name, "name3"), new NodeAttribute<>(TestOne.anInt, 2), new NodeAttribute<>(TestOne.aLong, 20L));
			app.create(TestOne.class, new NodeAttribute<>(AbstractNode.name, "name4"), new NodeAttribute<>(TestOne.anInt, 1), new NodeAttribute<>(TestOne.aLong, 10L));
			app.create(TestOne.class, new NodeAttribute<>(AbstractNode.name, "name9"), new NodeAttribute<>(TestOne.anInt, 3), new NodeAttribute<>(TestOne.aLong, 10L));
			app.create(TestOne.class, new NodeAttribute<>(AbstractNode.name, "name8"), new NodeAttribute<>(TestOne.anInt, 2), new NodeAttribute<>(TestOne.aLong, 10L));
			app.create(TestOne.class, new NodeAttribute<>(AbstractNode.name, "name6"), new NodeAttribute<>(TestOne.anInt, 1), new NodeAttribute<>(TestOne.aLong, 10L));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final List<TestOne> result1 = app.nodeQuery(TestOne.class).sort(TestOne.aLong).sort(TestOne.name).getAsList();

			assertEquals("Sorting by multiple keys returns wrong result", "name4", result1.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result1.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result1.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name9", result1.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name1", result1.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result1.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result1.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result1.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result1.get(8).getName());

			final List<TestOne> result2 = app.nodeQuery(TestOne.class).sort(TestOne.aLong, true).sort(TestOne.name).getAsList();

			assertEquals("Sorting by multiple keys returns wrong result", "name1", result2.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result2.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result2.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result2.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result2.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name4", result2.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result2.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result2.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name9", result2.get(8).getName());

			final List<TestOne> result3 = app.nodeQuery(TestOne.class).sort(TestOne.aLong).sort(TestOne.name, true).getAsList();

			assertEquals("Sorting by multiple keys returns wrong result", "name9", result3.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result3.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result3.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name4", result3.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result3.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result3.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result3.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result3.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name1", result3.get(8).getName());

			final List<TestOne> result4 = app.nodeQuery(TestOne.class).sort(TestOne.aLong).sort(TestOne.anInt).sort(TestOne.name).getAsList();

			assertEquals("Sorting by multiple keys returns wrong result", "name4", result4.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result4.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result4.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name9", result4.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result4.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result4.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result4.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name1", result4.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result4.get(8).getName());

			final List<TestOne> result5 = app.nodeQuery(TestOne.class).sort(TestOne.aLong).sort(TestOne.anInt, true).sort(TestOne.name).getAsList();

			assertEquals("Sorting by multiple keys returns wrong result", "name9", result5.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result5.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name4", result5.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result5.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name1", result5.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result5.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result5.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result5.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result5.get(8).getName());

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			System.out.println(fex.getMessage());
			fail("Unexpected exception.");
		}
	}

	// ----- private methods -----
	private void testPaging(final Class type, final int pageSize, final int page, final int number, final int offset, final boolean includeHidden, final PropertyKey sortKey, final boolean sortDesc) throws FrameworkException {

		final Query query = app.nodeQuery(type).sort(sortKey, sortDesc).page(page).pageSize(pageSize);

		if (includeHidden) {
			query.includeHidden();
		}

		final List<GraphObject> result = query.getAsList();

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
