package org.structr.core.script;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.IdFunctionCall;
import org.mozilla.javascript.IdFunctionObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.Wrapper;
import org.structr.common.CaseHelper;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.parser.Functions;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 * @author Christian Morgner
 */
public class StructrScriptable extends ScriptableObject {

	private static final Logger logger = Logger.getLogger(StructrScriptable.class.getName());
	private static final Object[] IDs  = { "id", "type" };

	private ActionContext actionContext     = null;
	private FrameworkException exception    = null;
	private GraphObject entity              = null;

	public StructrScriptable(final ActionContext actionContext, final GraphObject entity) {

		this.actionContext   = actionContext;
		this.entity          = entity;
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

					if (parameters.length > 0 && parameters[0] != null) {

						try {

							return wrap(thisObject, null, actionContext.evaluate(entity, parameters[0].toString(), null, null));

						} catch (FrameworkException ex) {
							exception = ex;
						}
					}

					return null;
				}
			}, null, 0, 0);
		}

		if ("print".equals(name)) {

			return new IdFunctionObject(new IdFunctionCall() {

				@Override
				public Object execIdCall(final IdFunctionObject info, final Context context, final Scriptable scope, final Scriptable thisObject, final Object[] parameters) {
					actionContext.print(parameters);
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

		if ("log".equals(name)) {

			return new IdFunctionObject(new IdFunctionCall() {

				@Override
				public Object execIdCall(final IdFunctionObject info, final Context context, final Scriptable scope, final Scriptable thisObject, final Object[] parameters) {

					if (parameters.length > 0 && parameters[0] != null) {

						final StringBuilder buf = new StringBuilder();
						for (final Object obj : parameters) {

							buf.append(obj);
						}

						logger.log(Level.INFO, buf.toString());
					}

					return null;
				}
			}, null, 0, 0);
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

								final Function func = Functions.functions.get(name);

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

		// execute builtin function?
		final Function<Object, Object> function = Functions.functions.get(CaseHelper.toUnderscore(name, false));
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

	// ----- private methods -----
	private Object wrap(final Scriptable scope, final String key, final Object value) {

		if (value instanceof Collection) {
			return new StructrArray(scope, key, wrapCollection(scope, key, (Collection)value));
		}

		if (value instanceof GraphObject) {
			return new GraphObjectWrapper(scope, (GraphObject)value);
		}

		if (value instanceof HttpServletRequest) {
			return new HttpServletRequestWrapper((HttpServletRequest)value);
		}

		return value;
	}

	private Object[] wrapCollection(final Scriptable scope, final String key, final Collection collection) {

		final int size       = collection.size();
		final Object[] array = new Object[size];
		int i                = 0;

		for (final Object obj : collection) {
			array[i++] = wrap(scope, key, obj);
		}

		return array;
	}

	public Object unwrap(final Object source) {

		if (source != null) {

			if (source instanceof Wrapper) {

				return unwrap(((Wrapper)source).unwrap());

			} else {

				if (source.getClass().isArray()) {

					final List list = new ArrayList();
					for (final Object obj : (Object[])source) {

						list.add(unwrap(obj));
					}

					return list;
				}
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

				return wrap(scope, null, function.apply(actionContext, entity, unwrappedParameters));

			} catch (FrameworkException fex) {
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

		private Scriptable prototype = null;
		private Scriptable scope     = null;
		private GraphObject obj      = null;

		public GraphObjectWrapper(final Scriptable scope, final GraphObject obj) {

			this.scope = scope;
			this.obj         = obj;
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

				return wrap(this, name, obj.getProperty(key));
			}

			// second try, methods
			final Object value = wrap(this, null, checkEntityMethods(name));
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
				return wrap(this, null, obj.evaluate(actionContext.getSecurityContext(), name, null));

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
					fex.printStackTrace();
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
					fex.printStackTrace();
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

								final Function grant = Functions.functions.get("grant");
								if (grant != null) {

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
										grant.apply(actionContext, null, new Object[] { principal, obj, permissions } );
									}
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

			logger.log(Level.WARNING, "getDefaultValue() of HttpServletRequestWrapper called, don't know what to return here.. Please report to team@structr.com what you were trying to do with this object when you encountered this error message.");

			return null;
		}

	}
}
