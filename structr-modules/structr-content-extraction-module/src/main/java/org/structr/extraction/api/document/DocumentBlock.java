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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A structural unit of an extracted document: a heading, paragraph, list item, table, code block or quote.
 *
 * @param type        the structural role
 * @param level       heading level (1-6) for headings, nesting depth (1-based) for list items, 0 otherwise
 * @param text        the normalized text; for tables, the cells of each row joined with " | ", rows by newlines
 * @param page        1-based page (or slide) number, or null if the format has no pages
 * @param headingPath titles of the enclosing sections, outermost first; a heading includes itself
 * @param rows        the cells of a table, row by row; empty for all other block types
 * @param embeddedPath names of the embedded documents (archive entries, attachments) the block is in,
 *                    outermost first, joined with "/"; null if the block is not in an embedded document
 * @param startOffset start of the block in {@link ExtractedDocument#getText()}
 * @param endOffset   end (exclusive) of the block in {@link ExtractedDocument#getText()}
 */
public record DocumentBlock(DocumentBlockType type, int level, String text, Integer page, List<String> headingPath, List<List<String>> rows, String embeddedPath, int startOffset, int endOffset) {

	public DocumentBlock {

		headingPath = List.copyOf(headingPath);
		rows        = rows.stream().map(List::copyOf).toList();
	}

	public Map<String, Object> toMap() {

		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("type",        type.getValue());
		map.put("level",       level);
		map.put("text",        text);
		map.put("page",        page);
		map.put("headingPath", headingPath);
		map.put("embeddedPath", embeddedPath);
		map.put("startOffset", startOffset);
		map.put("endOffset",   endOffset);

		if (type == DocumentBlockType.TABLE) {

			map.put("rows", rows);
		}

		return map;
	}
}
