package org.structr.files.text;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.language.LanguageIdentifier;

/**
 *
 * @author Christian Morgner
 */
public class FulltextTokenizer extends Writer {

	public static final Set<Character> SpecialChars = new LinkedHashSet<>();

	private final StringBuilder rawText      = new StringBuilder();
	private final StringBuilder wordBuffer   = new StringBuilder();
	private final Map<String, Integer> words = new LinkedHashMap<>();
	private String language                  = "de";
	private char lastCharacter               = 0;
	private int consecutiveCharCount         = 0;

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

	public String getLanguage() {
		return language;
	}

	public String getRawText() {
		return rawText.toString();
	}

	public Set<String> getWords() {
		return words.keySet();
	}

	public Map<String, Integer> getWordsWithFrequency() {
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

	// ----- private methods -----
	private void addWord(final String word) {

		final int length = word.length();
		if (length > 3 && length < 40) {

			final Integer count = words.get(word);
			if (count == null) {

				words.put(word, 1);

			} else {

				words.put(word, count+1);
			}
		}
	}
}
