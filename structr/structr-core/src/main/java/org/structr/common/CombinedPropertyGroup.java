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

package org.structr.common;

import org.structr.core.GraphObject;
import org.structr.core.PropertyGroup;

/**
 * A property group that combines two or more property values
 * into a single string value separated by a given string.
 *
 * @author Christian Morgner
 */
public class CombinedPropertyGroup implements PropertyGroup {

	private PropertyKey[] propertyKeys = null;
	private String separator = null;

	public CombinedPropertyGroup(String separator, PropertyKey... propertyKeys) {
		this.propertyKeys = propertyKeys;
		this.separator = separator;
	}

	@Override
	public Object getGroupedProperties(GraphObject source) {

		StringBuilder combinedPropertyValue = new StringBuilder();
		int len = propertyKeys.length;

		for(int i=0; i<len; i++) {
			combinedPropertyValue.append(source.getProperty(propertyKeys[i]));
			if(i < len-1) {
				combinedPropertyValue.append(separator);
			}
		}

		return combinedPropertyValue.toString();
	}

	@Override
	public void setGroupedProperties(Object source, GraphObject destination) {
	}
}
