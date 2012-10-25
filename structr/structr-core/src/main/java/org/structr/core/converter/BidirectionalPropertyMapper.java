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
import org.structr.common.SecurityContext;
import org.structr.common.property.PropertyKey;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;

/**
 *
 * @author Christian Morgner
 */
public class BidirectionalPropertyMapper extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(BidirectionalPropertyMapper.class.getName());
	
	private PropertyKey mappedKey = null;
	
	public BidirectionalPropertyMapper(SecurityContext securityContext, GraphObject entity, PropertyKey key) {
		super(securityContext, entity);
		
		this.mappedKey = key;
	}
	
	@Override
	public Object convert(Object source) {

		try {
			currentObject.setProperty(mappedKey, source);

		} catch(FrameworkException fex) {

			logger.log(Level.WARNING, "Unable to set mapped property {0} on {1}: {2}", new Object[] { mappedKey, currentObject, fex.getMessage() } );
		}

		return null;
	}

	@Override
	public Object revert(Object source) {

		return currentObject.getProperty(mappedKey);
	}
	
}
