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
import java.util.Set;

/**
 * Whether OCR is available, and with which languages.
 *
 * @param available whether a working tesseract binary was found
 * @param languages the installed Tesseract language packs, e.g. "eng", "deu", "osd"
 */
public record OcrInfo(boolean available, Set<String> languages) {

	public OcrInfo {

		languages = Set.copyOf(languages);
	}

	public Map<String, Object> toMap() {

		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("available", available);
		map.put("languages", languages.stream().sorted().toList());

		return map;
	}
}
