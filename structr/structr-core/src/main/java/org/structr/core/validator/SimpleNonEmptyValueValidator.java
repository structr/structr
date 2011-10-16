/*
 *  Copyright (C) 2011 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.core.validator;

import org.structr.core.PropertyValidator;
import org.structr.core.Value;

/**
 * A simple validator that checks for non-empty values.
 *
 * @author Christian Morgner
 */
public class SimpleNonEmptyValueValidator implements PropertyValidator {

	@Override
	public boolean isValid(String key, Object value, Value parameter, StringBuilder errorMessage) {

		if(value != null && value.toString().length() > 0) {
			return true;
		}

		errorMessage.append("Property '");
		errorMessage.append(key);
		errorMessage.append("' must not be empty.");

		return false;
	}
}
