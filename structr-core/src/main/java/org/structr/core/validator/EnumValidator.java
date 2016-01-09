/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.validator;

import java.util.LinkedHashSet;
import java.util.Set;
import org.structr.common.SecurityContext;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.ValueToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.property.PropertyKey;

//~--- classes ----------------------------------------------------------------

/**
 * A validator that ensures a given String value is a valid enum element of the
 * given type.
 *
 *
 */
public class EnumValidator<T> implements PropertyValidator<T> {

	private final Set<T> values = new LinkedHashSet<>();

	public EnumValidator(T[] values) {
		for (final T t : values) {
			this.values.add(t);
		}
	}

	@Override
	public boolean isValid(SecurityContext securityContext, GraphObject object, PropertyKey<T> key, T value, ErrorBuffer errorBuffer) {

		if (value == null) {

			errorBuffer.add(new EmptyPropertyToken(object.getType(), key));

		} else {

			if (values.contains(value)) {

				return true;

			} else {

				errorBuffer.add(new ValueToken(object.getType(), key, values.toArray()));
			}
		}

		return false;
	}

	@Override
	public boolean requiresSynchronization() {
		return false;
	}
}
