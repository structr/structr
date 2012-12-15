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
package org.structr.core.property;

import java.util.Collection;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;

/**
 * A read-only property that returns the number of elements in a collection returned from a given property.
 *
 * @author Christian Morgner
 */
public class ElementCounter extends AbstractReadOnlyProperty<Integer> {
	
	private Property<? extends Iterable> collectionProperty = null;
	
	public ElementCounter(String name, Property<? extends Iterable> collectionProperty) {
		super(name);
		
		this.collectionProperty = collectionProperty;
	}
	
	@Override
	public Integer getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		
		int count = 0;
		
		if(obj != null) {
			
			Object toCount = obj.getProperty(collectionProperty);
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

	@Override
	public Class relatedType() {
		return null;
	}

	@Override
	public boolean isCollection() {
		return false;
	}
}
