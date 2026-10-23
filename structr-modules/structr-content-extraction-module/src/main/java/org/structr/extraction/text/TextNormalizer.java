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
package org.structr.extraction.text;

import org.structr.extraction.api.options.NormalizeOptions;
import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Cleans up extracted text: the artifacts PDFs and scans leave behind, without touching the content.
 */
public final class TextNormalizer {

	private static final Pattern LINE_BREAKS      = Pattern.compile("\\r\\n?");
	private static final Pattern CONTROL          = Pattern.compile("[\\p{Cc}&&[^\\n\\t]]|\\u00AD");
	private static final Pattern HYPHENATION      = Pattern.compile("(\\p{L})-[ \\t]*\\n[ \\t]*(\\p{Ll})");
	private static final Pattern HORIZONTAL_SPACE = Pattern.compile("[\\t\\x0B\\f\\u00A0 ]+");
	private static final Pattern LINE_EDGES       = Pattern.compile("(?m)^ +| +$");
	private static final Pattern BLANK_LINES      = Pattern.compile("\\n{3,}");

	private TextNormalizer() {
	}

	public static String normalize(final String text, final NormalizeOptions options) {

		String result = LINE_BREAKS.matcher(text).replaceAll("\n");

		if (options.isControlCharacters()) {

			result = CONTROL.matcher(result).replaceAll("");
		}

		if (options.isUnicode()) {

			result = Normalizer.normalize(result, Normalizer.Form.NFKC);
		}

		if (options.isHyphenation()) {

			result = HYPHENATION.matcher(result).replaceAll("$1$2");
		}

		if (options.isWhitespace()) {

			result = HORIZONTAL_SPACE.matcher(result).replaceAll(" ");
			result = LINE_EDGES.matcher(result).replaceAll("");
			result = BLANK_LINES.matcher(result).replaceAll("\n\n");
			result = result.strip();
		}

		return result;
	}
}
