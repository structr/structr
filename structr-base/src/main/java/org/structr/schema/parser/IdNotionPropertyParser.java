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
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.entity.SchemaNode;
import org.structr.core.property.CollectionIdProperty;
import org.structr.core.property.EntityIdProperty;
import org.structr.schema.Schema;
import org.structr.schema.SchemaHelper.Type;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public class IdNotionPropertyParser extends PropertySourceGenerator {

	private final Set<String> properties = new LinkedHashSet<>();
	private boolean isPropertySet  = false;
	private boolean isAutocreate   = false;
	private String parameters      = "";
	private String propertyType    = null;
	private String baseProperty    = null;
	private String multiplicity    = null;

	public IdNotionPropertyParser(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);
	}

	@Override
	public String getPropertyType() {
		return propertyType;
	}

	@Override
	public String getValueType() {
		return String.class.getSimpleName();
	}

	@Override
	public String getUnqualifiedValueType() {
		return String.class.getSimpleName();
	}

	@Override
	public String getPropertyParameters() {
		return parameters;
	}

	@Override
	public Type getKey() {
		return Type.IdNotion;
	}

	@Override
	public void parseFormatString(final Map<String, SchemaNode> schemaNodes, final Schema entity, String expression) throws FrameworkException {

		if (StringUtils.isBlank(expression)) {

			//reportError(new InvalidPropertySchemaToken(SchemaNode.class.getSimpleName(), expression, "invalid_property_definition", "Empty notion property expression."));
			throw new FrameworkException(422, "Empty notion property expression for property ‛" + source.getPropertyName() +  "‛", new InvalidPropertySchemaToken(SchemaNode.class.getSimpleName(), this.source.getPropertyName(), expression, "invalid_property_definition", "Empty notion property expression."));
		}

		final StringBuilder buf = new StringBuilder();
		final String[] parts    = expression.split("[, ]+");

		if (parts.length > 0) {

			boolean isBuiltinProperty = false;
			baseProperty              = parts[0];
			multiplicity              = entity.getMultiplicity(schemaNodes, baseProperty);

			if (multiplicity != null) {

				switch (multiplicity) {

					case "1X":
						// this line exists because when a NotionProperty is set up for a builtin propery
						// (like for example "owner", there must not be the string "Property" appended
						// to the property name, and the SchemaNode returns the above "extended" multiplicity
						// string when it has detected a fallback property name like "owner" from NodeInterface.
						isBuiltinProperty = true; // no break!
					case "1":
						propertyType = EntityIdProperty.class.getSimpleName();
						break;

					case "*X":
						// this line exists because when a NotionProperty is set up for a builtin propery
						// (like for example "owner", there must not be the string "Property" appended
						// to the property name, and the SchemaNode returns the above "extended" multiplicity
						// string when it has detected a fallback property name like "owner" from NodeInterface.
						isBuiltinProperty = true; // no break!
					case "*":
						propertyType = CollectionIdProperty.class.getSimpleName();
						break;

					default:
						break;
				}

				buf.append(", ");
				buf.append(entity.getClassName());
				buf.append(".");
				buf.append(baseProperty);

				// append "Property" only if it is NOT a builtin property!
				if (!isBuiltinProperty) {
					buf.append("Property");
				}

				if (parts.length == 3 && "true".equals(parts[2].toLowerCase())) {

					isAutocreate = true;

					buf.append(", true");
				}

			} else {

				throw new FrameworkException(422, "Invalid notion property expression for property ‛" + source.getPropertyName() + "‛", new InvalidPropertySchemaToken(SchemaNode.class.getSimpleName(), this.source.getPropertyName(), expression, "invalid_property_definition", "Invalid notion property expression."));
			}
		}


		parameters = buf.toString();
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
