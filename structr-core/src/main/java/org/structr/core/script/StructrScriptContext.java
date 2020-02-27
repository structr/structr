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

import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.script.Bindings;
import javax.script.ScriptContext;

/**
 *
 *
 */
public class StructrScriptContext implements ScriptContext {

	private Map<Integer, Bindings> bindingMap = new HashMap<>();
	private Writer errorWriter                = null;
	private Reader reader                     = null;
	private Writer writer                     = null;

	@Override
	public void setBindings(Bindings bindings, int scope) {
		bindingMap.put(scope, bindings);
	}

	@Override
	public Bindings getBindings(int scope) {
		return bindingMap.get(scope);
	}

	@Override
	public void setAttribute(String name, Object value, int scope) {

		final Bindings bindings = getBindings(scope);
		if (bindings != null) {

			bindings.put(name, value);
		}
	}

	@Override
	public Object getAttribute(String name, int scope) {

		final Bindings bindings = getBindings(scope);
		if (bindings != null) {

			return bindings.get(name);
		}

		return null;
	}

	@Override
	public Object removeAttribute(String name, int scope) {

		final Bindings bindings = getBindings(scope);
		if (bindings != null) {

			return bindings.remove(name);
		}

		return null;
	}

	@Override
	public Object getAttribute(final String name) {

		final Bindings bindings = getBindings(ScriptContext.GLOBAL_SCOPE);
		if (bindings != null) {

			return bindings.get(name);
		}

		return null;

	}

	@Override
	public int getAttributesScope(final String name) {

		for (final Entry<Integer, Bindings> entry : bindingMap.entrySet()) {

			if (entry.getValue().containsKey(name)) {

				return entry.getKey();
			}
		}

		return -1;
	}

	@Override
	public Writer getWriter() {
		return writer;
	}

	@Override
	public Writer getErrorWriter() {
		return errorWriter;
	}

	@Override
	public void setWriter(Writer writer) {
		this.writer = writer;
	}

	@Override
	public void setErrorWriter(Writer writer) {
		this.errorWriter = writer;
	}

	@Override
	public Reader getReader() {
		return reader;
	}

	@Override
	public void setReader(Reader reader) {
		this.reader = reader;
	}

	@Override
	public List<Integer> getScopes() {
		return new LinkedList<>(bindingMap.keySet());
	}

}
