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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class StructrPolyglotWrapper {

	public static Object wrap(Object obj) {

		if (obj instanceof Iterable) {

			return wrapIterable((Iterable)obj);
		} else if (obj instanceof Map) {

			return wrapMap((Map<String, Object>)obj);
		}

		return obj;
	}

	public static Object unwrap(Object obj) {

		if (obj instanceof Value) {
			Value value = (Value) obj;

			if (value.isHostObject()) {

				return unwrap(value.asHostObject());
			} else if (value.hasArrayElements()) {

				return convertValueToList(value);
			} else if (value.hasMembers()) {

				return convertValueToMap(value);
			} else {

				return unwrap(value.as(Object.class));
			}
		} else if (obj instanceof Iterable) {

			return unwrapIterable((Iterable) obj);
		} else if(obj instanceof Map) {

			return unwrapMap((Map<String, Object>) obj);
		} else {

			return obj;
		}
	}

	protected static List<Object> wrapIterable(final Iterable<Object> iterable) {

		final List<Object> wrappedList = new ArrayList<>();

		for (Object o : iterable) {

			wrappedList.add(wrap(o));
		}
		return wrappedList;
	}

	protected static List<Object> unwrapIterable(final Iterable<Object> iterable) {

		final List<Object> wrappedList = new ArrayList<>();

		for (Object o : iterable) {

			wrappedList.add(unwrap(o));
		}
		return wrappedList;
	}

	protected static Map<String, Object> wrapMap(final Map<String, Object> map) {

		final Map<String, Object> wrappedMap = new HashMap<>();

		for (Map.Entry<String,Object> entry : map.entrySet()) {

			wrappedMap.put(entry.getKey(), wrap(entry.getValue()));
		}
		return wrappedMap;
	}

	protected static Map<String, Object> unwrapMap(final Map<String, Object> map) {

		final Map<String, Object> wrappedMap = new HashMap<>();

		for (Map.Entry<String,Object> entry : map.entrySet()) {

			wrappedMap.put(entry.getKey(), unwrap(entry.getValue()));
		}
		return wrappedMap;
	}

	protected static List<Object> convertValueToList(final Value value) {

		final List<Object> resultList = new ArrayList<>();

		if (value.hasArrayElements()) {

			for (int i = 0; i < value.getArraySize(); i++) {

				resultList.add(unwrap(value.getArrayElement(i)));
			}
		}

		return resultList;
	}

	protected static Map<String, Object> convertValueToMap(final Value value) {

		final Map<String, Object> resultMap = new HashMap<>();

		if (value.hasMembers()) {

			for (String key : value.getMemberKeys()) {

				resultMap.put(key, unwrap(value.getMember(key)));
			}
		}

		return resultMap;
	}
}
