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
package org.structr.extraction.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns what a user types into a search box into a Lucene query for the fulltext index on chunk text,
 * and decides which words of a text that query matches, so hits can be shown in context.
 *
 * <ul>
 *   <li>Words are split at every character that is neither a letter nor a digit, the way the index's
 *       analyzer splits the text; "quoted phrases" are kept together.</li>
 *   <li>Every word must occur, each one exactly, as a prefix or inside a longer word (both from 3
 *       characters), or with typos (fuzzy, from 4 characters: one edit up to 7 characters, two from 8).
 *       Exact matches rank highest, then prefix matches. Within one chunk, the query requires all words;
 *       a search for files asks for any word and checks with {@link #matchedTerms(String)} that the
 *       file's chunks contain all of them.</li>
 *   <li>Matching inside words matters because the index's analyzer (Lucene's StandardTokenizer) keeps
 *       letters joined by '.', '_' or an apostrophe together and never splits camelCase: the text
 *       {@code $.invoices.sendReminder()} is indexed as the single term "invoices.sendreminder",
 *       {@code orderTotal} as "ordertotal". A leading wildcard ({@code *sendreminder*}) finds
 *       them; Neo4j's query parser allows it, and it reads the term dictionary of the chunk index once
 *       per word.</li>
 *   <li>Matching ignores case: the analyzer lower-cases the indexed text, and since wildcard and fuzzy
 *       terms bypass the analyzer, the words are lower-cased here.</li>
 * </ul>
 */
public final class ContentSearchQuery {

	private static final Pattern TOKEN         = Pattern.compile("\"([^\"]*)\"|[\\p{L}\\p{N}]+");
	private static final Pattern WORD          = Pattern.compile("[\\p{L}\\p{N}]+");
	private static final int MIN_PREFIX_LENGTH = 3;
	private static final int MIN_FUZZY_LENGTH  = 4;
	private static final int TWO_EDITS_LENGTH  = 8;

	/**
	 * One word or phrase of the query.
	 *
	 * @param text     lower-cased; for a phrase, its words separated by single spaces
	 * @param phrase   whether the user quoted it
	 * @param prefix   whether longer words starting with it match
	 * @param infix    whether longer words containing it match
	 * @param maxEdits how many typos a matching word may have (0 = none)
	 */
	public record Term(String text, boolean phrase, boolean prefix, boolean infix, int maxEdits) {
	}

	private final List<Term> terms;

	private ContentSearchQuery(final List<Term> terms) {

		this.terms = terms;
	}

	/**
	 * Parses user input. The result is empty if the input holds no word.
	 */
	public static ContentSearchQuery parse(final String input) {

		final List<Term> terms = new ArrayList<>();

		if (input != null) {

			final Matcher matcher = TOKEN.matcher(input);

			while (matcher.find()) {

				if (matcher.group(1) != null) {

					final List<String> words = words(matcher.group(1));
					if (words.size() == 1) {

						terms.add(word(words.get(0)));

					} else if (!words.isEmpty()) {

						terms.add(new Term(String.join(" ", words), true, false, false, 0));
					}

				} else {

					terms.add(word(matcher.group().toLowerCase(Locale.ROOT)));
				}
			}
		}

		return new ContentSearchQuery(List.copyOf(terms));
	}

	public List<Term> terms() {

		return terms;
	}

	public boolean isEmpty() {

		return terms.isEmpty();
	}

	/**
	 * The query in Lucene syntax, requiring all words, or null if it is empty.
	 */
	public String toLucene() {

		return toLucene(true);
	}

	/**
	 * The query in Lucene syntax, or null if it is empty.
	 *
	 * @param allTerms whether every word must occur (AND) or any word (OR)
	 */
	public String toLucene(final boolean allTerms) {

		if (terms.isEmpty()) {

			return null;
		}

		final List<String> clauses = new ArrayList<>();

		for (final Term term : terms) {

			if (term.phrase()) {

				clauses.add("\"" + term.text() + "\"");
				continue;
			}

			final List<String> alternatives = new ArrayList<>();

			alternatives.add(term.text() + "^3");

			if (term.prefix()) {

				alternatives.add(term.text() + "*^2");
			}

			if (term.infix()) {

				alternatives.add("*" + term.text() + "*");
			}

			if (term.maxEdits() > 0) {

				alternatives.add(term.text() + "~" + term.maxEdits());
			}

			clauses.add(alternatives.size() == 1 ? term.text() : "(" + String.join(" OR ", alternatives) + ")");
		}

		return String.join(allTerms ? " AND " : " OR ", clauses);
	}

	/**
	 * The positions (in {@link #terms()}) of the terms that occur in a text.
	 */
	public Set<Integer> matchedTerms(final String text) {

		final Set<Integer> matched = new HashSet<>();

		if (text == null) {

			return matched;
		}

		for (int i = 0; i < terms.size(); i++) {

			if (terms.get(i).phrase() && phrasePattern(terms.get(i)).matcher(text).find()) {

				matched.add(i);
			}
		}

		final Matcher word = WORD.matcher(text);

		while (word.find() && matched.size() < terms.size()) {

			final String lower = word.group().toLowerCase(Locale.ROOT);

			for (int i = 0; i < terms.size(); i++) {

				if (!terms.get(i).phrase() && matchSpan(terms.get(i), lower) != null) {

					matched.add(i);
				}
			}
		}

		return matched;
	}

	/**
	 * A pattern that finds a phrase in a text: its words, ignoring case, separated by anything that is
	 * neither a letter nor a digit.
	 */
	public static Pattern phrasePattern(final Term term) {

		final List<String> parts = new ArrayList<>();

		for (final String word : term.text().split(" ")) {

			parts.add(Pattern.quote(word));
		}

		return Pattern.compile(String.join("[^\\p{L}\\p{N}]+", parts), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	}

	/**
	 * Whether a single word of a text matches one of the (non-phrase) terms: exactly, as a prefix, inside
	 * the word, or within the allowed number of typos, ignoring case.
	 */
	public boolean matchesWord(final String word) {

		return matchSpan(word) != null;
	}

	/**
	 * The part of a word that matches one of the (non-phrase) terms, as {start, end} relative to the word,
	 * or null if no term matches. That is the whole word, except where a term only occurs inside it: then
	 * it is just that occurrence, e.g. "Total" in "orderTotal".
	 */
	public int[] matchSpan(final String word) {

		final String lower = word.toLowerCase(Locale.ROOT);
		int[] best         = null;

		for (final Term term : terms) {

			final int[] span = term.phrase() ? null : matchSpan(term, lower);
			if (span != null && (best == null || span[1] - span[0] > best[1] - best[0])) {

				best = span;
			}
		}

		// lower-casing changed the length (rare Unicode cases): the offsets do not fit the word
		if (best != null && lower.length() != word.length()) {

			return new int[] { 0, word.length() };
		}

		return best;
	}

	// ----- private methods -----
	private static Term word(final String word) {

		final int length   = word.length();
		final int maxEdits = length < MIN_FUZZY_LENGTH ? 0 : length < TWO_EDITS_LENGTH ? 1 : 2;

		return new Term(word, false, length >= MIN_PREFIX_LENGTH, length >= MIN_PREFIX_LENGTH, maxEdits);
	}

	private static List<String> words(final String text) {

		final List<String> words = new ArrayList<>();
		final Matcher matcher    = WORD.matcher(text);

		while (matcher.find()) {

			words.add(matcher.group().toLowerCase(Locale.ROOT));
		}

		return words;
	}

	/**
	 * The matching part of a lower-cased word for one term, or null.
	 */
	private static int[] matchSpan(final Term term, final String word) {

		final String text = term.text();

		if (word.equals(text)
			|| (term.prefix() && word.startsWith(text))
			|| (term.maxEdits() > 0 && Math.abs(word.length() - text.length()) <= term.maxEdits() && editDistance(word, text, term.maxEdits()) <= term.maxEdits())) {

			return new int[] { 0, word.length() };
		}

		final int position = term.infix() ? word.indexOf(text) : -1;
		if (position >= 0) {

			return new int[] { position, position + text.length() };
		}

		return null;
	}

	/**
	 * Levenshtein distance with transpositions counted as one edit (as Lucene's fuzzy query does), or
	 * limit + 1 as soon as it exceeds the limit.
	 */
	static int editDistance(final String a, final String b, final int limit) {

		final int n = a.length();
		final int m = b.length();
		int[] previous2 = new int[m + 1];
		int[] previous  = new int[m + 1];
		int[] current   = new int[m + 1];

		for (int j = 0; j <= m; j++) {

			previous[j] = j;
		}

		for (int i = 1; i <= n; i++) {

			current[0] = i;
			int rowMinimum = current[0];

			for (int j = 1; j <= m; j++) {

				final int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
				int value      = Math.min(Math.min(previous[j] + 1, current[j - 1] + 1), previous[j - 1] + cost);

				if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {

					value = Math.min(value, previous2[j - 2] + 1);
				}

				current[j] = value;
				rowMinimum = Math.min(rowMinimum, value);
			}

			if (rowMinimum > limit) {

				return limit + 1;
			}

			final int[] recycled = previous2;
			previous2 = previous;
			previous  = current;
			current   = recycled;
		}

		return previous[m];
	}
}
