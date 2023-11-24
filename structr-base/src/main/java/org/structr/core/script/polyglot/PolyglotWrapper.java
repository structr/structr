/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.core.GraphObject;
import org.structr.core.script.polyglot.wrappers.GraphObjectWrapper;
import org.structr.core.script.polyglot.wrappers.NonWrappableObject;
import org.structr.core.script.polyglot.wrappers.PolyglotProxyArray;
import org.structr.core.script.polyglot.wrappers.PolyglotProxyMap;
import org.structr.schema.action.ActionContext;

import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.structr.common.error.FrameworkException;

public abstract class PolyglotWrapper {

	// Wraps values going into the scripting context. E.g.: GraphObject -> StructrPolyglotGraphObjectWrapper
	public static Object wrap(final ActionContext actionContext, final Object obj) {

		if (obj == null) {

			return null;
		}

		if (obj instanceof Value) {
			return obj;
		}

		if (obj instanceof NonWrappableObject) {

			return ((NonWrappableObject)obj).unwrap();
		}

		if (obj instanceof GraphObject) {

			return new GraphObjectWrapper(actionContext, (GraphObject) obj);
		}

		if (obj instanceof List) {

			return new PolyglotProxyArray(actionContext, (List)obj);
		}

		if (obj instanceof Iterable) {

			return new PolyglotProxyArray(actionContext, (List)StreamSupport.stream(((Iterable)obj).spliterator(), false).collect(Collectors.toList()));
		}

		if (obj instanceof Map) {

			return new PolyglotProxyMap(actionContext, (Map<String, Object>)obj);
		}

		if (obj instanceof Enumeration) {

			final Enumeration enumeration = (Enumeration)obj;
			final List<Object> enumList = new ArrayList<>();

			while (enumeration.hasMoreElements()) {

				enumList.add(enumeration.nextElement());
			}

			return new PolyglotProxyArray(actionContext, enumList.toArray());
		}

		if (obj instanceof Date) {

			final Context context = Context.getCurrent();
			final Value jsDateValue = context.eval("js", "new Date()");
			jsDateValue.invokeMember("setTime", ((Date)obj).getTime());

			return jsDateValue;

		}

		if (obj.getClass().isArray() && !(obj instanceof byte[]) ) {

			return new PolyglotProxyArray(actionContext, (Object[]) obj);
		}

		if (obj instanceof Value && !((Value)obj).getContext().equals(Context.getCurrent())) {

			// Try to rewrap objects from foreign contexts
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
			}

			if (value.isBoolean()) {

				return value.asBoolean();
			}

			if (value.isNumber()) {

				if (value.fitsInInt()) {

					return value.asInt();
				}

				if (value.fitsInLong()) {

					return value.asLong();
				}

				if (value.fitsInFloat()) {

					Float f = value.asFloat();
					if (!Float.isNaN(f)) {

						return f;
					}

					return null;
				}

				if (value.fitsInDouble()) {

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

			}

			if (value.isHostObject()) {

				return unwrap(actionContext, value.asHostObject());

			}

			if (value.isDate()) {

				if (value.isTime()) {

					return Date.from(value.asDate().atTime(value.asTime()).atZone(ZoneId.systemDefault()).toInstant());
				}

				return Date.from(value.asDate().atStartOfDay(ZoneId.systemDefault()).toInstant());

			}

			if (value.isProxyObject() && value.hasMembers()) {

				ProxyObject proxy = value.asProxyObject();

				if (proxy instanceof GraphObjectWrapper) {

					return ((GraphObjectWrapper)proxy).getOriginalObject();
				}

				if (proxy instanceof PolyglotProxyMap) {

					return ((PolyglotProxyMap)proxy).getOriginalObject();
				}

				return proxy;
			}

			if (value.hasArrayElements()) {

				return convertValueToList(actionContext, value);
			}

			if (value.hasMembers() && value.getMetaObject().getMetaSimpleName().toLowerCase().equals("object")) {

				return convertValueToMap(actionContext, value);
			}

			if (value.isNull()) {

				return null;
			}

			return value;
		}

		if (obj instanceof GraphObjectWrapper) {

			return ((GraphObjectWrapper)obj).getOriginalObject();

		}

		if (obj instanceof Iterable) {

			return unwrapIterable(actionContext, (Iterable) obj);
		}

		if(obj instanceof Map) {

			return unwrapMap(actionContext, (Map<String, Object>) obj);
		}

		return obj;
	}

	public static Map<String, Object> unwrapExecutableArguments(final ActionContext actionContext, final String methodName, final Value[] arguments) throws FrameworkException {

		/*
		int paramCount = method.getParameterCount();

		if (paramCount == 0) {

			return PolyglotWrapper.wrap(actionContext, method.invoke(null));

		} else if (paramCount == 1) {

			return PolyglotWrapper.wrap(actionContext, method.invoke(null, actionContext.getSecurityContext()));

		} else if (paramCount == 2 && arguments.length == 0) {

			return PolyglotWrapper.wrap(actionContext, method.invoke(null, actionContext.getSecurityContext(), new HashMap<String, Object>()));

		} else if (arguments.length == 0) {

			return PolyglotWrapper.wrap(actionContext, method.invoke(null, actionContext.getSecurityContext()));

		} else {

			return PolyglotWrapper.wrap(actionContext, method.invoke(null, ArrayUtils.add(Arrays.stream(arguments).map(arg -> PolyglotWrapper.unwrap(actionContext, arg)).toArray(), 0, actionContext.getSecurityContext())));
		}
		*/


		final Map<String, Object> parameters = new LinkedHashMap<>();

		if (arguments.length > 0) {

			final Object value = PolyglotWrapper.unwrap(actionContext, arguments[0]);

			if (arguments.length == 1 && value instanceof Map map) {

				parameters.putAll(map);

			} else {

				throw new RuntimeException(
					new FrameworkException(
						422,
						"Tried to call method \"" + methodName + "\" with invalid parameters. SchemaMethods expect their parameters to be passed as an object."
					)
				);

			}
		}

		return parameters;
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

			final long size = value.getArraySize();

			for (int i = 0; i < size; i++) {

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


					Object result = func.execute(processedArgs.toArray());
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
