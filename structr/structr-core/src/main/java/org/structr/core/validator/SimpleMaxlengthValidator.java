/*
 *  Copyright (C) 2010-2012 Axel Morgner
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

import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.TooLongToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;

/**
 * A simple string max length validator.
 *
 * @author Christian Morgner
 */
public class SimpleMaxlengthValidator extends PropertyValidator<String> {

	private int maxLength = 0;

	public SimpleMaxlengthValidator(int maxLength) {
		this.maxLength = maxLength;
	}

	@Override
	public boolean isValid(GraphObject object, String key, String value, ErrorBuffer errorBuffer) {
		
		if(value.length() <= maxLength) {
			return true;
		}

		errorBuffer.add(object.getType(), new TooLongToken(key, maxLength));

		return false;
	}
}
