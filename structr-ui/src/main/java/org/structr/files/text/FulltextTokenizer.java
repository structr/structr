/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.files.text;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.language.LanguageIdentifier;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;

/**
 *
 *
 */
public class FulltextTokenizer extends Writer {

	private static final Logger logger = Logger.getLogger(FulltextTokenizer.class.getName());
	public static final Set<Character> SpecialChars = new LinkedHashSet<>();

	private final int wordCountLimit         = Services.parseInt(StructrApp.getConfigurationValue(Services.APPLICATION_FILESYSTEM_INDEXING_LIMIT), 50_000);
	private final int wordMinLength          = Services.parseInt(StructrApp.getConfigurationValue(Services.APPLICATION_FILESYSTEM_INDEXING_MINLENGTH), 4);
	private final int wordMaxLength          = Services.parseInt(StructrApp.getConfigurationValue(Services.APPLICATION_FILESYSTEM_INDEXING_MAXLENGTH), 40);
	private final StringBuilder rawText      = new StringBuilder();
	private final StringBuilder wordBuffer   = new StringBuilder();
	private final Set<String> words          = new LinkedHashSet<>();
	private String language                  = "de";
	private String fileName                  = null;
	private char lastCharacter               = 0;
	private int consecutiveCharCount         = 0;
	private int wordCount                    = 0;

	static {

		SpecialChars.add('_');
		SpecialChars.add('ä');
		SpecialChars.add('ö');
		SpecialChars.add('ü');
		SpecialChars.add('Ä');
		SpecialChars.add('Ö');
		SpecialChars.add('Ü');
		SpecialChars.add('ß');
		SpecialChars.add('§');
		SpecialChars.add('-');
		SpecialChars.add('%');
		SpecialChars.add('/');
		SpecialChars.add('@');
		SpecialChars.add('$');
		SpecialChars.add('€');
		SpecialChars.add('æ');
		SpecialChars.add('¢');
		SpecialChars.add('.');
		SpecialChars.add(',');
		SpecialChars.add('\'');
		SpecialChars.add('\"');
		SpecialChars.add('`');
	}

	public FulltextTokenizer(final String fileName) {
		this.fileName       = fileName;
	}

	@Override
	public void write(final char[] cbuf, final int off, final int len) throws IOException {

		if (wordCount < wordCountLimit) {

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
	}

	public String getLanguage() {
		return language;
	}

	public String getRawText() {
		return rawText.toString();
	}

	public Set<String> getWords() {
		return words;
	}

	@Override
	public void flush() throws IOException {

		final String word = wordBuffer.toString().trim();
		if (StringUtils.isNotBlank(word)) {

			// check for numbers
			if (word.contains(".") || word.contains(",")) {

				// try to separate numbers
				if (word.matches("[\\-0-9\\.,]+")) {

					addWord(word);

				} else {

					final String[] parts = word.split("[\\.,]+");
					final int len        = parts.length;

					for (int i=0; i<len; i++) {

						final String part = parts[i].trim();

						if (StringUtils.isNotBlank(part)) {

							addWord(part.toLowerCase());
						}
					}
				}

			} else {

				addWord(word.toLowerCase());
			}
		}

		wordBuffer.setLength(0);
	}

	@Override
	public void close() throws IOException {

		flush();

		final LanguageIdentifier identifier = new LanguageIdentifier(rawText.toString());
		if (identifier.isReasonablyCertain()) {

			language = identifier.getLanguage();
		}
	}

	public int getWordCount() {
		return wordCount;
	}

	// ----- private methods -----
	private void addWord(final String word) {

		final int length = word.length();
		if (length >= wordMinLength && length <= wordMaxLength) {

			words.add(word);

			wordCount++;

			if (wordCount > wordCountLimit) {

				logger.log(Level.INFO, "Indexing word count of {0} reached for {1}, no more words will be indexed. Set {2} in structr.conf to increase this limit.",

					new Object[] {
						wordCountLimit,
						fileName,
						Services.APPLICATION_FILESYSTEM_INDEXING_LIMIT
					}
				);
			}
		}
	}
}
