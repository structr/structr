/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.extraction;

import org.structr.extraction.api.ContentExtraction;
import org.structr.extraction.api.search.SearchSnippet;
import org.structr.extraction.query.ContentSearchQuery;
import org.structr.extraction.query.SearchSnippets;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.testng.AssertJUnit.*;

/**
 * The search box query (words, prefixes, typos, phrases), snippets around matches, and stop words,
 * without a database.
 */
public class SearchQueryTest {

	private static final String CODE = "{\n    let orderTotal = $.calculate('total');\n\n    $.invoices.sendReminder()\n\n}";

	private static final String TEXT = "Structr needs a Java runtime. The installation of Neo4j is described below.\n\n"
		+ "Release Notes: read them first. Installing an update replaces the binaries.";

	// ----- query -----
	@Test
	public void testWordsBecomeExactPrefixAndFuzzyClauses() {

		final ContentSearchQuery query = ContentSearchQuery.parse("Instalation  GUIDE");

		assertEquals("(instalation^3 OR instalation*^2 OR *instalation* OR instalation~2) AND (guide^3 OR guide*^2 OR *guide* OR guide~1)", query.toLucene());
		assertEquals("(instalation^3 OR instalation*^2 OR *instalation* OR instalation~2) OR (guide^3 OR guide*^2 OR *guide* OR guide~1)", query.toLucene(false));
	}

	@Test
	public void testShortWordsAreNotFuzzy() {

		assertEquals("go", ContentSearchQuery.parse("go").toLucene());
		assertEquals("(jav^3 OR jav*^2 OR *jav*)", ContentSearchQuery.parse("Jav").toLucene());
		assertEquals("(java^3 OR java*^2 OR *java* OR java~1)", ContentSearchQuery.parse("java").toLucene());
	}

	@Test
	public void testPhrasesAndSpecialCharacters() {

		assertEquals("\"release notes\" AND (neo4j^3 OR neo4j*^2 OR *neo4j* OR neo4j~1)", ContentSearchQuery.parse("\"Release  Notes\" neo4j").toLucene());

		// Lucene syntax is not interpreted: special characters separate words like the analyzer does
		assertEquals("(foo^3 OR foo*^2 OR *foo*) AND (bar^3 OR bar*^2 OR *bar*) AND (baz^3 OR baz*^2 OR *baz*)", ContentSearchQuery.parse("foo:bar (baz)~ *").toLucene());

		// a quoted single word is just a word
		assertEquals("(java^3 OR java*^2 OR *java* OR java~1)", ContentSearchQuery.parse("\"Java\"").toLucene());
	}

	@Test
	public void testEmptyInput() {

		assertTrue(ContentSearchQuery.parse("").isEmpty());
		assertTrue(ContentSearchQuery.parse("  :-) ").isEmpty());
		assertTrue(ContentSearchQuery.parse(null).isEmpty());
		assertNull(ContentSearchQuery.parse(" ").toLucene());
	}

	@Test
	public void testWordMatching() {

		final ContentSearchQuery query = ContentSearchQuery.parse("instalation");

		assertTrue("exact, ignoring case", ContentSearchQuery.parse("installation").matchesWord("INSTALLATION"));
		assertTrue("one missing letter", query.matchesWord("installation"));
		assertTrue("prefix", ContentSearchQuery.parse("instal").matchesWord("Installing"));
		assertTrue("transposed letters", ContentSearchQuery.parse("relaese").matchesWord("release"));
		assertFalse("too different", query.matchesWord("information"));
		assertFalse("short words need an exact match", ContentSearchQuery.parse("go").matchesWord("to"));
	}

	@Test
	public void testMatchedTerms() {

		final ContentSearchQuery query = ContentSearchQuery.parse("java \"release notes\" missing");

		assertEquals(Set.of(0, 1), query.matchedTerms(TEXT));
	}

	@Test
	public void testWordsInsideLongerWords() {

		// the index keeps "invoices.sendReminder" and "orderTotal" as one term each, so words are also searched inside terms
		final ContentSearchQuery query = ContentSearchQuery.parse("Total");

		assertTrue(query.matchesWord("orderTotal"));
		assertEquals("[5, 10]", Arrays.toString(query.matchSpan("orderTotal")));
		assertEquals("[0, 10]", Arrays.toString(ContentSearchQuery.parse("ordertotal").matchSpan("orderTotal")));
		assertNull(ContentSearchQuery.parse("go").matchSpan("ago"));
		assertEquals(Set.of(0, 1), ContentSearchQuery.parse("sendReminder calculate").matchedTerms(CODE));
	}

	// ----- snippets -----
	@Test
	public void testSnippetsAroundMatches() {

		final List<SearchSnippet> snippets = SearchSnippets.find(TEXT, ContentSearchQuery.parse("Install"), 3, 30, 10);

		assertEquals(2, snippets.size());

		final SearchSnippet first = snippets.get(0);

		assertEquals("installation", first.match());
		assertEquals("needs a Java runtime. The ", first.before());
		assertEquals(" of Neo4j is described below.", first.after());
		assertEquals(Integer.valueOf(3), first.page());

		// the context does not reach across the paragraph break
		final SearchSnippet second = snippets.get(1);

		assertEquals("Installing", second.match());
		assertEquals("Notes: read them first. ", second.before());
		assertEquals(" an update replaces the", second.after());
	}

	@Test
	public void testSnippetsInCode() {

		final List<SearchSnippet> method = SearchSnippets.find(CODE, ContentSearchQuery.parse("sendReminder"), null, 30, 10);

		assertEquals(1, method.size());
		assertEquals("sendReminder", method.get(0).match());
		assertTrue(method.get(0).before().endsWith("$.invoices."));
		assertEquals("()", method.get(0).after());

		// only the matching part of a word is the match, the rest of the word stays in the context
		final List<SearchSnippet> variable = SearchSnippets.find(CODE, ContentSearchQuery.parse("total"), null, 10, 10);

		assertEquals(2, variable.size());
		assertEquals("Total", variable.get(0).match());
		assertTrue(variable.get(0).before().endsWith("order"));
		assertEquals("total", variable.get(1).match());
	}

	@Test
	public void testPhraseSnippetAndLimit() {

		final List<SearchSnippet> phrase = SearchSnippets.find(TEXT, ContentSearchQuery.parse("\"release notes\""), null, 10, 10);

		assertEquals(1, phrase.size());
		assertEquals("Release Notes", phrase.get(0).match());
		assertEquals("", phrase.get(0).before());
		assertNull(phrase.get(0).page());

		assertEquals(1, SearchSnippets.find(TEXT, ContentSearchQuery.parse("the"), null, 10, 1).size());
		assertTrue(SearchSnippets.find(TEXT, ContentSearchQuery.parse("nothing"), null, 10, 5).isEmpty());
	}

	@Test
	public void testMatchesInsideTheContextStartNoNewSnippet() {

		final List<SearchSnippet> snippets = SearchSnippets.find("one two one two one", ContentSearchQuery.parse("one"), null, 100, 10);

		assertEquals(1, snippets.size());
	}

	// ----- stop words -----
	@Test
	public void testStopWords() {

		final Set<String> english = ContentExtraction.stopWords("en");

		assertTrue(english.containsAll(List.of("is", "not", "he", "the")));
		assertEquals(english, ContentExtraction.stopWords("en-US"));
		assertTrue(ContentExtraction.stopWords("de").contains("und"));
		assertFalse(ContentExtraction.stopWords("cs").isEmpty());
		assertTrue(ContentExtraction.stopWords("xx").isEmpty());
		assertTrue(ContentExtraction.stopWords(null).isEmpty());
	}
}
