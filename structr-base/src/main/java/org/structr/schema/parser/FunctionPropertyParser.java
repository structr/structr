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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.property.FunctionProperty;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 *
 */
public class FunctionPropertyParser extends PropertySourceGenerator {

	private static final Logger logger = LoggerFactory.getLogger(FunctionPropertyParser.class.getName());

	public FunctionPropertyParser(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);
	}

	@Override
	public String getPropertyType() {
		return FunctionProperty.class.getSimpleName();
	}

	@Override
	public String getValueType() {
		return Object.class.getName();
	}

	@Override
	public String getUnqualifiedValueType() {
		return "Object";
	}

	@Override
	public String getPropertyParameters() {
		return "";
	}

	@Override
	public Type getKey() {
		return Type.Function;
	}

	@Override
	public void parseFormatString(final AbstractSchemaNode entity, String expression) throws FrameworkException {
	}
}
