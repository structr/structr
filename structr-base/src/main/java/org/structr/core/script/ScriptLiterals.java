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
package org.structr.core.script;

import java.util.Set;

/** Tells a regular expression literal from a division in script blocks, for the scanners that find those blocks in templates. */
public final class ScriptLiterals {

	private static final Set<String> KEYWORDS_BEFORE_REGEX = Set.of("return", "typeof", "case", "in", "of", "delete", "void", "throw", "new", "else", "do", "instanceof", "yield", "await");

	private ScriptLiterals() {}

	/** After an operator, an opening bracket or a keyword a slash starts a regular expression, after an operand it divides. */
	public static boolean regexMayStart(final String source, final int index, final char lastSignificant, final CharSequence lastWord) {

		final char next = index + 1 < source.length() ? source.charAt(index + 1) : 0;

		// comments keep their own handling
		if (next == '/' || next == '*' || next == 0) {

			return false;
		}

		if (isIdentifierPart(lastSignificant)) {

			return KEYWORDS_BEFORE_REGEX.contains(lastWord.toString());
		}

		return switch (lastSignificant) {

			case ')', ']', '\'', '"', '`' -> false;
			default -> true;
		};
	}

	/** The index after a regular expression literal starting at the given slash, including its flags, or -1 if it does not end on the same line. */
	public static int endOfRegex(final String source, final int slash) {

		boolean inClass = false;
		boolean escaped = false;

		for (int j = slash + 1; j < source.length(); j++) {

			final char c = source.charAt(j);

			if (c == '\n' || c == '\r') {

				return -1;
			}

			if (escaped) {

				escaped = false;

			} else if (c == '\\') {

				escaped = true;

			} else if (c == '[') {

				inClass = true;

			} else if (c == ']') {

				inClass = false;

			} else if (c == '/' && !inClass) {

				int end = j + 1;

				while (end < source.length() && Character.isLetter(source.charAt(end))) {

					end++;
				}

				return end;
			}
		}

		return -1;
	}

	public static boolean isIdentifierPart(final char c) {

		return Character.isLetterOrDigit(c) || c == '_' || c == '$';
	}

}
