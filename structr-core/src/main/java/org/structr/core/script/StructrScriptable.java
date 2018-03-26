/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import jdk.nashorn.api.scripting.ScriptUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.Wrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.CaseHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedException;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.function.Functions;
import org.structr.core.function.GrantFunction;
import org.structr.core.parser.CacheExpression;
import org.structr.core.parser.ConstantExpression;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 *
 */
public class StructrScriptable extends ScriptableObject {

	private static final Logger logger = LoggerFactory.getLogger(StructrScriptable.class.getName());
	private static final Object[] IDs  = { "id", "type" };

	private ActionContext actionContext     = null;
	private FrameworkException exception    = null;
	private GraphObject entity              = null;
	private Context scriptingContext        = null;

	public StructrScriptable(final ActionContext actionContext, final GraphObject entity, final Context scriptingContext) {

		this.actionContext    = actionContext;
		this.entity           = entity;
		this.scriptingContext = scriptingContext;
	}

	@Override
	public String getClassName() {
		return "Structr";
	}

	@Override
	public Object get(final String name, Scriptable start) {

		if ("get".equals(name)) {

			return new IdFunctionObject(new IdFunctionCall() {

				@Override
				public Object execIdCall(final IdFunctionObject info, final Context context, final Scriptable scope, final Scriptable thisObject, final Object[] parameters) {

					if (parameters.length == 1 && parameters[0] != null) {

						try {

							return wrap(context, thisObject, null, actionContext.evaluate(entity, parameters[0].toString(), null, null, 0));

						} catch (FrameworkException ex) {
							exception = ex;
						}

					} else if (parameters.length > 1) {

						// execute builtin get function
						final Function<Object, Object> function = Functions.get("get");
						try {

							final Object[] unwrappedParameters = new Object[parameters.length];
							int i                              = 0;

							// unwrap JS objects
							for (final Object param : parameters) {
								unwrappedParameters[i++] = unwrap(param);
							}

							return wrap(context, scope, null, function.apply(actionContext, entity, unwrappedParameters));

						} catch (FrameworkException fex) {
							exception = fex;
						}

						return null;

					}

					return null;
				}
			}, null, 0, 0);
		}

		if ("clear".equals(name)) {

			return new IdFunctionObject(new IdFunctionCall() {

				@Override
				public Object execIdCall(final IdFunctionObject info, final Context context, final Scriptable scope, final Scriptable thisObject, final Object[] parameters) {
					actionContext.clear();
					return null;
				}
			}, null, 0, 0);
		}

		if ("this".equals(name)) {

			return wrap(this.scriptingContext, start, null, entity);

		}

		if ("me".equals(name)) {

			return wrap(this.scriptingContext, start, null, actionContext.getSecurityContext().getUser(false));

		}

		if ("vars".equals(name)) {

			NativeObject nobj = new NativeObject();

			for (Map.Entry<String, Object> entry : actionContext.getAllVariables().entrySet()) {
				nobj.defineProperty(entry.getKey(), entry.getValue(), NativeObject.READONLY);
			}

			return nobj;
		}

		if ("include".equals(name) || "render".equals(name)) {

			return new IdFunctionObject(new IdFunctionCall() {

				@Override
				public Object execIdCall(final IdFunctionObject info, final Context context, final Scriptable scope, final Scriptable thisObject, final Object[] parameters) {

					if (parameters.length > 0 && parameters[0] != null) {

						try {

							final Function func = Functions.get(name);

							if (func != null) {

								actionContext.print(func.apply(actionContext, entity, parameters ));
							}

							return null;

						} catch (FrameworkException ex) {
							exception = ex;
						}
					}

					return null;
				}
			}, null, 0, 0);
		}

		if ("includeJs".equals(name)) {

			return new IdFunctionObject(new IdFunctionCall() {

				@Override
				public Object execIdCall(final IdFunctionObject info, final Context context, final Scriptable scope, final Scriptable thisObject, final Object[] parameters) {

					if (parameters.length == 1) {

						final String fileName = parameters[0].toString();
						final String source   = actionContext.getJavascriptLibraryCode(fileName);

						// use cached / compiled source code for JS libs
						Scripting.compileOrGetCached(context, source, fileName, 1).exec(context, scope);

					} else {

						logger.warn("Incorrect usage of includeJs function. Takes exactly one parameter: The filename of the javascript file!");

					}

					return null;
				}

			}, null, 0, 0);

		}

		if ("batch".equals(name)) {

			return new IdFunctionObject(new BatchFunctionCall(actionContext, this), null, 0, 0);
		}

		if ("cache".equals(name)) {

			return new IdFunctionObject(new IdFunctionCall() {

				@Override
				public Object execIdCall(final IdFunctionObject info, final Context context, final Scriptable scope, final Scriptable thisObject, final Object[] parameters) {

					final CacheExpression cacheExpr = new CacheExpression();

					Object retVal = null;

					try {
						for (int i = 0; i < parameters.length; i++) {
							cacheExpr.add(new ConstantExpression(parameters[i]));
						}

						retVal = cacheExpr.evaluate(actionContext, entity);

					} catch (FrameworkException ex) {
						exception = ex;
					}

					return retVal;
				}
			}, null, 0, 0);
		}

		if ("slice".equals(name)) {

			return new IdFunctionObject(new SliceFunctionCall(actionContext, entity, scriptingContext), null, 0, 0);

		}

		if ("doPrivileged".equals(name) || "do_privileged".equals(name)) {

			return new IdFunctionObject(new IdFunctionCall() {

				@Override
				public Object execIdCall(final IdFunctionObject info, final Context context, final Scriptable scope, final Scriptable thisObject, final Object[] parameters) {

					// backup security context
					final SecurityContext securityContext = StructrScriptable.this.actionContext.getSecurityContext();

					try {

						// replace security context with super user context
						actionContext.setSecurityContext(SecurityContext.getSuperUserInstance());

						if (parameters != null && parameters.length == 1) {

							final Object param = parameters[0];
							if (param instanceof Script) {

								final Script script = (Script)param;
								return script.exec(context, scope);

							} else {

								// ...
							}

						} else {

							//...
						}

						return null;

					} finally {

						// restore saved security context
						StructrScriptable.this.actionContext.setSecurityContext(securityContext);
					}

				}

			}, null, 0, 0);
		}

		// execute builtin function?
		final Function<Object, Object> function = Functions.get(CaseHelper.toUnderscore(name, false));
		if (function != null) {

			return new IdFunctionObject(new FunctionWrapper(function), null, 0, 0);
		}

		return null;
	}

	public boolean hasException() {
		return exception != null;
	}

	public FrameworkException getException() {
		return exception;
	}

	public void clearException() {
		exception = null;
	}

	// ----- private methods -----
	private Object wrap(final Context context, final Scriptable scope, final String key, final Object value) {

		if (value instanceof Collection) {
			return new StructrArray(scope, key, wrapCollection(context, scope, key, (Collection)value));
		}

		if (value instanceof Object[]) {
			return new StructrArray(scope, key, (Object[])value);
		}

		if (value instanceof GraphObject) {
			return new GraphObjectWrapper(context, scope, (GraphObject)value);
		}

		if (value instanceof HttpServletRequest) {
			return new HttpServletRequestWrapper((HttpServletRequest)value);
		}

		if (value != null && value.getClass().isEnum()) {

			return ((Enum)value).name();
		}

		if (value instanceof Map && !(value instanceof PropertyMap)) {

			return new MapWrapper((Map)value);
		}

		if (value instanceof Date) {

			return context.newObject(scope, "Date", new Object[] {((Date)value).getTime()});
		}

		return value;
	}

	private Object[] wrapCollection(final Context context, final Scriptable scope, final String key, final Collection collection) {

		final int size       = collection.size();
		final Object[] array = new Object[size];
		int i                = 0;

		for (final Object obj : collection) {
			array[i++] = wrap(context, scope, key, obj);
		}

		return array;
	}

	public Object unwrap(final Object source) {

		if (source != null) {

			if (source instanceof Wrapper) {

				return unwrap(((Wrapper)source).unwrap());

			} else if (source.getClass().isArray()) {

				final List list = new ArrayList();
				for (final Object obj : (Object[])source) {

					list.add(unwrap(obj));
				}

				return list;

			} else if (source instanceof StructrArray) {

				final List list = new ArrayList();
				for (final Object obj : ((StructrArray)source).toArray()) {

					list.add(unwrap(obj));
				}

				return list;

			} else if (source.getClass().getName().equals("org.mozilla.javascript.NativeDate")) {

				final Double value = ScriptRuntime.toNumber(source);
				return new Date(value.longValue());

			} else {

				return ScriptUtils.unwrap(source);
			}

		}

		return source;
	}

	// ----- nested classes -----
	public class FunctionWrapper implements IdFunctionCall {

		private Function<Object, Object> function = null;

		public FunctionWrapper(final Function<Object, Object> function) {
			this.function = function;
		}

		@Override
		public Object execIdCall(final IdFunctionObject info, final Context context, final Scriptable scope, final Scriptable thisObject, final Object[] parameters) {

			try {

				final Object[] unwrappedParameters = new Object[parameters.length];
				int i                              = 0;

				// unwrap JS objects
				for (final Object param : parameters) {
					unwrappedParameters[i++] = unwrap(param);
				}

				return wrap(context, scope, null, function.apply(actionContext, entity, unwrappedParameters));

			} catch (final UnlicensedException uex) {
				uex.log(logger);
			} catch (final FrameworkException fex) {
				exception = fex;
			}

			return null;
		}
	}

	public class StructrArray extends NativeArray {

		private Scriptable rootScriptable = null;
		private String key                = null;

		public StructrArray(final Scriptable rootScriptable, final String key, final Object[] array) {

			super(array);

			ScriptRuntime.setBuiltinProtoAndParent(this, rootScriptable, TopLevel.Builtins.Array);

			this.rootScriptable = rootScriptable;
			this.key            = key;
		}

		@Override
		public String toString() {

			final StringBuilder buf = new StringBuilder();
			boolean first           = true;

			buf.append("[");

			for (final Object obj : this.toArray()) {

				if (!first) {
					buf.append(",");
				}

				buf.append(obj.toString());
				first = false;
			}

			buf.append("]");

			return buf.toString();
		}

		@Override
		public Object get(final String name, final Scriptable s) {

			final Object obj = super.get(name, s);

			if (key != null && obj != null && "push".equals(name)) {

				final Scriptable prototype = getPrototype();
				if (prototype != null) {

					final Object pushFunction = prototype.get(name, s);
					if (pushFunction != null && pushFunction instanceof IdFunctionObject) {

						final IdFunctionObject push = (IdFunctionObject)pushFunction;

						return new IdFunctionObject(new IdFunctionCall() {

							@Override
							public Object execIdCall(IdFunctionObject f, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {

								// exec wrapped function call
								final Object result = push.call(cx, scope, thisObj, args);

								// do association
								rootScriptable.put(key, rootScriptable, StructrArray.this);

								return result;
							}

						}, "Array", push.methodId(), push.getArity());
					}
				}
			}

			return obj;
		}

		@Override
		public Object getDefaultValue(final Class<?> hint) {

			if (String.class.equals(hint)) {
				return toString();
			}

			return "[object Array]";
		}
	}

	public class GraphObjectWrapper implements Scriptable, Wrapper {

		private Context context      = null;
		private Scriptable prototype = null;
		private Scriptable scope     = null;
		private GraphObject obj      = null;

		public GraphObjectWrapper(final Context context, final Scriptable scope, final GraphObject obj) {

			this.context = context;
			this.scope = scope;
			this.obj   = obj;
		}

		@Override
		public String toString() {
			return getClassName();
		}

		@Override
		public String getClassName() {
			return obj.getType();
		}

		@Override
		public Object get(final String name, final Scriptable s) {

			// first try: property key
			final PropertyKey key = getKey(name);
			if (key != null) {

				return wrap(context, this, name, obj.getProperty(key));
			}

			// second try, methods
			final Object value = wrap(context, this, null, checkEntityMethods(name));
			if (value != null) {

				return value;
			}

			// third try: exported method
			final Method method = StructrApp.getConfiguration().getAnnotatedMethods(obj.getClass(), Export.class).get(name);
			if (method != null) {

				return new NativeJavaMethod(method, name);
			}

			// default: direct evaluation of object
			try {
				return wrap(context, this, null, obj.evaluate(actionContext, name, null));

			} catch (FrameworkException fex) {
				exception = fex;
			}

			return null;
		}

		@Override
		public Object get(int i, Scriptable s) {
			return null;
		}

		@Override
		public boolean has(String string, Scriptable s) {
			return get(string, s) != null;
		}

		@Override
		public boolean has(int i, Scriptable s) {
			return false;
		}

		@Override
		public void put(String string, Scriptable s, Object o) {

			if (obj instanceof GraphObjectMap) {

				try {

					obj.setProperty(new GenericProperty(string), StructrScriptable.this.unwrap(o));

				} catch (FrameworkException fex) {
					exception = fex;
				}

			} else {

				final PropertyKey key = getKey(string);
				if (key != null) {

					try {

						// call enclosing class's unwrap method instead of ours
						Object value = StructrScriptable.this.unwrap(o);
	//
	//					if (source instanceof Scriptable && "Date".equals(((Scriptable)source).getClassName())) {
	//						return Context.jsToJava(source, Date.class);
	//					}
	//
	//					// ECMA will return numbers a double, all the time.....
	//					if (value instanceof Double && key instanceof NumericalPropertyKey) {
	//						value = ((NumericalPropertyKey)key).convertToNumber((Double)value);
	//					}
	//
						// use inputConverter of EnumProperty to convert to native enums
						if (key instanceof EnumProperty) {

							// should we really use the inputConverter here??
							PropertyConverter inputConverter = key.inputConverter(actionContext.getSecurityContext());
							if (inputConverter != null) {

								value = inputConverter.convert(value);
							}


						} else {

							final Class valueType   = key.valueType();
							final Class relatedType = key.relatedType();

							// do not convert entity / collection properties
							if (valueType != null && relatedType == null) {

								value = Context.jsToJava(value, valueType);
							}
						}

						obj.setProperty(key, value);

					} catch (FrameworkException fex) {
						exception = fex;
					}
				}
			}
		}

		@Override
		public void put(int i, Scriptable s, Object o) {
		}

		@Override
		public void delete(String string) {

			final PropertyKey key = getKey(string);
			if (key != null) {

				try {
					obj.setProperty(key, null);

				} catch (FrameworkException fex) {
					exception = fex;
				}
			}
		}

		@Override
		public void delete(int i) {
		}

		@Override
		public Scriptable getPrototype() {
			return prototype;
		}

		@Override
		public void setPrototype(Scriptable s) {
			this.prototype = s;
		}

		@Override
		public Scriptable getParentScope() {
			return scope;
		}

		@Override
		public void setParentScope(Scriptable s) {
			this.scope = s;
		}

		@Override
		public Object[] getIds() {
			return IDs;
		}

		@Override
		public Object getDefaultValue(final Class<?> hint) {

			if (hint == null) {
				return unwrap();
			}

			if (String.class.equals(hint)) {
				return toString();
			}

			return null;
		}

		@Override
		public boolean hasInstance(Scriptable s) {
			return false;
		}

		@Override
		public Object unwrap() {
			return obj;
		}

		// ----- private methods -----
		private PropertyKey getKey(final String name) {
			return StructrApp.getConfiguration().getPropertyKeyForJSONName(obj.getClass(), name, false);
		}

		private Object checkEntityMethods(final String name) {

			if ("grant".equals(name)) {

				return new IdFunctionObject(new IdFunctionCall() {

					@Override
					public Object execIdCall(final IdFunctionObject info, final Context context, final Scriptable scope, final Scriptable thisObject, final Object[] parameters) {

						if (parameters.length > 0 && parameters[0] != null) {

							try {

								if (parameters.length >= 2 && parameters[0] != null && parameters[1] != null) {

									// principal, node, string
									final Object principal = StructrScriptable.this.unwrap(parameters[0]);
									String permissions     = parameters[1].toString();

									// append additional parameters to permission string
									if (parameters.length > 2) {

										for (int i=2; i<parameters.length; i++) {

											if (parameters[i] != null) {
												permissions += "," + parameters[i].toString();
											}
										}
									}

									// call function, entity can be null here!
									new GrantFunction().apply(actionContext, null, new Object[] { principal, obj, permissions } );
								}

								return null;

							} catch (FrameworkException ex) {
								exception = ex;
							}
						}

						return null;
					}
				}, null, 0, 0);
			}

			return null;
		}
	}

	public class HttpServletRequestWrapper extends ScriptableObject {

		private HttpServletRequest request = null;

		public HttpServletRequestWrapper(final HttpServletRequest request) {
			this.request = request;
		}

		@Override
		public String getClassName() {
			return "HttpServletRequest";
		}

		@Override
		public Object get(String name, Scriptable start) {
			return request.getParameter(name);
		}

		@Override
		public Object[] getIds() {
			return request.getParameterMap().values().toArray();
		}

		@Override
		public Object getDefaultValue(Class<?> hint) {

			logger.warn("getDefaultValue() of HttpServletRequestWrapper called, don't know what to return here.. Please report to team@structr.com what you were trying to do with this object when you encountered this error message.");

			return null;
		}

	}

	public class MapWrapper extends ScriptableObject implements Map {

		private Map<String, Object> map = null;

		public MapWrapper(final Map<String, Object> map) {
			this.map = map;
		}

		@Override
		public String toString() {
			return map.toString();
		}

		@Override
		public String getClassName() {
			return "Map";
		}

		@Override
		public Object get(String name, Scriptable start) {
			return map.get(name);
		}

		@Override
		public Object[] getIds() {
			return map.keySet().toArray();
		}

		@Override
		public Object getDefaultValue(Class<?> hint) {
			return map.toString();
		}

		@Override
		public boolean containsKey(Object key) {
			return map.containsKey(key);
		}

		@Override
		public boolean containsValue(Object value) {
			return map.containsValue(value);
		}

		@Override
		public Object put(Object key, Object value) {
			return map.put(null, value);
		}

		@Override
		public Object remove(Object key) {
			return map.remove(key);
		}

		@Override
		public void putAll(Map m) {
			map.putAll(m);
		}

		@Override
		public void clear() {
			map.clear();
		}

		@Override
		public Set keySet() {
			return map.keySet();
		}

		@Override
		public Collection values() {
			return map.values();
		}

		@Override
		public Set entrySet() {
			return map.entrySet();
		}
	}

	public interface JsDateWrap {

	    long getTime();
	    int getTimezoneOffset();
	}
}
