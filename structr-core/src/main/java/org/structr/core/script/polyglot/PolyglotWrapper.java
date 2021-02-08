/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.core.script.polyglot;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.script.polyglot.context.ContextFactory;
import org.structr.core.script.polyglot.wrappers.GraphObjectWrapper;
import org.structr.core.script.polyglot.wrappers.PolyglotProxyArray;
import org.structr.core.script.polyglot.wrappers.PolyglotProxyMap;
import org.structr.schema.action.ActionContext;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class PolyglotWrapper {

	// Wraps values going into the scripting context. E.g.: GraphObject -> StructrPolyglotGraphObjectWrapper
	public static Object wrap(ActionContext actionContext, Object obj) {

		if (obj == null) {

			return null;
		} else if (obj instanceof GraphObject) {

			return new GraphObjectWrapper(actionContext, (GraphObject) obj);
		} else if (obj.getClass().isArray()) {

			return new PolyglotProxyArray(actionContext, (Object[]) obj);
		} else 	if (obj instanceof List) {

			return new PolyglotProxyArray(actionContext, (List)obj);
		} else 	if (obj instanceof Iterable) {

			return new PolyglotProxyArray(actionContext, (List)StreamSupport.stream(((Iterable)obj).spliterator(), false).collect(Collectors.toList()));
		} else if (obj instanceof Map) {

			return new PolyglotProxyMap(actionContext, (Map<String, Object>)obj);
		} else if (obj instanceof Enumeration) {

			Enumeration enumeration = (Enumeration)obj;
			List<Object> enumList = new ArrayList<>();
			while (enumeration.hasMoreElements()) {

				enumList.add(enumeration.nextElement());
			}

			return new PolyglotProxyArray(actionContext, enumList.toArray());
		} else if (obj instanceof Date) {
			
			return Date.from(((Date)obj).toInstant());
		} else if (obj instanceof Value && !((Value)obj).getContext().equals(Context.getCurrent())) {
			// Try tu rewrap objects from foreign contexts
			return wrap(actionContext, unwrap(actionContext, obj));
		}

		return obj;
	}

	// Unwraps values coming out of the scripting engine. Maps/Lists will be unwrapped recursively to ensure all values will be in their native state.
	public static Object unwrap(final ActionContext actionContext, final Object obj) {

		if (obj instanceof Value) {
			Value value = (Value) obj;

			// Deal with wrapped primitives
			if (value.isString()) {

				return value.asString();
			} else if (value.isBoolean()) {

				return value.asBoolean();
			} else if (value.isNumber()) {

				if (value.fitsInInt()) {

					return value.asInt();
				} else if (value.fitsInLong()) {

					return value.asLong();
				} else if (value.fitsInFloat()) {

					Float f = value.asFloat();
					if (!Float.isNaN(f)) {

						return f;
					}
					return null;
				} else if (value.fitsInDouble()) {

					Double d = value.asDouble();
					if (!Double.isNaN(d)) {

						return d;
					}
					return null;
				}
			}

			// Deal with more complex values
			if (value.canExecute()) {

				return new FunctionWrapper(actionContext, value);
			} else if (value.isHostObject()) {

				return unwrap(actionContext, value.asHostObject());
			} else if (value.isDate()) {

				if (value.isTime()) {

					return Date.from(value.asDate().atTime(value.asTime()).atZone(ZoneId.systemDefault()).toInstant());
				} else {

					return Date.from(value.asDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
				}
			} else if (value.isProxyObject() && value.hasMembers()) {
				ProxyObject proxy = value.asProxyObject();

				if (proxy instanceof GraphObjectWrapper) {
					return ((GraphObjectWrapper)proxy).getOriginalObject();
				} else if (proxy instanceof PolyglotProxyMap) {

					return ((PolyglotProxyMap)proxy).getOriginalObject();
				} else {

					return proxy;
				}
			} else if (value.hasArrayElements()) {

				return convertValueToList(actionContext, value);
			} else if (value.hasMembers()) {

				return convertValueToMap(actionContext, value);
			} else if (value.isNull()) {

				return null;
			} else {

				return unwrap(actionContext, value.as(Object.class));
			}
		} else if (obj instanceof GraphObjectWrapper) {

			return ((GraphObjectWrapper)obj).getOriginalObject();
		} else if (obj instanceof Iterable) {

			return unwrapIterable(actionContext, (Iterable) obj);
		} else if(obj instanceof Map) {

			return unwrapMap(actionContext, (Map<String, Object>) obj);
		} else {

			return obj;
		}
	}

	protected static List<Object> unwrapIterable(final ActionContext actionContext, final Iterable<Object> iterable) {

		final List<Object> unwrappedList = new ArrayList<>();

		for (Object o : iterable) {

			unwrappedList.add(unwrap(actionContext, o));
		}
		return unwrappedList;
	}

	protected static Map<String, Object> unwrapMap(final ActionContext actionContext, final Map<String, Object> map) {

		final Map<String, Object> unwrappedMap = new HashMap<>();

		for (Map.Entry<String,Object> entry : map.entrySet()) {

			unwrappedMap.put(entry.getKey(), unwrap(actionContext, entry.getValue()));
		}
		return unwrappedMap;
	}

	protected static List<Object> convertValueToList(final ActionContext actionContext, final Value value) {

		final List<Object> resultList = new ArrayList<>();

		if (value.hasArrayElements()) {

			for (int i = 0; i < value.getArraySize(); i++) {

				resultList.add(unwrap(actionContext, value.getArrayElement(i)));
			}
		}

		return resultList;
	}

	protected static Map<String, Object> convertValueToMap(final ActionContext actionContext, final Value value) {

		final Map<String, Object> resultMap = new HashMap<>();

		if (value.hasMembers()) {

			for (String key : value.getMemberKeys()) {

				resultMap.put(key, unwrap(actionContext, value.getMember(key)));
			}
		}

		return resultMap;
	}

	public static class FunctionWrapper implements ProxyExecutable {
		private Value func;
		private final ActionContext actionContext;
		private final ReentrantLock lock;

		public FunctionWrapper(final ActionContext actionContext, final Value func) {

			this.actionContext = actionContext;
			this.lock = new ReentrantLock();

			if (func.canExecute()) {

				this.func = func;
			}
		}

		@Override
		public Object execute(Value... arguments) {

			synchronized (func.getContext()) {

				if (func != null) {

					lock.lock();
					List<Value> processedArgs = Arrays.stream(arguments)
							.map(a -> unwrap(actionContext, a))
							.map(a -> wrap(actionContext, a))
							.map(Value::asValue)
							.collect(Collectors.toList());


					Object result = func.execute(processedArgs.toArray(new Value[processedArgs.size()]));
					lock.unlock();

					return wrap(actionContext, unwrap(actionContext, result));
				}

			}
			return null;
		}

		public Value getValue() {

			return func;
		}
	}
}
