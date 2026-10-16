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
import java.util.Map;

/**
 * A document embedded in another one: a mail attachment, an archive entry or an embedded office document.
 *
 * @param name     the file name of the embedded document, if known
 * @param path     the path inside the container, e.g. "/attachment.pdf" or "/archive.zip/report.docx"
 * @param mimeType the detected content type
 * @param depth    nesting depth, 1 for documents directly inside the container
 * @param text     the extracted text, or null if text was not requested
 */
public record EmbeddedDocument(String name, String path, String mimeType, int depth, String text) {

	public Map<String, Object> toMap() {

		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("name",     name);
		map.put("path",     path);
		map.put("mimeType", mimeType);
		map.put("depth",    depth);
		map.put("text",     text);

		return map;
	}
}
