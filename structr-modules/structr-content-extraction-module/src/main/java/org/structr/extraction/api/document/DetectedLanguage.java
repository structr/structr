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
 * The result of language detection.
 *
 * @param language     ISO 639-1 code of the most likely language (e.g. "de"), or null if undetermined
 * @param confidence   probability of the most likely language, between 0 and 1
 * @param alternatives all candidates with their probability, most likely first
 */
public record DetectedLanguage(String language, double confidence, List<Candidate> alternatives) {

	public static final DetectedLanguage UNKNOWN = new DetectedLanguage(null, 0.0, List.of());

	public DetectedLanguage {

		alternatives = List.copyOf(alternatives);
	}

	public Map<String, Object> toMap() {

		final Map<String, Object> map = new LinkedHashMap<>();

		map.put("language",     language);
		map.put("confidence",   confidence);
		map.put("alternatives", alternatives.stream().map(Candidate::toMap).toList());

		return map;
	}

	/**
	 * A candidate language.
	 *
	 * @param language   ISO 639-1 code
	 * @param confidence probability, between 0 and 1
	 */
	public record Candidate(String language, double confidence) {

		public Map<String, Object> toMap() {

			final Map<String, Object> map = new LinkedHashMap<>();

			map.put("language",   language);
			map.put("confidence", confidence);

			return map;
		}
	}
}
