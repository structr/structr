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
 * A table found in a document.
 *
 * @param index       0-based position among the tables of the document
 * @param page        1-based page number, or null if the format has no pages
 * @param headingPath titles of the sections enclosing the table, outermost first
 * @param embeddedPath names of the embedded documents the table is in, joined with "/", or null
 * @param rows        the cell texts, row by row
 */
public record ExtractedTable(int index, Integer page, List<String> headingPath, String embeddedPath, List<List<String>> rows) {

	public Map<String, Object> toMap() {

		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("index",       index);
		map.put("page",        page);
		map.put("headingPath", headingPath);
		map.put("embeddedPath", embeddedPath);
		map.put("rows",        rows);

		return map;
	}
}
