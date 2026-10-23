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

import org.structr.extraction.api.options.SizeUnit;
import org.structr.extraction.chunking.token.TokenCounter;
import java.util.ArrayList;
import java.util.List;

/**
 * Measures spans of a text in the configured unit, and cuts a span into pieces of a maximum size where
 * no separator helps.
 */
final class ChunkSizeMeasure {

	private final String text;
	private final SizeUnit unit;
	private final TokenCounter tokens;

	ChunkSizeMeasure(final String text, final SizeUnit unit, final TokenCounter tokens) {

		this.text   = text;
		this.unit   = unit;
		this.tokens = tokens;
	}

	String text() {

		return text;
	}

	int length(final TextSpan span) {

		if (unit == SizeUnit.CHARACTERS) {

			return span.end() - span.start();
		}

		return tokens.count(text.substring(span.start(), span.end()));
	}

	/**
	 * Cuts a span into consecutive pieces of at most {@code size} units, at character or token
	 * boundaries, without splitting a surrogate pair.
	 */
	List<TextSpan> cut(final TextSpan span, final int size) {

		final List<TextSpan> pieces = new ArrayList<>();

		if (unit == SizeUnit.CHARACTERS) {

			int start = span.start();

			while (start < span.end()) {

				int end = Math.min(span.end(), start + size);
				if (end < span.end() && end > start + 1 && Character.isHighSurrogate(text.charAt(end - 1))) {

					end--;
				}

				pieces.add(new TextSpan(start, end));
				start = end;
			}

			return pieces;
		}

		for (final TextSpan window : tokens.windows(text.substring(span.start(), span.end()), size, 0)) {

			pieces.add(new TextSpan(span.start() + window.start(), span.start() + window.end()));
		}

		return pieces;
	}
}
