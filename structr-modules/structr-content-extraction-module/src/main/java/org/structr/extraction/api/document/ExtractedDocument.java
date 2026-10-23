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
package org.structr.extraction.api.document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The result of an extraction: the document as an ordered list of structural blocks, rendered as plain
 * text and as Markdown, plus its links and metadata.
 *
 * Block offsets refer to the plain text ({@link #getText()}).
 */
public final class ExtractedDocument {

	private final DocumentMetadata metadata;
	private final List<DocumentBlock> blocks;
	private final List<ExtractedLink> links;
	private final String text;
	private final String markdown;

	public ExtractedDocument(final DocumentMetadata metadata, final List<DocumentBlock> blocks, final List<ExtractedLink> links, final String text, final String markdown) {

		this.metadata = metadata;
		this.blocks   = List.copyOf(blocks);
		this.links    = List.copyOf(links);
		this.text     = text;
		this.markdown = markdown;
	}

	public DocumentMetadata getMetadata() {

		return metadata;
	}

	public List<DocumentBlock> getBlocks() {

		return blocks;
	}

	public List<ExtractedLink> getLinks() {

		return links;
	}

	/**
	 * The document as plain text: blocks separated by a blank line, list items by a line break, table
	 * cells by " | ".
	 */
	public String getText() {

		return text;
	}

	/**
	 * The document as Markdown, with headings, lists, tables, quotes and code blocks.
	 */
	public String getMarkdown() {

		return markdown;
	}

	public List<ExtractedTable> getTables() {

		final List<ExtractedTable> tables = new ArrayList<>();

		for (final DocumentBlock block : blocks) {

			if (block.type() == DocumentBlockType.TABLE) {

				tables.add(new ExtractedTable(tables.size(), block.page(), block.headingPath(), block.embeddedPath(), block.rows()));
			}
		}

		return tables;
	}

	/**
	 * Map representation for scripting.
	 *
	 * @param includeBlocks whether to include the block list, which roughly doubles the size of the result
	 */
	public Map<String, Object> toMap(final boolean includeBlocks) {

		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("text",      text);
		map.put("markdown",  markdown);
		map.put("metadata",  metadata.toMap());
		map.put("language",  metadata.language());
		map.put("pageCount", metadata.pageCount());

		if (includeBlocks) {

			map.put("blocks", blocks.stream().map(DocumentBlock::toMap).toList());
		}

		return map;
	}
}
