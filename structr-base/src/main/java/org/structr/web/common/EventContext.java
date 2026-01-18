/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.common;

import java.util.LinkedHashMap;
import java.util.Map;

public class EventContext extends LinkedHashMap<String, Object> {

	private static final String KEY_DATA = "data";

	public void data(final String key, final Object value) {
		getData().put(key, value);
	}

	public void data(final Map<String, Object> data) {
		getData().putAll(data);
	}

	// ----- private methods -----
	private Map<String, Object> getData() {

		Map<String, Object> data = (Map<String, Object>)get(KEY_DATA);
		if (data == null) {

			data = new LinkedHashMap<>();
			put(KEY_DATA, data);
		}

		return data;
	}
}
