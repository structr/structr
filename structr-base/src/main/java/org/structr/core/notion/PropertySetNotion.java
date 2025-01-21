/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.core.notion;

import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;

import java.util.Map;
import java.util.Set;

/**
 * Combines a {@link PropertySetSerializationStrategy} and a {@link TypeAndPropertySetDeserializationStrategy}
 * to read/write a {@link GraphObject}.
 *
 *
 */
public class PropertySetNotion<S extends NodeInterface> extends Notion<S, Map<String, Object>> {

	public PropertySetNotion(final Set<String> propertyKeys) {
		this(false, propertyKeys);
	}
	
	public PropertySetNotion(final boolean createIfNotExisting, final Set<String> propertyKeys) {
		this(
			new PropertySetSerializationStrategy(propertyKeys),
			new TypeAndPropertySetDeserializationStrategy(createIfNotExisting, propertyKeys)
		);

	}

	public PropertySetNotion(final SerializationStrategy serializationStrategy, final DeserializationStrategy deserializationStrategy) {
		super(serializationStrategy, deserializationStrategy);
	}

	@Override
	public PropertyKey getPrimaryPropertyKey() {
		return null; // this notion cannot deserialize objects from a single key
	}
}
