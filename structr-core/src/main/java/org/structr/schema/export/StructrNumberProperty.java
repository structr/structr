package org.structr.schema.export;

import java.net.URISyntaxException;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.NodeAttribute;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.json.JsonNumberProperty;
import org.structr.schema.json.JsonProperty;

/**
 *
 * @author Christian Morgner
 */
public class StructrNumberProperty extends StructrPropertyDefinition implements JsonNumberProperty {

	public StructrNumberProperty(final StructrTypeDefinition parent, final String name) throws URISyntaxException {

		super(parent, "properties/" + name);

		setType("number");
		setName(name);
	}

	@Override
	SchemaProperty createDatabaseSchema(final App app, final SchemaNode schemaNode) throws FrameworkException {

		final SchemaProperty schemaProperty = app.create(SchemaProperty.class,
			new NodeAttribute(SchemaProperty.schemaNode, schemaNode),
			new NodeAttribute(AbstractNode.name, getName())
		);

		setDefaultProperties(schemaProperty);

		schemaProperty.setProperty(SchemaProperty.propertyType, Type.Double.name());

		return schemaProperty;
	}

	@Override
	void initializeFromProperty(final JsonProperty property) {

		if (property instanceof JsonNumberProperty) {


		} else {

			throw new IllegalStateException("Invalid property type " + property.getType());
		}
	}
}
