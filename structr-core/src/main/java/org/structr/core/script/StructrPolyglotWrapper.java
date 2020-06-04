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
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.schema.action.ActionContext;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class StructrPolyglotWrapper {

	public static Object wrap(ActionContext actionContext, Object obj) {

		if (obj instanceof GraphObject) {

			return new StructrPolyglotGraphObjectWrapper(actionContext, (GraphObject) obj);
		} else 	if (obj instanceof Iterable) {

			return new StructrPolyglotProxyArray(actionContext, (List)StreamSupport.stream(((Iterable)obj).spliterator(), false).collect(Collectors.toList()));
		} else if (obj instanceof Map) {

			return ProxyObject.fromMap(wrapMap(actionContext, (Map<String, Object>)obj));
		}

		return obj;
	}

	public static Object unwrap(Object obj) {

		if (obj instanceof Value) {
			Value value = (Value) obj;

			if (value.canExecute()) {

				return new FunctionWrapper(value);
			} else if (value.isHostObject()) {

				return unwrap(value.asHostObject());
			} else if (value.isProxyObject() && value.hasMembers()) {
				ProxyObject proxy = value.asProxyObject();

				if (proxy instanceof StructrPolyglotGraphObjectWrapper) {
					return ((StructrPolyglotGraphObjectWrapper)proxy).getOriginalObject();
				} else {

					return proxy;
				}
			} else if (value.hasArrayElements()) {

				return convertValueToList(value);
			} else if (value.hasMembers()) {

				return convertValueToMap(value);
			} else {

				return unwrap(value.as(Object.class));
			}
		} else if (obj instanceof StructrPolyglotGraphObjectWrapper) {

			return ((StructrPolyglotGraphObjectWrapper)obj).getOriginalObject();
		} else if (obj instanceof Iterable) {

			return unwrapIterable((Iterable) obj);
		} else if(obj instanceof Map) {

			return unwrapMap((Map<String, Object>) obj);
		} else {

			return obj;
		}
	}

	protected static List<Object> wrapIterable(ActionContext actionContext, final Iterable<Object> iterable) {

		final List<Object> wrappedList = new ArrayList<>();

		for (Object o : iterable) {

			wrappedList.add(wrap(actionContext, o));
		}
		return wrappedList;
	}

	protected static List<Object> unwrapIterable(final Iterable<Object> iterable) {

		final List<Object> unwrappedList = new ArrayList<>();

		for (Object o : iterable) {

			unwrappedList.add(unwrap(o));
		}
		return unwrappedList;
	}

	protected static Map<String, Object> wrapMap(ActionContext actionContext, final Map<String, Object> map) {

		final Map<String, Object> wrappedMap = new HashMap<>();

		for (Map.Entry<String,Object> entry : map.entrySet()) {

			wrappedMap.put(entry.getKey(), wrap(actionContext, entry.getValue()));
		}
		return wrappedMap;
	}

	protected static Map<String, Object> unwrapMap(final Map<String, Object> map) {

		final Map<String, Object> unwrappedMap = new HashMap<>();

		for (Map.Entry<String,Object> entry : map.entrySet()) {

			unwrappedMap.put(entry.getKey(), unwrap(entry.getValue()));
		}
		return unwrappedMap;
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

	private static class FunctionWrapper implements Function {
		private Value func;

		public FunctionWrapper(final Value func) {

			if (func.canExecute()) {

				this.func = func;
			}
		}

		@Override
		public Object apply(Object o) {
			return unwrap(func.execute(o));
		}
	}
}
