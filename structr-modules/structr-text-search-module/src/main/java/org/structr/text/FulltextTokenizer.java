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
package org.structr.text;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *
 *
 */
public class FulltextTokenizer extends Writer {

	public static final Set<Character> SpecialChars = new LinkedHashSet<>();

	private final StringBuilder rawText    = new StringBuilder();
	private final StringBuilder wordBuffer = new StringBuilder();
	private final List<String> words       = new LinkedList<>();
	private char lastCharacter             = 0;
	private int consecutiveCharCount       = 0;

	static {

		SpecialChars.add('_');
		SpecialChars.add('ä');
		SpecialChars.add('ö');
		SpecialChars.add('ü');
		SpecialChars.add('Ä');
		SpecialChars.add('Ö');
		SpecialChars.add('Ü');
		SpecialChars.add('ß');
		SpecialChars.add('-');
		SpecialChars.add('@');
		SpecialChars.add('.');
		SpecialChars.add(',');
	}

	@Override
	public void write(final char[] cbuf, final int off, final int len) throws IOException {

		final int limit  = off + len;
		final int length = Math.min(limit, cbuf.length);

		for (int i=off; i<length; i++) {

			final char c = cbuf[i];

			// remove occurrences of more than 10 identical chars in a row
			if (c == lastCharacter) {

				if (consecutiveCharCount++ >= 10) {
					continue;
				}

			} else {

				consecutiveCharCount = 0;
			}

			if (!Character.isAlphabetic(c) && !Character.isDigit(c) && !SpecialChars.contains(c)) {

				flush();

				if (Character.isWhitespace(c)) {

					rawText.append(c);

				} else {

					rawText.append(" ");
				}

			} else {

				wordBuffer.append(c);
				rawText.append(c);
			}

			lastCharacter = c;
		}
	}

	public String getRawText() {
		return rawText.toString().trim();
	}

	@Override
	public void flush() throws IOException {

		String word = wordBuffer.toString().trim();

		if (accept(word)) {

			final String[] parts = word.split("[\\.,]+");
			final int len        = parts.length;

			for (int i=0; i<len; i++) {

				String part = parts[i].trim();
				part        = part.replaceAll("[\\-/]+", "");

				if (StringUtils.isNotBlank(part) && !StringUtils.isNumeric(part)) {

					words.add(part.toLowerCase());
				}
			}
		}

		wordBuffer.setLength(0);

	}

	@Override
	public void close() throws IOException {
		flush();
	}

	private boolean accept(final String word) {

		if (word == null) {
			return false;
		}

		int letters = 0;

		for (final char c : word.toCharArray()) {

			if (Character.isDigit(c)) {
				return false;
			}

			// might not be suited to handle non-latin characters..
			if (Character.isLetter(c)) {
				letters++;
			}
		}

		return letters >= 3;
	}
}
