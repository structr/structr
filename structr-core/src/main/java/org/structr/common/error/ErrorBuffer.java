/**
 * Copyright (C) 2010-2015 Morgner UG (haftungsbeschränkt)
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
package org.structr.common.error;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Map;

/**
 * A buffer that collects error tokens to allow for i18n and human readable
 * output.
 *
 * @author Christian Morgner
 */
public class ErrorBuffer {

	private Map<String, Map<String, Set<ErrorToken>>> tokens = new LinkedHashMap<>();

	public void add(String type, ErrorToken msg) {
		getTokenSet(type, msg.getKey()).add(msg);
	}

	public boolean hasError() {
		return !tokens.isEmpty();
	}

	public Map<String, Map<String, Set<ErrorToken>>> getErrorTokens() {
		return tokens;
	}

	// ----- private methods -----
	private Set<ErrorToken> getTokenSet(String type, String key) {

		Map<String, Set<ErrorToken>> map = getTypeSet(type);
		Set<ErrorToken> list = map.get(key);
		if(list == null) {

			list = new LinkedHashSet<>();
			map.put(key, list);
		}

		return list;
	}

	private Map<String, Set<ErrorToken>> getTypeSet(String type) {

		Map<String, Set<ErrorToken>> map = tokens.get(type);
		if(map == null) {
			
			map = new LinkedHashMap<>();
			tokens.put(type, map);
		}

		return map;
	}
}
