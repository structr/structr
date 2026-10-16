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

import org.structr.extraction.entity.ExtractedContentNode;
import org.structr.web.entity.File;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A chunk together with its neighbours, as one continuous text.
 *
 * @param text        the text of the chunks, with overlapping parts included once
 * @param nodes       the chunks, in reading order
 * @param file        the file they belong to
 * @param pageStart   first page, or null
 * @param pageEnd     last page, or null
 * @param headingPath the section path of the first chunk
 */
public record ChunkContext(String text, List<ExtractedContentNode> nodes, File file, Integer pageStart, Integer pageEnd, List<String> headingPath) {

	public ChunkContext {

		nodes       = List.copyOf(nodes);
		headingPath = List.copyOf(headingPath);
	}

	public Map<String, Object> toMap() {

		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("text",        text);
		map.put("nodes",       nodes);
		map.put("file",        file);
		map.put("pageStart",   pageStart);
		map.put("pageEnd",     pageEnd);
		map.put("headingPath", headingPath);

		return map;
	}
}
