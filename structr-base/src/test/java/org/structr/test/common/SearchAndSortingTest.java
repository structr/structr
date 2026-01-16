/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseFeature;
import org.structr.api.config.Settings;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.search.ComparisonQuery;
import org.structr.api.search.Operation;
import org.structr.api.util.ResultStream;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.QueryGroup;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
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
import org.structr.core.script.ScriptTestHelper;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.RelationshipInterfaceTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.test.core.traits.definitions.TestSevenTraitDefinition;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.testng.AssertJUnit.*;

/**
 */
public class SearchAndSortingTest extends StructrTest {

	private static final Logger logger = LoggerFactory.getLogger(SearchAndSortingTest.class.getName());

	@Test
	public void test01SearchByName() {

		try  {

			String type                     = "TestOne";
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

					node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery(type).getAsList();

				assertEquals(4, result.size());

				for (NodeInterface node : result) {
					System.out.println(node);
				}

				result = app.nodeQuery(type).name("TestOne-12").getAsList();

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

			String type                     = "TestOne";
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

					node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery(type).key(Traits.of("TestOne").key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "TestOne-13").getAsList();

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

			String type                     = "TestOne";
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

					node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery(type).key(Traits.of("TestOne").key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "testone", false).getAsList();

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

			String type                     = "TestOne";
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

					node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery(type).key(Traits.of("TestOne").key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "TestOne", false).getAsList();

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

			String type                     = "TestOne";
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

					node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery(type).key(Traits.of("TestOne").key("stringWithDefault"), "default value", false).getAsList();

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
					app.create("TestOne", source);
				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				for (final String source : testResults.keySet()) {

					final String query         = source.substring(0, 6);
					final int count            = testResults.get(source);
					final List<NodeInterface> result = app.nodeQuery("TestOne").key(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), query, false).getAsList();

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

			String type                     = "TestOne";
			int countBefore                 = 0;
			int numberToCreate              = 1000;

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery(StructrTraits.NODE_INTERFACE).getAsList();

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

				List<? extends NodeInterface> result = app.nodeQuery(StructrTraits.NODE_INTERFACE).getAsList();

				long t1 = System.currentTimeMillis();
				logger.info("Query with inexact type took {} ms", t1-t0);
				assertEquals(expectedNumber, result.size());

				result = app.nodeQuery(StructrTraits.NODE_INTERFACE).getAsList();

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
			final NodeInterface test = createTestNode("TestOne",
				new NodeAttribute(Traits.of("TestOne").key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "TestOne"),
				new NodeAttribute(Traits.of("TestOne").key("aBoolean"), true),
				new NodeAttribute(Traits.of("TestOne").key("aDate"), date),
				new NodeAttribute(Traits.of("TestOne").key("aDouble"), 1.234),
				new NodeAttribute(Traits.of("TestOne").key("aLong"), 12345L),
				new NodeAttribute(Traits.of("TestOne").key("anEnum"), "One"),
				new NodeAttribute(Traits.of("TestOne").key("anInt"), 123)
			);

			try (final Tx tx = app.tx()) {

				assertEquals("Invalid inexact search result for type String",  test, app.nodeQuery("TestOne").key(Traits.of("TestOne").key(NodeInterfaceTraitDefinition.NAME_PROPERTY),    "TestOne", false).getFirst());
				assertEquals("Invalid inexact search result for type Boolean", test, app.nodeQuery("TestOne").key(Traits.of("TestOne").key("aBoolean"), true,     false).getFirst());
				assertEquals("Invalid inexact search result for type Date",    test, app.nodeQuery("TestOne").key(Traits.of("TestOne").key("aDate"),    date,     false).getFirst());
				assertEquals("Invalid inexact search result for type Double",  test, app.nodeQuery("TestOne").key(Traits.of("TestOne").key("aDouble"),  1.234,    false).getFirst());
				assertEquals("Invalid inexact search result for type Long",    test, app.nodeQuery("TestOne").key(Traits.of("TestOne").key("aLong"),    12345L,   false).getFirst());
				assertEquals("Invalid inexact search result for type String",  test, app.nodeQuery("TestOne").key(Traits.of("TestOne").key("anEnum"),   "One", false).getFirst());
				assertEquals("Invalid inexact search result for type Enum",    test, app.nodeQuery("TestOne").key(Traits.of("TestOne").key("anInt"),    123,      false).getFirst());

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

			final NodeInterface test1       = createTestNode("TestOne");
			final List<NodeInterface> tests = createTestNodes("TestSix", 5);
			final Traits traits             = Traits.of("SixOneManyToMany");

			try (final Tx tx = app.tx()) {

				test1.setProperty(Traits.of("TestOne").key("manyToManyTestSixs"), tests);

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				final List<RelationshipInterface> result1 = app.relationshipQuery("SixOneManyToMany").key(traits.key(RelationshipInterfaceTraitDefinition.SOURCE_ID_PROPERTY), tests.get(0).getUuid()).getAsList();

				assertEquals("Invalid sourceId query result", 1, result1.size());

				tx.success();

			}


			try (final Tx tx = app.tx()) {

				final List<RelationshipInterface> result1 = app.relationshipQuery("SixOneManyToMany").key(traits.key(RelationshipInterfaceTraitDefinition.TARGET_ID_PROPERTY), test1.getUuid()).getAsList();

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

			String type                     = "TestOne";
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

					node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery(type).getAsList();

				assertEquals(number, result.size());

				PropertyKey sortKey = Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
				boolean sortDesc    = false;
				int pageSize        = 10;
				int page            = 1;

				result = app.nodeQuery(type).sort(sortKey, sortDesc).page(page).pageSize(pageSize).getAsList();

				logger.info("Result size: {}, expected: {}", new Object[] { result.size(), Math.min(number, pageSize) });
				assertEquals(Math.min(number, pageSize), result.size());

				for (int j = 0; j < Math.min(result.size(), pageSize); j++) {

					String expectedName = "TestOne-" + (offset + j);
					String gotName     = result.get(j).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY));

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

			String type                     = "TestOne";
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

					node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery(type).getAsList();

				assertEquals(number, result.size());

				PropertyKey sortKey = Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
				boolean sortDesc    = true;
				int pageSize        = 10;
				int page            = 1;

				result = app.nodeQuery(type).sort(sortKey, sortDesc).page(page).pageSize(pageSize).getAsList();

				logger.info("Result size: {}, expected: {}", new Object[] { result.size(), Math.min(number, pageSize) });
				assertTrue(result.size() == Math.min(number, pageSize));

				for (int j = 0; j < Math.min(result.size(), pageSize); j++) {

					int expectedNumber = number + offset - 1 - j;
					String gotName     = result.get(j).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY));

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

			String type                     = "TestOne";
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

					node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "TestOne-" + name);

					node.setProperty(Traits.of("TestOne").key("aDate"), new Date());

					// slow down execution speed to make sure distinct changes fall in different milliseconds
					try { Thread.sleep(2); } catch (Throwable t) {}

				}
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery(type).getAsList();

				assertEquals(number, result.size());

				PropertyKey sortKey = Traits.of("TestOne").key("aDate");
				boolean sortDesc    = false;
				int pageSize        = 10;
				int page            = 1;

				result = app.nodeQuery(type).sort(sortKey, sortDesc).page(page).pageSize(pageSize).getAsList();

				logger.info("Result size: {}, expected: {}", new Object[] { result.size(), pageSize });
				assertTrue(result.size() == Math.min(number, pageSize));

				for (int j = 0; j < Math.min(result.size(), pageSize); j++) {

					String expectedName = "TestOne-" + (offset + j);
					String gotName     = result.get(j).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY));

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

			String type                      = "TestOne";
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

					node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

					// slow down execution speed to make sure distinct changes fall in different milliseconds
					try { Thread.sleep(2); } catch (Throwable t) {}
				}
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery(type).getAsList();

				assertEquals(number, result.size());

				PropertyKey sortKey = Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.LAST_MODIFIED_DATE_PROPERTY);
				boolean sortDesc    = true;
				int pageSize        = 10;
				int page            = 1;

				result = app.nodeQuery(type).sort(sortKey, sortDesc).page(page).pageSize(pageSize).getAsList();

				logger.info("Result size: {}, expected: {}", new Object[] { result.size(), pageSize });
				assertTrue(result.size() == Math.min(number, pageSize));

				for (int j = 0; j < pageSize; j++) {

					int expectedNumber = number + offset - 1 - j;
					String gotName     = result.get(j).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY));

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

			String type                      = "TestOne";
			int number                      = 61;
			final List<NodeInterface> nodes = this.createTestNodes(type, number);
			final PropertyKey key           = Traits.of("TestOne").key("anInt");
			final int offset                = 10;

			Collections.shuffle(nodes, new Random(System.nanoTime()));

			try (final Tx tx = app.tx()) {

				int i = offset;

				for (NodeInterface node : nodes) {

					node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), Integer.toString(i));
					node.setProperty(key, i);

					i++;
				}
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery(type).getAsList();

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

			String type                      = "TestOne";
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

					node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "TestOne-" + name);

					if ((i % 2) != 0) {
						node.setProperty(Traits.of("TestOne").key("aDate"), new Date());
						System.out.println("TestOne-" + name + ": indexed with date");
					} else {
						node.setProperty(Traits.of("TestOne").key("aDate"), null);
						System.out.println("TestOne-" + name + ": null date");
					}

					// slow down execution speed to make sure distinct changes fall in different milliseconds
					try { Thread.sleep(2); } catch (Throwable t) {}

				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery(type).getAsList();

				assertEquals(number, result.size());

				PropertyKey sortKey = Traits.of("TestOne").key("aDate");
				boolean sortDesc    = true;
				int pageSize        = 10;
				int page            = 2;

				result = app.nodeQuery(type).sort(sortKey, sortDesc).page(page).pageSize(pageSize).getAsList();

				logger.info("Result size: {}, expected: {}", new Object[] { result.size(), pageSize });
				assertTrue(result.size() == Math.min(number, pageSize));

				for (int j = 0; j < Math.min(result.size(), pageSize); j++) {

					String expectedName = "TestOne-" + (30 - (j+1)*2);
					String gotName     = result.get(j).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY));

					System.out.println(j + ": " +  expectedName + ", got: " + gotName);
					assertEquals(expectedName, gotName);

				}

				// allow visual inspection of test results
				final List<NodeInterface> list = app.nodeQuery(type).sort(sortKey, sortDesc).getAsList();
				list.stream().forEach(n -> System.out.println(n.getName() + ": " + n.getProperty(Traits.of("TestOne").key("aDate"))));

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

			final List<NodeInterface> nodes = this.createTestNodes("TestOne", 10, 100);
			try (final Tx tx = app.tx()) {

				int i = 0;

				for (NodeInterface node : nodes) {

					node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), Long.toString(i));
					if (i < 7) {
						node.setProperty(Traits.of("TestOne").key("anInt"), i);
					}

					i++;
				}
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				boolean sortDesc    = false;

				final PropertyKey<String> nameKey   = Traits.of("TestOne").key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
				final PropertyKey<Integer> anIntKey = Traits.of("TestOne").key("anInt");
				final List<NodeInterface> result    = app.nodeQuery("TestOne").sort(Traits.of("TestOne").key("anInt"), sortDesc).sort(nameKey).getAsList();


				// check that the sorting is stable, i.e. the position of nodes
				// with equal values (and null) is not modified by sorting

				final Iterator<NodeInterface> nameIterator = result.iterator();
				while (nameIterator.hasNext()) {

					// values first
					assertEquals("Invalid sort result with mixed values (null vs. int)", "0", nameIterator.next().getProperty(nameKey));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "1", nameIterator.next().getProperty(nameKey));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "2", nameIterator.next().getProperty(nameKey));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "3", nameIterator.next().getProperty(nameKey));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "4", nameIterator.next().getProperty(nameKey));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "5", nameIterator.next().getProperty(nameKey));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "6", nameIterator.next().getProperty(nameKey));

					// nulls after that
					assertEquals("Invalid sort result with mixed values (null vs. int)", "7", nameIterator.next().getProperty(nameKey));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "8", nameIterator.next().getProperty(nameKey));
					assertEquals("Invalid sort result with mixed values (null vs. int)", "9", nameIterator.next().getProperty(nameKey));
				}


				// check that the sorting is "nulls last" as documented
				final Iterator<NodeInterface> intIterator = result.iterator();
				while (intIterator.hasNext()) {

					// values first
					assertEquals("Invalid sort result with mixed values (null vs. int)", 0L, (long)intIterator.next().getProperty(anIntKey));
					assertEquals("Invalid sort result with mixed values (null vs. int)", 1L, (long)intIterator.next().getProperty(anIntKey));
					assertEquals("Invalid sort result with mixed values (null vs. int)", 2L, (long)intIterator.next().getProperty(anIntKey));
					assertEquals("Invalid sort result with mixed values (null vs. int)", 3L, (long)intIterator.next().getProperty(anIntKey));
					assertEquals("Invalid sort result with mixed values (null vs. int)", 4L, (long)intIterator.next().getProperty(anIntKey));
					assertEquals("Invalid sort result with mixed values (null vs. int)", 5L, (long)intIterator.next().getProperty(anIntKey));
					assertEquals("Invalid sort result with mixed values (null vs. int)", 6L, (long)intIterator.next().getProperty(anIntKey));

					// nulls after that
					assertEquals("Invalid sort result with mixed values (null vs. int)", null, intIterator.next().getProperty(anIntKey));
					assertEquals("Invalid sort result with mixed values (null vs. int)", null, intIterator.next().getProperty(anIntKey));
					assertEquals("Invalid sort result with mixed values (null vs. int)", null, intIterator.next().getProperty(anIntKey));
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
			final PropertyKey key = Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
			final String name     = "89w3hkl sdfghsdkljth";

			props.put(key, name);

			final NodeInterface node = createTestNode("TestOne", props);

			List<NodeInterface> result = null;

			try (final Tx tx = app.tx()) {

				result = app.nodeQuery("TestOne").name(name).includeHidden().getAsList();

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

				result = app.nodeQuery("TestOne").name(name2).includeHidden().getAsList();

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
			PropertyKey key   = Traits.of("TestOne").key("aDate");
			Date date         = new Date();
			String type        = "TestOne";

			props.put(key, date);

			NodeInterface node = createTestNode(type, props);

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery(type).key(key, date).includeHidden().getAsList();

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
	public void test03SearchRelationship() {

		try {

			final RelationshipInterface rel = createTestRelationships("TestOne", "TestTwo", "OneTwoOneToOne", 1).get(0);
			final PropertyKey key1          = new StringProperty("jghsdkhgshdhgsdjkfgh").indexed();
			final String type               = "OneTwoOneToOne";
			final String val1               = "54354354546806849870";

			final List<RelationshipInterface> result;

			try (final Tx tx = app.tx()) {

				rel.setProperty(key1, val1);
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				assertTrue(rel.getProperty(key1).equals(val1));

				result = app.relationshipQuery(type).key(key1, val1).getAsList();

				assertEquals(1, result.size());
				assertEquals(rel, result.get(0));

				tx.success();
			}

			final String val2 = "ölllldjöoa8w4rasf";

			try (final Tx tx = app.tx()) {

				rel.setProperty(key1, val2);

				assertEquals(1, result.size());
				assertEquals(rel, result.get(0));

				tx.success();
			}

		} catch (FrameworkException ex) {

			ex.printStackTrace();
			fail("Unexpected exception");

		}

	}

	@Test
	public void test04SearchByLocation() {

		try {

			final PropertyMap props = new PropertyMap();
			final PropertyKey lat   = Traits.of("TestSeven").key(TestSevenTraitDefinition.LATITUDE_PROPERTY);
			final PropertyKey lon   = Traits.of("TestSeven").key(TestSevenTraitDefinition.LONGITUDE_PROPERTY);
			final String type       = "TestSeven";

			props.put(lat, 50.12284d);
			props.put(lon, 8.73923d);
			props.put(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "TestSeven-0");

			NodeInterface node = createTestNode(type, props);

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery(type).location("Hanauer Landstraße", "200", "60314", "Frankfurt", "Germany", 10.0).includeHidden().getAsList();

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

			final String type       = "TestSeven";
			final PropertyMap props = new PropertyMap();
			final PropertyKey lat   = Traits.of("TestSeven").key(TestSevenTraitDefinition.LATITUDE_PROPERTY);
			final PropertyKey lon   = Traits.of("TestSeven").key(TestSevenTraitDefinition.LONGITUDE_PROPERTY);

			props.put(Traits.of(StructrTraits.NODE_INTERFACE).key(GraphObjectTraitDefinition.TYPE_PROPERTY), type);
			props.put(lat, 50.12284d);
			props.put(lon, 8.73923d);
			props.put(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "TestSeven-0");

			try (final Tx tx = app.tx()) {

				// this will work
				NodeInterface node = app.create("TestSeven", props);

				props.remove(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY));
				props.put(lat, 50.12285d);
				props.put(lon, 8.73924d);

				// this will fail
				NodeInterface node2 = app.create("TestSeven", props);

				// adding another
				NodeInterface node3 = app.create("TestSeven", props);

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

			List<NodeInterface> result = app.nodeQuery("TestOne").location("Hanauer Landstraße", "200", "60314", "Frankfurt", "Germany", 10.0).includeHidden().getAsList();

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
			final PropertyKey key = Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
			final String name     = "89w3hkl sdfghsdkljth";

			props.put(key, name);

			final NodeInterface node = createTestNode("TestOne", props);

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery("TestOne").name(name).includeHidden().getAsList();

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
	public void test08SearchByStaticMethod02() {

		try {

			PropertyMap props     = new PropertyMap();
			final PropertyKey key = Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
			final String name     = "89w3hkl sdfghsdkljth";

			props.put(key, name);

			final NodeInterface node = createTestNode("TestOne", props);

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery("TestOne").name(name).includeHidden().getAsList();

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
	public void test08SearchByStaticMethodWithNullSearchValue01() {

		try {

			PropertyMap props     = new PropertyMap();
			final PropertyKey key = Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
			final String name     = "abc";

			props.put(key, name);

			createTestNode("TestOne", props);

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery("TestOne").name(null).includeHidden().getAsList();

				assertTrue(result.isEmpty());

				tx.success();
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
			NodeInterface node = createTestNode("TestOne", props);

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery("TestOne").key(Traits.of("TestOne").key("aString"), null).includeHidden().getAsList();

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
	public void test10SearchByEmptyDateField() {

		try {

			PropertyMap props     = new PropertyMap();
			NodeInterface node = createTestNode("TestOne", props);

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery("TestOne").key(Traits.of("TestOne").key("aDate"), null).includeHidden().getAsList();

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
	public void test11SearchByEmptyIntField() {

		try {

			PropertyMap props     = new PropertyMap();
			NodeInterface node = createTestNode("TestOne", props);

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery("TestOne").key(Traits.of("TestOne").key("anInt"), null).includeHidden().getAsList();

				assertTrue(result.size() == 1);
				assertTrue(result.get(0).equals(node));

				tx.success();
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
			NodeInterface node = createTestNode("TestOne", props);

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery("TestOne").key(Traits.of("TestOne").key("aLong"), null).includeHidden().getAsList();

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
	public void test13SearchByEmptyDoubleField() {

		try {

			PropertyMap props = new PropertyMap();
			NodeInterface node = createTestNode("TestOne", props);

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery("TestOne").key(Traits.of("TestOne").key("aDouble"), null).includeHidden().getAsList();
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

			String type                             = "TestOne";
			int number                             = 43;

			// create nodes
			this.createTestNodes(type, number);

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery(type).getAsList();

				assertTrue(result.size() == number);

				PropertyKey sortKey = Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
				boolean sortDesc    = false;
				int pageSize        = 10;
				int page            = 1;

				result = app.nodeQuery(type).includeHidden().sort(sortKey, sortDesc).page(page).pageSize(pageSize).getAsList();

				logger.info("Result size: {}, expected: {}", new Object[] { result.size(), pageSize });
				assertTrue(result.size() == pageSize);

				tx.success();
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
			String type                      = "TestOne";
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

					node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), _name);
				}
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				List<NodeInterface> result = app.nodeQuery(type).getAsList();

				assertEquals(number, result.size());

				PropertyKey sortKey = Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
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
			String type                      = "TestOne";
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

					node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), _name);
				}
				tx.success();
			}

			try (final Tx tx = app.tx()) {

				final List<NodeInterface> result = app.nodeQuery(type).getAsList();

				assertTrue(result.size() == number);

				PropertyKey sortKey = Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
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

				PropertyKey sortKey = Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
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
			tester1 = app.create(StructrTraits.USER, "tester1").as(Principal.class);
			tester2 = app.create(StructrTraits.USER, "tester2").as(Principal.class);

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
			final String type                         = "TestOne";
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

						node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), tester1);
						tester1Nodes.add(node);

					} else if (rand < 0.6) {

						node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), tester2);
						tester2Nodes.add(node);
					}

					i++;

					node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), _name);
				}

				tx.success();
			}

			final int tester1NodeCount   = tester1Nodes.size();
			final int tester2NodeCount   = tester2Nodes.size();

			try (final Tx tx = app.tx()) {

				final PropertyKey sortKey        = Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
				final boolean sortDesc           = false;
				final int pageSize               = 10;
				final int page                   = 22;
				final ResultStream<NodeInterface> result = tester1App.nodeQuery(type).sort(sortKey, sortDesc).pageSize(pageSize).page(page).getResultStream();

				assertEquals("Invalid paging result count with non-superuser security context", tester1NodeCount, result.calculateTotalResultCount(null, softLimit));

				result.close();

				tx.success();

			} catch (Exception ex) {
				fail("Unexpected exception");
			}

			try (final Tx tx = app.tx()) {

				final PropertyKey sortKey        = Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
				final boolean sortDesc           = false;
				final int pageSize               = 10;
				final int page                   = 22;
				final ResultStream<NodeInterface> result = tester2App.nodeQuery(type).sort(sortKey, sortDesc).pageSize(pageSize).page(page).getResultStream();

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

			final String type = "TestOne";

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
			createTestNodes("TestOne", 10);

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> testOnes = app.nodeQuery("TestOne").getAsList();

			final NodeInterface test1 = testOnes.get(3);
			final NodeInterface test2 = testOnes.get(4);
			final NodeInterface test3 = testOnes.get(7);

			test1.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.HIDDEN_PROPERTY), true);

			test3.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.HIDDEN_PROPERTY), true);

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> result = app.nodeQuery("TestOne").includeHidden(false).getAsList();

			assertEquals("Actual result size should be equal to result count", 8, (int)result.size());


			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testManyToManyReverseNodeSearch() {

		final String groupType                               = StructrTraits.GROUP;
		final PropertyKey<Iterable<NodeInterface>> groupsKey = Traits.of(groupType).key(PrincipalTraitDefinition.GROUPS_PROPERTY);
		final List<Group> groups                             = new LinkedList<>();

		try (final Tx tx = app.tx()) {

			for (int i=0; i<5; i++) {

				groups.add(createTestNode(groupType, StructrTraits.GROUP + i).as(Group.class));
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
			final List<NodeInterface> result1 = app.nodeQuery(StructrTraits.GROUP).key(groupsKey, new LinkedList<>()).getAsList();
			assertEquals("Invalid search result", 1, result1.size());

			// search for a group with group2 as a parent
			final List<NodeInterface> result2 = app.nodeQuery(StructrTraits.GROUP).key(groupsKey, Arrays.asList(groups.get(1))).getAsList();
			assertEquals("Invalid search result", 2, result2.size());

			// search for a group with group2 as a parent and a given name
			final List<NodeInterface> result3 = app.nodeQuery(StructrTraits.GROUP).name("Group3").key(groupsKey, Arrays.asList(groups.get(1))).getAsList();
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

			final String groupType            = StructrTraits.GROUP;
			final PropertyKey<String> nameKey = Traits.of(groupType).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

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

		final String groupType            = StructrTraits.GROUP;
		final PropertyKey<String> nameKey = Traits.of(groupType).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

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

			List<NodeInterface> list = app.nodeQuery(groupType)
					.or()
						.name("aaa")
						.not().name("bbb")
					.or()
						.name("ttt")
					.or()
						.name("xxx")
					.or()
						.name("bbb")
					.sort(nameKey, false)
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

		final String groupType                = StructrTraits.GROUP;
		final Traits traits                   = Traits.of(groupType);
		final PropertyKey<String> nameKey     = traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final PropertyKey<Principal> ownerKey = traits.key(NodeInterfaceTraitDefinition.OWNER_PROPERTY);

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
			final SearchAttributeGroup rootGroup         = new SearchAttributeGroup(securityContext, null, Operation.AND);
			final SearchAttributeGroup mainMatchingGroup = new SearchAttributeGroup(securityContext, null, Operation.OR);

			mainMatchingGroup.add(new ComparisonSearchAttribute(nameKey, ComparisonQuery.Comparison.equal, "a"));
			mainMatchingGroup.add(new ComparisonSearchAttribute(nameKey, ComparisonQuery.Comparison.equal, "b"));
			mainMatchingGroup.add(new ComparisonSearchAttribute(nameKey, ComparisonQuery.Comparison.equal, "c"));
			mainMatchingGroup.add(new ComparisonSearchAttribute(nameKey, ComparisonQuery.Comparison.equal, "d"));

			final SearchAttributeGroup secondaryMatchingGroup = new SearchAttributeGroup(securityContext, null, Operation.AND);

			secondaryMatchingGroup.add(new ComparisonSearchAttribute(ownerKey, ComparisonQuery.Comparison.isNull, null));

			final SearchAttributeGroup notGroup = new SearchAttributeGroup(securityContext, null, Operation.NOT);
			secondaryMatchingGroup.add(notGroup);

			notGroup.add(new ComparisonSearchAttribute(ownerKey, ComparisonQuery.Comparison.isNotNull, null));

			// Test Greater/Less with ASCII chars
			final SearchAttributeGroup andGroup = new SearchAttributeGroup(securityContext, null, Operation.AND);

			secondaryMatchingGroup.add(andGroup);

			andGroup.add(new ComparisonSearchAttribute(nameKey, ComparisonQuery.Comparison.greater, "_"));
			andGroup.add(new ComparisonSearchAttribute(nameKey, ComparisonQuery.Comparison.greaterOrEqual, "a"));
			andGroup.add(new ComparisonSearchAttribute(nameKey, ComparisonQuery.Comparison.lessOrEqual, "d"));
			andGroup.add(new ComparisonSearchAttribute(nameKey, ComparisonQuery.Comparison.less, "e"));
			andGroup.add(new ComparisonSearchAttribute(nameKey, ComparisonQuery.Comparison.notEqual, "b"));

			rootGroup.add(mainMatchingGroup);
			rootGroup.add(secondaryMatchingGroup);
			attributes.add(rootGroup);

			final List<NodeInterface> list = app.nodeQuery(groupType)
					.attributes(attributes, Operation.AND)
					.sort(nameKey, false)
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

		final Traits traits                 = Traits.of("TestOne");
		final PropertyKey<String> nameKey   = traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
		final PropertyKey<Integer> anIntKey = traits.key("anInt");
		final PropertyKey<Long> aLongKey    = traits.key("aLong");

		try (final Tx tx = app.tx()) {

			app.create("TestOne", new NodeAttribute<>(nameKey, "name7"), new NodeAttribute<>(anIntKey, 3), new NodeAttribute<>(aLongKey, 20L));
			app.create("TestOne", new NodeAttribute<>(nameKey, "name5"), new NodeAttribute<>(anIntKey, 2), new NodeAttribute<>(aLongKey, 20L));
			app.create("TestOne", new NodeAttribute<>(nameKey, "name2"), new NodeAttribute<>(anIntKey, 1), new NodeAttribute<>(aLongKey, 20L));
			app.create("TestOne", new NodeAttribute<>(nameKey, "name1"), new NodeAttribute<>(anIntKey, 3), new NodeAttribute<>(aLongKey, 20L));
			app.create("TestOne", new NodeAttribute<>(nameKey, "name3"), new NodeAttribute<>(anIntKey, 2), new NodeAttribute<>(aLongKey, 20L));
			app.create("TestOne", new NodeAttribute<>(nameKey, "name4"), new NodeAttribute<>(anIntKey, 1), new NodeAttribute<>(aLongKey, 10L));
			app.create("TestOne", new NodeAttribute<>(nameKey, "name9"), new NodeAttribute<>(anIntKey, 3), new NodeAttribute<>(aLongKey, 10L));
			app.create("TestOne", new NodeAttribute<>(nameKey, "name8"), new NodeAttribute<>(anIntKey, 2), new NodeAttribute<>(aLongKey, 10L));
			app.create("TestOne", new NodeAttribute<>(nameKey, "name6"), new NodeAttribute<>(anIntKey, 1), new NodeAttribute<>(aLongKey, 10L));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> result1 = app.nodeQuery("TestOne").sort(aLongKey).sort(nameKey).getAsList();

			assertEquals("Sorting by multiple keys returns wrong result", "name4", result1.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result1.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result1.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name9", result1.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name1", result1.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result1.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result1.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result1.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result1.get(8).getName());

			final List<NodeInterface> result2 = app.nodeQuery("TestOne").sort(aLongKey, true).sort(nameKey).getAsList();

			assertEquals("Sorting by multiple keys returns wrong result", "name1", result2.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result2.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result2.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result2.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result2.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name4", result2.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result2.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result2.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name9", result2.get(8).getName());

			final List<NodeInterface> result3 = app.nodeQuery("TestOne").sort(aLongKey).sort(nameKey, true).getAsList();

			assertEquals("Sorting by multiple keys returns wrong result", "name9", result3.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result3.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result3.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name4", result3.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result3.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result3.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result3.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result3.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name1", result3.get(8).getName());

			final List<NodeInterface> result4 = app.nodeQuery("TestOne").sort(aLongKey).sort(anIntKey).sort(nameKey).getAsList();

			assertEquals("Sorting by multiple keys returns wrong result", "name4", result4.get(0).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name6", result4.get(1).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name8", result4.get(2).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name9", result4.get(3).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name2", result4.get(4).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name3", result4.get(5).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name5", result4.get(6).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name1", result4.get(7).getName());
			assertEquals("Sorting by multiple keys returns wrong result", "name7", result4.get(8).getName());

			final List<NodeInterface> result5 = app.nodeQuery("TestOne").sort(aLongKey).sort(anIntKey, true).sort(nameKey).getAsList();

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

	@Test
	public void testGraphSearchWithMultipleComponents() {

		// setup

		try (final Tx tx = app.tx()) {

			JsonSchema schema          = StructrSchema.createEmptySchema();

			final JsonObjectType center = schema.addType("Center");
			final JsonObjectType type1  = schema.addType("Type1");
			final JsonObjectType type2  = schema.addType("Type2");
			final JsonObjectType type3  = schema.addType("Type3");
			final JsonObjectType type4  = schema.addType("Type4");
			final JsonObjectType type5  = schema.addType("Type5");

			center.relate(type1, "type1", Cardinality.ManyToMany, "centers", "types1");
			type2.relate(center, "type2", Cardinality.ManyToMany, "types2", "centers");
			center.relate(type3, "type3", Cardinality.ManyToMany, "centers", "types3");
			type4.relate(center, "type4", Cardinality.ManyToMany, "types4", "centers");
			center.relate(type5, "type5", Cardinality.ManyToMany, "centers", "types5");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String centerType = "Center";
		final String type1      = "Type1";
		final String type2      = "Type2";
		final String type3      = "Type3";
		final String type4      = "Type4";
		final String type5      = "Type5";

		final PropertyKey type1CenterKey = Traits.of(type1).key("centers");
		final PropertyKey type2CenterKey = Traits.of(type2).key("centers");
		final PropertyKey type3CenterKey = Traits.of(type3).key("centers");
		final PropertyKey type4CenterKey = Traits.of(type4).key("centers");
		final PropertyKey type5CenterKey = Traits.of(type5).key("centers");

		final PropertyKey types1Key = Traits.of(centerType).key("types1");
		final PropertyKey types2Key = Traits.of(centerType).key("types2");
		final PropertyKey types3Key = Traits.of(centerType).key("types3");
		final PropertyKey types4Key = Traits.of(centerType).key("types4");
		final PropertyKey types5Key = Traits.of(centerType).key("types5");

		NodeInterface center1 = null;
		NodeInterface center2 = null;
		NodeInterface center3 = null;
		NodeInterface center4 = null;
		NodeInterface center5 = null;

		NodeInterface type11 = null;
		NodeInterface type12 = null;
		NodeInterface type13 = null;
		NodeInterface type14 = null;
		NodeInterface type15 = null;

		NodeInterface type21 = null;
		NodeInterface type22 = null;
		NodeInterface type23 = null;
		NodeInterface type24 = null;

		NodeInterface type31 = null;
		NodeInterface type32 = null;
		NodeInterface type33 = null;

		NodeInterface type41 = null;
		NodeInterface type42 = null;

		NodeInterface type51 = null;

		try (final Tx tx = app.tx()) {

			center1 = app.create(centerType, "center1");
			center2 = app.create(centerType, "center2");
			center3 = app.create(centerType, "center3");
			center4 = app.create(centerType, "center4");
			center5 = app.create(centerType, "center5");

			type11 = app.create(type1, new NodeAttribute<>(type1CenterKey, Arrays.asList(center1)));
			type12 = app.create(type1, new NodeAttribute<>(type1CenterKey, Arrays.asList(center2)));
			type13 = app.create(type1, new NodeAttribute<>(type1CenterKey, Arrays.asList(center3)));
			type14 = app.create(type1, new NodeAttribute<>(type1CenterKey, Arrays.asList(center4)));
			type15 = app.create(type1, new NodeAttribute<>(type1CenterKey, Arrays.asList(center5)));

			type21 = app.create(type2, new NodeAttribute<>(type2CenterKey, Arrays.asList(center2)));
			type22 = app.create(type2, new NodeAttribute<>(type2CenterKey, Arrays.asList(center3)));
			type23 = app.create(type2, new NodeAttribute<>(type2CenterKey, Arrays.asList(center4)));
			type24 = app.create(type2, new NodeAttribute<>(type2CenterKey, Arrays.asList(center5)));

			type31 = app.create(type3, new NodeAttribute<>(type3CenterKey, Arrays.asList(center3)));
			type32 = app.create(type3, new NodeAttribute<>(type3CenterKey, Arrays.asList(center4)));
			type33 = app.create(type3, new NodeAttribute<>(type3CenterKey, Arrays.asList(center5)));

			type41 = app.create(type4, new NodeAttribute<>(type4CenterKey, Arrays.asList(center4)));
			type42 = app.create(type4, new NodeAttribute<>(type4CenterKey, Arrays.asList(center5)));

			type51 = app.create(type5, new NodeAttribute<>(type5CenterKey, Arrays.asList(center5)));

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> result1 = app.nodeQuery(centerType)
				.key(types1Key, Arrays.asList(type11))
				.getAsList();

			final List<NodeInterface> result2 = app.nodeQuery(centerType)
				.key(types1Key, Arrays.asList(type12))
				.key(types2Key, Arrays.asList(type21))
				.getAsList();

			final List<NodeInterface> result3 = app.nodeQuery(centerType)
				.key(types1Key, Arrays.asList(type13))
				.key(types2Key, Arrays.asList(type22))
				.key(types3Key, Arrays.asList(type31))
				.getAsList();

			final List<NodeInterface> result4 = app.nodeQuery(centerType)
				.key(types1Key, Arrays.asList(type14))
				.key(types2Key, Arrays.asList(type23))
				.key(types3Key, Arrays.asList(type32))
				.key(types4Key, Arrays.asList(type41))
				.getAsList();

			final List<NodeInterface> result5 = app.nodeQuery(centerType)
				.key(types1Key, Arrays.asList(type15))
				.key(types2Key, Arrays.asList(type24))
				.key(types3Key, Arrays.asList(type33))
				.key(types4Key, Arrays.asList(type42))
				.key(types5Key, Arrays.asList(type51))
				.getAsList();


			// expected result: center object is found
			assertEquals("Invalid graph search result with 1 component",  1, result1.size());
			assertEquals("Invalid graph search result with 2 components", 1, result2.size());
			assertEquals("Invalid graph search result with 3 components", 1, result3.size());
			assertEquals("Invalid graph search result with 4 components", 1, result4.size());
			assertEquals("Invalid graph search result with 5 components", 1, result5.size());

			assertEquals("Invalid graph search result with 1 component",  center1.getUuid(), result1.get(0).getUuid());
			assertEquals("Invalid graph search result with 2 components", center2.getUuid(), result2.get(0).getUuid());
			assertEquals("Invalid graph search result with 3 components", center3.getUuid(), result3.get(0).getUuid());
			assertEquals("Invalid graph search result with 4 components", center4.getUuid(), result4.get(0).getUuid());
			assertEquals("Invalid graph search result with 5 components", center5.getUuid(), result5.get(0).getUuid());

			// test negative results as well (expect 0 results)
			assertEquals("Invalid graph search result with 2 wrong components", 0, app.nodeQuery(centerType)
				.key(types1Key, Arrays.asList(type12))
				.key(types2Key, Arrays.asList(type23))
				.getAsList().size());

			assertEquals("Invalid graph search result with 3 wrong components", 0, app.nodeQuery(centerType)
				.key(types1Key, Arrays.asList(type12))
				.key(types2Key, Arrays.asList(type23))
				.key(types3Key, Arrays.asList(type31))
				.getAsList().size());

			assertEquals("Invalid graph search result with 3 wrong components", 0, app.nodeQuery(centerType)
				.key(types1Key, Arrays.asList(type12))
				.key(types2Key, Arrays.asList(type23))
				.key(types3Key, Arrays.asList(type31))
				.key(types4Key, Arrays.asList(type41))
				.getAsList().size());

			assertEquals("Invalid graph search result with 3 wrong components", 0, app.nodeQuery(centerType)
				.key(types1Key, Arrays.asList(type12))
				.key(types2Key, Arrays.asList(type23))
				.key(types3Key, Arrays.asList(type31))
				.key(types4Key, Arrays.asList(type42))
				.key(types5Key, Arrays.asList(type51))
				.getAsList().size());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testGraphSearchWithRange() {

		// setup

		try (final Tx tx = app.tx()) {

			final JsonSchema schema     = StructrSchema.createEmptySchema();
			final JsonObjectType center = schema.addType("Center");
			final JsonObjectType type1  = schema.addType("Type1");

			center.relate(type1, "type1", Cardinality.ManyToMany, "centers", "types1");

			center.addStringProperty("string").setIndexed(true);
			center.addIntegerProperty("integer").setIndexed(true);
			center.addLongProperty("long").setIndexed(true);
			center.addDoubleProperty("double").setIndexed(true);
			center.addDateProperty("date").setIndexed(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final String centerType = "Center";
		final String type1      = "Type1";

		final PropertyKey types1Key      = Traits.of(centerType).key("types1");
		final PropertyKey stringKey      = Traits.of(centerType).key("string");
		final PropertyKey intKey         = Traits.of(centerType).key("integer");
		final PropertyKey longKey        = Traits.of(centerType).key("long");
		final PropertyKey doubleKey      = Traits.of(centerType).key("double");
		final PropertyKey dateKey        = Traits.of(centerType).key("date");

		try (final Tx tx = app.tx()) {

			final NodeInterface type11 = app.create(type1);

			app.create(centerType, new NodeAttribute<>(stringKey, "string3"), new NodeAttribute<>(intKey, 1), new NodeAttribute<>(longKey, 1L), new NodeAttribute<>(doubleKey, 1.0), new NodeAttribute<>(dateKey, new Date(5000L)), new NodeAttribute<>(types1Key, Arrays.asList(type11)));
			app.create(centerType, new NodeAttribute<>(stringKey, "string1"), new NodeAttribute<>(intKey, 2), new NodeAttribute<>(longKey, 2L), new NodeAttribute<>(doubleKey, 2.0), new NodeAttribute<>(dateKey, new Date(4000L)), new NodeAttribute<>(types1Key, Arrays.asList(type11)));
			app.create(centerType, new NodeAttribute<>(stringKey, "string5"), new NodeAttribute<>(intKey, 3), new NodeAttribute<>(longKey, 3L), new NodeAttribute<>(doubleKey, 3.0), new NodeAttribute<>(dateKey, new Date(3000L)), new NodeAttribute<>(types1Key, Arrays.asList(type11)));
			app.create(centerType, new NodeAttribute<>(stringKey, "string2"), new NodeAttribute<>(intKey, 4), new NodeAttribute<>(longKey, 4L), new NodeAttribute<>(doubleKey, 4.0), new NodeAttribute<>(dateKey, new Date(2000L)), new NodeAttribute<>(types1Key, Arrays.asList(type11)));
			app.create(centerType, new NodeAttribute<>(stringKey, "string4"), new NodeAttribute<>(intKey, 5), new NodeAttribute<>(longKey, 5L), new NodeAttribute<>(doubleKey, 5.0), new NodeAttribute<>(dateKey, new Date(1000L)), new NodeAttribute<>(types1Key, Arrays.asList(type11)));
			app.create(centerType, new NodeAttribute<>(stringKey, "string2"), new NodeAttribute<>(intKey, 1), new NodeAttribute<>(longKey, 1L), new NodeAttribute<>(doubleKey, 1.0), new NodeAttribute<>(dateKey, new Date(5000L)));
			app.create(centerType, new NodeAttribute<>(stringKey, "string4"), new NodeAttribute<>(intKey, 2), new NodeAttribute<>(longKey, 2L), new NodeAttribute<>(doubleKey, 2.0), new NodeAttribute<>(dateKey, new Date(4000L)));
			app.create(centerType, new NodeAttribute<>(stringKey, "string3"), new NodeAttribute<>(intKey, 3), new NodeAttribute<>(longKey, 3L), new NodeAttribute<>(doubleKey, 3.0), new NodeAttribute<>(dateKey, new Date(3000L)));
			app.create(centerType, new NodeAttribute<>(stringKey, "string1"), new NodeAttribute<>(intKey, 4), new NodeAttribute<>(longKey, 4L), new NodeAttribute<>(doubleKey, 4.0), new NodeAttribute<>(dateKey, new Date(2000L)));
			app.create(centerType, new NodeAttribute<>(stringKey, "string5"), new NodeAttribute<>(intKey, 5), new NodeAttribute<>(longKey, 5L), new NodeAttribute<>(doubleKey, 5.0), new NodeAttribute<>(dateKey, new Date(1000L)));

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface type11 = (NodeInterface)app.nodeQuery(type1).getFirst();

			// string
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(stringKey, null, "string4", false, false).getAsList().size());
			assertEquals("", 4, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(stringKey, null, "string4", false,  true).getAsList().size());
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(stringKey, null, "string4",  true, false).getAsList().size());
			assertEquals("", 4, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(stringKey, null, "string4",  true,  true).getAsList().size());
			assertEquals("", 2, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(stringKey, "string3", null, false, false).getAsList().size());
			assertEquals("", 2, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(stringKey, "string3", null, false,  true).getAsList().size());
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(stringKey, "string3", null,  true, false).getAsList().size());
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(stringKey, "string3", null,  true,  true).getAsList().size());

			// integer
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(intKey, null, 4, false, false).getAsList().size());
			assertEquals("", 4, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(intKey, null, 4, false,  true).getAsList().size());
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(intKey, null, 4,  true, false).getAsList().size());
			assertEquals("", 4, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(intKey, null, 4,  true,  true).getAsList().size());
			assertEquals("", 2, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(intKey, 3, null, false, false).getAsList().size());
			assertEquals("", 2, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(intKey, 3, null, false,  true).getAsList().size());
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(intKey, 3, null,  true, false).getAsList().size());
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(intKey, 3, null,  true,  true).getAsList().size());

			// long
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(longKey, null, 4L, false, false).getAsList().size());
			assertEquals("", 4, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(longKey, null, 4L, false,  true).getAsList().size());
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(longKey, null, 4L,  true, false).getAsList().size());
			assertEquals("", 4, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(longKey, null, 4L,  true,  true).getAsList().size());
			assertEquals("", 2, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(longKey, 3L, null, false, false).getAsList().size());
			assertEquals("", 2, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(longKey, 3L, null, false,  true).getAsList().size());
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(longKey, 3L, null,  true, false).getAsList().size());
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(longKey, 3L, null,  true,  true).getAsList().size());

			// double
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(doubleKey, null, 4.0, false, false).getAsList().size());
			assertEquals("", 4, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(doubleKey, null, 4.0, false,  true).getAsList().size());
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(doubleKey, null, 4.0,  true, false).getAsList().size());
			assertEquals("", 4, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(doubleKey, null, 4.0,  true,  true).getAsList().size());
			assertEquals("", 2, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(doubleKey, 3.0, null, false, false).getAsList().size());
			assertEquals("", 2, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(doubleKey, 3.0, null, false,  true).getAsList().size());
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(doubleKey, 3.0, null,  true, false).getAsList().size());
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(doubleKey, 3.0, null,  true,  true).getAsList().size());

			// date
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(dateKey, null, new Date(4000L), false, false).getAsList().size());
			assertEquals("", 4, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(dateKey, null, new Date(4000L), false,  true).getAsList().size());
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(dateKey, null, new Date(4000L),  true, false).getAsList().size());
			assertEquals("", 4, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(dateKey, null, new Date(4000L),  true,  true).getAsList().size());
			assertEquals("", 2, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(dateKey, new Date(3000L), null, false, false).getAsList().size());
			assertEquals("", 2, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(dateKey, new Date(3000L), null, false,  true).getAsList().size());
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(dateKey, new Date(3000L), null,  true, false).getAsList().size());
			assertEquals("", 3, app.nodeQuery(centerType).key(types1Key, Arrays.asList(type11)).range(dateKey, new Date(3000L), null,  true,  true).getAsList().size());

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testFindQueryWithOrPredicate() {

		/*
		 * This test verifies that the query builder creates efficient queries for
		 * $.or() and $.and() predicates with graph properties.
		 */

		// schema setup
		try (final Tx tx = app.tx()) {

			final JsonSchema schema      = StructrSchema.createEmptySchema();
			final JsonObjectType project = schema.addType("Project");
			final JsonObjectType task    = schema.addType("Task");

			project.relate(task, "TASK", Cardinality.OneToMany, "project", "tasks");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception.");
		}

		final ActionContext ctx = new ActionContext(securityContext);

		// test
		try (final Tx tx = app.tx()) {

			final List<AbstractNode> result1 = (List)ScriptTestHelper.testExternalScript(ctx, SearchAndSortingTest.class.getResourceAsStream("/test/scripting/testFindQueryWithOrPredicate.js"));

			assertEquals("Wrong result for predicate list,", "[Project0, Project1]", result1.stream().map(r -> r.getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY))).collect(Collectors.toList()).toString());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();

			fail("Unexpected exception");
		}
	}

	@Test
	public void testSortWithPathResolution() {

		final PropertyKey<String> nameKey = Traits.of(StructrTraits.GROUP).key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

		try (final Tx tx = app.tx()) {

			final Principal ownerC = createTestNode(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "C")).as(Principal.class);
			final Principal ownerD = createTestNode(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "D")).as(Principal.class);
			final Principal ownerA = createTestNode(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "A")).as(Principal.class);
			final Principal ownerE = createTestNode(StructrTraits.USER, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "E")).as(Principal.class);

			createTestNode(StructrTraits.GROUP, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "zzz"));
			createTestNode(StructrTraits.GROUP, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "aaa"), new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), ownerA));
			createTestNode(StructrTraits.GROUP, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "ttt"), new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), ownerE));
			createTestNode(StructrTraits.GROUP, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "xxx"), new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), ownerC));
			createTestNode(StructrTraits.GROUP, new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "bbb"), new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.OWNER_PROPERTY), ownerD));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> list = (List<NodeInterface>)Scripting.evaluate(new ActionContext(securityContext), null, "${sort(find('Group'), 'owner.name')}", "test");

			// the collection must be sorted according to the name of the owner node
			// with the ownerless node at the end because we're sorting "nulls last"

			assertEquals("Invalid sort() result", "aaa", list.get(0).getProperty(nameKey));
			assertEquals("Invalid sort() result", "xxx", list.get(1).getProperty(nameKey));
			assertEquals("Invalid sort() result", "bbb", list.get(2).getProperty(nameKey));
			assertEquals("Invalid sort() result", "ttt", list.get(3).getProperty(nameKey));
			assertEquals("Invalid sort() result", "zzz", list.get(4).getProperty(nameKey));

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			System.out.println(fex.getMessage());
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testRegexPredicate() {

		try  {

			String type                     = "TestOne";
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

					node.setProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name);

				}

				tx.success();
			}

			try (final Tx tx = app.tx()) {

				assertEquals(4, app.nodeQuery(type).matches(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "(?i).*one.*").getAsList().size());
				assertEquals(1, app.nodeQuery(type).matches(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), ".*One\\-11").getAsList().size());

				tx.success();
			}

		} catch (FrameworkException ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");

		}
	}

	@Test
	public void testFindWithALotOfPredicates() {

		try (final Tx tx = app.tx()) {

			final QueryGroup<NodeInterface> query          = app.nodeQuery(StructrTraits.USER).or();
			final PropertyKey<Iterable<NodeInterface>> key = Traits.of(StructrTraits.USER).key("ownedNodes");
			final NodeInterface dummy                      = app.create(StructrTraits.MAIL_TEMPLATE);
			final List<NodeInterface> nodes                = List.of(dummy);

			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);
			query.key(key, nodes, false);

			query.getAsList();

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	// ----- private methods -----
	private void testPaging(final String type, final int pageSize, final int page, final int number, final int offset, final boolean includeHidden, final PropertyKey sortKey, final boolean sortDesc) throws FrameworkException {

		final Query query = app.nodeQuery(type).sort(sortKey, sortDesc).page(page).pageSize(pageSize);

		if (includeHidden) {
			query.includeHidden();
		}

		final List<NodeInterface> result = query.getAsList();

		long expectedResultCount = (pageSize == 0 || page == 0)
					   ? 0
					   : Math.min(number, pageSize);

		int startIndex = (Math.max(page, 1) - 1) * pageSize;

		logger.info("Result size: {}, expected: {}, start index: {}", new Object[] { result.size(), expectedResultCount, startIndex });
		assertTrue(result.size() == expectedResultCount);


		for (int j = 0; j < expectedResultCount; j++) {

			String expectedName = "TestOne-" + (offset + j + startIndex);
			String gotName      = result.get(j).getProperty(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY));

			System.out.println(expectedName + ", got: " + gotName);
			assertEquals(expectedName, gotName);

		}
	}
}
