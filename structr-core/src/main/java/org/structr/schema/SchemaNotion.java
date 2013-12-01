package org.structr.schema;

import org.structr.core.GraphObject;
import org.structr.core.entity.SchemaNode;
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
			new PropertySetSerializationStrategy(GraphObject.id, SchemaNode.className),
			new SchemaDeserializationStrategy(true, targetType, GraphObject.id, SchemaNode.className)
		);
	}

	@Override
	public PropertyKey getPrimaryPropertyKey() {
		return GraphObject.id;
	}
}
