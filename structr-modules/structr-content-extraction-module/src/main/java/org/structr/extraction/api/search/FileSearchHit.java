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
package org.structr.extraction.api.search;

import org.structr.web.entity.File;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A file whose extracted content matches a search, with the places that match.
 *
 * @param file     the file
 * @param score    the score of its best matching chunk
 * @param snippets the matches in context, best chunk first
 */
public record FileSearchHit(File file, double score, List<SearchSnippet> snippets) {

	public Map<String, Object> toMap() {

		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("file",     file);
		map.put("score",    score);
		map.put("snippets", snippets.stream().map(SearchSnippet::toMap).toList());

		return map;
	}
}
