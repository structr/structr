package org.structr.schema.export;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.json.JsonEnumProperty;
import org.structr.schema.json.JsonSchema;

/**
 *
 * @author Christian Morgner
 */
public class StructrEnumProperty extends StructrStringProperty implements JsonEnumProperty {

	protected Set<String> enums = new TreeSet<>();

	public StructrEnumProperty(final StructrTypeDefinition parent, final String name) {

		super(parent, name);
	}

	@Override
	public JsonEnumProperty setEnums(String... values) {

		for (final String value : values) {
			enums.add(value.trim());
		}

		return this;
	}

	@Override
	public Set<String> getEnums() {
		return enums;
	}

	@Override
	Map<String, Object> serialize() {

		final Map<String, Object> map = super.serialize();

		map.put(JsonSchema.KEY_ENUM, enums);

		return map;
	}

	@Override
	void deserialize(final Map<String, Object> source) {

		super.deserialize(source);

		final Object enumValues = source.get(JsonSchema.KEY_ENUM);
		if (enumValues != null) {

			if (enumValues instanceof List) {

				enums.addAll((List)enumValues);

			} else {

				throw new IllegalStateException("Invalid enum values for property " + name + ", expected array.");
			}

		} else {

			throw new IllegalStateException("Missing enum values for property " + name);
		}
	}

	@Override
	void deserialize(final SchemaProperty schemaProperty) {

		super.deserialize(schemaProperty);

		setEnums(schemaProperty.getEnumDefinitions().toArray(new String[0]));
	}

	@Override
	SchemaProperty createDatabaseSchema(final App app, final AbstractSchemaNode schemaNode) throws FrameworkException {

		final SchemaProperty property = super.createDatabaseSchema(app, schemaNode);

		property.setProperty(SchemaProperty.propertyType, Type.Enum.name());
		property.setProperty(SchemaProperty.format, StringUtils.join(getEnums(), ", "));

		return property;
	}
}
