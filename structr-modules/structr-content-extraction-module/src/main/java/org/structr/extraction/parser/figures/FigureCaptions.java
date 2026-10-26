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

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recognizes captions ("Figure 3: ...", "Fig. 3.", "TABLE II", "Abbildung 2 – ...") and the places in the
 * text that refer to them ("as Fig. 3 shows").
 *
 * A caption starts with a label word and a number. If punctuation or the end of the line follows the
 * number, the line is a caption. Without punctuation ("Figure 3 Results of the ..."), a line is a caption
 * only if a figure is right next to it, since a sentence of the text can start the same way ("Figure 3
 * shows ...").
 */
final class FigureCaptions {

	private static final String WORDS  = "Figure|Fig\\.?|Table|Tab\\.?|Tabelle|Abbildung|Abb\\.?|Chart|Exhibit|Scheme|Plate";
	private static final String NUMBER = "(\\d+(?:\\.\\d+)*[a-z]?|[IVXLC]+)\\b";

	private static final Pattern CAPTION = Pattern.compile("^\\s*(" + WORDS + ")\\s*" + NUMBER + "(\\s*[:.|–—-]|\\s*$)?", Pattern.CASE_INSENSITIVE);

	private FigureCaptions() {
	}

	/**
	 * A recognized caption start.
	 *
	 * @param label  the normalized label, e.g. "Figure 3"
	 * @param word   the normalized label word, e.g. "Figure"
	 * @param number the number, e.g. "3" or "II"
	 * @param table  whether it is the caption of a table
	 * @param strong whether punctuation or the end of the line follows the number
	 */
	record Caption(String label, String word, String number, boolean table, boolean strong) {
	}

	/**
	 * The caption a line of text starts with, or null.
	 */
	static Caption parse(final String line) {

		final Matcher matcher = CAPTION.matcher(line);
		if (!matcher.find()) {

			return null;
		}

		final String word   = normalizeWord(matcher.group(1));
		final String number = matcher.group(2);

		// roman numerals only in upper case ("TABLE II"), so that "Table I" is not read from "Table in"
		if (number.chars().allMatch(Character::isLetter) && !number.equals(number.toUpperCase(Locale.ROOT))) {

			return null;
		}

		final boolean table = word.equals("Table") || word.equals("Tabelle");

		return new Caption(word + " " + number, word, number, table, matcher.group(3) != null);
	}

	/**
	 * A pattern that finds references to a label in the text, in all spellings of its word: for "Figure 3"
	 * also "Fig. 3", "Figs. 3", "Figure 3b"; not "Figure 30" or "Figure 3.1".
	 */
	static Pattern mentionPattern(final Caption caption) {

		final String words = switch (caption.word()) {

			case "Figure"    -> "Figures?|Figs?\\.?";
			case "Table"     -> "Tables?|Tabs?\\.?";
			case "Tabelle"   -> "Tabellen?|Tab\\.?";
			case "Abbildung" -> "Abbildung(?:en)?|Abb\\.?";
			default          -> Pattern.quote(caption.word()) + "s?";
		};

		return Pattern.compile("\\b(?:" + words + ")\\s*" + Pattern.quote(caption.number()) + "(?![0-9])(?!\\.\\d)(?![IVXLC])", Pattern.CASE_INSENSITIVE);
	}

	// ----- private methods -----
	private static String normalizeWord(final String word) {

		final String lower = word.toLowerCase(Locale.ROOT).replace(".", "");

		return switch (lower) {

			case "fig", "figure" -> "Figure";
			case "tab", "table"  -> "Table";
			case "abb"           -> "Abbildung";
			default              -> Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
		};
	}
}
