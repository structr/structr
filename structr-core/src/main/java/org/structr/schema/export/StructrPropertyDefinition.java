package org.structr.schema.export;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonProperty;

/**
 *
 * @author Christian Morgner
 */
public abstract class StructrPropertyDefinition extends StructrDefinition implements JsonProperty {

	private StructrTypeDefinition parent = null;
	private String name                  = null;
	private boolean required             = false;

	StructrPropertyDefinition(final StructrTypeDefinition parent, final String id) throws URISyntaxException {

		super(parent.root, id);
		this.parent = parent;
	}

	StructrPropertyDefinition(final StructrTypeDefinition parent, final String id, final JsonProperty source) throws URISyntaxException {

		super(parent.root, id);
		this.parent = parent;

		initializeFrom(source);
	}

	abstract SchemaProperty createDatabaseSchema(final App app, final SchemaNode schemaNode) throws FrameworkException;
	abstract void initializeFromProperty(final JsonProperty property);

	@Override
	public StructrTypeDefinition getParent() {
		return parent;
	}

	@Override
	public String getType() {
		return getString(this, JsonSchema.KEY_TYPE);
	}

	@Override
	public String getName() {
		return name;
	}
	@Override
	public String getFormat() {
		return getString(this, JsonSchema.KEY_FORMAT);
	}


	@Override
	public JsonProperty setName(final String name) {

		this.name = name;
		return this;
	}

	@Override
	public boolean isRequired() {
		return required;
	}

	@Override
	public Object getDefaultValue() {
		return get(JsonSchema.KEY_DEFAULT);
	}

	@Override
	public JsonProperty setRequired(final boolean isRequired) {

		this.required = isRequired;

		if (isRequired) {

			parent.getRequiredProperties().add(name);

		} else {

			parent.getRequiredProperties().remove(name);
		}

		return this;
	}

	@Override
	public boolean isUnique() {
		return getBoolean(this, JsonSchema.KEY_UNIQUE);
	}

	@Override
	public JsonProperty setUnique(final boolean isUnique) {

		if (isUnique) {
			put(JsonSchema.KEY_UNIQUE, isUnique);
		}

		return this;
	}

	@Override
	public JsonProperty setType(final String type) {

		put(JsonSchema.KEY_TYPE, type);
		return this;
	}

	@Override
	public JsonProperty setFormat(String format) {

		put(JsonSchema.KEY_FORMAT, format);
		return this;
	}

	@Override
	public JsonProperty setDefaultValue(final Object defaultValue) {
		put(JsonSchema.KEY_DEFAULT, defaultValue);
		return this;
	}

	// ----- interface Comparable<JsonSchemaProperty> -----
	@Override
	public int compareTo(JsonProperty other) {
		return getName().compareTo(other.getName());
	}

	// ----- package methods -----
	void createFromSource(final Map<String, Object> source) {
		putAll(source);
	}

	void setDefaultProperties(final SchemaProperty schemaProperty) throws FrameworkException {

		final Object defaultValueObject = getDefaultValue();
		if (defaultValueObject != null) {

			schemaProperty.setProperty(SchemaProperty.defaultValue, defaultValueObject.toString());
		}

		schemaProperty.setProperty(SchemaProperty.notNull, isRequired());
		schemaProperty.setProperty(SchemaProperty.unique, isUnique());
		schemaProperty.setProperty(SchemaProperty.format, getFormat());
	}

	// ----- static methods -----
	static StructrPropertyDefinition forJsonType(final StructrTypeDefinition parent, final JsonProperty property, final boolean isEnum) throws URISyntaxException {

		final String type                   = property.getType();
		final String name                   = property.getName();
		final StructrPropertyDefinition def = StructrPropertyDefinition.forStringType(parent, type, name, isEnum);

		def.setUnique(property.isUnique());
		def.setRequired(property.isRequired());
		def.setDefaultValue(property.getDefaultValue());

		def.initializeFrom(property);

		return def;
	}

	static StructrPropertyDefinition forStructrType(final StructrTypeDefinition parent, final SchemaProperty property) throws URISyntaxException {

		final String parentName = parent.getName();
		final String name       = property.getName();
		final Type type         = property.getPropertyType();
		final String _format    = property.getFormat();

		switch (type) {

			case Function:
				final StructrScriptProperty func = new StructrScriptProperty(parent, name);
				func.setContentType(property.getSourceContentType());
				func.setRequired(property.isRequired());
				func.setUnique(property.isUnique());
				func.setSource(_format);
				return func;

			case Cypher:
				final StructrScriptProperty cypher = new StructrScriptProperty(parent, name);
				cypher.setContentType(property.getSourceContentType());
				cypher.setSource("text/cypher");
				return cypher;


			case Notion:
				final String reference             = "#/definitions/" + parentName + "/properties/" + property.getNotionBaseProperty();
				final Set<String> notionProperties = property.getPropertiesForNotionProperty();
				final StructrPropertyDefinition notionProperty;

				if (property.getNotionMultiplicity().startsWith("*")) {

					notionProperty = new StructrArrayProperty(parent, name);
					final Map<String, Object> items = new TreeMap<>();

					items.put(JsonSchema.KEY_REFERENCE, reference);
					if (!notionProperties.isEmpty()) {

						items.put(JsonSchema.KEY_PROPERTIES, notionProperties);
					}
					notionProperty.put(JsonSchema.KEY_ITEMS, items);

				} else {

					notionProperty = new StructrObjectProperty(parent, name);
					notionProperty.put(JsonSchema.KEY_REFERENCE, reference);
					if (!notionProperties.isEmpty()) {

						notionProperty.put(JsonSchema.KEY_PROPERTIES, notionProperties);
					}
				}

				return notionProperty;

			case StringArray:
				final StructrArrayProperty arr = new StructrArrayProperty(parent, name);
				final Map<String, Object> items = new TreeMap<>();
				items.put(JsonSchema.KEY_TYPE, "string");
				arr.put(JsonSchema.KEY_TYPE, "array");
				arr.put(JsonSchema.KEY_ITEMS, items);
				return arr;

			case String:
				final StructrStringProperty str = new StructrStringProperty(parent, name);
				str.setRequired(property.isRequired());
				str.setUnique(property.isUnique());
				str.setDefaultValue(property.getDefaultValue());
				return str;
			case Boolean:
				final StructrBooleanProperty bool = new StructrBooleanProperty(parent,  name);
				bool.setRequired(property.isRequired());
				bool.setUnique(property.isUnique());
				bool.setDefaultValue(property.getDefaultValue());
				return bool;

			case Count:
				final StructrNumberProperty count = new StructrNumberProperty(parent, name);
				count.setRequired(property.isRequired());
				count.setUnique(property.isUnique());
				count.setDefaultValue(property.getDefaultValue());
				count.put(JsonSchema.KEY_SIZE_OF, "#/definitions/" + name + "/properties/" + _format);
				return count;

			case Integer:
			case Long:
			case Double:
				final StructrNumberProperty num = new StructrNumberProperty(parent, name);
				num.setRequired(property.isRequired());
				num.setUnique(property.isUnique());
				num.setDefaultValue(property.getDefaultValue());
				return num;

			case Date:
				final StructrStringProperty date = new StructrStringProperty(parent, name);
				date.setRequired(property.isRequired());
				date.setUnique(property.isUnique());
				date.setDefaultValue(property.getDefaultValue());
				date.setFormat(JsonSchema.FORMAT_DATE_TIME);
				return date;

			case Enum:
				final StructrEnumProperty enumProperty = new StructrEnumProperty(parent, name);
				enumProperty.setRequired(property.isRequired());
				enumProperty.setUnique(property.isUnique());
				enumProperty.setDefaultValue(property.getDefaultValue());
				enumProperty.setEnums(property.getEnumDefinitions().toArray(new String[0]));
				return enumProperty;
		}

		throw new IllegalStateException("Unknown type " + type);
	}

	static StructrPropertyDefinition forStructrType(final StructrTypeDefinition parent, final SchemaRelationshipNode property, final Set<String> existingPropertyNames, final boolean outgoing) throws URISyntaxException {

		final String relType = property.getProperty(SchemaRelationshipNode.relationshipType);
		final StructrPropertyDefinition def;

		if (outgoing) {

			final String targetType = property.getTargetNode().getName();
			String name             = property.getProperty(SchemaRelationshipNode.targetJsonName);

			if (name == null) {
				name = property.getPropertyName(targetType, existingPropertyNames, outgoing);
			}

			if ("1".equals(property.getProperty(SchemaRelationshipNode.targetMultiplicity))) {

				def = StructrPropertyDefinition.forStringType(parent, "object", name, false);

				// to-many relationship
				def.put(JsonSchema.KEY_REFERENCE, "#/definitions/" + targetType);
				def.put(JsonSchema.KEY_DIRECTION, "out");

			} else {

				def = StructrPropertyDefinition.forStringType(parent, "array", name, false);

				// to-one relationship
				final Map<String, Object> items = new TreeMap<>();
				items.put(JsonSchema.KEY_REFERENCE, "#/definitions/" + targetType);
				def.put(JsonSchema.KEY_TYPE, "array");
				def.put(JsonSchema.KEY_ITEMS, items);
				def.put(JsonSchema.KEY_DIRECTION, "out");
			}

		} else {

			final String sourceType = property.getSourceNode().getName();
			String name             = property.getProperty(SchemaRelationshipNode.sourceJsonName);

			if (name == null) {
				name = property.getPropertyName(sourceType, existingPropertyNames, outgoing);
			}

			if ("1".equals(property.getProperty(SchemaRelationshipNode.sourceMultiplicity))) {

				def = StructrPropertyDefinition.forStringType(parent, "object", name, false);

				// to-many relationship
				def.put(JsonSchema.KEY_REFERENCE, "#/definitions/" + sourceType);
				def.put(JsonSchema.KEY_DIRECTION, "in");

			} else {

				def = StructrPropertyDefinition.forStringType(parent, "array", name, false);

				// to-one relationship
				final Map<String, Object> items = new TreeMap<>();
				items.put(JsonSchema.KEY_REFERENCE, "#/definitions/" + sourceType);
				def.put(JsonSchema.KEY_TYPE, "array");
				def.put(JsonSchema.KEY_ITEMS, items);
				def.put(JsonSchema.KEY_DIRECTION, "in");
			}

		}

		// do not output the default relationship type
		if (!SchemaRelationshipNode.getDefaultRelationshipType(property).equals(relType)) {
			def.put(JsonSchema.KEY_RELATIONSHIP, relType);
		}

		return def;
	}

	static StructrPropertyDefinition forStringType(final StructrTypeDefinition parent, final String type, final String name, final boolean isEnum) throws URISyntaxException {

		switch (type) {

			case "string":
				if (isEnum) {
					return new StructrEnumProperty(parent, name);
				} else {
					return new StructrStringProperty(parent, name);
				}

			case "number":
				return new StructrNumberProperty(parent, name);

			case "object":
				return new StructrObjectProperty(parent, name);

			case "boolean":
				return new StructrBooleanProperty(parent, name);

			case "array":
				return new StructrArrayProperty(parent, name);

			case "script":
				return new StructrScriptProperty(parent, name);

		}

		throw new IllegalStateException("Unknown type " + type);
	}

	// ----- private methods -----
	private void initializeFrom(final JsonProperty source) {

		setName(source.getName());
		setType(source.getType());
		setUnique(source.isUnique());
		setRequired(source.isRequired());
		setDefaultValue(source.getDefaultValue());
		setFormat(source.getFormat());

		initializeFromProperty(source);
	}
}
