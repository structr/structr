package org.structr.core.script;

import java.util.Collection;
import sun.org.mozilla.javascript.NativeObject;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.parser.Functions;
import org.structr.core.property.PropertyKey;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import sun.org.mozilla.javascript.Context;
import sun.org.mozilla.javascript.IdFunctionCall;
import sun.org.mozilla.javascript.IdFunctionObject;
import sun.org.mozilla.javascript.NativeJavaArray;
import sun.org.mozilla.javascript.Scriptable;

/**
 *
 * @author Christian Morgner
 */
public class StructrScriptable extends NativeObject {

	private SecurityContext securityContext = null;
	private ActionContext actionContext     = null;
	private GraphObject entity              = null;
	private Object data                     = null;

	public StructrScriptable(final SecurityContext securityContext, final ActionContext actionContext, final GraphObject entity) {

		this.securityContext = securityContext;
		this.actionContext   = actionContext;
		this.entity          = entity;
	}

	@Override
	public Object get(String name, Scriptable start) {

		// execute builtin function?
		final Function<Object, Object> function = Functions.functions.get(name);
		if (function != null) {

			return new IdFunctionObject(new FunctionWrapper(function), null, 0, 0);
		}

		try {

			return wrap(start, actionContext.evaluate(securityContext, entity, name, data, null));

		} catch (FrameworkException fex) {
			fex.printStackTrace();
		}

		return null;
	}

	@Override
	public Object get(int index, Scriptable start) {
		return super.get(index, start);
	}

	public void setData(final Object data) {
		this.data = data;
	}

	public class FunctionWrapper implements IdFunctionCall {

		private Function<Object, Object> function = null;

		public FunctionWrapper(final Function<Object, Object> function) {
			this.function = function;
		}

		@Override
		public Object execIdCall(final IdFunctionObject info, final Context context, final Scriptable externalScriptable, Scriptable structrScriptable, Object[] parameters) {

			try {
				return wrap(externalScriptable, function.apply(actionContext, entity, parameters));

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}

			return null;
		}
	}

	private Object wrap(final Scriptable rootScriptable, final Object value) {

		if (value instanceof Collection) {
			return new NativeJavaArray(rootScriptable, wrapCollection(rootScriptable, (Collection)value));
		}

		if (value instanceof GraphObject) {
			return new GraphObjectWrapper((GraphObject)value);
		}

		return value;
	}

	private Object[] wrapCollection(final Scriptable rootScriptable, final Collection collection) {

		final int size       = collection.size();
		final Object[] array = new Object[size];
		int i                = 0;

		for (final Object obj : collection) {
			array[i++] = wrap(rootScriptable, obj);
		}

		return array;
	}

	public class GraphObjectWrapper implements Scriptable {

		private Scriptable parentScope = null;
		private Scriptable prototype   = null;
		private GraphObject obj        = null;

		public GraphObjectWrapper(final GraphObject obj) {
			this.obj = obj;
		}

		@Override
		public String getClassName() {
			return obj.getType();
		}

		@Override
		public Object get(String string, Scriptable s) {

			final PropertyKey key = getKey(string);
			if (key != null) {

				return obj.getProperty(key);
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
					obj.setProperty(key, o);

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
			return parentScope;
		}

		@Override
		public void setParentScope(Scriptable s) {
			this.parentScope = s;
		}

		@Override
		public Object[] getIds() {
			return null;
		}

		@Override
		public Object getDefaultValue(Class<?> type) {
			return null;
		}

		@Override
		public boolean hasInstance(Scriptable s) {
			return false;
		}

		private PropertyKey getKey(final String name) {
			return StructrApp.getConfiguration().getPropertyKeyForJSONName(obj.getClass(), name, false);
		}
	}
}
