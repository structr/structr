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

import org.testng.annotations.Test;

import java.util.List;
import java.util.Locale;

import static org.testng.AssertJUnit.assertEquals;

/**
 * The sort predicate of find(): the same sort keys as the sort() function, in any combination, and
 * several sort predicates in one query. The database sorts by plain keys, Structr sorts in Java when
 * a key is a path, in natural order or with an explicit position of nulls, before the page is taken.
 *
 * Each query is checked in StructrScript, find('Person', sort(...)), and in JavaScript,
 * $.find('Person', $.predicate.sort(...)).
 */
public class FindSortTest extends SortTestBase {

	// ----- one sort key -----

	@Test
	public void testOneKeyInAllForms() {

		createSortSchema();
		createPeople(person("b"), person("a"), person("c"));

		assertFound("A name",                        List.of("a", "b", "c"), "sort('name')",                                   "$.predicate.sort('name')");
		assertFound("A name and true",               List.of("c", "b", "a"), "sort('name', true)",                             "$.predicate.sort('name', true)");
		assertFound("A name and false",              List.of("a", "b", "c"), "sort('name', false)",                            "$.predicate.sort('name', false)");
		assertFound("A name and desc",               List.of("c", "b", "a"), "sort('name', 'desc')",                           "$.predicate.sort('name', 'desc')");
		assertFound("A name and asc",                List.of("a", "b", "c"), "sort('name', 'asc')",                            "$.predicate.sort('name', 'asc')");
		assertFound("A name and true as text",       List.of("c", "b", "a"), "sort('name', 'true')",                           "$.predicate.sort('name', 'true')");
		assertFound("An object",                     List.of("a", "b", "c"), "sort(" + json("{'key': 'name'}") + ")",                    "$.predicate.sort({ key: 'name' })");
		assertFound("An object with descending",     List.of("c", "b", "a"), "sort(" + json("{'key': 'name', 'descending': true}") + ")", "$.predicate.sort({ key: 'name', descending: true })");
		assertFound("An object with desc",           List.of("c", "b", "a"), "sort(" + json("{'key': 'name', 'desc': true}") + ")",       "$.predicate.sort({ key: 'name', desc: true })");
	}

	@Test
	public void testDefaultKey() {

		createSortSchema();
		createPeople(person("b"), person("a"), person("c"));

		assertFound("Without arguments, the predicate sorts by name", List.of("a", "b", "c"), "sort()",                                      "$.predicate.sort()");
		assertFound("An order without a name",                        List.of("c", "b", "a"), "sort(true)",                                  "$.predicate.sort(true)");
		assertFound("desc without a name",                            List.of("c", "b", "a"), "sort('desc')",                                "$.predicate.sort('desc')");
		assertFound("An object without key",                          List.of("c", "b", "a"), "sort(" + json("{'desc': true}") + ")",        "$.predicate.sort({ desc: true })");
		assertFound("An empty object",                                List.of("a", "b", "c"), "sort(" + json("{}") + ")",                    "$.predicate.sort({})");
	}

	@Test
	public void testNumbersAreSortedByValue() {

		createSortSchema();
		createPeople(person("p30").age(30), person("p5").age(5), person("p100").age(100));

		assertFound("Numbers by value",            List.of("p5", "p30", "p100"), "sort('age')",       "$.predicate.sort('age')");
		assertFound("Numbers by value descending", List.of("p100", "p30", "p5"), "sort('age', true)", "$.predicate.sort('age', true)");
	}

	// ----- several sort keys in one predicate -----

	@Test
	public void testSeveralKeysInOnePredicate() {

		createSortSchema();
		createMuellersAndAdams();

		assertFound("Two names",                     List.of("aa", "bb", "ma", "mz"), "sort('lastName', 'firstName')",                       "$.predicate.sort('lastName', 'firstName')");
		assertFound("Two pairs, ascending, descending", List.of("bb", "aa", "mz", "ma"), "sort('lastName', false, 'firstName', true)",     "$.predicate.sort('lastName', false, 'firstName', true)");
		assertFound("Two pairs, descending, ascending", List.of("ma", "mz", "aa", "bb"), "sort('lastName', true, 'firstName', false)",     "$.predicate.sort('lastName', true, 'firstName', false)");
		assertFound("Two pairs, both descending",    List.of("mz", "ma", "bb", "aa"), "sort('lastName', 'desc', 'firstName', 'desc')",       "$.predicate.sort('lastName', 'desc', 'firstName', 'desc')");
		assertFound("Two objects",                   List.of("bb", "aa", "mz", "ma"), "sort(" + json("{'key': 'lastName'}") + ", " + json("{'key': 'firstName', 'desc': true}") + ")", "$.predicate.sort({ key: 'lastName' }, { key: 'firstName', desc: true })");
		assertFound("A name with order and an object", List.of("ma", "mz", "aa", "bb"), "sort('lastName', true, " + json("{'key': 'firstName'}") + ")",                         "$.predicate.sort('lastName', true, { key: 'firstName' })");
		assertFound("An object and a name",          List.of("mz", "ma", "bb", "aa"), "sort(" + json("{'key': 'lastName', 'desc': true}") + ", 'firstName', 'desc')",                 "$.predicate.sort({ key: 'lastName', desc: true }, 'firstName', 'desc')");
	}

	@Test
	public void testThreeKeysInOnePredicate() {

		createSortSchema();
		createPeople(
			person("m-a-40").lastName("Müller").firstName("Anna").age(40),
			person("a-b-30").lastName("Adams").firstName("Bob").age(30),
			person("m-a-20").lastName("Müller").firstName("Anna").age(20),
			person("m-z-10").lastName("Müller").firstName("Zoe").age(10)
		);

		assertFound("Three names",                      List.of("a-b-30", "m-a-20", "m-a-40", "m-z-10"), "sort('lastName', 'firstName', 'age')",       "$.predicate.sort('lastName', 'firstName', 'age')");
		assertFound("Three names, the last descending", List.of("a-b-30", "m-a-40", "m-a-20", "m-z-10"), "sort('lastName', 'firstName', 'age', true)", "$.predicate.sort('lastName', 'firstName', 'age', true)");
	}

	// ----- several sort predicates -----

	@Test
	public void testSeveralPredicates() {

		createSortSchema();
		createMuellersAndAdams();

		assertFoundWith("Two predicates",                           List.of("aa", "bb", "ma", "mz"), "sort('lastName'), sort('firstName')",                   "$.predicate.sort('lastName'), $.predicate.sort('firstName')");
		assertFoundWith("Two predicates, the second descending",    List.of("bb", "aa", "mz", "ma"), "sort('lastName'), sort('firstName', true)",             "$.predicate.sort('lastName'), $.predicate.sort('firstName', true)");
		assertFoundWith("Two predicates, the first descending",     List.of("ma", "mz", "aa", "bb"), "sort('lastName', 'desc'), sort('firstName')",           "$.predicate.sort('lastName', 'desc'), $.predicate.sort('firstName')");
		assertFoundWith("A predicate with two keys and one more",   List.of("aa", "bb", "ma", "mz"), "sort('lastName', 'firstName'), sort('name')",           "$.predicate.sort('lastName', 'firstName'), $.predicate.sort('name')");
		assertFoundWith("Predicates with objects",                  List.of("mz", "ma", "bb", "aa"), "sort(" + json("{'key': 'lastName', 'desc': true}") + "), sort(" + json("{'key': 'firstName', 'desc': true}") + ")", "$.predicate.sort({ key: 'lastName', desc: true }), $.predicate.sort({ key: 'firstName', desc: true })");
	}

	@Test
	public void testSeveralPredicatesWithPathsInAnyOrder() {

		createSortSchema();
		createPeople(person("p3").team("Beta"), person("p2").team("Alpha"), person("p4"), person("p1").team("Alpha"), person("p0"));

		// before, a path predicate replaced the keys of the predicates before it, and a key after a path was ignored
		assertFoundWith("A path predicate after a key predicate", List.of("p1", "p2", "p3", "p0", "p4"), "sort('nick'), sort('team.name'), sort('name')",       "$.predicate.sort('nick'), $.predicate.sort('team.name'), $.predicate.sort('name')");
		assertFoundWith("A key predicate after a path predicate", List.of("p1", "p2", "p3", "p0", "p4"), "sort('team.name'), sort('name')",                     "$.predicate.sort('team.name'), $.predicate.sort('name')");
		assertFoundWith("A key predicate descending after a path", List.of("p2", "p1", "p3", "p4", "p0"), "sort('team.name'), sort('name', true)",              "$.predicate.sort('team.name'), $.predicate.sort('name', true)");
		assertFoundWith("A path predicate descending, then a key", List.of("p0", "p4", "p3", "p1", "p2"), "sort('team.name', true), sort('name')",              "$.predicate.sort('team.name', true), $.predicate.sort('name')");
		assertFoundWith("A key predicate before a path",           List.of("p0", "p1", "p2", "p3", "p4"), "sort('age'), sort('name')",                          "$.predicate.sort('age'), $.predicate.sort('name')");
	}

	// ----- paths -----

	@Test
	public void testPaths() {

		createSortSchema();
		createPeople(person("p2").team("Beta"), person("p0"), person("p1").team("Alpha"));

		assertFound("A path",                                List.of("p1", "p2", "p0"), "sort('team.name')",                                     "$.predicate.sort('team.name')");
		assertFound("A path descending",                     List.of("p0", "p2", "p1"), "sort('team.name', true)",                               "$.predicate.sort('team.name', true)");
		assertFound("A path in an object with nulls first",  List.of("p0", "p1", "p2"), "sort(" + json("{'key': 'team.name', 'nulls': 'first'}") + ")", "$.predicate.sort({ key: 'team.name', nulls: 'first' })");
		assertFound("A path and a name in one predicate",    List.of("p1", "p2", "p0"), "sort('team.name', 'name')",                             "$.predicate.sort('team.name', 'name')");
	}

	@Test
	public void testPathThroughACollection() {

		createSortSchema();
		createPeople(person("item1").projects("B", "D"), person("item2").projects("C"), person("item3"), person("item4").projects("E", "A"));

		assertFound("Ascending by the smallest value",             List.of("item4", "item1", "item2", "item3"), "sort('projects.name')",       "$.predicate.sort('projects.name')");
		assertFound("Descending by the largest value",             List.of("item3", "item4", "item1", "item2"), "sort('projects.name', true)", "$.predicate.sort('projects.name', true)");
		assertFound("Without a value last, also when descending",  List.of("item4", "item1", "item2", "item3"), "sort(" + json("{'key': 'projects.name', 'desc': true, 'nulls': 'last'}") + ")", "$.predicate.sort({ key: 'projects.name', desc: true, nulls: 'last' })");
	}

	// ----- natural order and nulls, sorted in Java -----

	@Test
	public void testNaturalOrder() {

		createSortSchema();
		createPeople(person("Item 10"), person("item 2"), person("Item 1"), person("item 20"));

		assertFound("Without natural order",        List.of("Item 1", "Item 10", "item 2", "item 20"), "sort('name')",                                                                 "$.predicate.sort('name')");
		assertFound("Natural order",                List.of("Item 1", "item 2", "Item 10", "item 20"), "sort(" + json("{'key': 'name', 'natural': true}") + ")",                   "$.predicate.sort({ key: 'name', natural: true })");
		assertFound("Natural order descending",     List.of("item 20", "Item 10", "item 2", "Item 1"), "sort(" + json("{'key': 'name', 'natural': true, 'desc': true}") + ")",     "$.predicate.sort({ key: 'name', natural: true, desc: true })");
	}

	@Test
	public void testNaturalOrderDependsOnTheLocale() {

		createSortSchema();
		createPeople(person("Zebra"), person("Äpfel"), person("Bär"), person("Apfel"));

		assertSorted("In German, umlauts belong to their base letter", List.of("Apfel", "Äpfel", "Bär", "Zebra"), find("sort(" + json("{'key': 'name', 'natural': true}") + ")"), findJs("$.predicate.sort({ key: 'name', natural: true })"), Locale.GERMAN);
		assertSorted("In Swedish, ä comes after z",                    List.of("Apfel", "Bär", "Zebra", "Äpfel"), find("sort(" + json("{'key': 'name', 'natural': true}") + ")"), findJs("$.predicate.sort({ key: 'name', natural: true })"), Locale.of("sv"));
	}

	@Test
	public void testNulls() {

		createSortSchema();
		createPeople(person("x").nick("x"), person("none"), person("a").nick("a"));

		assertFound("Without option, nulls last when ascending",  List.of("a", "x", "none"), "sort('nick')",                                                                    "$.predicate.sort('nick')");
		assertFound("Without option, nulls first when descending", List.of("none", "x", "a"), "sort('nick', true)",                                                             "$.predicate.sort('nick', true)");
		assertFound("Nulls first, ascending",                     List.of("none", "a", "x"), "sort(" + json("{'key': 'nick', 'nulls': 'first'}") + ")",                        "$.predicate.sort({ key: 'nick', nulls: 'first' })");
		assertFound("Nulls first, descending",                    List.of("none", "x", "a"), "sort(" + json("{'key': 'nick', 'nulls': 'first', 'desc': true}") + ")",          "$.predicate.sort({ key: 'nick', nulls: 'first', desc: true })");
		assertFound("Nulls last, ascending",                      List.of("a", "x", "none"), "sort(" + json("{'key': 'nick', 'nulls': 'last'}") + ")",                         "$.predicate.sort({ key: 'nick', nulls: 'last' })");
		assertFound("Nulls last, descending",                     List.of("x", "a", "none"), "sort(" + json("{'key': 'nick', 'nulls': 'last', 'desc': true}") + ")",           "$.predicate.sort({ key: 'nick', nulls: 'last', desc: true })");
	}

	@Test
	public void testNullsPerKeyAndNaturalOrderTogether() {

		createSortSchema();
		createPeople(person("p10").lastName("Müller").nick("Item 10"), person("none").lastName("Müller"), person("p2").lastName("Müller").nick("item 2"), person("adams").lastName("Adams").nick("Item 1"));

		assertFound("A name, then natural order with nulls first", List.of("adams", "none", "p2", "p10"), "sort('lastName', " + json("{'key': 'nick', 'natural': true, 'nulls': 'first'}") + ")",           "$.predicate.sort('lastName', { key: 'nick', natural: true, nulls: 'first' })");
		assertFoundWith("The same in two predicates",              List.of("adams", "none", "p2", "p10"), "sort('lastName'), sort(" + json("{'key': 'nick', 'natural': true, 'nulls': 'first'}") + ")", "$.predicate.sort('lastName'), $.predicate.sort({ key: 'nick', natural: true, nulls: 'first' })");
	}

	// ----- paging -----

	@Test
	public void testPagingAfterSortingInTheDatabase() {

		createSortSchema();
		createPeople(person("d"), person("b"), person("e"), person("a"), person("c"));

		assertFoundWith("First page",  List.of("a", "b"), "sort('name'), page(1, 2)",       "$.predicate.sort('name'), $.predicate.page(1, 2)");
		assertFoundWith("Second page", List.of("c", "d"), "sort('name'), page(2, 2)",       "$.predicate.sort('name'), $.predicate.page(2, 2)");
		assertFoundWith("Last page",   List.of("e"),      "sort('name'), page(3, 2)",       "$.predicate.sort('name'), $.predicate.page(3, 2)");
		assertFoundWith("Descending",  List.of("c", "b"), "sort('name', true), page(2, 2)", "$.predicate.sort('name', true), $.predicate.page(2, 2)");
	}

	@Test
	public void testPagingAfterSortingInJava() {

		createSortSchema();
		createPeople(person("Item 10"), person("item 2"), person("Item 1"), person("item 20"), person("Item 3"));

		// the page must be taken from the result sorted in natural order, not from the order of the database
		final String natural   = "sort(" + json("{'key': 'name', 'natural': true}") + ")";
		final String naturalJs = "$.predicate.sort({ key: 'name', natural: true })";

		assertFoundWith("First page in natural order",  List.of("Item 1", "item 2"),  natural + ", page(1, 2)", naturalJs + ", $.predicate.page(1, 2)");
		assertFoundWith("Second page in natural order", List.of("Item 3", "Item 10"), natural + ", page(2, 2)", naturalJs + ", $.predicate.page(2, 2)");
		assertFoundWith("Last page in natural order",   List.of("item 20"),           natural + ", page(3, 2)", naturalJs + ", $.predicate.page(3, 2)");
	}

	@Test
	public void testPagingAfterSortingByAPathAndNulls() {

		createSortSchema();
		createPeople(person("p3").team("Beta"), person("p2").team("Alpha"), person("p4"), person("p1").team("Alpha"), person("p0"));

		assertFoundWith("Path, then name, second page",       List.of("p3", "p0"), "sort('team.name', 'name'), page(2, 2)", "$.predicate.sort('team.name', 'name'), $.predicate.page(2, 2)");
		assertFoundWith("Nulls first, then name, first page", List.of("p0", "p4"), "sort(" + json("{'key': 'team.name', 'nulls': 'first'}") + ", 'name'), page(1, 2)", "$.predicate.sort({ key: 'team.name', nulls: 'first' }, 'name'), $.predicate.page(1, 2)");
	}

	// ----- together with other predicates -----

	@Test
	public void testSortingAFilteredResult() {

		createSortSchema();
		createMuellersAndAdams();

		assertSorted("A filter and a sort key",            List.of("mz", "ma"), "find('Person', 'lastName', 'Müller', sort('firstName', true))", "$.find('Person', { lastName: 'Müller' }, $.predicate.sort('firstName', true))");
		assertSorted("A filter and natural order",         List.of("aa", "bb"), "find('Person', 'lastName', 'Adams', sort(" + json("{'key': 'firstName', 'natural': true}") + "))", "$.find('Person', { lastName: 'Adams' }, $.predicate.sort({ key: 'firstName', natural: true }))");
	}

	@Test
	public void testKeyOfASubtype() {

		createSortSchema();
		createPeople(person("plain"));
		createEmployee("e2", "Sales");
		createEmployee("e1", "Finance");

		// the result of a query for a type can contain objects of its subtypes, so their keys are known
		assertFound("A key that only a subtype has", List.of("e1", "e2", "plain"), "sort('department')", "$.predicate.sort('department')");
	}

	// ----- errors -----

	@Test
	public void testErrors() {

		createSortSchema();
		createPeople(person("a").team("Alpha"));

		assertFails("An unknown key",                   find("sort('unknown')"),                                                    findJs("$.predicate.sort('unknown')"));
		assertFails("An unknown second key",            find("sort('name', 'unknown')"),                                            findJs("$.predicate.sort('name', 'unknown')"));
		assertFails("An unknown key in an object",      find("sort(" + json("{'key': 'unknown'}") + ")"),                          findJs("$.predicate.sort({ key: 'unknown' })"));
		assertFails("A misspelled first part of a path", find("sort('tem.name')"),                                                  findJs("$.predicate.sort('tem.name')"));
		assertFails("A misspelled last part of a path", find("sort('team.nmae')"),                                                  findJs("$.predicate.sort('team.nmae')"));
		assertFails("An unknown option",                find("sort(" + json("{'key': 'name', 'naturel': true}") + ")"),           findJs("$.predicate.sort({ key: 'name', naturel: true })"));
		assertFails("An invalid position of nulls",     find("sort(" + json("{'key': 'name', 'nulls': 'top'}") + ")"),             findJs("$.predicate.sort({ key: 'name', nulls: 'top' })"));
		assertFails("An invalid order in an object",    find("sort(" + json("{'key': 'name', 'desc': 'yes'}") + ")"),              findJs("$.predicate.sort({ key: 'name', desc: 'yes' })"));
		assertFails("Contradicting desc and descending", find("sort(" + json("{'key': 'name', 'desc': true, 'descending': false}") + ")"), findJs("$.predicate.sort({ key: 'name', desc: true, descending: false })"));
		assertFails("A key that is a number",           find("sort(" + json("{'key': 1}") + ")"),                                  findJs("$.predicate.sort({ key: 1 })"));
	}

	// ----- the same result as the sort() function -----

	@Test
	public void testSameResultAsTheSortFunction() {

		createSortSchema();
		createPeople(
			person("t10-b").lastName("b").firstName("Item 10").team("Team 10").nick("b").age(3).projects("Project 10", "Project 3"),
			person("t2-none").lastName("A").firstName("Item 2").team("team 2").age(1),
			person("t2-a").lastName("b").firstName("Item 2").team("team 2").nick("a").projects("Project 9"),
			person("none-c").lastName("a").firstName("Item 1").nick("c").age(2),
			person("t10-none").lastName("a").firstName("item 1").team("Team 10")
		);

		final List<String[]> specs = List.of(
			new String[] { "'name'",                                                             "'name'" },
			new String[] { "'lastName', 'firstName'",                                            "'lastName', 'firstName'" },
			new String[] { "'lastName', true, 'firstName', 'desc'",                              "'lastName', true, 'firstName', 'desc'" },
			new String[] { "'age', 'name'",                                                      "'age', 'name'" },
			new String[] { "'age', true, 'name'",                                                "'age', true, 'name'" },
			new String[] { "'team.name', 'name'",                                                "'team.name', 'name'" },
			new String[] { "'projects.name', true, 'name'",                                      "'projects.name', true, 'name'" },
			new String[] { json("{'key': 'firstName', 'natural': true}") + ", 'name'",          "{ key: 'firstName', natural: true }, 'name'" },
			new String[] { json("{'key': 'nick', 'nulls': 'first'}") + ", 'name'",              "{ key: 'nick', nulls: 'first' }, 'name'" },
			new String[] { json("{'key': 'team.name', 'natural': true, 'desc': true}") + ", " + json("{'key': 'nick', 'nulls': 'last'}") + ", 'name'", "{ key: 'team.name', natural: true, desc: true }, { key: 'nick', nulls: 'last' }, 'name'" },
			new String[] { "'lastName', " + json("{'key': 'projects.name', 'natural': true, 'nulls': 'first'}") + ", 'name'", "'lastName', { key: 'projects.name', natural: true, nulls: 'first' }, 'name'" }
		);

		for (final String[] spec : specs) {

			final List<String> sorted = evaluate("${sort(find('Person'), " + spec[0] + ")}", Locale.ENGLISH);

			assertEquals("find() with the sort predicate must sort like sort(): " + spec[0], sorted, evaluate("${find('Person', sort(" + spec[0] + "))}", Locale.ENGLISH));
			assertEquals("$.predicate.sort must sort like $.sort(): " + spec[1],             sorted, evaluate("${{ $.find('Person', $.predicate.sort(" + spec[1] + ")); }}", Locale.ENGLISH));
			assertEquals("$.sort() must sort like sort(): " + spec[1],                      sorted, evaluate("${{ $.sort($.find('Person'), " + spec[1] + "); }}", Locale.ENGLISH));
		}
	}

	// ----- private methods -----
	private void createMuellersAndAdams() {

		createPeople(person("mz").lastName("Müller").firstName("Zoe"), person("bb").lastName("Adams").firstName("Bob"), person("ma").lastName("Müller").firstName("Anna"), person("aa").lastName("Adams").firstName("Amy"));
	}

	/**
	 * Queries the people with the given sort predicate in both languages.
	 */
	private void assertFound(final String message, final List<String> expected, final String structrScript, final String javaScript) {

		assertFoundWith(message, expected, structrScript, javaScript);
	}

	/**
	 * Queries the people with the given predicates, separated by commas, in both languages.
	 */
	private void assertFoundWith(final String message, final List<String> expected, final String structrScriptPredicates, final String javaScriptPredicates) {

		assertSorted(message, expected, find(structrScriptPredicates), findJs(javaScriptPredicates));
	}

	private String find(final String predicates) {

		return "find('Person', " + predicates + ")";
	}

	private String findJs(final String predicates) {

		return "$.find('Person', " + predicates + ")";
	}
}
