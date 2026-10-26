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
package org.structr.extraction.parser.figures;

import org.structr.extraction.parser.figures.FigureCaptions.Caption;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Finds the sentences of a text that refer to a figure, e.g. "As Fig. 3 shows, the error drops with ...".
 * They say what a figure is meant to show, which its caption often does not.
 */
final class FigureMentions {

	private static final int MAX_MENTIONS = 10;
	private static final int MAX_LENGTH   = 600;

	/** Words with a period that do not end a sentence. */
	private static final Set<String> ABBREVIATIONS = Set.of("fig", "figs", "tab", "tabs", "eq", "eqs", "sec", "secs", "ch", "ref", "refs", "no", "nos", "vol", "pp", "cf",
		"vs", "al", "e.g", "i.e", "etc", "approx", "resp", "abb", "bzw", "vgl", "z.b", "d.h", "u.a", "ca", "nr", "s");

	private FigureMentions() {
	}

	static List<String> find(final String text, final Caption caption) {

		final Set<String> sentences = new LinkedHashSet<>();
		final Matcher matcher       = FigureCaptions.mentionPattern(caption).matcher(text);

		while (matcher.find() && sentences.size() < MAX_MENTIONS) {

			final int start = sentenceStart(text, matcher.start());
			final int end   = sentenceEnd(text, matcher.end());
			String sentence = text.substring(start, end).replaceAll("\\s+", " ").strip();

			if (sentence.length() > MAX_LENGTH) {

				// keep the part around the reference
				final int at   = Math.max(0, matcher.start() - start - MAX_LENGTH / 2);
				final int stop = Math.min(sentence.length(), at + MAX_LENGTH);

				sentence = (at > 0 ? "…" : "") + sentence.substring(at, stop).strip() + (stop < sentence.length() ? "…" : "");
			}

			if (!sentence.isEmpty()) {

				sentences.add(sentence);
			}
		}

		return new ArrayList<>(sentences);
	}

	// ----- private methods -----
	private static int sentenceStart(final String text, final int from) {

		for (int i = from - 1; i > 0; i--) {

			final char c = text.charAt(i);
			if (c == '\n' && text.charAt(i - 1) == '\n') {

				return i + 1;
			}

			if (Character.isWhitespace(c) && isSentenceEnd(text, i - 1)) {

				return i + 1;
			}
		}

		return 0;
	}

	private static int sentenceEnd(final String text, final int from) {

		for (int i = from; i < text.length(); i++) {

			final char c = text.charAt(i);
			if (c == '\n' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {

				return i;
			}

			if (isSentenceEnd(text, i) && (i + 1 == text.length() || Character.isWhitespace(text.charAt(i + 1)))) {

				return i + 1;
			}
		}

		return text.length();
	}

	/**
	 * Whether the character at the index ends a sentence: a period, question mark or exclamation mark
	 * that does not end an abbreviation or an initial.
	 */
	private static boolean isSentenceEnd(final String text, final int index) {

		final char c = text.charAt(index);
		if (c == '?' || c == '!') {

			return true;
		}

		if (c != '.') {

			return false;
		}

		int wordStart = index;

		while (wordStart > 0 && !Character.isWhitespace(text.charAt(wordStart - 1)) && text.charAt(wordStart - 1) != '(') {

			wordStart--;
		}

		final String word = text.substring(wordStart, index).toLowerCase(Locale.ROOT);

		// an initial ("J. Smith") or an abbreviation

		return !(word.length() == 1 && Character.isLetter(word.charAt(0))) && !ABBREVIATIONS.contains(word);
	}
}
