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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.schema.JsonEnumProperty;
import org.structr.api.schema.JsonProperty;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.property.EnumProperty;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.schema.SchemaHelper.Type;
import org.structr.schema.SchemaService;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public class StructrEnumProperty extends StructrStringProperty implements JsonEnumProperty {

	private static final Logger logger = LoggerFactory.getLogger(StructrEnumProperty.class);

	protected Set<String> enums = new LinkedHashSet<>();
	protected String fqcn       = null;

	public StructrEnumProperty(final StructrTypeDefinition parent, final String name) {

		super(parent, name);
	}

	@Override
	public JsonEnumProperty setEnumType(final Class type) {

		this.fqcn = type.getName().replace("$", ".");

		return this;
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

		if (fqcn != null) {

			map.put(JsonSchema.KEY_FQCN, fqcn);

		} else {

			map.put(JsonSchema.KEY_ENUM, enums);
		}

		map.remove(JsonSchema.KEY_FORMAT);

		return map;
	}

	@Override
	void deserialize(final Map<String, Object> source) {

		super.deserialize(source);

		final List<String> enumValues = getListOrNull(source.get(JsonSchema.KEY_ENUM));
		final Object typeValue        = source.get(JsonSchema.KEY_FQCN);

		if (enumValues != null && !enumValues.isEmpty()) {

			enums.addAll((List)enumValues);

		} else if (typeValue != null && typeValue instanceof String) {

			this.fqcn = typeValue.toString();

		} else {

			final String typeName = this.parent.getName();
			if (typeName != null) {

				final JsonSchema builtInSchema = SchemaService.getDynamicSchema();
				final JsonType type            = builtInSchema.getType(typeName, false);

				if (type != null) {

					final Set<JsonProperty> properties = type.getProperties();
					for (final JsonProperty prop : properties) {

						if (this.name.equals(prop.getName())) {

							if (prop instanceof StructrEnumProperty) {

								StructrEnumProperty e = (StructrEnumProperty) prop;

								this.fqcn = e.fqcn;
							}
						}
					}
				}

				if (this.fqcn == null) {

					final Traits staticType = Traits.of(typeName);
					if (staticType != null) {

						try {

							Field enumField = getFieldOrNull(staticType, getName());
							if (enumField == null) {

								enumField = getFieldOrNull(staticType, getName() + "Property");
							}

							if (enumField != null) {

								enumField.setAccessible(true);

								final EnumProperty enumProperty = (EnumProperty) enumField.get(null);
								final Class enumType = enumProperty.valueType();

								if (enumType != null) {

									// Enum types are often defined inside of classes, and getName() returns
									// the wrong string for these enums, e.g. my.pkg.MyClass$MyEnum, whereas
									// getCanonicalName() correctly returns my.pkg.MyClass.MyEnum.
									this.fqcn = enumType.getCanonicalName();
								}
							}

						} catch (Throwable ignore) {}
					}
				}
			}

			if (this.fqcn == null) {
				throw new IllegalStateException("Missing enum values for property " + name);
			}
		}
	}

	@Override
	void deserialize(final Map<String, SchemaNode> schemaNodes, final SchemaProperty schemaProperty) {

		super.deserialize(schemaNodes, schemaProperty);

		setEnums(schemaProperty.getEnumDefinitions().toArray(new String[0]));

		this.fqcn = schemaProperty.getFqcn();
	}

	@Override
	SchemaProperty createDatabaseSchema(final App app, final AbstractSchemaNode schemaNode) throws FrameworkException {

		final SchemaProperty property = super.createDatabaseSchema(app, schemaNode);
		final Traits traits           = Traits.of("SchemaProperty");
		final PropertyMap properties  = new PropertyMap();

		properties.put(traits.key("fqcn"), this.fqcn);

		property.getWrappedNode().setProperties(SecurityContext.getSuperUserInstance(), properties);

		return property;
	}

	@Override
	public String getFormat() {
		return StringUtils.join(getEnums(), ", ");
	}

	// ----- protected methods -----
	@Override
	protected Type getTypeToSerialize() {
		return Type.Enum;
	}

	// ----- private methods -----
	private List<String> getListOrNull(final Object o) {

		if (o instanceof List) {

			return (List<String>)o;
		}

		return null;
	}
}
