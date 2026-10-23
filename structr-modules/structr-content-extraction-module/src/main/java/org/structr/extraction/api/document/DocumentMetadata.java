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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Metadata of an extracted document: the common fields, how the extraction went, and the raw Tika
 * metadata for everything else.
 *
 * @param mimeType           the detected content type
 * @param title              the document title, if the document declares one
 * @param author             the author or creator, if the document declares one
 * @param language           the detected language (ISO 639-1, e.g. "de"), or the declared one if no text was extracted
 * @param languageConfidence the detector's confidence in the language, between 0 and 1, or null if declared
 * @param pageCount          number of pages or slides, or null if the format has no pages
 * @param created            creation date, if the document declares one
 * @param modified           last modification date, if the document declares one
 * @param ocrApplied         whether OCR contributed text
 * @param ocrSkippedReason   why OCR did not run, or null if it ran or was not needed
 * @param truncated          whether text beyond the maximum number of characters was dropped
 * @param raw                all metadata Tika reported, name to values
 */
public record DocumentMetadata(String mimeType, String title, String author, String language, Double languageConfidence, Integer pageCount, Date created, Date modified,
	boolean ocrApplied, OcrSkipReason ocrSkippedReason, boolean truncated, Map<String, List<String>> raw) {

	public DocumentMetadata {

		raw = Map.copyOf(raw);
	}

	public Map<String, Object> toMap() {

		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("mimeType",           mimeType);
		map.put("title",              title);
		map.put("author",             author);
		map.put("language",           language);
		map.put("languageConfidence", languageConfidence);
		map.put("pageCount",          pageCount);
		map.put("created",            created);
		map.put("modified",           modified);
		map.put("ocrApplied",         ocrApplied);
		map.put("ocrSkippedReason",   ocrSkippedReason != null ? ocrSkippedReason.getValue() : null);
		map.put("truncated",          truncated);
		map.put("raw",                new LinkedHashMap<>(raw));

		return map;
	}
}
