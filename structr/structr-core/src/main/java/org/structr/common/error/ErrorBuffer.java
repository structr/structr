/*
 *  Copyright (C) 2011 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common.error;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A buffer that collects error tokens to allow for i18n
 * and human readable output later.
 *
 * @author Christian Morgner
 */
public class ErrorBuffer {

	private Map<String, Map<String, List<ErrorToken>>> tokens = new LinkedHashMap<String, Map<String, List<ErrorToken>>>();

	public void add(String type, ErrorToken msg) {
		getTokenList(type, msg.getKey()).add(msg);
	}

	public boolean hasError() {
		return !tokens.isEmpty();
	}

	public Map<String, Map<String, List<ErrorToken>>> getErrorTokens() {
		return tokens;
	}

	// ----- private methods -----
	private List<ErrorToken> getTokenList(String type, String key) {

		Map<String, List<ErrorToken>> map = getTypeList(type);
		List<ErrorToken> list = map.get(key);
		if(list == null) {
			list = new LinkedList<ErrorToken>();
			map.put(key, list);
		}

		return list;
	}

	private Map<String, List<ErrorToken>> getTypeList(String type) {

		Map<String, List<ErrorToken>> map = tokens.get(type);
		if(map == null) {
			map = new LinkedHashMap<String, List<ErrorToken>>();
			tokens.put(type, map);
		}

		return map;
	}
}
