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

import org.structr.extraction.api.search.SearchSnippet;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the places in a text that a {@link ContentSearchQuery} matches, and cuts a snippet around each:
 * the match plus up to a given number of characters on each side, cut at word boundaries and never across
 * a paragraph break. Successor of the keyword-in-context snippets of the removed text-search module.
 */
public final class SearchSnippets {

	private static final Pattern WORD      = Pattern.compile("[\\p{L}\\p{N}]+");
	private static final Pattern SPACES    = Pattern.compile("\\s+");
	private static final String PARAGRAPH  = "\n\n";

	private SearchSnippets() {
	}

	/**
	 * Snippets for the matches of a query in a text, in text order.
	 *
	 * @param text         the text to search
	 * @param query        the parsed query
	 * @param page         the page of the text, copied into every snippet; may be null
	 * @param contextChars at most this many characters of context on each side
	 * @param maxSnippets  at most this many snippets; a match inside the context of the previous one starts no new one
	 */
	public static List<SearchSnippet> find(final String text, final ContentSearchQuery query, final Integer page, final int contextChars, final int maxSnippets) {

		if (text == null || text.isEmpty() || query.isEmpty() || maxSnippets <= 0) {

			return new ArrayList<>();
		}

		return snippets(text, matches(text, query), page, contextChars, maxSnippets);
	}

	/**
	 * Snippets for the matches of a pattern in a text, in text order, cut like those of a query.
	 *
	 * @see #exactPattern(String, boolean)
	 */
	public static List<SearchSnippet> find(final String text, final Pattern pattern, final Integer page, final int contextChars, final int maxSnippets) {

		final List<int[]> spans = new ArrayList<>();

		if (text != null) {

			final Matcher found = pattern.matcher(text);

			while (found.find()) {

				if (found.end() > found.start()) {

					spans.add(new int[] { found.start(), found.end() });
				}
			}
		}

		return snippets(text, spans, page, contextChars, maxSnippets);
	}

	/**
	 * A pattern that finds the input itself in a text, where any run of whitespace in the input matches any
	 * run of whitespace in the text. Null if the input is blank.
	 */
	public static Pattern exactPattern(final String input, final boolean ignoreCase) {

		if (input == null || input.isBlank()) {

			return null;
		}

		final List<String> parts = new ArrayList<>();

		for (final String part : SPACES.split(input.strip())) {

			parts.add(Pattern.quote(part));
		}

		return Pattern.compile(String.join("\\s+", parts), ignoreCase ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0);
	}

	// ----- private methods -----
	private static List<SearchSnippet> snippets(final String text, final List<int[]> spans, final Integer page, final int contextChars, final int maxSnippets) {

		final List<SearchSnippet> snippets = new ArrayList<>();

		if (text == null || text.isEmpty() || maxSnippets <= 0) {

			return snippets;
		}

		int coveredUntil = 0;

		for (final int[] span : spans) {

			if (span[0] < coveredUntil) {

				continue;
			}

			final int start     = contextStart(text, span[0], contextChars);
			final int end       = contextEnd(text, span[1], contextChars);
			final String before = clean(text.substring(start, span[0])).stripLeading();
			final String after  = clean(text.substring(span[1], end)).stripTrailing();

			snippets.add(new SearchSnippet(before, text.substring(span[0], span[1]), after, page));

			if (snippets.size() >= maxSnippets) {

				break;
			}

			coveredUntil = end;
		}

		return snippets;
	}

	/**
	 * The spans [start, end) of all matches, in text order: phrases where they occur (ignoring case),
	 * single words where the query matches them, or only the matching part of a word where a term occurs
	 * inside it.
	 */
	private static List<int[]> matches(final String text, final ContentSearchQuery query) {

		final List<int[]> spans = new ArrayList<>();

		for (final ContentSearchQuery.Term term : query.terms()) {

			if (term.phrase()) {

				final Matcher found = ContentSearchQuery.phrasePattern(term).matcher(text);

				while (found.find()) {

					spans.add(new int[] { found.start(), found.end() });
				}
			}
		}

		final Matcher word = WORD.matcher(text);

		while (word.find()) {

			final int[] span = query.matchSpan(word.group());
			if (span != null) {

				spans.add(new int[] { word.start() + span[0], word.start() + span[1] });
			}
		}

		spans.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(b[1], a[1]));

		return spans;
	}

	private static int contextStart(final String text, final int matchStart, final int contextChars) {

		int start           = Math.max(0, matchStart - contextChars);
		final int paragraph = text.lastIndexOf(PARAGRAPH, matchStart - 1);

		if (paragraph >= 0 && paragraph + PARAGRAPH.length() > start) {

			return paragraph + PARAGRAPH.length();
		}

		// do not start inside a word: skip a cut word, but keep the rest of the word a match lies in
		if (start > 0 && isWordChar(text.charAt(start - 1))) {

			int next = start;

			while (next < matchStart && isWordChar(text.charAt(next))) {

				next++;
			}

			if (next < matchStart) {

				return next;
			}

			// bounded, since a "word" can be a long token such as base64 data
			while (start > 0 && isWordChar(text.charAt(start - 1)) && matchStart - start < 2 * contextChars) {

				start--;
			}
		}

		return start;
	}

	private static int contextEnd(final String text, final int matchEnd, final int contextChars) {

		int end             = Math.min(text.length(), matchEnd + contextChars);
		final int paragraph = text.indexOf(PARAGRAPH, matchEnd);

		if (paragraph >= 0 && paragraph < end) {

			return paragraph;
		}

		// do not end inside a word: drop a cut word, but keep the rest of the word a match lies in
		if (end < text.length() && isWordChar(text.charAt(end))) {

			int previous = end;

			while (previous > matchEnd && isWordChar(text.charAt(previous - 1))) {

				previous--;
			}

			if (previous > matchEnd) {

				return previous;
			}

			while (end < text.length() && isWordChar(text.charAt(end)) && end - matchEnd < 2 * contextChars) {

				end++;
			}
		}

		return end;
	}

	private static boolean isWordChar(final char c) {

		return Character.isLetterOrDigit(c);
	}

	/** Line breaks inside the context become spaces. */
	private static String clean(final String context) {

		return SPACES.matcher(context).replaceAll(" ");
	}
}
