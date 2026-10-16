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

import org.structr.extraction.api.document.DocumentBlock;
import org.structr.extraction.api.document.DocumentBlockType;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits along the document structure.
 *
 * <ul>
 *   <li>A chunk never spans two major sections: headings of level 1 and 2 (or the top level the
 *       document uses, if it starts deeper) always start a new chunk.</li>
 *   <li>Within a section, whole blocks are packed into chunks; a block is only divided if it is larger
 *       than a chunk on its own, recursively at paragraphs, lines, sentences and words.</li>
 *   <li>A heading never ends a chunk: it is kept together with the block that follows it.</li>
 * </ul>
 */
final class DocumentStructureSplitter {

	/** Headings up to this level always start a new chunk. */
	private static final int SECTION_LEVEL = 2;

	private final ChunkSizeMeasure measure;
	private final RecursiveTextSplitter recursive;
	private final int chunkSize;
	private final int overlap;

	DocumentStructureSplitter(final ChunkSizeMeasure measure, final RecursiveTextSplitter recursive, final int chunkSize, final int overlap) {

		this.measure   = measure;
		this.recursive = recursive;
		this.chunkSize = chunkSize;
		this.overlap   = overlap;
	}

	List<TextSpan> split(final List<DocumentBlock> blocks) {

		final List<TextSpan> chunks = new ArrayList<>();

		for (final List<TextSpan> section : sections(blocks)) {

			final List<TextSpan> pieces = new ArrayList<>();

			for (final TextSpan unit : section) {

				pieces.addAll(recursive.pieces(unit));
			}

			chunks.addAll(SpanMerger.merge(pieces, measure, chunkSize, overlap));
		}

		return chunks;
	}

	// ----- private methods -----
	/**
	 * The sections of a document, each a list of units: a block, or headings together with the block
	 * after them.
	 */
	private static List<List<TextSpan>> sections(final List<DocumentBlock> blocks) {

		final int sectionLevel           = Math.max(SECTION_LEVEL, topHeadingLevel(blocks));
		final List<List<TextSpan>> sections  = new ArrayList<>();
		List<TextSpan> section               = new ArrayList<>();
		Integer headingStart             = null;

		for (final DocumentBlock block : blocks) {

			final boolean heading = block.type() == DocumentBlockType.HEADING;

			// Headings without content between them stay together and only the first of them can start a
			// section: a document title directly followed by its first section heading must not become a
			// chunk of its own.
			if (heading && block.level() <= sectionLevel && headingStart == null) {

				if (!section.isEmpty()) {

					sections.add(section);
				}

				section = new ArrayList<>();
			}

			if (heading) {

				// consecutive headings stay together, up to the first block of content
				if (headingStart == null) {

					headingStart = block.startOffset();
				}

				continue;
			}

			section.add(new TextSpan(headingStart != null ? headingStart : block.startOffset(), block.endOffset()));
			headingStart = null;
		}

		if (headingStart != null) {

			// a heading at the very end, without content
			section.add(new TextSpan(headingStart, blocks.getLast().endOffset()));
		}

		if (!section.isEmpty()) {

			sections.add(section);
		}

		return sections;
	}

	private static int topHeadingLevel(final List<DocumentBlock> blocks) {

		return blocks.stream().filter(b -> b.type() == DocumentBlockType.HEADING).mapToInt(DocumentBlock::level).min().orElse(SECTION_LEVEL);
	}
}
