package org.structr.files.text;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author Christian Morgner
 */
public class FulltextTokenizer extends Writer {

	private static final Set<Character> specialChars = new LinkedHashSet<>();

	final StringBuilder rawText    = new StringBuilder();
	final StringBuilder wordBuffer = new StringBuilder();
	final Set<String> words        = new LinkedHashSet<>();

	static {

		specialChars.add('_');
		specialChars.add('ä');
		specialChars.add('ö');
		specialChars.add('ü');
		specialChars.add('Ä');
		specialChars.add('Ö');
		specialChars.add('Ü');
		specialChars.add('ß');
		specialChars.add('§');
		specialChars.add('-');
		specialChars.add('%');
		specialChars.add('/');
		specialChars.add('@');
		specialChars.add('$');
		specialChars.add('€');
		specialChars.add('æ');
		specialChars.add('¢');
		specialChars.add('.');
		specialChars.add(',');
	}

	@Override
	public void write(final char[] cbuf, final int off, final int len) throws IOException {

		// FIXME: we cannot be sure that the data in the buffer is not truncated
		// so we need to collect the data of more than one write call.
		final int limit  = off + len;
		final int length = Math.min(limit, cbuf.length);

		for (int i=off; i<length; i++) {

			final char c = cbuf[i];

			if (!Character.isAlphabetic(c) && !Character.isDigit(c) && !specialChars.contains(c)) {

				// split character
				flush();

			} else {

				wordBuffer.append(c);
			}
		}

		// make raw text available
		rawText.append(new String(cbuf, off, len));
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
		if (StringUtils.isNotBlank(word) && word.length() > 4) {

			// check for numbers
			if (word.contains(".") || word.contains(",")) {

				// try to separate numbers
				if (word.matches("[\\-0-9\\.,]+")) {

					words.add(word);

				} else {

					final String[] parts = word.split("[\\.,]+");
					final int len        = parts.length;

					for (int i=0; i<len; i++) {

						final String part = parts[i].trim();

						if (StringUtils.isNotBlank(part)) {

							words.add(part.toLowerCase());
						}
					}
				}

			} else {

				words.add(word.toLowerCase());
			}
		}

		wordBuffer.setLength(0);
	}

	@Override
	public void close() throws IOException {
		flush();
	}
}
