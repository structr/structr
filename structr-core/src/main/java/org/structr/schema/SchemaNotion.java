/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
package org.structr.schema;

import java.util.LinkedHashSet;
import java.util.Set;
import org.structr.core.GraphObject;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.relationship.SchemaRelationship;
import org.structr.core.notion.Notion;
import org.structr.core.notion.PropertySetSerializationStrategy;
import org.structr.core.property.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
public class SchemaNotion extends Notion {

	public SchemaNotion(final Class targetType) {

		super(

			// output serialization
			new PropertySetSerializationStrategy(
				GraphObject.id,
				SchemaNode.name
			),

			// input deserialization
			new SchemaDeserializationStrategy(
				true,
				targetType,

				// identifying properties
				toSet(SchemaNode.name),

				// "foreign" properties that should be routed to the relationship
				toSet(
					SchemaRelationship.relationshipType,
					SchemaRelationship.sourceMultiplicity,
					SchemaRelationship.targetMultiplicity,
					SchemaRelationship.sourceNotion,
					SchemaRelationship.targetNotion,
					SchemaRelationship.sourceJsonName,
					SchemaRelationship.targetJsonName
				)
			)
		);
	}

	@Override
	public PropertyKey getPrimaryPropertyKey() {
		return GraphObject.id;
	}

	private static <T> Set<T> toSet(final T... values) {

		final Set<T> set = new LinkedHashSet<>();
		for (final T t : values) {

			set.add(t);
		}

		return set;
	}
}
