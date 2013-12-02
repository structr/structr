package org.structr.schema;

import java.util.LinkedHashSet;
import java.util.Set;
import org.structr.core.GraphObject;
import org.structr.core.entity.Schema;
import org.structr.core.entity.relationship.NodeIsRelatedToNode;
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
				Schema.className
			),
			
			// input deserialization
			new SchemaDeserializationStrategy(
				true,
				targetType,
			
				// identifying properties
				toSet(
					GraphObject.id,
					Schema.className
				),
			
				// "foreign" properties that should be routed to the relationship
				toSet(
					NodeIsRelatedToNode.relationshipType,
					NodeIsRelatedToNode.sourceMultiplicity,
					NodeIsRelatedToNode.targetMultiplicity,
					NodeIsRelatedToNode.sourceNotion,
					NodeIsRelatedToNode.targetNotion     
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
