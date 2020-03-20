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
import org.graalvm.polyglot.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.property.PropertyKey;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class StructrPolyglotGraphObjectWrapper implements ProxyObject {

	private static final Logger logger                       = LoggerFactory.getLogger(Scripting.class.getName());

	private final GraphObject graphObject;

	public StructrPolyglotGraphObjectWrapper(final GraphObject graphObject) {
		this.graphObject = graphObject;
	}

	@Override
	public Object getMember(String key) {
		return graphObject.getProperty(key);
	}

	@Override
	public Object getMemberKeys() {
		return StreamSupport.stream(graphObject.getPropertyKeys("all").spliterator(), true).map(key -> key.dbName()).collect(Collectors.toList());
	}

	@Override
	public boolean hasMember(String key) {
		return ((List<String>)getMemberKeys()).contains(key);
	}

	@Override
	public void putMember(String key, Value value) {
		PropertyKey propKey = StructrApp.getConfiguration().getPropertyKeyForDatabaseName(graphObject.getEntityType(), key);

		if (propKey != null) {

			try {

				graphObject.setProperty(propKey, StructrPolyglotWrapper.unwrap(value));
			} catch (FrameworkException ex) {

				logger.error("Exception while trying to set property.", ex);
			}
		}
	}

	public GraphObject getGraphObject() {
		return graphObject;
	}
}
