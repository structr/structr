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
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.schema.SchemaHelper.Type;

import java.util.List;

/**
 *
 *
 */
public class LongPropertyGenerator extends NumericalPropertyGenerator<Long> {

	public LongPropertyGenerator(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);
	}

	@Override
	public String getValueType() {
		return Long.class.getName();
	}

	@Override
	public Type getPropertyType() {
		return Type.Long;
	}

	@Override
	public Number parseNumber(final ErrorBuffer errorBuffer, final String propertyName, final String source, final String which) {

		try {
			return Long.parseLong(source);

		} catch (Throwable t) {

			errorBuffer.add(new InvalidPropertySchemaToken(StructrTraits.SCHEMA_NODE, propertyName, source, "invalid_" + which +"_bound", StringUtils.capitalize(which) + " bound must be of type Long."));
		}

		return null;
	}

	@Override
	protected Property<Long> newInstance() throws FrameworkException {
		return new LongProperty(source.getPropertyName());
	}

	@Override
	public List<IsValid> getValidators(final String key) throws FrameworkException {

		final List<IsValid> validators = super.getValidators(key);
		final String format            = source.getFormat();

		if (StringUtils.isNotBlank(format) && !error) {

			validators.add((obj, errorBuffer) -> ValidationHelper.isValidLongInRange(obj, obj.getTraits().key(key), format, errorBuffer));
		}

		return validators;
	}

	@Override
	public Long getDefaultValue() {

		final String def = source.getDefaultValue();
		if (def != null) {

			return Long.valueOf(def);
		}

		return null;
	}
}
