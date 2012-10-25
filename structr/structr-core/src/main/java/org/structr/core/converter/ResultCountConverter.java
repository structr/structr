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

import java.util.Collection;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.common.property.PropertyKey;
import org.structr.core.GraphObject;

/**
 *
 * @author Axel Morgner
 */

public class ResultCountConverter extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(ResultCountConverter.class.getName());
	
	private PropertyKey propertyKey = null;
	
	public ResultCountConverter(SecurityContext securityContext, GraphObject entity, PropertyKey propertyKey) {
		
		super(securityContext, entity);
		
		this.propertyKey = propertyKey;
	}
	
	@Override
	public Object convert(Object source) {
		return source;
	}

	@Override
	public Object revert(Object source) {
		
		int count = 0;
		
		if(currentObject != null) {
			
			Object toCount = currentObject.getProperty(propertyKey);
			if(toCount != null) {

				if (toCount instanceof Collection) {

					count = ((Collection)toCount).size();

				} else if (toCount instanceof Iterable) {

					for(Object o : ((Iterable)toCount)) {
						count++;
					}

				} else {

					// a single object
					count = 1;
				}
			}
		}
		
		return count;
	}
}
