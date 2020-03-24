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
import org.structr.common.error.FrameworkException;
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
		}

		return obj;
	}

	public static Object unwrap(Object obj) {

		if (obj instanceof Value) {
			Value value = (Value) obj;

			if (value.isHostObject()) {

				return value.asHostObject();
			} else if (value.hasArrayElements()) {

				return convertValueToList(value);
			} else if (value.hasMembers()) {

				return convertValueToMap(value);
			} else {

				return unwrap(value.as(Object.class));
			}
		} else if (obj instanceof StructrPolyglotGraphObjectWrapper) {

			return ((StructrPolyglotGraphObjectWrapper)obj).getGraphObject();
		} else {

			return obj;
		}
	}

	protected static List<Object> convertValueToList(Value value) {
		final List<Object> resultList = new ArrayList<>();

		if (value.hasArrayElements()) {

			for (int i = 0; i < value.getArraySize(); i++) {

				resultList.add(unwrap(value.getArrayElement(i)));
			}
		}

		return resultList;
	}

	protected static Map<String, Object> convertValueToMap(Value value) {
		final Map<String, Object> resultMap = new HashMap<>();

		if (value.hasMembers()) {

			for (String key : value.getMemberKeys()) {

				resultMap.put(key, unwrap(value.getMember(key)));
			}
		}

		return resultMap;
	}
}
