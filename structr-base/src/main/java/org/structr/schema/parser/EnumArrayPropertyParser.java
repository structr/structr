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
import org.structr.core.property.EnumArrayProperty;
import org.structr.schema.Schema;
import org.structr.schema.SchemaHelper.Type;

import java.util.Map;

/**
 *
 *
 */
public class EnumArrayPropertyParser extends PropertySourceGenerator {

    private String enumTypeName = "";
    private String enumType     = "";

    public EnumArrayPropertyParser(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
        super(errorBuffer, className, params);
    }

    @Override
    public String getPropertyType() {
        return EnumArrayProperty.class.getSimpleName();
    }

    @Override
    public String getValueType() {
        return enumTypeName;
    }

    @Override
    public String getUnqualifiedValueType() {
        return enumTypeName;
    }

    @Override
    public String getPropertyParameters() {
        return enumType;
    }

    @Override
    public Type getKey() {
        return Type.EnumArray;
    }

    @Override
    public void parseFormatString(final Map<String, SchemaNode> schemaNodes, final Schema entity, String expression) throws FrameworkException {

        if (StringUtils.isNotBlank(expression)) {

            final String[] enumTypes = expression.split("[, ]+");

            enumTypeName = StringUtils.capitalize(getSourcePropertyName());

            // create enum type
            enumType = ", " + enumTypeName + ".class";

            // build enum type definition

            final StringBuilder buf = new StringBuilder();

            buf.append("\n\tpublic enum ").append(enumTypeName).append(" {\n\t\t");
            for (int i=0; i<enumTypes.length; i++) {

                final String element = enumTypes[i];

                if (element.matches("[a-zA-Z_]{1}[a-zA-Z0-9_]*")) {

                    buf.append(element);

                    // comma separation
                    if (i < enumTypes.length-1) {
                        buf.append(", ");
                    }

                } else {

                    reportError(new InvalidPropertySchemaToken(SchemaNode.class.getSimpleName(), this.source.getPropertyName(), expression, "invalid_property_definition", "Invalid enum type name, must match [a-zA-Z_]{1}[a-zA-Z0-9_]*."));

                }
            }
            buf.append("\n\t};");

            addEnumDefinition(buf.toString());

        } else if (source.getFqcn() != null) {

            // no enum type definition, use external type
            enumTypeName = source.getFqcn();

            // create enum type
            enumType = ", " + enumTypeName + ".class";

        } else {

            reportError(new InvalidPropertySchemaToken(SchemaNode.class.getSimpleName(), this.source.getPropertyName(), expression, "invalid_property_definition", "No enum types found, please specify a list of types, e.g. (red, green, blue)"));
        }
    }

    @Override
    public String getDefaultValue() {
        return enumTypeName.concat(".").concat(getSourceDefaultValue());
    }
}
