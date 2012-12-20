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

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;

/**
 * A read-only property that returns the concatenated values of two other properties.
 *
 * @author Christian Morgner
 */
public class ConcatProperty extends AbstractReadOnlyProperty<String> {

	private PropertyKey<String>[] propertyKeys = null;
	private String separator = null;

	public ConcatProperty(String name, String separator, PropertyKey<String>... propertyKeys) {
		
		super(name);
		
		this.propertyKeys = propertyKeys;
		this.separator = separator;
	}
	
	@Override
	public String getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		
		StringBuilder combinedPropertyValue = new StringBuilder();
		int len = propertyKeys.length;

		for(int i=0; i<len; i++) {
			
			combinedPropertyValue.append(obj.getProperty(propertyKeys[i]));
			if(i < len-1) {
				combinedPropertyValue.append(separator);
			}
		}

		return combinedPropertyValue.toString();
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
