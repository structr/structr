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
package org.structr.test.core.script;

import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.graph.search.DefaultSortOrder;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.export.StructrSchema;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * The sort() function, which sorts a collection in memory: by one or several keys, each of which can
 * be a path through related objects, optionally in natural order and with nulls first or last.
 *
 * Each expression is checked in StructrScript and in JavaScript. The people of a test are created in
 * an order that differs from every expected order, so a result that is not sorted at all is detected.
 */
public class SortFunctionTest extends SortTestBase {

	// ----- behaviour that existed before multiple keys, natural order and nulls became options -----

	@Test
	public void testSingleKeyAscendingAndDescending() {

		createSortSchema();
		createPeople(person("b"), person("a"), person("c"));

		assertSorted("Ascending by name",  List.of("a", "b", "c"), "sort(find('Person'), 'name')",        "$.sort($.find('Person'), 'name')");
		assertSorted("Ascending explicit", List.of("a", "b", "c"), "sort(find('Person'), 'name', false)", "$.sort($.find('Person'), 'name', false)");
		assertSorted("Descending by name", List.of("c", "b", "a"), "sort(find('Person'), 'name', true)",  "$.sort($.find('Person'), 'name', true)");
	}

	@Test
	public void testNameIsTheDefaultKey() {

		createSortSchema();
		createPeople(person("b"), person("a"), person("c"));

		assertSorted("Without a key, sort() must sort by name",           List.of("a", "b", "c"), "sort(find('Person'))",             "$.sort($.find('Person'))");
		assertSorted("A null key must be the default key, here descending", List.of("c", "b", "a"), "sort(find('Person'), null, true)", "$.sort($.find('Person'), null, true)");
	}

	@Test
	public void testNumbersAreSortedByValue() {

		createSortSchema();
		createPeople(person("p30").age(30), person("p5").age(5), person("p100").age(100));

		assertSorted("Numbers must be sorted by value, not as text", List.of("p5", "p30", "p100"), "sort(find('Person'), 'age')",       "$.sort($.find('Person'), 'age')");
		assertSorted("Numbers descending",                           List.of("p100", "p30", "p5"), "sort(find('Person'), 'age', true)", "$.sort($.find('Person'), 'age', true)");
	}

	@Test
	public void testNullsAreLastAscendingAndFirstDescending() {

		createSortSchema();
		createPeople(person("x").nick("x"), person("none"), person("a").nick("a"));

		assertSorted("Without a value, a node must come last in ascending order",  List.of("a", "x", "none"), "sort(find('Person'), 'nick')",       "$.sort($.find('Person'), 'nick')");
		assertSorted("Without a value, a node must come first in descending order", List.of("none", "x", "a"), "sort(find('Person'), 'nick', true)", "$.sort($.find('Person'), 'nick', true)");
	}

	@Test
	public void testStringsAreSortedByCharacterCode() {

		createSortSchema();

		assertSorted("Without natural order, upper case letters come before all lower case letters", List.of("A", "B", "a", "b"), "sort(merge('b', 'A', 'a', 'B'))",       "$.sort(['b', 'A', 'a', 'B'])");
		assertSorted("Descending strings",                                                         List.of("b", "a", "B", "A"), "sort(merge('b', 'A', 'a', 'B'), null, true)", "$.sort(['b', 'A', 'a', 'B'], null, true)");
	}

	@Test
	public void testPathKeyWithMissingRelatedObject() {

		createSortSchema();
		createPeople(person("p2").team("Beta"), person("p0"), person("p1").team("Alpha"));

		assertSorted("Sorting by a path, a node without related object must come last",       List.of("p1", "p2", "p0"), "sort(find('Person'), 'team.name')",       "$.sort($.find('Person'), 'team.name')");
		assertSorted("Sorting by a path descending, a node without related object comes first", List.of("p0", "p2", "p1"), "sort(find('Person'), 'team.name', true)", "$.sort($.find('Person'), 'team.name', true)");
	}

	@Test
	public void testTwoPlainKeys() {

		createSortSchema();
		createPeople(person("mz").lastName("Müller").firstName("Zoe"), person("ab").lastName("Adams").firstName("Bob"), person("ma").lastName("Müller").firstName("Anna"));

		assertSorted("Two keys, both ascending",            List.of("ab", "ma", "mz"), "sort(find('Person'), 'lastName', false, 'firstName', false)", "$.sort($.find('Person'), 'lastName', false, 'firstName', false)");
		assertSorted("Two keys, the second one descending", List.of("ab", "mz", "ma"), "sort(find('Person'), 'lastName', false, 'firstName', true)",  "$.sort($.find('Person'), 'lastName', false, 'firstName', true)");
		assertSorted("Two keys, the first one descending",  List.of("ma", "mz", "ab"), "sort(find('Person'), 'lastName', true, 'firstName', false)",  "$.sort($.find('Person'), 'lastName', true, 'firstName', false)");
	}

	// ----- several sort keys -----

	@Test
	public void testTwoKeysInAllCombinationsOfOrder() {

		createSortSchema();
		createPeople(person("mz").lastName("Müller").firstName("Zoe"), person("bb").lastName("Adams").firstName("Bob"), person("ma").lastName("Müller").firstName("Anna"), person("aa").lastName("Adams").firstName("Amy"));

		assertSorted("Ascending, ascending",   List.of("aa", "bb", "ma", "mz"), "sort(find('Person'), 'lastName', false, 'firstName', false)", "$.sort($.find('Person'), 'lastName', false, 'firstName', false)");
		assertSorted("Ascending, descending",  List.of("bb", "aa", "mz", "ma"), "sort(find('Person'), 'lastName', false, 'firstName', true)",  "$.sort($.find('Person'), 'lastName', false, 'firstName', true)");
		assertSorted("Descending, ascending",  List.of("ma", "mz", "aa", "bb"), "sort(find('Person'), 'lastName', true, 'firstName', false)",  "$.sort($.find('Person'), 'lastName', true, 'firstName', false)");
		assertSorted("Descending, descending", List.of("mz", "ma", "bb", "aa"), "sort(find('Person'), 'lastName', true, 'firstName', true)",   "$.sort($.find('Person'), 'lastName', true, 'firstName', true)");
	}

	@Test
	public void testOrderOfTheLastKeyMayBeOmitted() {

		createSortSchema();
		createPeople(person("mz").lastName("Müller").firstName("Zoe"), person("ab").lastName("Adams").firstName("Bob"), person("ma").lastName("Müller").firstName("Anna"));

		assertSorted("Without an order, the last key must be ascending", List.of("ab", "ma", "mz"), "sort(find('Person'), 'lastName', false, 'firstName')", "$.sort($.find('Person'), 'lastName', false, 'firstName')");
		assertSorted("Only the omitted order is ascending",                List.of("ma", "mz", "ab"), "sort(find('Person'), 'lastName', true, 'firstName')",  "$.sort($.find('Person'), 'lastName', true, 'firstName')");
	}

	@Test
	public void testThreeKeys() {

		createSortSchema();
		createPeople(
			person("m-a-40").lastName("Müller").firstName("Anna").age(40),
			person("a-b-30").lastName("Adams").firstName("Bob").age(30),
			person("m-a-20").lastName("Müller").firstName("Anna").age(20),
			person("m-z-10").lastName("Müller").firstName("Zoe").age(10)
		);

		assertSorted("The third key must decide only between nodes equal in the first two", List.of("a-b-30", "m-a-20", "m-a-40", "m-z-10"), "sort(find('Person'), 'lastName', false, 'firstName', false, 'age', false)", "$.sort($.find('Person'), 'lastName', false, 'firstName', false, 'age', false)");
		assertSorted("The third key descending",                                              List.of("a-b-30", "m-a-40", "m-a-20", "m-z-10"), "sort(find('Person'), 'lastName', false, 'firstName', false, 'age', true)",  "$.sort($.find('Person'), 'lastName', false, 'firstName', false, 'age', true)");
	}

	@Test
	public void testNumberAsSecondKey() {

		createSortSchema();
		createPeople(person("b100").lastName("B").age(100), person("a5").lastName("A").age(5), person("b20").lastName("B").age(20), person("a30").lastName("A").age(30));

		assertSorted("A number as second key must be sorted by value", List.of("a5", "a30", "b20", "b100"), "sort(find('Person'), 'lastName', false, 'age', false)", "$.sort($.find('Person'), 'lastName', false, 'age', false)");
		assertSorted("A number as second key descending",               List.of("a30", "a5", "b100", "b20"), "sort(find('Person'), 'lastName', false, 'age', true)",  "$.sort($.find('Person'), 'lastName', false, 'age', true)");
	}

	@Test
	public void testNodesWithoutValueInTheFirstKeyAreSortedByTheNextKey() {

		createSortSchema();
		createPeople(person("c"), person("x").nick("x"), person("a"), person("b"));

		assertSorted("Nodes without a value in the first key must be sorted by the second key", List.of("x", "a", "b", "c"), "sort(find('Person'), 'nick', false, 'name', false)", "$.sort($.find('Person'), 'nick', false, 'name', false)");
		assertSorted("The same with the second key descending",                                   List.of("x", "c", "b", "a"), "sort(find('Person'), 'nick', false, 'name', true)",  "$.sort($.find('Person'), 'nick', false, 'name', true)");
		assertSorted("The same with the first key descending",                                    List.of("a", "b", "c", "x"), "sort(find('Person'), 'nick', true, 'name', false)",  "$.sort($.find('Person'), 'nick', true, 'name', false)");
	}

	// ----- paths in sort keys -----

	@Test
	public void testPathAsSecondKey() {

		createSortSchema();
		createPeople(person("m-beta").lastName("Müller").team("Beta"), person("a-beta").lastName("Adams").team("Beta"), person("m-alpha").lastName("Müller").team("Alpha"), person("m-none").lastName("Müller"));

		assertSorted("A path as second key",            List.of("a-beta", "m-alpha", "m-beta", "m-none"), "sort(find('Person'), 'lastName', false, 'team.name', false)", "$.sort($.find('Person'), 'lastName', false, 'team.name', false)");
		assertSorted("A path as second key descending", List.of("a-beta", "m-none", "m-beta", "m-alpha"), "sort(find('Person'), 'lastName', false, 'team.name', true)",  "$.sort($.find('Person'), 'lastName', false, 'team.name', true)");
	}

	@Test
	public void testPathAsFirstKeyWithFurtherKeys() {

		createSortSchema();
		createPeople(person("p3").team("Beta"), person("p2").team("Alpha"), person("p4"), person("p1").team("Alpha"), person("p0"));

		// before several keys were supported with paths, a path as first key ignored all further keys
		assertSorted("Keys after a path must still decide",        List.of("p1", "p2", "p3", "p0", "p4"), "sort(find('Person'), 'team.name', false, 'name', false)", "$.sort($.find('Person'), 'team.name', false, 'name', false)");
		assertSorted("Keys after a path, the second descending",   List.of("p2", "p1", "p3", "p4", "p0"), "sort(find('Person'), 'team.name', false, 'name', true)",  "$.sort($.find('Person'), 'team.name', false, 'name', true)");
		assertSorted("Keys after a path, the first descending",    List.of("p0", "p4", "p3", "p1", "p2"), "sort(find('Person'), 'team.name', true, 'name', false)",  "$.sort($.find('Person'), 'team.name', true, 'name', false)");
	}

	@Test
	public void testTwoPaths() {

		createSortSchema();
		createPeople(
			person("alpha-z").team("Alpha").projects("Z"),
			person("beta-a").team("Beta").projects("A"),
			person("alpha-a").team("Alpha").projects("A", "Y"),
			person("alpha-none").team("Alpha")
		);

		assertSorted("Two paths, the second through a collection",            List.of("alpha-a", "alpha-z", "alpha-none", "beta-a"), "sort(find('Person'), 'team.name', false, 'projects.name', false)", "$.sort($.find('Person'), 'team.name', false, 'projects.name', false)");
		assertSorted("Two paths, descending uses the largest value of a node", List.of("alpha-none", "alpha-z", "alpha-a", "beta-a"), "sort(find('Person'), 'team.name', false, 'projects.name', true)",  "$.sort($.find('Person'), 'team.name', false, 'projects.name', true)");
	}

	// ----- sort keys as objects -----

	@Test
	public void testObjectsInAllCombinationsOfOrder() {

		createSortSchema();
		createPeople(person("mz").lastName("Müller").firstName("Zoe"), person("bb").lastName("Adams").firstName("Bob"), person("ma").lastName("Müller").firstName("Anna"), person("aa").lastName("Adams").firstName("Amy"));

		assertSorted("Ascending, ascending",   List.of("aa", "bb", "ma", "mz"), "sort(find('Person'), " + json("{'key': 'lastName'}") + ", " + json("{'key': 'firstName'}") + ")",                                             "$.sort($.find('Person'), { key: 'lastName' }, { key: 'firstName' })");
		assertSorted("Ascending, descending",  List.of("bb", "aa", "mz", "ma"), "sort(find('Person'), " + json("{'key': 'lastName'}") + ", " + json("{'key': 'firstName', 'descending': true}") + ")",                       "$.sort($.find('Person'), { key: 'lastName' }, { key: 'firstName', descending: true })");
		assertSorted("Descending, ascending",  List.of("ma", "mz", "aa", "bb"), "sort(find('Person'), " + json("{'key': 'lastName', 'descending': true}") + ", " + json("{'key': 'firstName'}") + ")",                       "$.sort($.find('Person'), { key: 'lastName', descending: true }, { key: 'firstName' })");
		assertSorted("Descending, descending", List.of("mz", "ma", "bb", "aa"), "sort(find('Person'), " + json("{'key': 'lastName', 'descending': true}") + ", " + json("{'key': 'firstName', 'descending': true}") + ")", "$.sort($.find('Person'), { key: 'lastName', descending: true }, { key: 'firstName', descending: true })");
		assertSorted("Explicitly ascending",   List.of("aa", "bb", "ma", "mz"), "sort(find('Person'), " + json("{'key': 'lastName', 'descending': false}") + ", " + json("{'key': 'firstName', 'descending': false}") + ")", "$.sort($.find('Person'), { key: 'lastName', descending: false }, { key: 'firstName', descending: false })");
	}

	@Test
	public void testSingleObject() {

		createSortSchema();
		createPeople(person("mz").lastName("Müller").firstName("Zoe"), person("bb").lastName("Adams").firstName("Bob"), person("ma").lastName("Müller").firstName("Anna"), person("aa").lastName("Adams").firstName("Amy"));

		assertSorted("A single object",            List.of("aa", "ma", "bb", "mz"), "sort(find('Person'), " + json("{'key': 'firstName'}") + ")",                     "$.sort($.find('Person'), { key: 'firstName' })");
		assertSorted("A single object descending", List.of("mz", "bb", "ma", "aa"), "sort(find('Person'), " + json("{'key': 'firstName', 'descending': true}") + ")", "$.sort($.find('Person'), { key: 'firstName', descending: true })");
	}

	@Test
	public void testObjectWithoutKeySortsByName() {

		createSortSchema();
		createPeople(person("Item 10"), person("item 2"), person("Item 1"));

		assertSorted("An empty object sorts by name",           List.of("Item 1", "Item 10", "item 2"), "sort(find('Person'), " + json("{}") + ")",                   "$.sort($.find('Person'), {})");
		assertSorted("An object without key, descending",       List.of("item 2", "Item 10", "Item 1"), "sort(find('Person'), " + json("{'descending': true}") + ")", "$.sort($.find('Person'), { descending: true })");
		assertSorted("An object without key, natural",          List.of("Item 1", "item 2", "Item 10"), "sort(find('Person'), " + json("{'natural': true}") + ")",    "$.sort($.find('Person'), { natural: true })");
	}

	@Test
	public void testThreeObjects() {

		createSortSchema();
		createPeople(
			person("m-a-40").lastName("Müller").firstName("Anna").age(40),
			person("a-b-30").lastName("Adams").firstName("Bob").age(30),
			person("m-a-20").lastName("Müller").firstName("Anna").age(20),
			person("m-z-10").lastName("Müller").firstName("Zoe").age(10)
		);

		assertSorted("The third object decides only between nodes equal in the first two", List.of("a-b-30", "m-a-20", "m-a-40", "m-z-10"), "sort(find('Person'), " + json("{'key': 'lastName'}") + ", " + json("{'key': 'firstName'}") + ", " + json("{'key': 'age'}") + ")",                     "$.sort($.find('Person'), { key: 'lastName' }, { key: 'firstName' }, { key: 'age' })");
		assertSorted("The third object descending",                                          List.of("a-b-30", "m-a-40", "m-a-20", "m-z-10"), "sort(find('Person'), " + json("{'key': 'lastName'}") + ", " + json("{'key': 'firstName'}") + ", " + json("{'key': 'age', 'descending': true}") + ")", "$.sort($.find('Person'), { key: 'lastName' }, { key: 'firstName' }, { key: 'age', descending: true })");
	}

	@Test
	public void testObjectsWithPaths() {

		createSortSchema();
		createPeople(person("p3").team("Beta"), person("p2").team("Alpha"), person("p4"), person("p1").team("Alpha"), person("p0"));

		assertSorted("A path and a name",              List.of("p1", "p2", "p3", "p0", "p4"), "sort(find('Person'), " + json("{'key': 'team.name'}") + ", " + json("{'key': 'name'}") + ")",                     "$.sort($.find('Person'), { key: 'team.name' }, { key: 'name' })");
		assertSorted("A path descending, then a name", List.of("p0", "p4", "p3", "p1", "p2"), "sort(find('Person'), " + json("{'key': 'team.name', 'descending': true}") + ", " + json("{'key': 'name'}") + ")", "$.sort($.find('Person'), { key: 'team.name', descending: true }, { key: 'name' })");
		assertSorted("A path, then a name descending", List.of("p2", "p1", "p3", "p4", "p0"), "sort(find('Person'), " + json("{'key': 'team.name'}") + ", " + json("{'key': 'name', 'descending': true}") + ")", "$.sort($.find('Person'), { key: 'team.name' }, { key: 'name', descending: true })");
	}

	@Test
	public void testTwoPathsAsObjects() {

		createSortSchema();
		createPeople(
			person("alpha-z").team("Alpha").projects("Z"),
			person("beta-a").team("Beta").projects("A"),
			person("alpha-a").team("Alpha").projects("A", "Y"),
			person("alpha-none").team("Alpha")
		);

		assertSorted("Two paths, the second through a collection",            List.of("alpha-a", "alpha-z", "alpha-none", "beta-a"), "sort(find('Person'), " + json("{'key': 'team.name'}") + ", " + json("{'key': 'projects.name'}") + ")",                     "$.sort($.find('Person'), { key: 'team.name' }, { key: 'projects.name' })");
		assertSorted("Two paths, descending uses the largest value of a node", List.of("alpha-none", "alpha-z", "alpha-a", "beta-a"), "sort(find('Person'), " + json("{'key': 'team.name'}") + ", " + json("{'key': 'projects.name', 'descending': true}") + ")", "$.sort($.find('Person'), { key: 'team.name' }, { key: 'projects.name', descending: true })");
	}

	// ----- natural order -----

	@Test
	public void testNaturalOrderOfStrings() {

		createSortSchema();

		assertSorted("Natural order keeps upper and lower case together",  List.of("a", "A", "b", "B"),           "sort(merge('b', 'A', 'a', 'B'), " + json("{'natural': true}") + ")",                     "$.sort(['b', 'A', 'a', 'B'], { natural: true })");
		assertSorted("Natural order descending",                            List.of("B", "b", "A", "a"),           "sort(merge('b', 'A', 'a', 'B'), " + json("{'natural': true, 'descending': true}") + ")", "$.sort(['b', 'A', 'a', 'B'], { natural: true, descending: true })");
		assertSorted("Descending without natural order",                    List.of("b", "a", "B", "A"),           "sort(merge('b', 'A', 'a', 'B'), " + json("{'descending': true}") + ")",                  "$.sort(['b', 'A', 'a', 'B'], { descending: true })");
		assertSorted("Natural order compares numbers by value",             List.of("Item 1", "Item 2", "Item 10"), "sort(merge('Item 10', 'Item 1', 'Item 2'), " + json("{'natural': true}") + ")",         "$.sort(['Item 10', 'Item 1', 'Item 2'], { natural: true })");
		assertSorted("Without natural order, numbers are compared as text", List.of("Item 1", "Item 10", "Item 2"), "sort(merge('Item 10', 'Item 1', 'Item 2'))",                                            "$.sort(['Item 10', 'Item 1', 'Item 2'])");
		assertSorted("A key does not apply to strings",                     List.of("a", "A", "b", "B"),           "sort(merge('b', 'A', 'a', 'B'), " + json("{'key': 'name', 'natural': true}") + ")",      "$.sort(['b', 'A', 'a', 'B'], { key: 'name', natural: true })");
		assertSorted("For strings, only the first object applies",          List.of("a", "A", "b", "B"),           "sort(merge('b', 'A', 'a', 'B'), " + json("{'natural': true}") + ", " + json("{'descending': true}") + ")", "$.sort(['b', 'A', 'a', 'B'], { natural: true }, { descending: true })");
	}

	@Test
	public void testNaturalOrderOfNodes() {

		createSortSchema();
		createPeople(person("Item 10"), person("item 2"), person("Item 1"), person("item 20"));

		assertSorted("Natural order of a key",            List.of("Item 1", "item 2", "Item 10", "item 20"), "sort(find('Person'), " + json("{'key': 'name', 'natural': true}") + ")",                     "$.sort($.find('Person'), { key: 'name', natural: true })");
		assertSorted("Natural order of a key descending", List.of("item 20", "Item 10", "item 2", "Item 1"), "sort(find('Person'), " + json("{'key': 'name', 'natural': true, 'descending': true}") + ")", "$.sort($.find('Person'), { key: 'name', natural: true, descending: true })");
		assertSorted("Explicitly not in natural order",   List.of("Item 1", "Item 10", "item 2", "item 20"), "sort(find('Person'), " + json("{'key': 'name', 'natural': false}") + ")",                    "$.sort($.find('Person'), { key: 'name', natural: false })");
	}

	@Test
	public void testNaturalOrderDependsOnTheLocale() {

		createSortSchema();
		createPeople(person("Zebra"), person("Äpfel"), person("Bär"), person("Apfel"));

		assertSorted("In German, umlauts belong to their base letter", List.of("Apfel", "Äpfel", "Bär", "Zebra"), "sort(find('Person'), " + json("{'key': 'name', 'natural': true}") + ")", "$.sort($.find('Person'), { key: 'name', natural: true })", Locale.GERMAN);
		assertSorted("In Swedish, ä comes after z",                    List.of("Apfel", "Bär", "Zebra", "Äpfel"), "sort(find('Person'), " + json("{'key': 'name', 'natural': true}") + ")", "$.sort($.find('Person'), { key: 'name', natural: true })", Locale.of("sv"));
		assertSorted("Without natural order, the locale is ignored",   List.of("Apfel", "Bär", "Zebra", "Äpfel"), "sort(find('Person'), 'name')",                                            "$.sort($.find('Person'), 'name')",                         Locale.GERMAN);
	}

	@Test
	public void testNaturalOrderPerKey() {

		createSortSchema();
		createPeople(person("b10").lastName("b").firstName("Item 10"), person("A2").lastName("A").firstName("Item 2"), person("b2").lastName("b").firstName("Item 2"), person("a1").lastName("a").firstName("Item 1"));

		assertSorted("Both keys in natural order",       List.of("a1", "A2", "b2", "b10"), "sort(find('Person'), " + json("{'key': 'lastName', 'natural': true}") + ", " + json("{'key': 'firstName', 'natural': true}") + ")", "$.sort($.find('Person'), { key: 'lastName', natural: true }, { key: 'firstName', natural: true })");
		assertSorted("Only the first key natural",       List.of("a1", "A2", "b10", "b2"), "sort(find('Person'), " + json("{'key': 'lastName', 'natural': true}") + ", " + json("{'key': 'firstName'}") + ")",                   "$.sort($.find('Person'), { key: 'lastName', natural: true }, { key: 'firstName' })");
		assertSorted("Only the second key natural",      List.of("A2", "a1", "b2", "b10"), "sort(find('Person'), " + json("{'key': 'lastName'}") + ", " + json("{'key': 'firstName', 'natural': true}") + ")",                   "$.sort($.find('Person'), { key: 'lastName' }, { key: 'firstName', natural: true })");
		assertSorted("Natural, the second key descending", List.of("a1", "A2", "b10", "b2"), "sort(find('Person'), " + json("{'key': 'lastName', 'natural': true}") + ", " + json("{'key': 'firstName', 'natural': true, 'descending': true}") + ")", "$.sort($.find('Person'), { key: 'lastName', natural: true }, { key: 'firstName', natural: true, descending: true })");
		assertSorted("Neither key natural",              List.of("A2", "a1", "b10", "b2"), "sort(find('Person'), " + json("{'key': 'lastName'}") + ", " + json("{'key': 'firstName'}") + ")",                                     "$.sort($.find('Person'), { key: 'lastName' }, { key: 'firstName' })");
	}

	@Test
	public void testNaturalOrderOfPaths() {

		createSortSchema();
		createPeople(person("p10").team("Team 10"), person("p2").team("team 2"), person("many").projects("Project 10", "Project 3"), person("p1").team("Team 1"));

		assertSorted("Natural order of a path", List.of("p1", "p2", "p10", "many"), "sort(find('Person'), " + json("{'key': 'team.name', 'natural': true}") + ")", "$.sort($.find('Person'), { key: 'team.name', natural: true })");

		createPeople(person("other").projects("Project 9"));

		// the smallest value of a node through a collection must also be chosen in natural order: Project 3, not Project 10
		assertSorted("Natural order chooses the value of a node through a collection", List.of("many", "other", "p1", "p2", "p10"), "sort(find('Person'), " + json("{'key': 'projects.name', 'natural': true}") + ", " + json("{'key': 'name', 'natural': true}") + ")", "$.sort($.find('Person'), { key: 'projects.name', natural: true }, { key: 'name', natural: true })");
		assertSorted("Without natural order, Project 10 is the smallest value",         List.of("many", "other", "p1", "p10", "p2"), "sort(find('Person'), " + json("{'key': 'projects.name'}") + ", " + json("{'key': 'name'}") + ")",                                   "$.sort($.find('Person'), { key: 'projects.name' }, { key: 'name' })");
	}

	@Test
	public void testNaturalOrderDoesNotChangeNumbers() {

		createSortSchema();
		createPeople(person("p30").age(30), person("p5").age(5), person("p100").age(100));

		assertSorted("Numbers must be sorted by value in natural order too", List.of("p5", "p30", "p100"), "sort(find('Person'), " + json("{'key': 'age', 'natural': true}") + ")", "$.sort($.find('Person'), { key: 'age', natural: true })");
	}

	// ----- position of nulls -----

	@Test
	public void testNullsFirstAndLastInBothOrders() {

		createSortSchema();
		createPeople(person("x").nick("x"), person("none"), person("a").nick("a"));

		assertSorted("Nulls first, ascending",  List.of("none", "a", "x"), "sort(find('Person'), " + json("{'key': 'nick', 'nulls': 'first'}") + ")",                     "$.sort($.find('Person'), { key: 'nick', nulls: 'first' })");
		assertSorted("Nulls first, descending", List.of("none", "x", "a"), "sort(find('Person'), " + json("{'key': 'nick', 'nulls': 'first', 'descending': true}") + ")", "$.sort($.find('Person'), { key: 'nick', nulls: 'first', descending: true })");
		assertSorted("Nulls last, ascending",   List.of("a", "x", "none"), "sort(find('Person'), " + json("{'key': 'nick', 'nulls': 'last'}") + ")",                      "$.sort($.find('Person'), { key: 'nick', nulls: 'last' })");
		assertSorted("Nulls last, descending",  List.of("x", "a", "none"), "sort(find('Person'), " + json("{'key': 'nick', 'nulls': 'last', 'descending': true}") + ")",  "$.sort($.find('Person'), { key: 'nick', nulls: 'last', descending: true })");
		assertSorted("Default, descending",     List.of("none", "x", "a"), "sort(find('Person'), " + json("{'key': 'nick', 'descending': true}") + ")",                   "$.sort($.find('Person'), { key: 'nick', descending: true })");
	}

	@Test
	public void testNullsWithSeveralNodesWithoutValue() {

		createSortSchema();
		createPeople(person("n2"), person("x").nick("x"), person("n1"), person("a").nick("a"), person("n3"));

		assertSorted("Nulls first, the nodes without value sorted by the second key", List.of("n1", "n2", "n3", "a", "x"), "sort(find('Person'), " + json("{'key': 'nick', 'nulls': 'first'}") + ", " + json("{'key': 'name'}") + ")",                     "$.sort($.find('Person'), { key: 'nick', nulls: 'first' }, { key: 'name' })");
		assertSorted("Nulls last, the second key descending",                         List.of("a", "x", "n3", "n2", "n1"), "sort(find('Person'), " + json("{'key': 'nick', 'nulls': 'last'}") + ", " + json("{'key': 'name', 'descending': true}") + ")", "$.sort($.find('Person'), { key: 'nick', nulls: 'last' }, { key: 'name', descending: true })");
	}

	@Test
	public void testNullsPerKey() {

		createSortSchema();
		createPeople(person("m-none").lastName("Müller"), person("m-b").lastName("Müller").nick("b"), person("none-a").nick("a"), person("m-a").lastName("Müller").nick("a"));

		assertSorted("Nulls first for both keys",                List.of("none-a", "m-none", "m-a", "m-b"), "sort(find('Person'), " + json("{'key': 'lastName', 'nulls': 'first'}") + ", " + json("{'key': 'nick', 'nulls': 'first'}") + ")",                      "$.sort($.find('Person'), { key: 'lastName', nulls: 'first' }, { key: 'nick', nulls: 'first' })");
		assertSorted("Nulls first, then last",                   List.of("none-a", "m-a", "m-b", "m-none"), "sort(find('Person'), " + json("{'key': 'lastName', 'nulls': 'first'}") + ", " + json("{'key': 'nick', 'nulls': 'last'}") + ")",                       "$.sort($.find('Person'), { key: 'lastName', nulls: 'first' }, { key: 'nick', nulls: 'last' })");
		assertSorted("Nulls last for the second key only",       List.of("m-a", "m-b", "m-none", "none-a"), "sort(find('Person'), " + json("{'key': 'lastName'}") + ", " + json("{'key': 'nick', 'nulls': 'last'}") + ")",                                          "$.sort($.find('Person'), { key: 'lastName' }, { key: 'nick', nulls: 'last' })");
		assertSorted("Nulls first for the first key only",       List.of("none-a", "m-a", "m-b", "m-none"), "sort(find('Person'), " + json("{'key': 'lastName', 'nulls': 'first'}") + ", " + json("{'key': 'nick'}") + ")",                                         "$.sort($.find('Person'), { key: 'lastName', nulls: 'first' }, { key: 'nick' })");
		assertSorted("Nulls first with the second key descending", List.of("m-none", "m-b", "m-a", "none-a"), "sort(find('Person'), " + json("{'key': 'lastName'}") + ", " + json("{'key': 'nick', 'descending': true, 'nulls': 'first'}") + ")",                   "$.sort($.find('Person'), { key: 'lastName' }, { key: 'nick', descending: true, nulls: 'first' })");
	}

	@Test
	public void testNullsWithPaths() {

		createSortSchema();
		createPeople(person("beta").team("Beta").projects("B"), person("none"), person("alpha").team("Alpha").projects("A"), person("empty").team("Gamma"));

		assertSorted("Nulls first with a path to a missing related node", List.of("none", "alpha", "beta", "empty"), "sort(find('Person'), " + json("{'key': 'team.name', 'nulls': 'first'}") + ")",                    "$.sort($.find('Person'), { key: 'team.name', nulls: 'first' })");
		assertSorted("Nulls last with a path, descending",                List.of("empty", "beta", "alpha", "none"), "sort(find('Person'), " + json("{'key': 'team.name', 'nulls': 'last', 'descending': true}") + ")", "$.sort($.find('Person'), { key: 'team.name', nulls: 'last', descending: true })");

		// an empty collection on the path has no value either
		assertSorted("Nulls first with a path through an empty collection", List.of("empty", "none", "alpha", "beta"), "sort(find('Person'), " + json("{'key': 'projects.name', 'nulls': 'first'}") + ", " + json("{'key': 'name'}") + ")", "$.sort($.find('Person'), { key: 'projects.name', nulls: 'first' }, { key: 'name' })");
		assertSorted("Nulls last with a path through an empty collection",  List.of("alpha", "beta", "empty", "none"), "sort(find('Person'), " + json("{'key': 'projects.name', 'nulls': 'last'}") + ", " + json("{'key': 'name'}") + ")",  "$.sort($.find('Person'), { key: 'projects.name', nulls: 'last' }, { key: 'name' })");
	}

	@Test
	public void testNullsInCollectionsOfStrings() {

		createSortSchema();

		// StructrScript cannot create a collection with null elements, so only JavaScript
		assertSortedJavaScript("Without an option, null comes last in ascending order",  Arrays.asList("a", "b", null), "$.sort(['b', null, 'a'])");
		assertSortedJavaScript("Without an option, null comes first in descending order", Arrays.asList(null, "b", "a"), "$.sort(['b', null, 'a'], { descending: true })");
		assertSortedJavaScript("Nulls first",                                              Arrays.asList(null, "a", "b"), "$.sort(['b', null, 'a'], { nulls: 'first' })");
		assertSortedJavaScript("Nulls last, descending",                                   Arrays.asList("b", "a", null), "$.sort(['b', null, 'a'], { descending: true, nulls: 'last' })");
		assertSortedJavaScript("A collection that starts with null",                       Arrays.asList("a", "b", null), "$.sort([null, 'b', 'a'])");
		assertSortedJavaScript("The short form",                                           Arrays.asList(null, "b", "a"), "$.sort(['b', null, 'a'], null, true)");
	}

	@Test
	public void testNullElementsInCollectionsOfNodes() {

		createSortSchema();
		createPeople(person("b"), person("a"));

		// StructrScript cannot create a collection with null elements, so only JavaScript
		assertSortedJavaScript("A null element has no value for any key and comes last in ascending order", Arrays.asList("a", "b", null), "$.sort([null, ...$.find('Person')], 'name')");
		assertSortedJavaScript("A null element comes first in descending order",                             Arrays.asList(null, "b", "a"), "$.sort([...$.find('Person'), null], { key: 'name', descending: true })");
		assertSortedJavaScript("A null element with nulls first",                                            Arrays.asList(null, "a", "b"), "$.sort([...$.find('Person'), null], { key: 'name', nulls: 'first' })");
	}

	@Test
	public void testCollectionOfNullsIsReturnedAsItIs() {

		createSortSchema();

		// StructrScript cannot create a collection with null elements, so only JavaScript
		assertSortedJavaScript("A collection of nulls has nothing to sort", Arrays.asList(null, null), "$.sort([null, null])");
		assertSortedJavaScript("A collection of nulls with a sort key",     Arrays.asList(null, null), "$.sort([null, null], { key: 'name' })");
	}

	// ----- all options together -----

	@Test
	public void testNaturalOrderAndNullsTogether() {

		createSortSchema();
		createPeople(person("p10").nick("Item 10"), person("none"), person("p2").nick("item 2"), person("p1").nick("Item 1"));

		assertSorted("Natural order with nulls first",           List.of("none", "p1", "p2", "p10"), "sort(find('Person'), " + json("{'key': 'nick', 'natural': true, 'nulls': 'first'}") + ")",                     "$.sort($.find('Person'), { key: 'nick', natural: true, nulls: 'first' })");
		assertSorted("Natural order descending with nulls last", List.of("p10", "p2", "p1", "none"), "sort(find('Person'), " + json("{'key': 'nick', 'natural': true, 'nulls': 'last', 'descending': true}") + ")", "$.sort($.find('Person'), { key: 'nick', natural: true, nulls: 'last', descending: true })");
		assertSortedJavaScript("Natural order and nulls for strings", Arrays.asList(null, "Item 1", "item 2", "Item 10"), "$.sort(['Item 10', null, 'item 2', 'Item 1'], { natural: true, nulls: 'first' })");
	}

	@Test
	public void testAllOptionsWithSeveralKeysAndPaths() {

		createSortSchema();
		createPeople(
			person("t10-b").team("Team 10").nick("b"),
			person("t2-none").team("team 2"),
			person("t2-a").team("team 2").nick("a"),
			person("none-c").nick("c"),
			person("t10-none").team("Team 10")
		);

		assertSorted("Path in natural order with nulls first, then a key with nulls first", List.of("none-c", "t2-none", "t2-a", "t10-none", "t10-b"), "sort(find('Person'), " + json("{'key': 'team.name', 'natural': true, 'nulls': 'first'}") + ", " + json("{'key': 'nick', 'nulls': 'first'}") + ")", "$.sort($.find('Person'), { key: 'team.name', natural: true, nulls: 'first' }, { key: 'nick', nulls: 'first' })");
		assertSorted("Path in natural order descending, then a key with nulls last",          List.of("t10-b", "t10-none", "t2-a", "t2-none", "none-c"), "sort(find('Person'), " + json("{'key': 'team.name', 'natural': true, 'descending': true, 'nulls': 'last'}") + ", " + json("{'key': 'nick', 'nulls': 'last'}") + ")", "$.sort($.find('Person'), { key: 'team.name', natural: true, descending: true, nulls: 'last' }, { key: 'nick', nulls: 'last' })");
	}

	// ----- errors -----

	@Test
	public void testUnknownOptionIsRejected() {

		createSortSchema();
		createPeople(person("a"));

		assertFails("A misspelled option",       "sort(find('Person'), " + json("{'key': 'name', 'naturel': true}") + ")", "$.sort($.find('Person'), { key: 'name', naturel: true })");
		assertFails("A misspelled key option",   "sort(find('Person'), " + json("{'kye': 'name'}") + ")",                  "$.sort($.find('Person'), { kye: 'name' })");
		assertFails("A misspelled order option", "sort(find('Person'), " + json("{'key': 'name', 'decs': true}") + ")",    "$.sort($.find('Person'), { key: 'name', decs: true })");
	}

	@Test
	public void testDescIsAnAliasOfDescending() {

		createSortSchema();
		createPeople(person("b"), person("a"), person("c"));

		assertSorted("desc instead of descending",          List.of("c", "b", "a"), "sort(find('Person'), " + json("{'key': 'name', 'desc': true}") + ")",                       "$.sort($.find('Person'), { key: 'name', desc: true })");
		assertSorted("desc false",                          List.of("a", "b", "c"), "sort(find('Person'), " + json("{'key': 'name', 'desc': false}") + ")",                      "$.sort($.find('Person'), { key: 'name', desc: false })");
		assertSorted("desc and descending with one value",  List.of("c", "b", "a"), "sort(find('Person'), " + json("{'key': 'name', 'desc': true, 'descending': true}") + ")",   "$.sort($.find('Person'), { key: 'name', desc: true, descending: true })");
		assertFails("desc and descending contradict each other", "sort(find('Person'), " + json("{'key': 'name', 'desc': true, 'descending': false}") + ")", "$.sort($.find('Person'), { key: 'name', desc: true, descending: false })");
		assertFails("desc with an invalid value",               "sort(find('Person'), " + json("{'key': 'name', 'desc': 'desc'}") + ")",                   "$.sort($.find('Person'), { key: 'name', desc: 'desc' })");
	}

	// ----- strings, orders and objects in any combination -----

	@Test
	public void testNamesWithoutOrder() {

		createSortSchema();
		createPeople(person("mz").lastName("Müller").firstName("Zoe"), person("bb").lastName("Adams").firstName("Bob"), person("ma").lastName("Müller").firstName("Anna"), person("aa").lastName("Adams").firstName("Amy"));

		assertSorted("Two names without order are both ascending", List.of("aa", "bb", "ma", "mz"), "sort(find('Person'), 'lastName', 'firstName')",       "$.sort($.find('Person'), 'lastName', 'firstName')");
		assertSorted("A name with order, then one without",       List.of("ma", "mz", "aa", "bb"), "sort(find('Person'), 'lastName', true, 'firstName')", "$.sort($.find('Person'), 'lastName', true, 'firstName')");
		assertSorted("A name without order, then one with",       List.of("bb", "aa", "mz", "ma"), "sort(find('Person'), 'lastName', 'firstName', true)", "$.sort($.find('Person'), 'lastName', 'firstName', true)");
		assertSorted("Three names without order",                 List.of("aa", "bb", "ma", "mz"), "sort(find('Person'), 'lastName', 'firstName', 'name')", "$.sort($.find('Person'), 'lastName', 'firstName', 'name')");
	}

	@Test
	public void testAscAndDescAsOrder() {

		createSortSchema();
		createPeople(person("mz").lastName("Müller").firstName("Zoe"), person("bb").lastName("Adams").firstName("Bob"), person("ma").lastName("Müller").firstName("Anna"), person("aa").lastName("Adams").firstName("Amy"));

		assertSorted("desc after a name",              List.of("mz", "bb", "ma", "aa"), "sort(find('Person'), 'firstName', 'desc')",                      "$.sort($.find('Person'), 'firstName', 'desc')");
		assertSorted("asc after a name",               List.of("aa", "ma", "bb", "mz"), "sort(find('Person'), 'firstName', 'asc')",                       "$.sort($.find('Person'), 'firstName', 'asc')");
		assertSorted("DESC in upper case",             List.of("mz", "bb", "ma", "aa"), "sort(find('Person'), 'firstName', 'DESC')",                      "$.sort($.find('Person'), 'firstName', 'DESC')");
		assertSorted("asc and desc for two names",     List.of("bb", "aa", "mz", "ma"), "sort(find('Person'), 'lastName', 'asc', 'firstName', 'desc')",   "$.sort($.find('Person'), 'lastName', 'asc', 'firstName', 'desc')");
		assertSorted("true and false as text",         List.of("ma", "mz", "aa", "bb"), "sort(find('Person'), 'lastName', 'true', 'firstName', 'false')", "$.sort($.find('Person'), 'lastName', 'true', 'firstName', 'false')");
		assertSorted("TRUE in upper case",             List.of("mz", "bb", "ma", "aa"), "sort(find('Person'), 'firstName', 'TRUE')",                      "$.sort($.find('Person'), 'firstName', 'TRUE')");
	}

	@Test
	public void testOrderWithoutNameIsTheOrderOfTheDefaultKey() {

		createSortSchema();
		createPeople(person("b"), person("a"), person("c"));

		assertSorted("true without a name",   List.of("c", "b", "a"), "sort(find('Person'), true)",   "$.sort($.find('Person'), true)");
		assertSorted("desc without a name",   List.of("c", "b", "a"), "sort(find('Person'), 'desc')", "$.sort($.find('Person'), 'desc')");
		assertSorted("false without a name",  List.of("a", "b", "c"), "sort(find('Person'), false)",  "$.sort($.find('Person'), false)");
		assertSorted("null for the default key, then its order", List.of("c", "b", "a"), "sort(find('Person'), null, 'desc')", "$.sort($.find('Person'), null, 'desc')");
		assertSorted("null as the order of a name",              List.of("a", "b", "c"), "sort(find('Person'), 'name', null)",  "$.sort($.find('Person'), 'name', null)");
	}

	@Test
	public void testNamesAndObjectsMixed() {

		createSortSchema();
		createPeople(person("b10").lastName("b").firstName("Item 10"), person("A2").lastName("A").firstName("Item 2"), person("b2").lastName("b").firstName("Item 2"), person("a1").lastName("a").firstName("Item 1"));

		assertSorted("A name with order, then an object",       List.of("b2", "b10", "a1", "A2"), "sort(find('Person'), 'lastName', true, " + json("{'key': 'firstName', 'natural': true}") + ")", "$.sort($.find('Person'), 'lastName', true, { key: 'firstName', natural: true })");
		assertSorted("A name without order, then an object",    List.of("A2", "a1", "b2", "b10"), "sort(find('Person'), 'lastName', " + json("{'key': 'firstName', 'natural': true}") + ")",       "$.sort($.find('Person'), 'lastName', { key: 'firstName', natural: true })");
		assertSorted("An object, then a name with order",       List.of("a1", "A2", "b2", "b10"), "sort(find('Person'), " + json("{'key': 'lastName', 'natural': true}") + ", 'firstName', 'desc')", "$.sort($.find('Person'), { key: 'lastName', natural: true }, 'firstName', 'desc')");
		assertSorted("An object between two names",             List.of("a1", "A2", "b10", "b2"), "sort(find('Person'), 'age', " + json("{'key': 'lastName', 'natural': true}") + ", 'name')",   "$.sort($.find('Person'), 'age', { key: 'lastName', natural: true }, 'name')");
	}

	@Test
	public void testNamesAndObjectsMixedWithPathsAndNulls() {

		createSortSchema();
		createPeople(
			person("t10-b").team("Team 10").nick("b"),
			person("t2-none").team("team 2"),
			person("t2-a").team("team 2").nick("a"),
			person("none-c").nick("c"),
			person("t10-none").team("Team 10")
		);

		assertSorted("A path as name, then an object with nulls first",            List.of("t10-none", "t10-b", "t2-none", "t2-a", "none-c"), "sort(find('Person'), 'team.name', " + json("{'key': 'nick', 'nulls': 'first'}") + ")",                         "$.sort($.find('Person'), 'team.name', { key: 'nick', nulls: 'first' })");
		assertSorted("An object with a natural path, then a name with order",      List.of("t2-none", "t2-a", "t10-none", "t10-b", "none-c"), "sort(find('Person'), " + json("{'key': 'team.name', 'natural': true}") + ", 'nick', 'desc')",                  "$.sort($.find('Person'), { key: 'team.name', natural: true }, 'nick', 'desc')");
		assertSorted("A path descending as name, then an object with nulls last", List.of("none-c", "t2-a", "t2-none", "t10-b", "t10-none"), "sort(find('Person'), 'team.name', 'desc', " + json("{'key': 'nick', 'nulls': 'last'}") + ")",                  "$.sort($.find('Person'), 'team.name', 'desc', { key: 'nick', nulls: 'last' })");
	}

	@Test
	public void testInvalidValuesAreRejected() {

		createSortSchema();
		createPeople(person("a"));

		assertFails("An invalid position of nulls", "sort(find('Person'), " + json("{'key': 'name', 'nulls': 'top'}") + ")",        "$.sort($.find('Person'), { key: 'name', nulls: 'top' })");
		assertFails("An invalid order",             "sort(find('Person'), " + json("{'key': 'name', 'descending': 'yes'}") + ")",   "$.sort($.find('Person'), { key: 'name', descending: 'yes' })");
		assertFails("An invalid natural option",    "sort(find('Person'), " + json("{'key': 'name', 'natural': 'yes'}") + ")",      "$.sort($.find('Person'), { key: 'name', natural: 'yes' })");
		assertFails("A key that is a number",       "sort(find('Person'), " + json("{'key': 1}") + ")",                             "$.sort($.find('Person'), { key: 1 })");
	}

	@Test
	public void testSortKeyMustBeKnown() {

		createSortSchema();
		createPeople(person("a"));

		assertFails("A number as second key of the short form", "sort(find('Person'), 'name', false, 1, false)",                                            "$.sort($.find('Person'), 'name', false, 1, false)");
		assertFails("An unknown first key",                     "sort(find('Person'), 'unknown')",                                                          "$.sort($.find('Person'), 'unknown')");
		assertFails("An unknown second key",                    "sort(find('Person'), 'name', false, 'unknown')",                                           "$.sort($.find('Person'), 'name', false, 'unknown')");
		assertFails("An unknown key in an object",              "sort(find('Person'), " + json("{'key': 'unknown'}") + ")",                                 "$.sort($.find('Person'), { key: 'unknown' })");
		assertFails("An unknown key in the second object",      "sort(find('Person'), " + json("{'key': 'name'}") + ", " + json("{'key': 'unknown'}") + ")", "$.sort($.find('Person'), { key: 'name' }, { key: 'unknown' })");
	}

	@Test
	public void testUnknownKeyInAPathIsRejected() {

		createSortSchema();
		createPeople(person("a").team("Alpha"));

		assertFails("A misspelled first part of a path", "sort(find('Person'), 'tem.name')",                                  "$.sort($.find('Person'), 'tem.name')");
		assertFails("A misspelled last part of a path",  "sort(find('Person'), 'team.nmae')",                                 "$.sort($.find('Person'), 'team.nmae')");
		assertFails("A misspelled path in an object",    "sort(find('Person'), " + json("{'key': 'team.nmae'}") + ")",       "$.sort($.find('Person'), { key: 'team.nmae' })");
		assertFails("A misspelled path as second key",   "sort(find('Person'), 'name', false, 'projects.nmae')",            "$.sort($.find('Person'), 'name', false, 'projects.nmae')");
	}

	// ----- other collections -----

	@Test
	public void testMixedCollectionsAreReturnedUnsorted() {

		createSortSchema();
		createPeople(person("b"), person("a"));

		// a collection of nodes and strings is neither, so it is returned as it is, and the error is logged
		assertSortedJavaScript("Nodes and a string",   List.of("x", "b", "a"), "$.sort(['x', ...$.find('Person', $.predicate.sort('name', true))], 'name')");
		assertSortedJavaScript("Strings and a number", List.of("b", "1", "a"), "$.sort(['b', 1, 'a'])");
	}

	@Test
	public void testMapsAreSortedByKeysAndPaths() {

		createSortSchema();

		// objects created from JSON or by a cypher query are maps without a schema type
		final String maps = json("[{'name': 'b', 'rank': 1, 'owner': {'name': 'x'}}, {'name': 'c', 'rank': 2, 'owner': {'name': 'y'}}, {'name': 'a', 'rank': 3, 'owner': {'name': 'x'}}]");
		final String jsMaps = maps.replace("fromJson", "$.fromJson");

		assertSorted("Maps by a key",            List.of("a", "b", "c"), "sort(" + maps + ", 'name')",                                                              "$.sort(" + jsMaps + ", 'name')");
		assertSorted("Maps by a path",           List.of("b", "a", "c"), "sort(" + maps + ", 'owner.name')",                                                        "$.sort(" + jsMaps + ", 'owner.name')");
		assertSorted("Maps by a path and a key", List.of("a", "b", "c"), "sort(" + maps + ", " + json("{'key': 'owner.name'}") + ", " + json("{'key': 'name'}") + ")", "$.sort(" + jsMaps + ", { key: 'owner.name' }, { key: 'name' })");
	}

	@Test
	public void testKeysOfTheShortFormAreIgnoredForStrings() {

		createSortSchema();

		assertSorted("Only the order of the first key counts for strings", List.of("b", "a"), "sort(merge('a', 'b'), 'x', true, 5, false)", "$.sort(['a', 'b'], 'x', true, 5, false)");
	}

	// ----- other in-memory sorting -----

	@Test
	public void testDefaultSortOrderUsesTheNextKeyWhenBothValuesAreNull() {

		createSortSchema();
		createPeople(person("c"), person("a"), person("b"));

		try (final Tx tx = app.tx()) {

			final Traits traits          = Traits.of("Person");
			final DefaultSortOrder order = new DefaultSortOrder();

			order.addElement(traits.key("nick"), false);
			order.addElement(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), false);

			final List<NodeInterface> people = new ArrayList<>(app.nodeQuery("Person").getAsList());

			people.sort(order);

			assertEquals("Without a value in the first key, the second key must decide", List.of("a", "b", "c"), people.stream().map(NodeInterface::getName).toList());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}
}
