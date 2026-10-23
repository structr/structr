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
package org.structr.extraction.chunking;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * LangChain's RecursiveCharacterTextSplitter on spans: a span that is too large is divided at the first
 * separator that occurs in it, and each part that is still too large at the next separator, down to a
 * hard cut. The pieces are then packed into chunks with overlap.
 *
 * A separator stays at the end of the piece before it, so the pieces of a span cover it without gaps.
 */
final class RecursiveTextSplitter {

	/** Paragraphs, lines, sentences, words, then a hard cut. */
	static final List<Separator> DEFAULT = List.of(new Separator.Literal("\n\n"), new Separator.Literal("\n"), new Separator.Sentences(), new Separator.Literal(" "), new Separator.Anywhere());

	/** Sentences, words, then a hard cut. */
	static final List<Separator> SENTENCES = List.of(new Separator.Sentences(), new Separator.Literal(" "), new Separator.Anywhere());

	private final ChunkSizeMeasure measure;
	private final int chunkSize;
	private final int overlap;
	private final List<Separator> separators;
	private final Locale locale;

	RecursiveTextSplitter(final ChunkSizeMeasure measure, final int chunkSize, final int overlap, final List<Separator> separators, final Locale locale) {

		this.measure    = measure;
		this.chunkSize  = chunkSize;
		this.overlap    = overlap;
		this.separators = separators;
		this.locale     = locale;
	}

	/**
	 * Separators from user-supplied strings: an empty string cuts anywhere, and a hard cut is always
	 * added as the last resort.
	 */
	static List<Separator> fromStrings(final List<String> values) {

		final List<Separator> result = new ArrayList<>();

		for (final String value : values) {

			result.add(value.isEmpty() ? new Separator.Anywhere() : new Separator.Literal(value));
		}

		if (!(result.getLast() instanceof Separator.Anywhere)) {

			result.add(new Separator.Anywhere());
		}

		return result;
	}

	/**
	 * Chunks of the span.
	 */
	List<TextSpan> split(final TextSpan span) {

		return SpanMerger.merge(pieces(span), measure, chunkSize, overlap);
	}

	/**
	 * Pieces of the span, each at most the chunk size, not yet packed into chunks.
	 */
	List<TextSpan> pieces(final TextSpan span) {

		final List<TextSpan> pieces = new ArrayList<>();

		collect(span, 0, pieces);

		return pieces;
	}

	// ----- private methods -----
	private void collect(final TextSpan span, final int separatorIndex, final List<TextSpan> pieces) {

		if (span.isEmpty()) {

			return;
		}

		if (measure.length(span) <= chunkSize) {

			pieces.add(span);

			return;
		}

		if (separatorIndex >= separators.size()) {

			pieces.addAll(measure.cut(span, chunkSize));

			return;
		}

		final Separator separator = separators.get(separatorIndex);
		if (separator instanceof Separator.Anywhere) {

			pieces.addAll(measure.cut(span, chunkSize));

			return;
		}

		final List<TextSpan> parts = divide(span, separator);
		if (parts.size() <= 1) {

			collect(span, separatorIndex + 1, pieces);

			return;
		}

		for (final TextSpan part : parts) {

			collect(part, separatorIndex + 1, pieces);
		}
	}

	private List<TextSpan> divide(final TextSpan span, final Separator separator) {

		final List<TextSpan> parts = new ArrayList<>();
		final String text      = measure.text();

		if (separator instanceof Separator.Literal literal) {

			// the search is bounded to the span: unbounded, every span without the separator would scan the
			// rest of the text, which makes splitting large documents quadratic
			int start = span.start();
			int index = text.indexOf(literal.value(), start, span.end());

			while (index >= 0) {

				final int end = index + literal.value().length();

				parts.add(new TextSpan(start, end));
				start = end;
				index = text.indexOf(literal.value(), start, span.end());
			}

			if (start < span.end()) {

				parts.add(new TextSpan(start, span.end()));
			}

		} else if (separator instanceof Separator.Sentences) {

			final BreakIterator sentences = BreakIterator.getSentenceInstance(locale);

			sentences.setText(text.substring(span.start(), span.end()));

			int start = sentences.first();

			for (int end = sentences.next(); end != BreakIterator.DONE; start = end, end = sentences.next()) {

				parts.add(new TextSpan(span.start() + start, span.start() + end));
			}
		}

		return parts;
	}

	/**
	 * Where a span may be divided.
	 */
	sealed interface Separator {

		/** At a literal string, e.g. a blank line. */
		record Literal(String value) implements Separator {
		}

		/** At sentence boundaries, as determined for the document language. */
		record Sentences() implements Separator {
		}

		/** Anywhere: a hard cut at the size limit. */
		record Anywhere() implements Separator {
		}
	}
}
