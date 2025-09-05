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
package org.structr.core.script.polyglot.wrappers;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.StreamSupport;

public class PolyglotProxyArray implements ProxyArray {
	private static final Logger logger = LoggerFactory.getLogger(PolyglotProxyArray.class);
	final ActionContext actionContext;
	final GraphObject node;
	final PropertyKey propKey;
	List<Object> list;

	// Using StructrPolyglotProxyArray as a simple Wrapper/Proxy
	public PolyglotProxyArray(final ActionContext actionContext, final Object[] arr) {

		this.actionContext = actionContext;
		this.list = Arrays.asList(arr);
		this.node = null;
		this.propKey = null;
	}

	public PolyglotProxyArray(final ActionContext actionContext, final List list) {

		this.actionContext = actionContext;
		this.list = list;
		this.node = null;
		this.propKey = null;
	}

	// Using StructrPolyglotProxyArray as a synchronized proxy that automatically writes updates to it's source object
	public PolyglotProxyArray(final ActionContext actionContext, final GraphObject node, final PropertyKey propKey) {

		this.actionContext = actionContext;
		this.list = new ArrayList<>();
		this.node = node;
		this.propKey = propKey;

		updateListFromSource();
	}

	@Override
	public Object get(long index) {

		this.checkIndex(index);

		if (index >= list.size()) {

			return null;
		}

		return PolyglotWrapper.wrap(actionContext, list.get((int)index));
	}

	@Override
	public void set(long index, Value value) {

		this.checkIndex(index);

		final int size = list.size();

		if (size <= index) {

			// Determine delta between current index and target index
			long indexDelta = index - (size - 1);

			// Fill intermediate indices with null and leave one slot in the final index for the actual value to be set
			while (indexDelta > 1) {

				list.add(null);
			}

			list.add(PolyglotWrapper.unwrap(actionContext, value));

		} else {

			list.set((int) index, PolyglotWrapper.unwrap(actionContext, value));
		}

		writeListToSource();
		updateListFromSource();
	}

	@Override
	public long getSize() {
		return list.size();
	}

	@Override
	public boolean remove(long index) {

		this.checkIndex(index);

		if (index < list.size()) {

			list.remove((int) index);
			writeListToSource();

			return true;
		}

		return false;
	}

	private void checkIndex(long index) {

		if (index > 2147483647L || index < 0L) {

			throw new ArrayIndexOutOfBoundsException("invalid index.");
		}
	}

	private void updateListFromSource() {

		if (this.node != null && propKey != null) {

			list.clear();

			final Object value = node.getProperty(propKey);
			if (value != null) {

				if (value.getClass().isArray()) {

					for (Object o : (Object[]) value) {

						list.add(o);
					}

				} else if (value instanceof Iterable) {

					Iterable it = (Iterable) value;

					StreamSupport.stream(it.spliterator(), false).forEach(list::add);

				} else if (value instanceof Collection) {

					if (!(value instanceof List)) {

						list = new ArrayList<>((Collection) value);

					} else {

						list = (List) value;
					}
				}
			}
		}
	}

	private void writeListToSource() {

		if (this.node != null && this.propKey != null) {

			try {

				node.setProperty(propKey, propKey.inputConverter(actionContext.getSecurityContext(), false).convert(list));

			} catch (FrameworkException ex) {

				throw new RuntimeException(ex);
			}
		}
	}

}
