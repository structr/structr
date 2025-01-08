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
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.entity.SchemaNode;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.schema.SchemaHelper.Type;

import java.util.List;

/**
 *
 *
 */
public class StringPropertySourceGenerator extends PropertyGenerator<String> {

	public StringPropertySourceGenerator(final ErrorBuffer errorBuffer, final String entity, final PropertyDefinition params) {
		super(errorBuffer, entity, params);
	}

	@Override
	public String getValueType() {
		return String.class.getName();
	}

	@Override
	public Type getKey() {
		return Type.String;
	}

	@Override
	protected Property newInstance() throws FrameworkException {

		final String name       = source.getPropertyName();
		final String expression = source.getFormat();

		if ("[]".equals(expression)) {
			reportError(new InvalidPropertySchemaToken(SchemaNode.class.getSimpleName(), name, expression, "invalid_validation_expression", "Empty validation expression."));
			return null;
		}

		final StringProperty propertyKey = new StringProperty(name);

		if (StringUtils.isNotBlank(source.getContentType())) {
			propertyKey.contentType(source.getContentType());
		}

		return propertyKey;
	}

	@Override
	public List<IsValid> getValidators(final PropertyKey<String> key) throws FrameworkException {

		final List<IsValid> validators = super.getValidators(key);
		final String expression        = source.getFormat();

		if (StringUtils.isNotBlank(expression) && !("multi-line".equals(expression))) {

			validators.add((obj, errorBuffer) -> ValidationHelper.isValidStringMatchingRegex(obj.getProperty(key), expression));
			//addGlobalValidator(new Validator("isValidStringMatchingRegex", source.getClassName(), source.getPropertyName(), expression));
		}


		return validators;
	}

	@Override
	public String getDefaultValue() {
		return source.getDefaultValue();
	}
}
