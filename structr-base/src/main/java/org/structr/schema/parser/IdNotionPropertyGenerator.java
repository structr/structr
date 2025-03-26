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
package org.structr.schema.parser;

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.entity.SchemaNode;
import org.structr.core.property.CollectionIdProperty;
import org.structr.core.property.EntityIdProperty;
import org.structr.core.property.Property;
import org.structr.schema.SchemaHelper.Type;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 *
 */
public class IdNotionPropertyGenerator extends PropertyGenerator {

	private final Set<String> properties = new LinkedHashSet<>();
	private boolean isPropertySet  = false;
	private boolean isAutocreate   = false;
	private String parameters      = "";
	private String propertyType    = null;
	private String baseProperty    = null;
	private String multiplicity    = null;

	public IdNotionPropertyGenerator(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);
	}

	@Override
	public String getValueType() {
		return String.class.getSimpleName();
	}

	@Override
	protected Object getDefaultValue() {
		return null;
	}

	@Override
	public Type getPropertyType() {
		return Type.IdNotion;
	}

	@Override
	public Property newInstance() throws FrameworkException {

		final String className  = source.getClassName();
		final String name       = source.getPropertyName();
		final String expression = source.getFormat();

		if (StringUtils.isBlank(expression)) {

			//reportError(new InvalidPropertySchemaToken(SchemaNode.class.getSimpleName(), expression, "invalid_property_definition", "Empty notion property expression."));
			throw new FrameworkException(422, "Empty notion property expression for property ‛" + name + "‛", new InvalidPropertySchemaToken(className, source.getPropertyName(), expression, "invalid_property_definition", "Empty notion property expression for property " + source.getPropertyName() + "."));
		}

		final String[] parts = expression.split("[, ]+");
		Property property    = null;

		if (parts.length > 0) {

			baseProperty = parts[0];

			final String relatedType = source.getRelatedType(baseProperty);
			final String baseType    = source.getClassName();

			multiplicity = source.getMultiplicity(baseProperty);

			if (multiplicity != null) {

				if (parts.length == 3 && "true".equals(parts[2].toLowerCase())) {
					isAutocreate = true;
				}

				switch (multiplicity) {

					case "1X":
					case "1":
						//public EntityIdProperty(final String name, final String baseType, final String basePropertyName, final String relatedType) {
						property = new EntityIdProperty(name, baseType, baseProperty, relatedType, isAutocreate);
						break;

					case "*X":
					case "*":
						//public CollectionIdProperty(final String name, final String baseType, final String basePropertyName, final String relatedType) {
						property = new CollectionIdProperty(name, baseType, baseProperty, relatedType, isAutocreate);
						break;

					default:
						break;
				}

			} else {

				throw new FrameworkException(422, "Invalid notion property expression for property ‛" + source.getPropertyName() + "‛", new InvalidPropertySchemaToken(SchemaNode.class.getSimpleName(), this.source.getPropertyName(), expression, "invalid_property_definition", "Invalid notion property expression."));
			}
		}

		return property;
	}

	public boolean isPropertySet() {
		return isPropertySet;
	}

	public Set<String> getProperties() {
		return properties;
	}

	public boolean isAutocreate() {
		return isAutocreate;
	}

	public String getBaseProperty() {
		return baseProperty;
	}

	public String getMultiplicity() {
		return multiplicity;
	}
}
