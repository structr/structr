package org.structr.schema.export;

import java.net.URISyntaxException;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.NodeAttribute;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.json.JsonProperty;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonStringProperty;

/**
 *
 * @author Christian Morgner
 */
public class StructrStringProperty extends StructrPropertyDefinition implements JsonStringProperty {

	public StructrStringProperty(final StructrTypeDefinition parent, final String name) throws URISyntaxException {

		super(parent, "properties/" + name);

		setType("string");
		setName(name);
	}

	@Override
	SchemaProperty createDatabaseSchema(final App app, final AbstractSchemaNode schemaNode) throws FrameworkException {

		final SchemaProperty schemaProperty = app.create(SchemaProperty.class,
			new NodeAttribute(SchemaProperty.schemaNode, schemaNode),
			new NodeAttribute(AbstractNode.name, getName())
		);

		setDefaultProperties(schemaProperty);

		if (JsonSchema.FORMAT_DATE_TIME.equals(getFormat())) {

			schemaProperty.setProperty(SchemaProperty.propertyType, Type.Date.name());

		} else {

			schemaProperty.setProperty(SchemaProperty.propertyType, Type.String.name());
			schemaProperty.setProperty(SchemaProperty.format, getFormat());
		}

		return schemaProperty;
	}

	@Override
	void initializeFromProperty(final JsonProperty property) {

		if (property instanceof JsonStringProperty) {

		} else {

			throw new IllegalStateException("Invalid property type " + property.getType());
		}
	}
}
