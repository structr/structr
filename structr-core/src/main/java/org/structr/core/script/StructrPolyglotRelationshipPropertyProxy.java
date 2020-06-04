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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.RelationProperty;
import org.structr.schema.action.ActionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class StructrPolyglotRelationshipPropertyProxy extends StructrPolyglotProxyArray {
	private static final Logger logger = LoggerFactory.getLogger(StructrPolyglotRelationshipPropertyProxy.class);
	private final GraphObject node;
	private final PropertyKey propKey;

	public StructrPolyglotRelationshipPropertyProxy(final ActionContext actionContext, final GraphObject node, final PropertyKey propKey) {
		super(actionContext, new ArrayList());
		this.node = node;
		this.propKey = propKey;
	}

	@Override
	public Object get(long index) {
		this.checkIndex(index);
		updateList();
		return StructrPolyglotWrapper.wrap(actionContext, list.get((int)index));
	}

	@Override
	public void set(long index, Value value) {
		this.checkIndex(index);
		updateList();

		if (list.size() == index) {
			list.add(StructrPolyglotWrapper.unwrap(value));
		}

		list.set((int)index, StructrPolyglotWrapper.unwrap(value));

		try {

			node.setProperty(propKey, list);
		} catch (FrameworkException ex) {

			logger.error("Could not set relationship property on node.", ex);
		}
	}

	@Override
	public long getSize() {
		updateList();
		return (long)list.size();
	}

	@Override
	public boolean remove(long index) {
		this.checkIndex(index);
		updateList();
		list.remove((int)index);
		return true;
	}

	private void checkIndex(long index) {
		if (index > 2147483647L || index < 0L) {
			throw new ArrayIndexOutOfBoundsException("invalid index.");
		}
	}

	private void updateList() {
		list.clear();
		Iterable it = (Iterable) node.getProperty(propKey);
		list = (List)StreamSupport.stream(it.spliterator(), false).collect(Collectors.toList());
	}
}
