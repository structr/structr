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

import org.structr.common.SecurityContext;
import org.structr.core.property.PropertyKey;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.ValueToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;

/**
 * A validator that ensures valid boolean values for a given property.
 *
 *
 */
public class BooleanValidator implements PropertyValidator {

	@Override
	public boolean isValid(SecurityContext securityContext, GraphObject object, PropertyKey key, Object value, ErrorBuffer errorBuffer) {

		String stringValue = value != null ? value.toString() : "";

		boolean valid = ("true".equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue));

		if(!valid) {
			errorBuffer.add(new ValueToken(object.getType(), key, new Object[] { true, false } ));
		}

		return valid;
	}

	@Override
	public boolean requiresSynchronization() {
		return false;
	}
}
