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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A search hit in its context: the matched text with some text before and after it.
 *
 * @param before the text before the match, cut at a word boundary
 * @param match  the matched word or phrase, as it appears in the text
 * @param after  the text after the match, cut at a word boundary
 * @param page   the page the text is on, or null for formats without pages
 */
public record SearchSnippet(String before, String match, String after, Integer page) {

	public Map<String, Object> toMap() {

		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("before", before);
		map.put("match",  match);
		map.put("after",  after);
		map.put("page",   page);

		return map;
	}
}
