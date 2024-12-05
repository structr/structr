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
package org.structr.common;

import org.structr.common.error.FrameworkException;
import org.structr.common.error.ReadOnlyPropertyToken;
import org.structr.core.GraphObject;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.definitions.TraitDefinition;
import org.structr.schema.Transformer;

/**
 */
public class ConstantBooleanTrue implements Transformer<Boolean> {

	@Override
	public Boolean getProperty(final GraphObject entity, final PropertyKey<Boolean> key, final Boolean value) {
		return true;
	}

	@Override
	public Boolean setProperty(final GraphObject entity, final PropertyKey<Boolean> key, final Boolean value) throws FrameworkException {

		final TraitDefinition declaringClass = key.getDeclaringTrait();
		String typeName                      = GraphObject.class.getSimpleName();

		if (declaringClass != null) {

			typeName = declaringClass.getName();
		}

		throw new FrameworkException(422, typeName + "." + key.jsonName() + " is_read_only_property", new ReadOnlyPropertyToken(typeName, key.jsonName()));
	}
}
