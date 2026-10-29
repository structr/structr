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
package org.structr.extraction.query;

import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.extraction.api.chunk.ChunkContext;
import org.structr.extraction.entity.ExtractedContentNode;
import org.structr.extraction.traits.properties.ContentFileStateProperties;
import org.structr.web.entity.File;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Reads the stored chunks of a file: in reading order, as one continuous text, and a chunk together
 * with its neighbours.
 */
public final class ContentChunkReader {

	private ContentChunkReader() {
	}

	/**
	 * The chunks of a file in reading order, as far as they are visible to the file's security context.
	 * The chunks with the text of figures are not part of the text and are left out.
	 */
	public static List<ExtractedContentNode> chunks(final File file) {

		return allChunks(file).stream().filter(chunk -> chunk.getFigure() == null).toList();
	}

	/**
	 * The chunks with the text of the document's figures.
	 */
	public static List<ExtractedContentNode> figureChunks(final File file) {

		return allChunks(file).stream().filter(chunk -> chunk.getFigure() != null).toList();
	}

	/**
	 * All chunks of a file: the text in reading order, then the chunks with the text of its figures, in the
	 * order they were created.
	 */
	public static List<ExtractedContentNode> allChunks(final File file) {

		final Iterable<NodeInterface> nodes  = file.getProperty(Traits.of(StructrTraits.FILE).key(ContentFileStateProperties.EXTRACTED_CONTENT_NODES));
		final List<ExtractedContentNode> list = new ArrayList<>();

		for (final NodeInterface node : nodes) {

			list.add(node.as(ExtractedContentNode.class));
		}

		list.sort(Comparator.comparing(ContentChunkReader::isFigureText)
			.thenComparing((ExtractedContentNode chunk) -> isFigureText(chunk) ? chunk.getCreatedDate() : null, Comparator.nullsFirst(Comparator.naturalOrder()))
			.thenComparingInt(ExtractedContentNode::getPosition));

		return list;
	}

	/**
	 * The text of consecutive chunks as one piece. Chunks can overlap; their offsets tell by how much,
	 * so overlapping text is included once. A gap between chunks becomes a paragraph break.
	 */
	public static String mergedText(final List<ExtractedContentNode> ordered) {

		final StringBuilder text = new StringBuilder();
		int end                  = -1;

		for (final ExtractedContentNode node : ordered) {

			final String content = content(node);

			if (end < 0) {

				text.append(content);

			} else if (node.getStartOffset() >= end) {

				text.append("\n\n").append(content);

			} else if (node.getEndOffset() > end) {

				text.append(content.substring(Math.min(content.length(), end - node.getStartOffset())));
			}

			end = Math.max(end, node.getEndOffset());
		}

		return text.toString();
	}

	/**
	 * A chunk with up to {@code before} chunks before and {@code after} chunks after it. Neighbours from
	 * another embedded document (archive entry, attachment) are not its context and are left out.
	 */
	public static ChunkContext context(final ExtractedContentNode node, final int before, final int after) {

		final LinkedList<ExtractedContentNode> nodes = new LinkedList<>();

		nodes.add(node);

		final String embeddedPath    = node.getEmbeddedPath();
		ExtractedContentNode current = node.getPrevious();

		for (int i = 0; i < before && current != null && Objects.equals(embeddedPath, current.getEmbeddedPath()); i++, current = current.getPrevious()) {

			nodes.addFirst(current);
		}

		current = node.getNext();

		for (int i = 0; i < after && current != null && Objects.equals(embeddedPath, current.getEmbeddedPath()); i++, current = current.getNext()) {

			nodes.addLast(current);
		}

		final Integer pageStart = nodes.stream().map(ExtractedContentNode::getPageStart).filter(Objects::nonNull).min(Integer::compare).orElse(null);
		final Integer pageEnd   = nodes.stream().map(ExtractedContentNode::getPageEnd).filter(Objects::nonNull).max(Integer::compare).orElse(null);

		return new ChunkContext(mergedText(nodes), nodes, node.getFile(), pageStart, pageEnd, nodes.getFirst().getHeadingPath(), embeddedPath);
	}

	// ----- private methods -----
	/**
	 * Whether a chunk holds the text of a figure instead of a part of the text of its file.
	 */
	private static boolean isFigureText(final ExtractedContentNode chunk) {

		return chunk.getFigure() != null;
	}

	/**
	 * The part of a chunk's text that its offsets cover; without the section path prefix, if any.
	 */
	private static String content(final ExtractedContentNode node) {

		final String text   = node.getText() != null ? node.getText() : "";
		final int length    = node.getEndOffset() - node.getStartOffset();

		return length >= 0 && length <= text.length() ? text.substring(text.length() - length) : text;
	}
}
