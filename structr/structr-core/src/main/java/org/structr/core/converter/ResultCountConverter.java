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
import org.structr.core.IterableAdapter;
import org.structr.core.Value;

/**
 *
 * @author Axel Morgner
 */

public class ResultCountConverter extends PropertyConverter {

	@Override
	public Object convertForSetter(Object source, Value value) {
		return source;
	}

	@Override
	public Object convertForGetter(Object source, Value value) {
		
		int count = 0;
		
		if(currentObject != null && value != null) {
			
			Object val = value.get(securityContext);
			
			if(val != null) {
				
				Object toCount = currentObject.getProperty(val.toString());
				if(toCount != null) {

					if (toCount instanceof Collection) {

						count = ((Collection)toCount).size();

					} else if (toCount instanceof IterableAdapter && ((IterableAdapter)toCount).size() >= 0) {

						return ((IterableAdapter)toCount).size();
						
						
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
		}
		
		return count;
	}
}
