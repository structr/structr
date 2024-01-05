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
import org.structr.common.error.InvalidPropertySchemaToken;
import org.structr.core.entity.SchemaNode;
import org.structr.core.property.IntProperty;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 *
 */
public class IntPropertyParser extends NumericalPropertyParser {

	public IntPropertyParser(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);
	}

	@Override
	public String getPropertyType() {
		return IntProperty.class.getSimpleName();
	}

	@Override
	public String getValueType() {
		return Integer.class.getName();
	}

	@Override
	public String getUnqualifiedValueType() {
		return "Integer";
	}

	@Override
	public Type getKey() {
		return Type.Integer;
	}

	@Override
	public Number parseNumber(final ErrorBuffer errorBuffer, final String source, final String which) {

		try {

			return Integer.parseInt(source);

		} catch (Throwable t) {

			errorBuffer.add(new InvalidPropertySchemaToken(SchemaNode.class.getSimpleName(), this.source.getPropertyName(), source, "invalid_" + which +"_bound", StringUtils.capitalize(which) + " bound must be of type Integer."));
		}

		return null;
	}
}
