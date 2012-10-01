/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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

import java.util.Date;
import java.util.logging.Logger;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FutureDateToken;
import org.structr.common.error.TypeToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;

/**
 * A validator that ensures a given date lies in the future.
 *
 * @author Christian Morgner
 */
public class FutureDateValidator extends PropertyValidator {

	private static final Logger logger = Logger.getLogger(FutureDateValidator.class.getName());

	@Override
	public boolean isValid(GraphObject object, String key, Object value, ErrorBuffer errorBuffer) {

		if(value != null) {

			if(value instanceof Long) {
				
				if(((Long)value).longValue() < new Date().getTime()) {

					errorBuffer.add(object.getType(), new FutureDateToken(key));
					return false;
				}

				return true;

			} else {

				errorBuffer.add(object.getType(), new TypeToken(key, "long"));
			}

		} else {

			errorBuffer.add(object.getType(), new EmptyPropertyToken(key));
		}

		return false;
	}
}
