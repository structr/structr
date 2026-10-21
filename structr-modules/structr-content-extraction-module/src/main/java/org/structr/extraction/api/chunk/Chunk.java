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
package org.structr.extraction.api.chunk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A piece of content, sized for embedding and retrieval.
 *
 * @param position    0-based index in the document
 * @param text        the chunk text; prefixed with the section path if requested
 * @param startOffset start of the chunk content in the source text (the document's plain text, or the text that was split)
 * @param endOffset   end (exclusive) of the chunk content in the source text
 * @param charCount   number of characters of {@code text}
 * @param tokenCount  number of tokens of {@code text} in the configured encoding
 * @param pageStart   first page the chunk is on, or null if the source has no pages
 * @param pageEnd     last page the chunk is on, or null if the source has no pages
 * @param headingPath titles of the sections the chunk belongs to, outermost first
 * @param embeddedPath names of the embedded documents (archive entries, attachments) the chunk is in,
 *                    outermost first, joined with "/"; null if the chunk is not in an embedded document
 * @param contentHash SHA-256 of {@code text}, hex encoded; equal hashes mean equal chunk text
 */
public record Chunk(int position, String text, int startOffset, int endOffset, int charCount, int tokenCount, Integer pageStart, Integer pageEnd, List<String> headingPath, String embeddedPath, String contentHash) {

	public Chunk {

		headingPath = List.copyOf(headingPath);
	}

	public Map<String, Object> toMap() {

		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("position",    position);
		map.put("text",        text);
		map.put("startOffset", startOffset);
		map.put("endOffset",   endOffset);
		map.put("charCount",   charCount);
		map.put("tokenCount",  tokenCount);
		map.put("pageStart",   pageStart);
		map.put("pageEnd",     pageEnd);
		map.put("headingPath", headingPath);
		map.put("embeddedPath", embeddedPath);
		map.put("contentHash", contentHash);

		return map;
	}
}
