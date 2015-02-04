package org.structr.core.script;

import static org.mozilla.javascript.NativeJavaArray.wrap;
import org.mozilla.javascript.Scriptable;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
public class GraphObjectWrapper implements Scriptable {

	private Scriptable parentScope = null;
	private Scriptable prototype   = null;
	private GraphObject obj        = null;

	public GraphObjectWrapper(final Scriptable parentScope, final GraphObject obj) {

		this.parentScope = parentScope;
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
	public Object get(String string, Scriptable s) {

		final PropertyKey key = getKey(string);
		if (key != null) {

			return wrap(parentScope, obj.getProperty(key));
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
