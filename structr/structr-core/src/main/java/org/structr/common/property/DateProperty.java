/*
 *  Copyright (C) 2012 Axel Morgner
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
package org.structr.common.property;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.structr.common.error.DateFormatToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;

/**
 *
 * @author Christian Morgner
 */
public class DateProperty extends Property<Date> {
	
	private SimpleDateFormat dateFormat = null;
	
	public DateProperty(String name, String pattern) {
		super(name);
		
		dateFormat = new SimpleDateFormat(pattern);
	}
	
	@Override
	public Date fromPrimitive(GraphObject entity, Object o) throws FrameworkException {
		
		if (o != null) {
			
			if (o instanceof Long) {
				
				// FIXME: should we really allow longs here?
				Date date = new Date();
				date.setTime((Long)o);
				
				return date;
				
			} else {
				
				try {
					return dateFormat.parse(o.toString());
					
				} catch(Throwable t) {
					
					throw new FrameworkException(entity.getType(), new DateFormatToken(this));
				}
			}
			
		}
		
		return null;
	}
	
}
