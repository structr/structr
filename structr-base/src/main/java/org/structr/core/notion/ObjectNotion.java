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

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.RelationProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;

/**
 * Combines an {@link ObjectSerializationStrategy} and an
 * {@link IdDeserializationStrategy} to read/write a
 * {@link org.structr.core.GraphObject}. This is the default notion.
 *
 *
 */
public class ObjectNotion extends Notion {

	public ObjectNotion() {

		this(
			new ObjectSerializationStrategy(),
			new IdDeserializationStrategy()
		);
	}

	public ObjectNotion(final SerializationStrategy serializationStrategy, final DeserializationStrategy deserializationStrategy) {
		super(serializationStrategy, deserializationStrategy);
	}

	/**
	 * Identity serialization strategy. This class returns the object unmodified.
	 */
	public static class ObjectSerializationStrategy implements SerializationStrategy {

		@Override
		public Object serialize(final SecurityContext securityContext, final String type, final GraphObject source) {
			return source;
		}

		@Override
		public void setRelationProperty(final RelationProperty relationProperty) {
			// not interested yet..
		}
	}

	@Override
	public PropertyKey getPrimaryPropertyKey() {
		return Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.ID_PROPERTY);
	}
}
