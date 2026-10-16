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
package org.structr.extraction.api.options;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The options a file is extracted and split with: per-call options over the nearest content extraction
 * configuration over the module settings, each value with where it came from.
 *
 * @param extraction the extraction options
 * @param split      the split options
 * @param values     every option by name, with its value and source ("call", "config:/path" or "setting")
 * @param warnings   adjustments made during resolution, e.g. an overlap reduced to fit the chunk size
 * @param hash       hash of all values that influence the chunks; differs whenever the chunks would
 */
public record EffectiveContentOptions(ExtractionOptions extraction, SplitOptions split, Map<String, Value> values, List<String> warnings, String hash) {

	public EffectiveContentOptions {

		values   = Map.copyOf(values);
		warnings = List.copyOf(warnings);
	}

	public Map<String, Object> toMap() {

		final Map<String, Object> map = new LinkedHashMap<>();

		for (final Map.Entry<String, Value> entry : new TreeMap<>(values).entrySet()) {

			map.put(entry.getKey(), entry.getValue().toMap());
		}

		return map;
	}

	/**
	 * An option value and its source.
	 *
	 * @param value  the value
	 * @param source "call", "config:/path" or "setting"
	 */
	public record Value(Object value, String source) {

		public Map<String, Object> toMap() {

			final Map<String, Object> map = new LinkedHashMap<>();

			map.put("value",  value);
			map.put("source", source);

			return map;
		}
	}
}
