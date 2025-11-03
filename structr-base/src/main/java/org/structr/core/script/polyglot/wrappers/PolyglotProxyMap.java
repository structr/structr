/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.core.script.polyglot.wrappers;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;

import java.util.Map;

public class PolyglotProxyMap implements ProxyObject {

	private final ActionContext actionContext;
	private final Map<String, Object> map;

	public PolyglotProxyMap(final ActionContext actionContext, final Map<String, Object> map) {

		this.actionContext = actionContext;
		this.map = map;
	}

	public Map<String, Object> getOriginalObject() {
		return this.map;
	}

	@Override
	public Object getMember(String key) {
		return PolyglotWrapper.wrap(actionContext, map.get(key));
	}

	@Override
	public Object getMemberKeys() {
		return map.keySet().toArray();
	}

	@Override
	public boolean hasMember(String key) {
		return map.containsKey(key);
	}

	@Override
	public void putMember(String key, Value value) {

		if (key != null) {

			if (value == null) {

				map.remove(key);
			} else {

				map.put(key, PolyglotWrapper.unwrap(actionContext, value));
			}
		}
	}
}
