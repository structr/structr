package org.structr.schema.export;

import java.net.URISyntaxException;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.NodeAttribute;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.json.JsonProperty;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonScriptProperty;

/**
 *
 * @author Christian Morgner
 */
public class StructrScriptProperty extends StructrPropertyDefinition implements JsonScriptProperty {

	public StructrScriptProperty(final StructrTypeDefinition parent, final String name) throws URISyntaxException {

		super(parent, "properties/" + name);

		setType("script");
		setName(name);
	}

	@Override
	SchemaProperty createDatabaseSchema(final App app, final SchemaNode schemaNode) throws FrameworkException {

		final SchemaProperty schemaProperty = app.create(SchemaProperty.class,
			new NodeAttribute(SchemaProperty.schemaNode, schemaNode),
			new NodeAttribute(AbstractNode.name, getName())
		);

		setDefaultProperties(schemaProperty);

		final String contentType = getContentType();
		if (contentType != null) {

			switch (contentType) {

				case "text/cypher":
					schemaProperty.setProperty(SchemaProperty.propertyType, Type.Cypher.name());
					break;

				case "text/structrscript":
				case "text/javascript":
					schemaProperty.setProperty(SchemaProperty.propertyType, Type.Function.name());
					break;
			}
		}

		schemaProperty.setProperty(SchemaProperty.format, getSource());

		return schemaProperty;
	}

	@Override
	public JsonScriptProperty setSource(final String source) {

		put(JsonSchema.KEY_SOURCE, source);
		return this;
	}

	@Override
	public String getSource() {
		return getString(this, JsonSchema.KEY_SOURCE);
	}

	@Override
	public JsonScriptProperty setContentType(final String contentType) {

		put(JsonSchema.KEY_CONTENT_TYPE, contentType);
		return this;
	}

	@Override
	public String getContentType() {
		return getString(this, JsonSchema.KEY_CONTENT_TYPE);
	}

	@Override
	void initializeFromProperty(final JsonProperty property) {

		if (property instanceof JsonScriptProperty) {

			final JsonScriptProperty script = (JsonScriptProperty)property;

			setContentType(script.getContentType());
			setSource(script.getSource());

		} else {

			throw new IllegalStateException("Invalid property type " + property.getType());
		}
	}
}
