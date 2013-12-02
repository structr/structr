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
				SchemaNode.className
			),
			
			// input deserialization
			new SchemaDeserializationStrategy(
				true,
				targetType,
			
				// identifying properties
				toSet(
					GraphObject.id,
					SchemaNode.className
				),
			
				// "foreign" properties that should be routed to the relationship
				toSet(
					SchemaRelationship.relationshipType,
					SchemaRelationship.sourceMultiplicity,
					SchemaRelationship.targetMultiplicity,
					SchemaRelationship.sourceNotion,
					SchemaRelationship.targetNotion     
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
