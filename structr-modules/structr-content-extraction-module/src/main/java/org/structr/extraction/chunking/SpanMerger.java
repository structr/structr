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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Packs consecutive pieces into chunks of at most the chunk size, with overlap: each new chunk starts
 * with as many trailing pieces of the previous chunk as fit into the overlap. This is LangChain's merge
 * step, on spans.
 *
 * Every piece is measured once and the sizes are added up. For characters that is exact. For tokens it
 * is very close but not exact, since a tokenizer can merge characters across a piece boundary into one
 * token; counting every candidate chunk again would make splitting quadratic.
 */
final class SpanMerger {

	private SpanMerger() {
	}

	static List<TextSpan> merge(final List<TextSpan> pieces, final ChunkSizeMeasure measure, final int chunkSize, final int overlap) {

		final List<TextSpan> chunks     = new ArrayList<>();
		final Deque<TextSpan> current   = new ArrayDeque<>();
		final Deque<Integer> sizes  = new ArrayDeque<>();
		int total                   = 0;

		for (final TextSpan piece : pieces) {

			final int size = measure.length(piece);
			if (total + size > chunkSize && !current.isEmpty()) {

				chunks.add(new TextSpan(current.getFirst().start(), current.getLast().end()));

				// keep the tail of the previous chunk as overlap, as long as the new piece still fits
				while (!current.isEmpty() && (total > overlap || total + size > chunkSize)) {

					current.removeFirst();
					total -= sizes.removeFirst();
				}
			}

			current.addLast(piece);
			sizes.addLast(size);
			total += size;
		}

		if (!current.isEmpty()) {

			chunks.add(new TextSpan(current.getFirst().start(), current.getLast().end()));
		}

		return chunks;
	}
}
