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

import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.ByteArrayProperty;
import org.structr.core.property.Property;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 *
 */
public class ByteArrayPropertyGenerator extends PropertyGenerator<Byte[]> {

	public ByteArrayPropertyGenerator(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);
	}

	@Override
	public String getValueType() {
		return Byte[].class.getSimpleName();
	}

	@Override
	protected Property newInstance() throws FrameworkException {
		return new ByteArrayProperty(source.getPropertyName());
	}

	@Override
	public Type getPropertyType() {
		return Type.ByteArray;
	}

	@Override
	public Byte[] getDefaultValue() {
		return null;
	}

}
