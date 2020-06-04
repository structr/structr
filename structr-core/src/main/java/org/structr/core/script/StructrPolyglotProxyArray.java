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
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class StructrPolyglotProxyArray implements ProxyArray {
	final ActionContext actionContext;
	List<Object> list;

	public StructrPolyglotProxyArray(final ActionContext actionContext, final List list) {
		this.actionContext = actionContext;
		this.list = list;
	}

	@Override
	public Object get(long index) {
		this.checkIndex(index);
		return StructrPolyglotWrapper.wrap(actionContext, list.get((int)index));
	}

	@Override
	public void set(long index, Value value) {
		this.checkIndex(index);

		if (list.size() == index) {
			list.add(StructrPolyglotWrapper.unwrap(value));
		}

		list.set((int)index, StructrPolyglotWrapper.unwrap(value));
	}

	@Override
	public long getSize() {
		return (long)list.size();
	}

	@Override
	public boolean remove(long index) {
		this.checkIndex(index);
		list.remove((int)index);
		return true;
	}

	private void checkIndex(long index) {
		if (index > 2147483647L || index < 0L) {
			throw new ArrayIndexOutOfBoundsException("invalid index.");
		}
	}

}
