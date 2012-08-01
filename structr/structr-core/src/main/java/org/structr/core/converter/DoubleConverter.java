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
import org.apache.commons.lang.math.NumberUtils;
import org.structr.core.Value;

/**
 *
 * @author Axel Morgner
 */
public class DoubleConverter extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(DoubleConverter.class.getName());

	@Override
	public Object convertForSetter(Object source, Value value) {

		if(source != null) {
			
			try {

				if(source instanceof Double) {
					return ((Double)source);
				} else if(source instanceof String) {
					return NumberUtils.createDouble((String) source);
				}

			} catch (Throwable t) {

				logger.log(Level.WARNING, "Exception while parsing double", t);
				return null;
			}
		}

		return source;

	}

	@Override
	public Object convertForGetter(Object source, Value value) {

//		if(source != null) {
//			return source.toString();
//		}

		return source;
	}
}
