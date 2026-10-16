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
package org.structr.extraction.chunking.token;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.IntArrayList;
import org.structr.extraction.chunking.TextSpan;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token counting with JTokkit, a pure Java implementation of OpenAI's tiktoken encodings.
 *
 * Counts are exact for OpenAI models and an approximation for other embedding models, whose tokenizers
 * differ. Special tokens such as "&lt;|endoftext|&gt;" in the text are counted as ordinary text.
 */
public final class TokenCounter {

	private static final EncodingRegistry registry   = Encodings.newLazyEncodingRegistry();
	private static final Map<String, TokenCounter> encodings = new ConcurrentHashMap<>();

	private final Encoding encoding;

	private TokenCounter(final Encoding encoding) {

		this.encoding = encoding;
	}

	/**
	 * The token counter for an encoding name such as "cl100k_base" or "o200k_base".
	 *
	 * @throws IllegalArgumentException if the encoding is unknown
	 */
	public static TokenCounter forEncoding(final String name) {

		final String key = name != null ? name.trim().toLowerCase(Locale.ROOT) : "";

		return encodings.computeIfAbsent(key, k -> new TokenCounter(registry.getEncoding(k).orElseThrow(() -> new IllegalArgumentException("Unknown token encoding '" + name + "', expected one of cl100k_base, o200k_base, p50k_base, r50k_base."))));
	}

	public int count(final String text) {

		return encoding.countTokensOrdinary(text);
	}

	/**
	 * Character offsets of the token boundaries of a text: the start of every token, plus the end of the
	 * text, so a text of n tokens has n + 1 boundaries. A boundary inside a character (a character such as
	 * an emoji can consist of several tokens) is snapped down to that character's start.
	 */
	public int[] boundaries(final String text) {

		return toCharOffsets(text, byteOffsets(text));
	}

	/**
	 * Windows of {@code size} tokens, each starting {@code size - overlap} tokens after the previous one,
	 * as character spans of the text.
	 *
	 * A character can consist of several tokens (an emoji, for example). A window boundary inside such a
	 * character moves to the character's start, on both sides of the boundary, so no text is lost; the
	 * window after the boundary can then hold the few extra tokens of that character.
	 */
	public List<TextSpan> windows(final String text, final int size, final int overlap) {

		final List<TextSpan> windows = new ArrayList<>();
		final int[] boundaries   = boundaries(text);
		final int tokenCount     = boundaries.length - 1;
		final int step           = size - overlap;

		for (int first = 0; first < tokenCount; first += step) {

			final int last = Math.min(first + size, tokenCount);
			if (boundaries[last] > boundaries[first]) {

				windows.add(new TextSpan(boundaries[first], boundaries[last]));
			}

			if (last == tokenCount) {

				break;
			}
		}

		return windows;
	}

	/**
	 * Cuts a text to at most the given number of tokens, preferably after a sentence, otherwise after a
	 * word. Returns the text unchanged if it fits.
	 */
	public String truncate(final String text, final int maxTokens) {

		if (count(text) <= maxTokens) {

			return text;
		}

		final int[] boundaries = boundaries(text);
		final int cut          = boundaries[Math.min(maxTokens, boundaries.length - 1)];

		return text.substring(0, niceCut(text, cut)).stripTrailing();
	}

	// ----- private methods -----
	private int[] byteOffsets(final String text) {

		final IntArrayList tokens = encoding.encodeOrdinary(text);
		final int count           = tokens.size();
		final int[] byteOffsets   = new int[count + 1];
		final IntArrayList single = new IntArrayList(1);

		for (int i = 0; i < count; i++) {

			single.clear();
			single.add(tokens.get(i));

			byteOffsets[i + 1] = byteOffsets[i] + encoding.decodeBytes(single).length;
		}

		return byteOffsets;
	}

	/**
	 * The last sentence end, or failing that the last word end, at or before the cut, unless that would
	 * throw away more than half of what fits.
	 */
	private static int niceCut(final String text, final int cut) {

		final BreakIterator sentences = BreakIterator.getSentenceInstance(Locale.ROOT);

		sentences.setText(text);

		final int sentence = sentences.isBoundary(cut) ? cut : sentences.preceding(cut);
		if (sentence != BreakIterator.DONE && sentence >= cut / 2 && sentence > 0) {

			return sentence;
		}

		for (int i = cut; i > cut / 2; i--) {

			if (Character.isWhitespace(text.charAt(i - 1))) {

				return i;
			}
		}

		return cut;
	}

	/**
	 * Maps ascending UTF-8 byte offsets of a text to character offsets.
	 */
	private static int[] toCharOffsets(final String text, final int[] byteOffsets) {

		final int[] charOffsets = new int[byteOffsets.length];
		int bytes               = 0;
		int chars               = 0;
		int next                = 0;

		while (next < byteOffsets.length) {

			if (byteOffsets[next] <= bytes || chars >= text.length()) {

				charOffsets[next] = chars;
				next++;
				continue;
			}

			final int codePoint = text.codePointAt(chars);
			final int width     = utf8Length(codePoint);

			if (bytes + width > byteOffsets[next]) {

				// the boundary falls inside this character
				charOffsets[next] = chars;
				next++;
				continue;
			}

			bytes += width;
			chars += Character.charCount(codePoint);
		}

		return charOffsets;
	}

	private static int utf8Length(final int codePoint) {

		if (codePoint < 0x80) {

			return 1;
		}

		if (codePoint < 0x800) {

			return 2;
		}

		return codePoint < 0x10000 ? 3 : 4;
	}
}
