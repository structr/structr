/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.graalvm.polyglot.proxy.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.AssertException;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.JsonException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.IllegalArgumentTypeException;
import org.structr.core.api.NamedArguments;
import org.structr.core.script.polyglot.context.ContextFactory;
import org.structr.core.script.polyglot.context.ContextHelper;
import org.structr.core.script.polyglot.wrappers.*;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;

import java.time.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class PolyglotWrapper {

	private static final Logger logger = LoggerFactory.getLogger(PolyglotWrapper.class);

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

				if (value.isProxyObject() && value.asProxyObject() instanceof PolyglotProxyDate proxyDate) {

					return proxyDate.getDateDelegate();
				}

				// Special handling to ensure proper unwrapping of JS Dates and making sure they're not accidentally treated as ZonedDateTime objects
				if (value.getMetaObject() != null && "Date".equals(value.getMetaObject().getMetaQualifiedName())) {

					return new Date(value.asInstant().toEpochMilli());
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

					// A host-side thenable, which today means the pending result of an async function call
					// that was returned without being awaited. It has to be settled here: handing it back raw
					// gives the caller an opaque object, and nothing would ever resolve it afterwards. The
					// check is on then() being *invokable*, so a response that merely has a "then" key is
					// unaffected -- a value in a map is never executable.
					if (value.canInvokeMember("then")) {

						return unwrapThenable(actionContext, value);
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

				// A thenable -- a promise, or anything shaped like one. An embedded script never gets here,
				// because its wrapper is resolved at the call boundary in Scripting; this is the path for a
				// non-embedded script whose completion value happens to be a promise.
				if (value.hasMembers() && value.canInvokeMember("then")) {

					return unwrapThenable(actionContext, value);
				}

				// A guest error -- new Error(...), a TypeError, or anything extending Error. It has members but
				// no meta name that matches the object conversion above, so without this it reaches the
				// fall-through below and becomes null: a promise rejected with one was reported as "rejected
				// with no reason given", discarding the only thing the author could act on.
				//
				// Answered as its own string ("Error: nope", "TypeError: bad type", "MyErr: custom"), which is
				// what a script author writes and reads. isException() is false for a plain object, so
				// Promise.reject({ code: 5 }) still converts to a map as before.
				//
				// Deliberately placed last, after every conversion that already works: the only values whose
				// treatment changes are the ones that were being silently dropped.
				if (value.isException()) {

					return value.toString();
				}

				if (value.isNull()) {

					return null;
				}

				// Even if we can't successfully unwrap the value, we can't return the raw value, since it's bound to it's original context.

				return null;
			}

			if (obj instanceof GraphObjectWrapper) {

				return ((GraphObjectWrapper) obj).getOriginalObject();

			}

			if (obj instanceof List list) {

				return unwrapList(actionContext, list);
			}

			if (obj instanceof Map map) {

				return unwrapMap(actionContext, map);
			}

			if (obj instanceof PolyglotProxyArray pa) {

				return unwrapProxyArray(actionContext, pa);
			}

			if (obj instanceof PolyglotProxyMap pm) {

				return pm.getOriginalObject();
			}

			return obj;

		} catch (final ThenableFailure tfx) {

			// deliberately not swallowed like everything else below: it is the script's own failure,
			// not a failure to unwrap. The finally still runs and restores the level.
			throw tfx;

		} catch (Throwable t) {

			logger.error("Unable to unwrap value of type {} coming out of the scripting engine.",
				obj != null ? obj.getClass().getName() : "null", t);

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

	protected static List<Object> unwrapProxyArray(final ActionContext actionContext, final PolyglotProxyArray proxyArray) {

		final List<Object> unwrappedList = new ArrayList<>();

		for (long i = 0, len = proxyArray.getSize(); i < len; i++) {

			unwrappedList.add(PolyglotWrapper.unwrap(actionContext, proxyArray.get(i)));
		}

		return unwrappedList;
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

		private final Value func;
		private final ContextFactory.LockedContext lockedContext;
		private ActionContext actionContext;
		private boolean hasRun;

		public FunctionWrapper(final ActionContext actionContext, final Value func) throws FrameworkException {

			this.actionContext = actionContext;
			this.hasRun = false;

			if (func.canExecute()) {

				this.func = func;
				ContextHelper.incrementReferenceCount(func.getContext());

				this.lockedContext = determineLockedContextFromFunction(func);

			} else {

				throw new FrameworkException(422, "Could not initialize FunctionWrapper, because given value was not executable.");
			}
		}

		public void setActionContext(final ActionContext actionContext) {

			this.actionContext = actionContext;
		}

		public ContextFactory.LockedContext getLockedContext() {

			return this.lockedContext;
		}

		private ContextFactory.LockedContext determineLockedContextFromFunction(Value func) {

            return actionContext.getScriptingContexts()
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().locksContext(func.getContext()))
                    .findFirst()
                    .map(Entry::getValue)
                    .orElse(null);
		}

		@Override
		public Object execute(Value... arguments) {

			if (func == null) {

				throw new IllegalStateException("FunctionWrapper: Function cannot be null.");
			}

			if (lockedContext == null) {

				throw new IllegalStateException("Could not execute function within PolyglotWrapper.FunctionWrapper, because the attached context is null.");
			}

			Object result = null;

			lockedContext.getLock().lock();

			try {

				lockedContext.getContext().enter();

				if (hasRun) {

					ContextHelper.incrementReferenceCount(lockedContext.getContext());
					hasRun = false;
				}

				List<Value> processedArgs = Arrays.stream(arguments)
						.map(a -> unwrap(actionContext, a))
						.map(a -> wrap(actionContext, a))
						.map(Value::asValue)
						.toList();

				try {

					result = unwrap(actionContext, func.execute(processedArgs.toArray()));
					hasRun = true;

				} finally {

					// Handle context reference counter and close current context if thread is the last one referencing it
					ContextHelper.decrementReferenceCount(lockedContext.getContext());

					lockedContext.getContext().leave();

					if (ContextHelper.getReferenceCount(lockedContext.getContext()) <= 0) {

						lockedContext.getContext().close();
						actionContext.removeScriptingContextByValue(lockedContext);
					}
				}

			} finally {

				lockedContext.getLock().unlock();
			}

            return wrap(actionContext, result);
		}

		public Value getValue() {

			return func;
		}
	}

	/**
	 * Resolves a thenable that has reached the host and answers its value.
	 *
	 * <p>This is the single settlement point for both script dialects. An embedded snippet's async
	 * wrapper is called through a plain guest arrow rather than directly (see
	 * {@code Scripting.evaluatePolyglot}), so its promise crosses to the host still pending, exactly as
	 * a non-embedded snippet's completion value does. Whether Structr wrapped the snippet is a choice it
	 * makes for the author, so it must not decide how -- or how expensively -- the script settles.</p>
	 *
	 * <p>Registering the reactions is itself an interop call, so GraalJS drains the promise job queue
	 * when it returns; for anything already settled, both callbacks have run by the time it does.</p>
	 *
	 * <p><b>The loop is what makes a race cheap.</b> While the script's own promise is still pending and
	 * something is still outstanding that could settle it, one deferred host call is joined -- the one
	 * that finished first. Each settlement is an interop call whose return drains the job queue, so the
	 * guest gets to act on it before the next one is considered. The moment the script's promise
	 * settles, the loop stops and the calls it no longer needs are never joined at all;
	 * {@code PendingThenables.closeFrame()} discards them and their workers finish unobserved.</p>
	 *
	 * <p>It has to be a host loop. {@code js.interop-complete-promises} drains the guest job queue to
	 * empty at an interop boundary rather than stopping when the promise it is completing has settled,
	 * so a drain scheduled as a guest job cannot decline to run -- it still parks on a loser nobody is
	 * waiting for. The stop condition is only visible here, where the completion promise's own reactions
	 * are held.</p>
	 *
	 * <p>A promise still pending once nothing is outstanding cannot be settled by anyone, because Structr
	 * scripting has no event loop, and is reported rather than answered with null.</p>
	 *
	 * <p>The value is unwrapped inside the callback, while the context is still open. The previous
	 * implementation kept the raw guest value and unwrapped it after the caller had already closed the
	 * context, so the unwrap threw and every async script quietly answered null.</p>
	 */
	private static Object unwrapThenable(final ActionContext actionContext, final Value thenable) {

		final Object[] outcome  = new Object[] { null, null };
		final boolean[] settled = new boolean[] { false, false };

		final ProxyExecutable onFulfilled = args -> {
			settled[0] = true;
			outcome[0] = args.length > 0 ? unwrap(actionContext, args[0]) : null;
			return null;
		};

		final ProxyExecutable onRejected = args -> {
			settled[1] = true;
			outcome[1] = args.length > 0 ? unwrap(actionContext, args[0]) : null;
			return null;
		};

		thenable.invokeMember("then", onFulfilled, onRejected);

		// Join the deferred calls one at a time, fastest first, for exactly as long as this script still
		// needs one of them. A script with nothing deferred -- the ordinary case -- never enters the loop.
		while (!settled[0] && !settled[1] && PendingThenables.hasDeferred()) {

			PendingThenables.settleNextCompleted();
		}

		if (settled[1]) {

			Object reason = outcome[1];

			// A rejection that carries the failure inside a RuntimeException is the shape the synchronous
			// call path throws, so it is the shape an asynchronous one has to throw for the two to be
			// indistinguishable. Unwrap one level, or the status-carrying branches below are missed and the
			// script is told its promise "rejected with java.lang.RuntimeException: ...".
			//
			// The test is JsonException rather than FrameworkException so that a wrapper is never mistaken
			// for the thing it wraps: anything already carrying a status is left alone, anything merely
			// holding one is opened.
			if (reason instanceof Throwable t && !(t instanceof JsonException) && t.getCause() instanceof JsonException) {

				reason = t.getCause();
			}

			// The two exceptions that carry a status the script author chose are exactly the two
			// implementors of JsonException, which exists for this and says so: "Common base class for
			// FrameworkException and AssertException to be able to handle them with the same code."
			//
			// Matching only FrameworkException sent every failing $.assert in an unwrapped snippet's promise
			// to the generic 422 below, stringifying the exception into the message and discarding the code.
			// Whether Structr wraps a snippet in the async arrow is a choice it makes for the author, so a
			// status must not depend on it.
			if (reason instanceof FrameworkException fex) {

				throw new ThenableFailure(fex);
			}

			if (reason instanceof AssertException aex) {

				throw new ThenableFailure(aex);
			}

			throw new ThenableFailure(new FrameworkException(422, "Server-side scripting error: promise rejected with "
				+ (reason != null ? reason.toString() : "no reason given")));
		}

		if (!settled[0]) {

			throw new ThenableFailure(new FrameworkException(422, "Server-side scripting error: script returned a"
				+ " promise that never resolved. Structr scripting has no event loop, so nothing can settle it after"
				+ " the script ends."));
		}

		return outcome[0];
	}

	/**
	 * Carries a script-level failure out of {@link #unwrap}, which has no {@code throws} clause and
	 * around a hundred call sites. Its cause is always the {@link FrameworkException} to report;
	 * {@code Scripting.evaluateScript} unwraps it again.
	 *
	 * <p>A distinct type rather than a plain {@code RuntimeException}, so that unwrap keeps swallowing
	 * everything else exactly as it did -- this is the one thing it must not swallow.</p>
	 */
	public static class ThenableFailure extends RuntimeException {

		public ThenableFailure(final FrameworkException cause) {

			super(cause.getMessage(), cause);
		}

		/**
		 * An {@link AssertException} carries its own status without being a {@link FrameworkException} --
		 * the two share {@link JsonException} and nothing else -- so it is carried as itself rather than
		 * repackaged, which would replace the author's status code with a generic one.
		 */
		public ThenableFailure(final AssertException cause) {

			super(cause.getMessage(), cause);
		}

		/**
		 * The failure to report, always one of the two {@link JsonException} implementors. Test it before
		 * calling {@link #getFrameworkException()}, which only covers one of them.
		 */
		public Throwable getReportedFailure() {

			return getCause();
		}

		/**
		 * @return the carried failure as a {@link FrameworkException}; only valid once
		 *         {@link #getReportedFailure()} has been shown not to be an {@link AssertException}.
		 */
		public FrameworkException getFrameworkException() {

			return (FrameworkException) getCause();
		}
	}
}
