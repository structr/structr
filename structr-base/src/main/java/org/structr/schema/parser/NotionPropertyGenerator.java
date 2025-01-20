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

import org.apache.commons.lang.StringUtils;
import org.structr.api.util.Iterables;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.entity.SchemaNode;
import org.structr.core.notion.Notion;
import org.structr.core.notion.PropertyNotion;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.CollectionNotionProperty;
import org.structr.core.property.EntityNotionProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.schema.SchemaHelper.Type;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 *
 */
public class NotionPropertyGenerator extends PropertyGenerator {

	private final Set<PropertyKey> properties = new LinkedHashSet<>();
	private boolean isPropertySet             = false;
	private boolean isAutocreate              = false;
	private String relatedType                = null;
	private String baseProperty               = null;
	private String multiplicity               = null;

	public NotionPropertyGenerator(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);
	}

	@Override
	public String getValueType() {
		return relatedType;
	}

	@Override
	protected Object getDefaultValue() {
		return null;
	}

	@Override
	public Type getPropertyType() {
		return Type.Notion;
	}

	@Override
	protected Property newInstance() throws FrameworkException {

		final String className  = source.getClassName();
		final String name       = source.getPropertyName();
		final String expression = source.getFormat();

		if (StringUtils.isBlank(expression)) {

			//reportError(new InvalidPropertySchemaToken(SchemaNode.class.getSimpleName(), expression, "invalid_property_definition", "Empty notion property expression."));
			throw new FrameworkException(422, "Empty notion property expression for property ‛" + name + "‛", new InvalidPropertySchemaToken(className, source.getPropertyName(), expression, "invalid_property_definition", "Empty notion property expression for property " + source.getPropertyName() + "."));
		}

		final String[] parts = expression.split("[, ]+");
		Property property    = null;

		if (parts.length > 0 && schemaNode instanceof SchemaNode entity) {

			baseProperty = parts[0];
			multiplicity = entity.getMultiplicity(baseProperty);

			if (multiplicity != null) {

				// determine related type from relationship
				relatedType  = entity.getRelatedType(baseProperty);

				final PropertyKey base  = Traits.of(entity.getClassName()).key(baseProperty);
				final boolean isBoolean = (parts.length == 3 && ("true".equals(parts[2].toLowerCase()) || "false".equals(parts[2].toLowerCase())));
				Notion notion           = null;
				isAutocreate            = isBoolean;

				// use PropertyNotion when only a single element is given
				isPropertySet = (parts.length == 2 || isBoolean);

				for (int i=1; i<parts.length; i++) {

					String propertyName     = parts[i];
					String fullPropertyName = propertyName;
					final boolean isFlag    = "true".equalsIgnoreCase(propertyName) || "false".equalsIgnoreCase(propertyName);

					if (!isFlag && !propertyName.contains(".")) {

						properties.add(Traits.of(relatedType).key(fullPropertyName));
					}

					/*
					fullPropertyName = extendPropertyName(fullPropertyName, isFlag);

					properties.add(fullPropertyName);

					propertyName = extendPropertyName(propertyName, isFlag);

					buf.append(propertyName);

					if (i < parts.length-1) {
						buf.append(", ");
					}
					*/
				}

				if (isPropertySet) {

					notion = new PropertySetNotion(isAutocreate, properties);

				} else {

					final PropertyKey p = Iterables.first(properties);

					notion = new PropertyNotion(p, isAutocreate);
				}

				switch (multiplicity) {

					case "1X":
					case "1":
						property = new EntityNotionProperty(name, (Property)base, notion);
						break;

					case "*X":
					case "*":
						property = new CollectionNotionProperty(name, (Property)base, notion);
						break;

					default:
						break;
				}

			} else {

				throw new FrameworkException(422, "Invalid notion property expression for property ‛" + source.getPropertyName() +  "‛", new InvalidPropertySchemaToken(entity.getClassName(), source.getPropertyName(), expression, "invalid_property_definition", "Invalid notion property expression for property " + source.getPropertyName() + "."));
			}

			if (properties.isEmpty()) {
				throw new FrameworkException(422, "Invalid notion property expression for property ‛" + source.getPropertyName() +  "‛", new InvalidPropertySchemaToken(entity.getClassName(), source.getPropertyName(), expression, "invalid_property_definition", "Invalid notion property expression for property " + source.getPropertyName() + ", notion must define at least one property."));
			}
		}

		// FIXME: this is really ugly, can't we find a better way to create the notion?

		return property;
	}

	private String extendPropertyName(final String propertyName, final Boolean isBoolean) throws FrameworkException {

		String extendedPropertyName = propertyName;

		// remove exactly one leading underscore if property name starts with one
		if (StringUtils.contains(extendedPropertyName, ".")) {

			String[] parts = StringUtils.split(extendedPropertyName, ".");

			if (StringUtils.startsWith(parts[1], "_")) {

				extendedPropertyName = parts[0] + "." + parts[1].substring(1);

			}

		} else {

			if (StringUtils.startsWith(extendedPropertyName, "_")) {

				extendedPropertyName = extendedPropertyName.substring(1);
			}

		}

		final String tmpPropertyName  = StringUtils.contains(extendedPropertyName, ".") ? StringUtils.substringAfterLast(extendedPropertyName, ".") : extendedPropertyName;
		final PropertyKey propertyKey = Traits.of("NodeInterface").key(tmpPropertyName);

		if (propertyKey != null) {
			return extendedPropertyName;
		}

		return (isBoolean || StringUtils.endsWith(extendedPropertyName, "Property")) ? extendedPropertyName : extendedPropertyName + "Property";
	}

	public boolean isPropertySet() {
		return isPropertySet;
	}

	public Set<PropertyKey> getProperties() {
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
