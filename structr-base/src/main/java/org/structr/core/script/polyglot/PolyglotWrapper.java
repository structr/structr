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
package org.structr.core.script.polyglot;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.*;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.IllegalArgumentTypeException;
import org.structr.core.api.NamedArguments;
import org.structr.core.script.polyglot.context.ContextHelper;
import org.structr.core.script.polyglot.wrappers.*;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;

import java.time.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class PolyglotWrapper {

	// Wraps values going into the scripting context. E.g.: GraphObject -> StructrPolyglotGraphObjectWrapper
	public static Object wrap(final ActionContext actionContext, final Object obj) {


		try {

			actionContext.level++;

			if (obj == null) {

				return null;
			}

			if (obj instanceof Value) {
				return obj;
			}

			if (obj instanceof NonWrappableObject) {

				return ((NonWrappableObject) obj).unwrap();
			}

			if (obj instanceof Traits t) {

				return new StaticTypeWrapper(actionContext, t);
			}

			if (obj instanceof GraphObject) {

				return new GraphObjectWrapper(actionContext, (GraphObject) obj);
			}

			if (obj instanceof List) {

				return new PolyglotProxyArray(actionContext, (List) obj);
			}

			if (obj instanceof Iterable) {

				return new PolyglotProxyArray(actionContext, (List) StreamSupport.stream(((Iterable) obj).spliterator(), false).collect(Collectors.toList()));
			}

			if (obj instanceof Map) {


				return new PolyglotProxyMap(actionContext, (Map<String, Object>) obj);
			}

			if (obj instanceof Enumeration) {

				final Enumeration enumeration = (Enumeration) obj;
				final List<Object> enumList = new ArrayList<>();

				while (enumeration.hasMoreElements()) {

					enumList.add(enumeration.nextElement());
				}

				return new PolyglotProxyArray(actionContext, enumList.toArray());
			}

			if (obj instanceof Date) {

				return new PolyglotProxyDate((Date) obj);
			}

			if (obj instanceof LocalDate lDate) {

				return ProxyDate.from(lDate);
			}

			if (obj instanceof LocalTime lTime) {

				return ProxyTime.from(lTime);
			}

			if (obj instanceof ZoneId zid) {

				return ProxyTimeZone.from(zid);
			}

			if (obj instanceof Instant inst) {

				return ProxyInstant.from(inst);
			}

			if (obj instanceof Duration dur) {

				return ProxyDuration.from(dur);
			}

			if (obj.getClass().isArray() && !(obj instanceof byte[])) {

				return new PolyglotProxyArray(actionContext, (Object[]) obj);
			}

			return obj;

		} finally {

			actionContext.level--;
		}
	}

	// Unwraps values coming out of the scripting engine. Maps/Lists will be unwrapped recursively to ensure all values will be in their native state.
	public static Object unwrap(final ActionContext actionContext, final Object obj) {

		try {

			if (obj instanceof Value) {

				Value value = (Value) obj;

				// Is value is a host object, return it's original type
				if (value.isHostObject()) {
					return unwrap(actionContext, value.asHostObject());
				}

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

				if (value.isHostObject() && value.asHostObject() instanceof PolyglotProxyDate proxyDate) {

					return proxyDate.getDateDelegate();
				}

				if (value.isInstant() && value.isTimeZone()) {

					return ZonedDateTime.ofInstant(value.asInstant(), value.asTimeZone());
				}

				if (value.isDate() && value.isTime() && value.isTimeZone()) {

					return ZonedDateTime.of(LocalDateTime.of(value.asDate(), value.asTime()), value.asTimeZone());
				}

				if (value.isDate() && value.isTime()) {
					return LocalDateTime.of(value.asDate(), value.asTime());
				}

				if (value.isDate()) {
					return value.asDate();
				}

				if (value.isTime()) {
					return value.asTime();
				}

				if (value.isInstant()) {
					return value.asInstant();
				}

				if (value.isDuration()) {
					return value.asDuration();
				}

				if (value.isTimeZone()) {
					return value.asTimeZone();
				}

				if (value.isProxyObject() && value.hasMembers()) {

					ProxyObject proxy = value.asProxyObject();

					if (proxy instanceof GraphObjectWrapper) {

						return ((GraphObjectWrapper) proxy).getOriginalObject();
					}

					if (proxy instanceof PolyglotProxyMap) {

						return ((PolyglotProxyMap) proxy).getOriginalObject();
					}

					return proxy;
				}

				if (value.hasArrayElements()) {

					return convertValueToList(actionContext, value);
				}

				if (value.hasHashEntries()) {

					return convertHashEntriestToMap(actionContext, value);
				}

				if (value.hasMembers() && Set.of("map", "object").contains(value.getMetaObject().getMetaSimpleName().toLowerCase())) {

					return convertValueToMap(actionContext, value);
				}

				if (value.hasIterator() && value.getMetaObject().getMetaSimpleName().toLowerCase().equals("set")) {

					return convertValueToSet(actionContext, value);
				}

				if (value.hasMembers() && value.getMetaObject() != null && "promise".equals(value.getMetaObject().getMetaSimpleName().toLowerCase())) {

					PromiseConsumer consumer = new PromiseConsumer();
					value.invokeMember("then", consumer);
					return consumer.getResult();
				}

				if (value.isNull()) {

					return null;
				}

				return value;
			}

			if (obj instanceof GraphObjectWrapper) {

				return ((GraphObjectWrapper) obj).getOriginalObject();

			}

			if (obj instanceof List) {

				return unwrapList(actionContext, (List) obj);
			}

			if (obj instanceof Map) {

				return unwrapMap(actionContext, (Map<String, Object>) obj);
			}

			if (obj != null) {
			}

			return obj;

		} catch (Throwable t) {

			t.printStackTrace();

		} finally {

			actionContext.level --;
		}

		return null;
	}

	public static Arguments unwrapExecutableArguments(final ActionContext actionContext, final AbstractMethod method, final Value[] args) throws FrameworkException {

		final NamedArguments arguments = new NamedArguments();

		for (final Value value : args) {

			final Object unwrapped = PolyglotWrapper.unwrap(actionContext, value);
			if (unwrapped instanceof Map map) {

				for (final Entry<String, Object> entry : ((Map<String, Object>)map).entrySet()) {

					arguments.add(entry);
				}

			} else {

				throw new IllegalArgumentTypeException();
				//arguments.add(unwrapped);
			}

		}

		return arguments;
	}

	protected static List<Object> unwrapList(final ActionContext actionContext, final List<Object> list) {

		final List<Object> unwrappedList = new ArrayList<>();

		for (Object o : list) {

			unwrappedList.add(unwrap(actionContext, o));
		}

		return unwrappedList;
	}

	protected static Map<String, Object> unwrapMap(final ActionContext actionContext, final Map<String, Object> map) {

		final Map<String, Object> unwrappedMap = new HashMap<>();

		for (Entry<String,Object> entry : map.entrySet()) {

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

	protected static Set<Object> convertValueToSet(final ActionContext actionContext, final Value value) {

		final Set<Object> resultSet = new HashSet<>();

		if (value.hasIterator()) {

			final Value it = value.getIterator();

			while (it.hasIteratorNextElement()) {
				resultSet.add(unwrap(actionContext, it.getIteratorNextElement()));
			}
		}

		return resultSet;
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

	protected static Map<String, Object> convertHashEntriestToMap(final ActionContext actionContext, final Value value) {

		final Map<String, Object> resultMap = new HashMap<>();

		if (value.hasHashEntries() && value.getHashSize() > 0) {

			Value keyIterator = value.getHashKeysIterator();

			while (keyIterator.isIterator() && keyIterator.hasIteratorNextElement()) {
				Value hashKey = keyIterator.getIteratorNextElement();
				Value hashValue = value.getHashValue(hashKey);

				String unwrappedKey = (String)unwrap(actionContext, hashKey);
				Object unwrappedValue = unwrap(actionContext, hashValue);
				if (unwrappedKey != null) {
					resultMap.put(unwrappedKey, unwrappedValue);
				}
			}
		}

		return resultMap;
	}

	public static class FunctionWrapper implements ProxyExecutable {

		private Value func;
		private ActionContext actionContext;
		private final ReentrantLock lock;
		private boolean hasRun;

		public FunctionWrapper(final ActionContext actionContext, final Value func) {

			this.actionContext = actionContext;
			this.lock = new ReentrantLock();
			this.hasRun = false;

			if (func.canExecute()) {

				this.func = func;
				ContextHelper.incrementReferenceCount(func.getContext());
			}
		}

		public void setActionContext(final ActionContext actionContext) {
			this.actionContext = actionContext;
		}

		@Override
		public Object execute(Value... arguments) {

			if (func == null) {
				throw new IllegalStateException("FunctionWrapper: Function cannot be null.");
			}

			synchronized (func.getContext()) {

				if (hasRun) {

					ContextHelper.incrementReferenceCount(func.getContext());
					hasRun = false;
				}

				lock.lock();
				List<Value> processedArgs = Arrays.stream(arguments)
						.map(a -> unwrap(actionContext, a))
						.map(a -> wrap(actionContext, a))
						.map(Value::asValue)
						.toList();

				Object result = func.execute(processedArgs.toArray());
				hasRun = true;
				lock.unlock();

				final Object wrappedResult = wrap(actionContext, unwrap(actionContext, result));

				// Handle context reference counter and close current context if thread is the last one referencing it
				ContextHelper.decrementReferenceCount(func.getContext());
				if (ContextHelper.getReferenceCount(func.getContext()) <= 0) {

					final Context curContext = actionContext.getScriptingContexts()
							.entrySet()
							.stream()
							.filter(entry -> entry.getValue().equals(func.getContext()))
							.findFirst()
							.map(Entry::getValue)
							.orElse(null);

					if (curContext != null) {

						curContext.close();
						actionContext.removeScriptingContextByValue(curContext);
					}
				}

				return wrappedResult;
			}

		}

		public Value getValue() {

			return func;
		}
	}

	public static class PromiseConsumer implements Consumer<Object> {
		private Object result;

		@HostAccess.Export
		@Override
		public void accept(Object o) {
			result = o;
		}

		public Object getResult() {
			return result;
		}
	}
}
