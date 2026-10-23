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

import org.structr.extraction.entity.ExtractedContentNode;
import org.structr.web.entity.File;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A chunk found by keyword search.
 *
 * @param node    the chunk
 * @param score   the fulltext score, higher is better
 * @param file    the file the chunk belongs to
 * @param context the text of the chunk and its neighbours, if requested, otherwise null
 */
public record ContentSearchHit(ExtractedContentNode node, double score, File file, String context) {

	public Map<String, Object> toMap() {

		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("node",        node);
		map.put("score",       score);
		map.put("text",        node.getText());
		map.put("file",        file);
		map.put("position",    node.getPosition());
		map.put("pageStart",   node.getPageStart());
		map.put("pageEnd",     node.getPageEnd());
		map.put("headingPath", node.getHeadingPath());
		map.put("embeddedPath", node.getEmbeddedPath());

		if (context != null) {

			map.put("context", context);
		}

		return map;
	}
}
