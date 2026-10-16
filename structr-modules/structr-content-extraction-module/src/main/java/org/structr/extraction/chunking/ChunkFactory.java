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

import org.structr.extraction.api.chunk.Chunk;
import org.structr.extraction.api.document.DocumentBlock;
import org.structr.extraction.api.document.DocumentBlockType;
import org.structr.extraction.chunking.token.TokenCounter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Turns chunk spans into chunks: trims them, finds the pages and section they belong to, adds the
 * section path if requested, and counts and hashes the text.
 */
final class ChunkFactory {

	private final String text;
	private final List<DocumentBlock> blocks;
	private final TokenCounter tokens;
	private final boolean includeHeadingPath;

	/**
	 * @param blocks the blocks of the source document, ordered by offset; empty for plain text
	 */
	ChunkFactory(final String text, final List<DocumentBlock> blocks, final TokenCounter tokens, final boolean includeHeadingPath) {

		this.text               = text;
		this.blocks             = blocks;
		this.tokens             = tokens;
		this.includeHeadingPath = includeHeadingPath;
	}

	List<Chunk> create(final List<TextSpan> spans) {

		final List<Chunk> chunks = new ArrayList<>();

		for (final TextSpan span : spans) {

			final TextSpan trimmed = trim(span);
			if (trimmed.isEmpty()) {

				continue;
			}

			final int index            = blockIndexAt(trimmed.start());
			final DocumentBlock first          = index < blocks.size() ? blocks.get(index) : null;
			final List<String> path    = first != null ? first.headingPath() : List.of();
			final String content       = text.substring(trimmed.start(), trimmed.end());
			final String chunkText     = includeHeadingPath ? withSectionPath(content, path, first, trimmed) : content;
			final Integer[] pages      = pages(trimmed, index);

			chunks.add(new Chunk(chunks.size(), chunkText, trimmed.start(), trimmed.end(), chunkText.length(), tokens.count(chunkText), pages[0], pages[1], path, sha256(chunkText)));
		}

		return chunks;
	}

	// ----- private methods -----
	private TextSpan trim(final TextSpan span) {

		int start = span.start();
		int end   = span.end();

		while (start < end && Character.isWhitespace(text.charAt(start))) {

			start++;
		}

		while (end > start && Character.isWhitespace(text.charAt(end - 1))) {

			end--;
		}

		return new TextSpan(start, end);
	}

	/**
	 * Index of the block containing the offset, or of the next block if the offset lies between blocks;
	 * the number of blocks if there is none. Blocks are ordered by offset, so this is a binary search.
	 */
	private int blockIndexAt(final int offset) {

		int low  = 0;
		int high = blocks.size();

		while (low < high) {

			final int middle = (low + high) >>> 1;
			if (blocks.get(middle).endOffset() > offset) {

				high = middle;

			} else {

				low = middle + 1;
			}
		}

		return low;
	}

	private Integer[] pages(final TextSpan span, final int firstIndex) {

		final int count = blocks.size();
		Integer first   = null;
		Integer last    = null;

		for (int i = firstIndex; i < count && blocks.get(i).startOffset() < span.end(); i++) {

			final DocumentBlock block = blocks.get(i);
			if (block.page() != null) {

				first = first == null ? block.page() : Math.min(first, block.page());
				last  = last == null ? block.page() : Math.max(last, block.page());
			}
		}

		return new Integer[] { first, last };
	}

	/**
	 * Prefixes the chunk with the path of its section. If the chunk starts with the section's own heading,
	 * that heading is already in the text and is left out of the prefix.
	 */
	private static String withSectionPath(final String content, final List<String> path, final DocumentBlock first, final TextSpan span) {

		List<String> prefix = path;

		if (first != null && first.type() == DocumentBlockType.HEADING && first.startOffset() >= span.start() && !path.isEmpty()) {

			prefix = path.subList(0, path.size() - 1);
		}

		if (prefix.isEmpty()) {

			return content;
		}

		return String.join(" > ", prefix) + "\n\n" + content;
	}

	private static String sha256(final String value) {

		try {

			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));

		} catch (final NoSuchAlgorithmException ex) {

			// every Java runtime is required to provide SHA-256
			throw new IllegalStateException(ex);
		}
	}
}
