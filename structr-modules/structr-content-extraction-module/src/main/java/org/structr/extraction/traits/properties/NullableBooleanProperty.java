/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.extraction.traits.properties;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.property.BooleanProperty;

/**
 * A boolean with three states: true, false, and unset (null), where unset means "inherit".
 *
 * BooleanProperty cannot express that: it reads an unset value as false, and therefore also treats
 * setting false on an unset node as no change and stores nothing, and it stores null as false. This
 * property keeps null as null in both directions.
 */
public class NullableBooleanProperty extends BooleanProperty {

	public NullableBooleanProperty(final String name) {

		super(name);
	}

	@Override
	public PropertyConverter<Boolean, ?> databaseConverter(final SecurityContext securityContext) {

		return databaseConverter(securityContext, null);
	}

	@Override
	public PropertyConverter<Boolean, ?> databaseConverter(final SecurityContext securityContext, final GraphObject entity) {

		return new PropertyConverter<Boolean, Object>(securityContext) {

			@Override
			public Boolean revert(final Object source) {

				if (source instanceof Boolean bool) {

					return bool;
				}

				return source != null ? Boolean.valueOf(source.toString()) : null;
			}

			@Override
			public Object convert(final Boolean source) {

				return source;
			}
		};
	}
}
