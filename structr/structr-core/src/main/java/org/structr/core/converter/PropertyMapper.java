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

package org.structr.core.converter;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.PropertyKey;
import org.structr.core.Value;

/**
 *
 * @author Christian Morgner
 */
public class PropertyMapper extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(PropertyMapper.class.getName());

	@Override
	public Object convertForSetter(Object source, Value value) {
		return source;
	}

	@Override
	public Object convertForGetter(Object source, Value value) {

		if(value != null) {

			Object valueObject = value.get(securityContext);
			if(valueObject instanceof PropertyKey) {

				PropertyKey mappedKey = (PropertyKey)valueObject;
				return currentObject.getProperty(mappedKey);

			} else {

				logger.log(Level.WARNING, "Value parameter is not a String!");
			}

		} else {

			logger.log(Level.WARNING, "Required value parameter is missing!");
		}

		return source;
	}
}
