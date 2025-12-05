/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.core.property.CypherQueryProperty;
import org.structr.core.property.Property;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 *
 */
public class CypherPropertyGenerator extends PropertyGenerator {

	public CypherPropertyGenerator(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition params) {
		super(errorBuffer, className, params);
	}

	@Override
	public String getValueType() {
		return "Iterable<GraphObject>";
	}

	@Override
	protected Object getDefaultValue() {
		return null;
	}

	@Override
	protected Property newInstance() throws FrameworkException {
		return new CypherQueryProperty(source.getPropertyName(), source.getFormat());
	}

	@Override
	public Type getPropertyType() {
		return Type.Cypher;
	}
}
