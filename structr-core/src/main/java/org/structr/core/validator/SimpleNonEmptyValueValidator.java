/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.validator;

import org.structr.core.property.PropertyKey;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;

/**
 * A simple validator that checks for non-empty values.
 *
 * @author Christian Morgner
 */
public class SimpleNonEmptyValueValidator extends PropertyValidator<String> {

	@Override
	public boolean isValid(GraphObject object, PropertyKey<String> key, String value, ErrorBuffer errorBuffer) {

		if(value != null && value.length() > 0) {
			return true;
		}

		errorBuffer.add(object.getType(), new EmptyPropertyToken(key));
		return false;
	}
}
