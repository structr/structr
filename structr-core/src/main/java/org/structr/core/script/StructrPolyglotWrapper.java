/**
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.core.script;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.structr.core.GraphObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class StructrPolyglotWrapper {

	public static Object wrap(Object obj) {

		if (obj instanceof GraphObject) {
			GraphObject graphObject = (GraphObject)obj;

			return new StructrPolyglotGraphObjectWrapper(graphObject);
		} else if (obj instanceof Iterable) {

			final List<Object> wrappedList = new ArrayList<>();

			for (final Object o : (Iterable)obj) {
				wrappedList.add(wrap(o));
			}

			return ProxyArray.fromList(wrappedList);
		} else if (obj instanceof Map) {

			final Map<String, Object> wrappedMap = new HashMap<>();

			for (Map.Entry<String, Object> entry : (((Map<String, Object>)obj).entrySet())) {

				wrappedMap.put(entry.getKey(), wrap(entry.getValue()));
			}

			return ProxyObject.fromMap(wrappedMap);
		}

		return obj;
	}

	public static Object unwrap(Object obj) {

		if (obj instanceof Value) {
			Value value = (Value)obj;

			if (value.isHostObject()) {

				return value.asHostObject();
			} else {
				return unwrap(value.as(Object.class));
			}
		} else {

			return obj;
		}
	}

}
