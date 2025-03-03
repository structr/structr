/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonProperty;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.SchemaService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 *
 *
 */
public abstract class StructrPropertyDefinition implements JsonProperty, StructrDefinition {

	private static final Logger logger = LoggerFactory.getLogger(StructrPropertyDefinition.class.getName());

	protected SchemaProperty schemaProperty = null;
	protected Set<String> transformers      = new LinkedHashSet<>();
	protected Set<String> validators        = new LinkedHashSet<>();
	protected JsonType parent               = null;
	protected String format                 = null;
	protected String name                   = null;
	protected String defaultValue           = null;
	protected String hint                   = null;
	protected String category               = null;
	protected boolean required              = false;
	protected boolean compound              = false;
	protected boolean unique                = false;
	protected boolean indexed               = false;
	protected boolean readOnly              = false;

	StructrPropertyDefinition(final JsonType parent, final String name) {
		this.parent = parent;
		this.name   = name;
	}

	@Override
	public String toString() {
		return getType() + " " + name;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(final Object other) {

		if (other instanceof StructrPropertyDefinition) {

			return other.hashCode() == hashCode();
		}

		return false;
	}

	@Override
	public URI getId() {

		final URI parentId = parent.getId();
		if (parentId != null) {

			try {
				final URI containerURI = new URI(parentId.toString() + "/");
				return containerURI.resolve("properties/" + getName());

			} catch (URISyntaxException urex) {
				logger.warn("", urex);
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
	public String getHint() {
		return hint;
	}

	@Override
	public String getCategory() {
		return category;
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
	public boolean isCompoundUnique() {
		return compound;
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
	public boolean isReadOnly() {
		return readOnly;
	}

	@Override
	public Set<String> getValidators() {
		return validators;
	}

	@Override
	public Set<String> getTransformators() {
		return transformers;
	}

	@Override
	public JsonProperty setHint(final String hint) {

		this.hint = hint;
		return this;
	}

	@Override
	public JsonProperty setCategory(final String category) {

		this.category = category;
		return this;
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
	public JsonProperty setCompound(boolean compound) {

		this.compound = compound;
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
	public JsonProperty setReadOnly(boolean readOnly) {

		this.readOnly = readOnly;
		return this;
	}

	@Override
	public JsonProperty setDefaultValue(final String defaultValue) {

		this.defaultValue = defaultValue;
		return this;
	}

	@Override
	public JsonProperty addValidator(final String fqcn) {

		this.validators.add(fqcn);
		return this;
	}

	@Override
	public JsonProperty addTransformer(final String fqcn) {

		this.transformers.add(fqcn);
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

	public SchemaProperty getSchemaProperty() {
		return schemaProperty;
	}

	// ----- package methods -----
	SchemaProperty createDatabaseSchema(final App app, final AbstractSchemaNode schemaNode) throws FrameworkException {

		final Traits traits     = Traits.of(StructrTraits.SCHEMA_PROPERTY);
		SchemaProperty property = schemaNode.getSchemaProperty(getName());

		if (property == null) {

			final PropertyMap getOrCreateProperties = new PropertyMap();

			getOrCreateProperties.put(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), getName());
			getOrCreateProperties.put(traits.key("schemaNode"), schemaNode);
			getOrCreateProperties.put(traits.key("compound"), isCompoundUnique());
			getOrCreateProperties.put(traits.key("unique"), isUnique());
			getOrCreateProperties.put(traits.key("indexed"), isIndexed());
			getOrCreateProperties.put(traits.key("notNull"), isRequired());
			getOrCreateProperties.put(traits.key("readOnly"), isReadOnly());
			getOrCreateProperties.put(traits.key("format"), getFormat());
			getOrCreateProperties.put(traits.key("hint"), getHint());
			getOrCreateProperties.put(traits.key("category"), getCategory());
			getOrCreateProperties.put(traits.key("validators"), listToArray(validators));
			getOrCreateProperties.put(traits.key("transformers"), listToArray(transformers));
			getOrCreateProperties.put(traits.key("defaultValue"), defaultValue);

			property = app.create(StructrTraits.SCHEMA_PROPERTY, getOrCreateProperties).as(SchemaProperty.class);
		}

		final PropertyMap updateProperties = new PropertyMap();

		if (parent != null) {

			final JsonSchema root = parent.getSchema();
			if (root != null) {

				if (SchemaService.DynamicSchemaRootURI.equals(root.getId())) {

					updateProperties.put(traits.key("isPartOfBuiltInSchema"), true);
				}
			}
		}

		// update properties
		property.setProperties(SecurityContext.getSuperUserInstance(), updateProperties);

		// return modified property
		return property;
	}


	void deserialize(final Map<String, Object> source) {

		if (source.containsKey(JsonSchema.KEY_COMPOUND)) {
			this.compound = (Boolean)source.get(JsonSchema.KEY_COMPOUND);
		}

		if (source.containsKey(JsonSchema.KEY_UNIQUE)) {
			this.unique = (Boolean)source.get(JsonSchema.KEY_UNIQUE);
		}

		if (source.containsKey(JsonSchema.KEY_INDEXED)) {
			this.indexed = (Boolean)source.get(JsonSchema.KEY_INDEXED);
		}

		if (source.containsKey(JsonSchema.KEY_READ_ONLY)) {
			this.readOnly = (Boolean)source.get(JsonSchema.KEY_READ_ONLY);
		}

		final Object _format = source.get(JsonSchema.KEY_FORMAT);
		if (_format != null) {

			this.format = _format.toString();
		}

		final Object _hint = source.get(JsonSchema.KEY_HINT);
		if (_hint != null) {

			this.hint = _hint.toString();
		}

		final Object _category = source.get(JsonSchema.KEY_CATEGORY);
		if (_category != null) {

			this.category = _category.toString();
		}

		final Object _defaultValue = source.get(JsonSchema.KEY_DEFAULT);
		if (_defaultValue != null) {

			this.defaultValue = _defaultValue.toString();
		}

		final Object _validators = source.get(JsonSchema.KEY_VALIDATORS);
		if (_validators != null && _validators instanceof List) {

			this.validators.addAll((List<String>)_validators);
		}

		final Object _transformators = source.get(JsonSchema.KEY_TRANSFORMATORS);
		if (_transformators != null && _transformators instanceof List) {

			this.transformers.addAll((List<String>)_transformators);
		}
	}

	void deserialize(final Map<String, SchemaNode> schemaNodes, final SchemaProperty property) {

		this.schemaProperty = property;

		setDefaultValue(property.getDefaultValue());
		setCompound(property.isCompound());
		setRequired(property.isNotNull());
		setUnique(property.isUnique());
		setIndexed(property.isIndexed());
		setReadOnly(property.isReadOnly());
		setHint(property.getHint());
		setCategory(property.getCategory());

		final String[] _validators = property.getValidators();
		if (_validators != null) {

			for (final String validator : _validators) {
				validators.add(validator);
			}
		}

		final String[] _transformators = property.getTransformators();
		if (_transformators != null) {

			for (final String transformator : _transformators) {
				transformers.add(transformator);
			}
		}
	}

	Map<String, Object> serialize() {

		final Map<String, Object> map = new TreeMap<>();

		map.put(JsonSchema.KEY_TYPE, getType());

		if (compound) {
			map.put(JsonSchema.KEY_COMPOUND, true);
		}

		if (unique) {
			map.put(JsonSchema.KEY_UNIQUE, true);
		}

		if (indexed) {
			map.put(JsonSchema.KEY_INDEXED, true);
		}

		if (readOnly) {
			map.put(JsonSchema.KEY_READ_ONLY, true);
		}

		if (format != null) {
			map.put(JsonSchema.KEY_FORMAT, format);
		}

		if (hint != null) {
			map.put(JsonSchema.KEY_HINT, hint);
		}

		if (category != null) {
			map.put(JsonSchema.KEY_CATEGORY, category);
		}

		if (defaultValue != null) {
			map.put(JsonSchema.KEY_DEFAULT, defaultValue);
		}

		if (!validators.isEmpty()) {
			map.put(JsonSchema.KEY_VALIDATORS, validators);
		}

		if (!transformers.isEmpty()) {
			map.put(JsonSchema.KEY_TRANSFORMATORS, transformers);
		}

		return map;
	}

	void initializeReferences() {
	}

	void diff(final StructrPropertyDefinition other) {
	}

	// ----- static methods -----
	static StructrPropertyDefinition deserialize(final StructrTypeDefinition parent, final String name, final Map<String, Object> source) {

		final String propertyType = (String)source.get(JsonSchema.KEY_TYPE);
		StructrPropertyDefinition newProperty = null;

		if (propertyType != null) {

			final boolean isDate   = source.containsKey(JsonSchema.KEY_FORMAT) && JsonSchema.FORMAT_DATE_TIME.equals(source.get(JsonSchema.KEY_FORMAT));
			final boolean isEnum   = source.containsKey(JsonSchema.KEY_ENUM) || source.containsKey(JsonSchema.KEY_FQCN);

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

				case "password":
					newProperty = new StructrPasswordProperty(parent, name);
					break;

				case "thumbnail":
					newProperty = new StructrThumbnailProperty(parent, name);
					break;

				case "count":
					newProperty = new StructrCountProperty(parent, name);
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

				case "custom":
					newProperty = new StructrCustomProperty(parent, name);
					break;

				case "encrypted":
					newProperty = new StructrEncryptedStringProperty(parent, name);
					break;

				case "object":

					// notion properties don't contain $link
					if (source.containsKey(JsonSchema.KEY_REFERENCE) && !source.containsKey(JsonSchema.KEY_LINK)) {

						final Object reference = source.get(JsonSchema.KEY_REFERENCE);
						if (reference != null && reference instanceof String) {

							final String refName = StructrPropertyDefinition.getPropertyNameFromJsonPointer(reference.toString());

							if (source.containsKey(JsonSchema.KEY_PROPERTIES)) {

								newProperty = new NotionReferenceProperty(parent, name, (String)source.get(JsonSchema.KEY_REFERENCE), "object", refName);

							} else {

								newProperty = new IdNotionReferenceProperty(parent, name, (String)source.get(JsonSchema.KEY_REFERENCE), "object", refName);
							}
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

									if (source.containsKey(JsonSchema.KEY_PROPERTIES)) {

										newProperty = new NotionReferenceProperty(parent, name, (String)source.get(JsonSchema.KEY_REFERENCE), "array", refName);

									} else {

										newProperty = new IdNotionReferenceProperty(parent, name, (String)source.get(JsonSchema.KEY_REFERENCE), "array", refName);
									}
								}

							} else if (items.containsKey(JsonSchema.KEY_TYPE)) {

								final Object typeValue = items.get(JsonSchema.KEY_TYPE);
								if (typeValue != null) {

									switch (typeValue.toString()) {

										case "string":
											newProperty = new StructrStringArrayProperty(parent, name);
											break;

										case "integer":
											newProperty = new StructrIntegerArrayProperty(parent, name);
											break;

										case "long":
											newProperty = new StructrLongArrayProperty(parent, name);
											break;

										case "number":
											newProperty = new StructrNumberArrayProperty(parent, name);
											break;

										case "boolean":
											newProperty = new StructrBooleanArrayProperty(parent, name);
											break;

										case "date":
											newProperty = new StructrDateArrayProperty(parent, name);
											break;

										case "Byte[]":
											newProperty = new StructrByteArrayProperty(parent, name);
											break;

									}
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

	static StructrPropertyDefinition deserialize(final Map<String, SchemaNode> schemaNodes, final StructrTypeDefinition parent, final SchemaProperty property) {

		final String parentName = parent.getName();
		final Type type         = property.getPropertyType();
		final String name       = property.getName();

		switch (type) {

			case Function:
				final StructrFunctionProperty func = new StructrFunctionProperty(parent, name);
				func.deserialize(schemaNodes, property);
				return func;

			case Cypher:
				final StructrScriptProperty cypher = new StructrScriptProperty(parent, name);
				cypher.deserialize(schemaNodes, property);
				cypher.setContentType("application/x-cypher");
				return cypher;

			case Notion:
			{
				final String referenceName         = property.getNotionBaseProperty();
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

				notionProperty.deserialize(schemaNodes, property);

				return notionProperty;
			}

			case IdNotion:
			{
				final String referenceName         = property.getNotionBaseProperty();
				final String reference             = "#/definitions/" + parentName + "/properties/" + referenceName;
				final Set<String> notionProperties = property.getPropertiesForNotionProperty();
				final IdNotionReferenceProperty notionProperty;

				final String multiplicity = property.getNotionMultiplicity();
				if (multiplicity != null) {

					if (multiplicity.startsWith("*")) {

						notionProperty = new IdNotionReferenceProperty(parent, name, reference, "array", referenceName);
						notionProperty.setProperties(notionProperties.toArray(new String[0]));

					} else {

						notionProperty = new IdNotionReferenceProperty(parent, name, reference, "object", referenceName);
						notionProperty.setProperties(notionProperties.toArray(new String[0]));
					}

					notionProperty.deserialize(schemaNodes, property);

					return notionProperty;
				}

				// notion property parsing can fail because of migration
				return new DeletedPropertyDefinition(parent, name, property);
			}

			case Password:
				final StructrPasswordProperty pwd = new StructrPasswordProperty(parent, name);
				pwd.deserialize(schemaNodes, property);
				pwd.setDefaultValue(property.getDefaultValue());
				return pwd;

			case String:
				final StructrStringProperty str = new StructrStringProperty(parent, name);
				str.deserialize(schemaNodes, property);
				str.setDefaultValue(property.getDefaultValue());
				return str;

			case StringArray:
				final StructrStringArrayProperty arr = new StructrStringArrayProperty(parent, name);
				arr.deserialize(schemaNodes, property);
				arr.setDefaultValue(property.getDefaultValue());
				return arr;

			case Boolean:
				final StructrBooleanProperty bool = new StructrBooleanProperty(parent, name);
				bool.deserialize(schemaNodes, property);
				bool.setDefaultValue(property.getDefaultValue());
				return bool;

			case BooleanArray:
				final StructrBooleanArrayProperty booleanArrayProperty = new StructrBooleanArrayProperty(parent, name);
				booleanArrayProperty.deserialize(schemaNodes, property);
				booleanArrayProperty.setDefaultValue(property.getDefaultValue());
				return booleanArrayProperty;

			case Count:
				final StructrCountProperty count = new StructrCountProperty(parent, name);
				count.deserialize(schemaNodes, property);
				return count;

			case Integer:
				final StructrIntegerProperty intProperty = new StructrIntegerProperty(parent, name);
				intProperty.deserialize(schemaNodes, property);
				intProperty.setDefaultValue(property.getDefaultValue());
				return intProperty;

			case IntegerArray:
				final StructrIntegerArrayProperty intArrayProperty = new StructrIntegerArrayProperty(parent, name);
				intArrayProperty.deserialize(schemaNodes, property);
				intArrayProperty.setDefaultValue(property.getDefaultValue());
				return intArrayProperty;

			case Long:
				final StructrLongProperty longProperty = new StructrLongProperty(parent, name);
				longProperty.deserialize(schemaNodes, property);
				longProperty.setDefaultValue(property.getDefaultValue());
				return longProperty;

			case LongArray:
				final StructrLongArrayProperty longArrayProperty = new StructrLongArrayProperty(parent, name);
				longArrayProperty.deserialize(schemaNodes, property);
				longArrayProperty.setDefaultValue(property.getDefaultValue());
				return longArrayProperty;

			case Double:
				final StructrNumberProperty doubleProperty = new StructrNumberProperty(parent, name);
				doubleProperty.deserialize(schemaNodes, property);
				doubleProperty.setDefaultValue(property.getDefaultValue());
				return doubleProperty;

			case DoubleArray:
				final StructrNumberArrayProperty doubleArrayProperty = new StructrNumberArrayProperty(parent, name);
				doubleArrayProperty.deserialize(schemaNodes, property);
				doubleArrayProperty.setDefaultValue(property.getDefaultValue());
				return doubleArrayProperty;

			case Date:
				final StructrDateProperty date = new StructrDateProperty(parent, name);
				date.deserialize(schemaNodes, property);
				date.setFormat(JsonSchema.FORMAT_DATE_TIME);
				date.setDefaultValue(property.getDefaultValue());
				return date;

			case ZonedDateTime:
				final StructrZonedDateTimeProperty zonedDateTimeProperty = new StructrZonedDateTimeProperty(parent, name);
				zonedDateTimeProperty.deserialize(schemaNodes, property);
				zonedDateTimeProperty.setFormat(JsonSchema.FORMAT_DATE_TIME);
				zonedDateTimeProperty.setDefaultValue(property.getDefaultValue());
				return zonedDateTimeProperty;

			case DateArray:
				final StructrDateArrayProperty dateArrayProperty = new StructrDateArrayProperty(parent, name);
				dateArrayProperty.deserialize(schemaNodes, property);
				dateArrayProperty.setDefaultValue(property.getDefaultValue());
				return dateArrayProperty;

			case ByteArray:
				final StructrByteArrayProperty byteArrayProperty = new StructrByteArrayProperty(parent, name);
				byteArrayProperty.deserialize(schemaNodes, property);
				byteArrayProperty.setDefaultValue(property.getDefaultValue());
				return byteArrayProperty;

			case Enum:
				final StructrEnumProperty enumProperty = new StructrEnumProperty(parent, name);
				enumProperty.deserialize(schemaNodes, property);
				enumProperty.setDefaultValue(property.getDefaultValue());
				return enumProperty;

			case EnumArray:
				final StructrEnumProperty enumArrayProperty = new StructrEnumProperty(parent, name);
				enumArrayProperty.deserialize(schemaNodes, property);
				enumArrayProperty.setDefaultValue(property.getDefaultValue());
				return enumArrayProperty;

			case Thumbnail:
				final StructrThumbnailProperty thumb = new StructrThumbnailProperty(parent, name);
				thumb.deserialize(schemaNodes, property);
				thumb.setDefaultValue(property.getDefaultValue());
				return thumb;

			case Custom:
				final StructrCustomProperty custom = new StructrCustomProperty(parent, name);
				custom.deserialize(schemaNodes, property);
				return custom;

			case Encrypted:
				final StructrEncryptedStringProperty encrypted = new StructrEncryptedStringProperty(parent, name);
				encrypted.deserialize(schemaNodes, property);
				return encrypted;
		}

		throw new IllegalStateException("Unknown type " + type);
	}

	// ----- private methods -----
	private static String getPropertyNameFromJsonPointer(final String pointer) {
		return pointer.substring(pointer.lastIndexOf("/") + 1);
	}
}
