package org.structr.schema.export;

import java.net.URISyntaxException;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.NodeAttribute;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.json.JsonBooleanProperty;
import org.structr.schema.json.JsonProperty;

/**
 *
 * @author Christian Morgner
 */
public class StructrBooleanProperty extends StructrPropertyDefinition implements JsonBooleanProperty {

	public StructrBooleanProperty(final StructrTypeDefinition parent, final String name) throws URISyntaxException {

		super(parent, "properties/" + name);

		setType("boolean");
		setName(name);
	}

	@Override
	SchemaProperty createDatabaseSchema(final App app, final SchemaNode schemaNode) throws FrameworkException {

		final SchemaProperty schemaProperty = app.create(SchemaProperty.class,
			new NodeAttribute(SchemaProperty.schemaNode, schemaNode),
			new NodeAttribute(AbstractNode.name, getName())
		);

		setDefaultProperties(schemaProperty);

		schemaProperty.setProperty(SchemaProperty.propertyType, Type.Boolean.name());


//		jsonSchemaDefinition.put(JsonSchema.KEY_TYPE, "script");
//		jsonSchemaDefinition.put(JsonSchema.KEY_SOURCE, _format);
//		jsonSchemaDefinition.put(JsonSchema.KEY_CONTENT_TYPE, getContentTypeFromFormat(_format));
//		jsonSchemaDefinition.put(JsonSchema.KEY_MAP, getTypeReferenceForNotionProperty());
//		jsonSchemaDefinition.put(JsonSchema.KEY_PROPERTIES, getPropertiesForNotionProperty());
//		jsonSchemaDefinition.put(JsonSchema.KEY_REFERENCE, getPropertyReferenceForNotionProperty());
//		jsonSchemaDefinition.put(JsonSchema.KEY_ITEMS, items);
//		jsonSchemaDefinition.put(JsonSchema.KEY_SIZE_OF, "#/definitions/" + getName() + "/properties/" + _format);
//		jsonSchemaDefinition.put(JsonSchema.KEY_FORMAT, "date-time");
//		jsonSchemaDefinition.put(JsonSchema.KEY_ENUM, getEnumDefinitions());

		return schemaProperty;
	}

	@Override
	void initializeFromProperty(final JsonProperty property) {

		if (property instanceof JsonBooleanProperty) {


		} else {

			throw new IllegalStateException("Invalid property type " + property.getType());
		}
	}
}
