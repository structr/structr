/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.schema.export;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.NodeAttribute;
import org.structr.schema.SchemaHelper.Type;
import static org.structr.schema.SchemaHelper.Type.Count;
import static org.structr.schema.SchemaHelper.Type.Cypher;
import static org.structr.schema.SchemaHelper.Type.Double;
import org.structr.schema.json.JsonProperty;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonType;

/**
 *
 *
 */
public abstract class StructrPropertyDefinition implements JsonProperty, StructrDefinition {

	protected JsonType parent     = null;
	protected String format       = null;
	protected String name         = null;
	protected String defaultValue = null;
	protected boolean required    = false;
	protected boolean unique      = false;
	protected boolean indexed     = false;

	StructrPropertyDefinition(final JsonType parent, final String name) {
		this.parent = parent;
		this.name   = name;
	}

	@Override
	public URI getId() {

		final URI parentId = parent.getId();
		if (parentId != null) {

			try {
				final URI containerURI = new URI(parentId.toString() + "/");
				return containerURI.resolve("properties/" + getName());

			} catch (URISyntaxException urex) {
				urex.printStackTrace();
			}
		}

		return null;
	}

	@Override
	public JsonType getParent() {
		return parent;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getFormat() {
		return format;
	}

	@Override
	public String getDefaultValue() {
		return defaultValue;
	}

	@Override
	public boolean isRequired() {
		return required;
	}

	@Override
	public boolean isUnique() {
		return unique;
	}

	@Override
	public boolean isIndexed() {
		return indexed;
	}

	@Override
	public JsonProperty setFormat(final String format) {

		this.format = format;
		return this;
	}

	@Override
	public JsonProperty setName(String name) {

		this.name = name;
		return this;
	}

	@Override
	public JsonProperty setRequired(boolean required) {

		this.required = required;
		return this;
	}

	@Override
	public JsonProperty setUnique(boolean unique) {

		this.unique = unique;
		return this;
	}

	@Override
	public JsonProperty setIndexed(boolean indexed) {

		this.indexed = indexed;
		return this;
	}

	@Override
	public JsonProperty setDefaultValue(final String defaultValue) {

		this.defaultValue = defaultValue;
		return this;
	}

	@Override
	public int compareTo(final JsonProperty o) {
		return getName().compareTo(o.getName());
	}

	@Override
	public StructrDefinition resolveJsonPointerKey(final String key) {
		return null;
	}

	// ----- package methods -----
	SchemaProperty createDatabaseSchema(final App app, final AbstractSchemaNode schemaNode) throws FrameworkException {

		return app.create(SchemaProperty.class,
			new NodeAttribute(SchemaProperty.name, getName()),
			new NodeAttribute(SchemaProperty.schemaNode, schemaNode),
			new NodeAttribute(SchemaProperty.unique, isUnique()),
			new NodeAttribute(SchemaProperty.indexed, isIndexed()),
			new NodeAttribute(SchemaProperty.notNull, isRequired()),
			new NodeAttribute(SchemaProperty.defaultValue, defaultValue)
		);
	}


	void deserialize(final Map<String, Object> source) {

		if (source.containsKey(JsonSchema.KEY_UNIQUE)) {
			this.unique = (Boolean)source.get(JsonSchema.KEY_UNIQUE);
		}

		if (source.containsKey(JsonSchema.KEY_INDEXED)) {
			this.indexed = (Boolean)source.get(JsonSchema.KEY_INDEXED);
		}

		final Object _defaultValue = source.get(JsonSchema.KEY_DEFAULT);
		if (_defaultValue != null) {

			this.defaultValue = _defaultValue.toString();
		}
	}

	void deserialize(final SchemaProperty property) {

		setDefaultValue(property.getDefaultValue());
		setRequired(property.isRequired());
		setUnique(property.isUnique());
		setIndexed(property.isIndexed());
	}

	Map<String, Object> serialize() {

		final Map<String, Object> map = new TreeMap<>();

		map.put(JsonSchema.KEY_TYPE, getType());

		if (unique) {
			map.put(JsonSchema.KEY_UNIQUE, true);
		}

		if (indexed) {
			map.put(JsonSchema.KEY_INDEXED, true);
		}

		if (format != null) {
			map.put(JsonSchema.KEY_FORMAT, format);
		}

		if (defaultValue != null) {
			map.put(JsonSchema.KEY_DEFAULT, defaultValue);
		}

		return map;
	}

	void initializeReferences() {
	}

	// ----- static methods -----
	static StructrPropertyDefinition deserialize(final StructrTypeDefinition parent, final String name, final Map<String, Object> source) {

		final String propertyType = (String)source.get(JsonSchema.KEY_TYPE);
		StructrPropertyDefinition newProperty = null;

		if (propertyType != null) {

			final boolean isDate   = source.containsKey(JsonSchema.KEY_FORMAT) && "date-time".equals(source.get(JsonSchema.KEY_FORMAT));
			final boolean isEnum   = source.containsKey(JsonSchema.KEY_ENUM);

			switch (propertyType) {

				case "string":	// can be string, date, enum or script

					if (isDate) {

						newProperty = new StructrDateProperty(parent, name);

					} else if (isEnum) {

						newProperty = new StructrEnumProperty(parent, name);

					} else {

						newProperty = new StructrStringProperty(parent, name);
					}
					break;

				case "script":
					newProperty = new StructrScriptProperty(parent, name);
					break;

				case "function":
					newProperty = new StructrFunctionProperty(parent, name);
					break;

				case "boolean":
					newProperty = new StructrBooleanProperty(parent, name);
					break;

				case "number":
					newProperty = new StructrNumberProperty(parent, name);
					break;

				case "integer":
					newProperty = new StructrIntegerProperty(parent, name);
					break;

				case "long":
					newProperty = new StructrLongProperty(parent, name);
					break;

				case "object":

					// notion properties don't contain $link
					if (source.containsKey(JsonSchema.KEY_REFERENCE) && !source.containsKey(JsonSchema.KEY_LINK)) {

						final Object reference = source.get(JsonSchema.KEY_REFERENCE);
						if (reference != null && reference instanceof String) {

							final String refName = StructrPropertyDefinition.getPropertyNameFromJsonPointer(reference.toString());
							newProperty = new NotionReferenceProperty(parent, name, (String)source.get(JsonSchema.KEY_REFERENCE), "object", refName);
						}
					}
					break;

				case "array":

					// notion properties don't contain $link
					if (!source.containsKey(JsonSchema.KEY_LINK)) {

						final Map<String, Object> items = (Map)source.get(JsonSchema.KEY_ITEMS);
						if (items != null) {

							if (items.containsKey(JsonSchema.KEY_REFERENCE)) {

								final Object reference = items.get(JsonSchema.KEY_REFERENCE);
								if (reference != null && reference instanceof String) {

									final String refName = StructrPropertyDefinition.getPropertyNameFromJsonPointer(reference.toString());
									newProperty = new NotionReferenceProperty(parent, name, (String)source.get(JsonSchema.KEY_REFERENCE), "array", refName);
								}

							} else if (items.containsKey(JsonSchema.KEY_TYPE)) {

								final Object typeValue = items.get(JsonSchema.KEY_TYPE);
								if (typeValue != null && "string".equals(typeValue.toString())) {

									newProperty = new StructrStringArrayProperty(parent, name);
								}
							}
						}
					}
					break;
			}

		} else {

			throw new IllegalStateException("Property " + name + " has no type.");
		}

		if (newProperty != null) {
			newProperty.deserialize(source);
		}

		return newProperty;
	}

	static StructrPropertyDefinition deserialize(final StructrTypeDefinition parent, final SchemaProperty property) {

		final String parentName = parent.getName();
		final Type type         = property.getPropertyType();
		final String name       = property.getName();

		switch (type) {

			case Function:
				final StructrFunctionProperty func = new StructrFunctionProperty(parent, name);
				func.deserialize(property);
				return func;

			case Cypher:
				final StructrScriptProperty cypher = new StructrScriptProperty(parent, name);
				cypher.deserialize(property);
				cypher.setContentType("text/cypher");
				return cypher;

			case Notion:
				final String referenceName         = property.getNotionBaseProperty();;
				final String reference             = "#/definitions/" + parentName + "/properties/" + referenceName;
				final Set<String> notionProperties = property.getPropertiesForNotionProperty();
				final NotionReferenceProperty notionProperty;

				if (property.getNotionMultiplicity().startsWith("*")) {

					notionProperty = new NotionReferenceProperty(parent, name, reference, "array", referenceName);
					notionProperty.setProperties(notionProperties.toArray(new String[0]));

				} else {

					notionProperty = new NotionReferenceProperty(parent, name, reference, "object", referenceName);
					notionProperty.setProperties(notionProperties.toArray(new String[0]));
				}

				notionProperty.deserialize(property);

				return notionProperty;

			case StringArray:
				final StructrStringArrayProperty arr = new StructrStringArrayProperty(parent, name);
				arr.deserialize(property);
				arr.setDefaultValue(property.getDefaultValue());
				return arr;

			case String:
				final StructrStringProperty str = new StructrStringProperty(parent, name);
				str.deserialize(property);
				str.setDefaultValue(property.getDefaultValue());
				return str;

			case Boolean:
				final StructrBooleanProperty bool = new StructrBooleanProperty(parent, name);
				bool.deserialize(property);
				return bool;

			case Count:
				final StructrIntegerProperty count = new StructrIntegerProperty(parent, name);
				count.deserialize(property);

				//count.put(JsonSchema.KEY_SIZE_OF, "#/definitions/" + name + "/properties/" + _format);
				return count;

			case Integer:
				final StructrIntegerProperty intProperty = new StructrIntegerProperty(parent, name);
				intProperty.deserialize(property);
				return intProperty;

			case Long:
				final StructrLongProperty longProperty = new StructrLongProperty(parent, name);
				longProperty.deserialize(property);
				return longProperty;

			case Double:
				final StructrNumberProperty doubleProperty = new StructrNumberProperty(parent, name);
				doubleProperty.deserialize(property);
				return doubleProperty;

			case Date:
				final StructrDateProperty date = new StructrDateProperty(parent, name);
				date.deserialize(property);
				date.setFormat(JsonSchema.FORMAT_DATE_TIME);
				return date;

			case Enum:
				final StructrEnumProperty enumProperty = new StructrEnumProperty(parent, name);
				enumProperty.deserialize(property);
				return enumProperty;
		}

		throw new IllegalStateException("Unknown type " + type);
	}

	// ----- private methods -----
	private static String getPropertyNameFromJsonPointer(final String pointer) {
		return pointer.substring(pointer.lastIndexOf("/") + 1);
	}
}
