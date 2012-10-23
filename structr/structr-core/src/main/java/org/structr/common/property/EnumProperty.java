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

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;

/**
 *
 * @author Christian Morgner
 */
public class EnumProperty<T extends Enum> extends Property<T> {
	
	private Class<T> enumType = null;
	
	public EnumProperty(String name, Class<T> enumType) {
		this(name, enumType, null);
	}
	
	public EnumProperty(String name, Class<T> enumType, T defaultValue) {
		
		super(name, defaultValue);
		
		this.enumType = enumType;
	}
	
	@Override
	public T fromPrimitive(GraphObject entity, Object o) throws FrameworkException {
		
		if (o != null) {
			
			return (T)Enum.valueOf(enumType, o.toString());
		}
		
		return null;
	}
}
