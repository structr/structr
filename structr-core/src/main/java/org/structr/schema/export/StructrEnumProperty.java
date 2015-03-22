package org.structr.schema.export;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.NodeAttribute;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.json.JsonEnumProperty;
import org.structr.schema.json.JsonProperty;
import org.structr.schema.json.JsonSchema;

/**
 *
 * @author Christian Morgner
 */
public class StructrEnumProperty extends StructrStringProperty implements JsonEnumProperty {

	public StructrEnumProperty(final StructrTypeDefinition parent, final String name) throws URISyntaxException {

		super(parent, "properties/" + name);

		setType("string");
		setName(name);
	}

	@Override
	public JsonEnumProperty setEnums(String... values) {

		final List<String> enums = getList(this, JsonSchema.KEY_ENUM, true);
		enums.addAll(Arrays.asList(values));

		Collections.sort(enums);

		return this;
	}

	@Override
	public Set<String> getEnums() {
		return new TreeSet<>(getList(this, JsonSchema.KEY_ENUM, false));
	}

	@Override
	SchemaProperty createDatabaseSchema(final App app, final SchemaNode schemaNode) throws FrameworkException {

		final SchemaProperty schemaProperty = app.create(SchemaProperty.class,
			new NodeAttribute(SchemaProperty.schemaNode, schemaNode),
			new NodeAttribute(AbstractNode.name, getName())
		);

		setDefaultProperties(schemaProperty);

		schemaProperty.setProperty(SchemaProperty.propertyType, Type.Enum.name());
		schemaProperty.setProperty(SchemaProperty.format, StringUtils.join(getEnums(), ","));

		return schemaProperty;
	}

	@Override
	void initializeFromProperty(final JsonProperty property) {

		if (property instanceof JsonEnumProperty) {

			final JsonEnumProperty str = (JsonEnumProperty)property;
			final Set<String> enums    = str.getEnums();

			setEnums(enums.toArray(new String[0]));

		} else {

			throw new IllegalStateException("Invalid property type " + property.getType());
		}
	}
}
