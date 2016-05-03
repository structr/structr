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

import java.util.Date;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.property.PropertyKey;
import org.structr.common.error.EmptyPropertyToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FutureDateToken;
import org.structr.common.error.TypeToken;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;

/**
 * A validator that ensures a given date lies in the future.
 *
 *
 */
public class FutureDateValidator implements PropertyValidator {

	private static final Logger logger = Logger.getLogger(FutureDateValidator.class.getName());

	@Override
	public boolean isValid(SecurityContext securityContext, GraphObject object, PropertyKey key, Object value, ErrorBuffer errorBuffer) {

		// FIXME: value should be of type Date, and the generic type of this class as well!


		if(value != null) {

			if(value instanceof Long) {

				if(((Long)value).longValue() < new Date().getTime()) {

					errorBuffer.add(new FutureDateToken(object.getType(), key));
					return false;
				}

				return true;

			} else {

				errorBuffer.add(new TypeToken(object.getType(), key, "long"));
			}

		} else {

			errorBuffer.add(new EmptyPropertyToken(object.getType(), key));
		}

		return false;
	}

	@Override
	public boolean requiresSynchronization() {
		return false;
	}
}
