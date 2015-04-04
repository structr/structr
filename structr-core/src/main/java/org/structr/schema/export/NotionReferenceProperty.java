package org.structr.schema.export;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.schema.SchemaHelper;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonType;

/**
 *
 * @author Christian Morgner
 */
class NotionReferenceProperty extends StructrReferenceProperty {

	private String referenceName = null;
	private String reference     = null;
	private String type          = null;

	NotionReferenceProperty(final JsonType parent, final String name, final String reference, final String type, final String referenceName) {

		super(parent, name);

		this.referenceName = referenceName;
		this.reference     = reference;
		this.type          = type;
	}

	@Override
	public URI getId() {
		return null;
	}

	@Override
	public String getType() {
		return type;
	}

	// ----- package methods -----

	@Override
	SchemaProperty createDatabaseSchema(final App app, final AbstractSchemaNode schemaNode) throws FrameworkException {

		final SchemaProperty property = super.createDatabaseSchema(app, schemaNode);

		property.setProperty(SchemaProperty.format, referenceName + ", " + StringUtils.join(properties, ", "));
		property.setProperty(SchemaProperty.propertyType, SchemaHelper.Type.Notion.name());

		return property;
	}

	@Override
	void deserialize(final Map<String, Object> source) {

		super.deserialize(source);

		final Object propertiesValue = source.get(JsonSchema.KEY_PROPERTIES);
		if (propertiesValue != null && propertiesValue instanceof List) {

			this.properties.addAll((List)propertiesValue);
		}

		final String type = (String)source.get(JsonSchema.KEY_TYPE);
		if ("object".equals(type)) {

			reference = (String)source.get(JsonSchema.KEY_REFERENCE);

		} else if ("array".equals(type)) {

			final Object itemsValue = source.get(JsonSchema.KEY_ITEMS);
			if (itemsValue != null && itemsValue instanceof Map) {

				final Map<String, Object> items = (Map)itemsValue;
				reference = (String)items.get(JsonSchema.KEY_REFERENCE);
			}
		}
	}

	@Override
	void deserialize(final SchemaProperty schemaProperty) {

		super.deserialize(schemaProperty);

	}

	@Override
	Map<String, Object> serialize() {

		final Map<String, Object> map = super.serialize();
//			final int propertyCount       = properties.size();

//			if (propertyCount == 1) {
//
//			} else {
//
			if ("object".equals(getType())) {

				map.put(JsonSchema.KEY_REFERENCE, reference);

			} else if ("array".equals(getType())) {

				final Map<String, Object> items = new TreeMap<>();
				map.put(JsonSchema.KEY_ITEMS, items);

				items.put(JsonSchema.KEY_REFERENCE, reference);
			}
//			}


		map.put(JsonSchema.KEY_PROPERTIES, this.properties);

		return map;
	}
}
