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
import org.structr.common.error.FrameworkException;
import org.structr.common.error.ValueToken;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

/**
* A property that stores and retrieves a simple enum value of the given type.
 *
 * @author Christian Morgner
 */
public class EnumProperty<T extends Enum> extends AbstractPrimitiveProperty<T> {
	
	private Class<T> enumType = null;
	
	public EnumProperty(String name, Class<T> enumType) {
		this(name, enumType, null);
	}
	
	public EnumProperty(String name, Class<T> enumType, T defaultValue) {
		this(name, name, enumType, defaultValue);
	}
	
	public EnumProperty(String jsonName, String dbName, Class<T> enumType, T defaultValue) {
		
		super(jsonName, dbName, defaultValue);
		
		this.enumType = enumType;
	}
	
	@Override
	public String typeName() {
		return "String";
	}
	
	@Override
	public PropertyConverter<T, String> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return new DatabaseConverter(securityContext, entity);
	}

	@Override
	public PropertyConverter<String, T> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
	}
	
	@Override
	public Object fixDatabaseProperty(Object value) {
		
		if (value != null) {
			
			if (value instanceof String) {
				return value;
			}
		}
		
		return null;
	}

	protected class DatabaseConverter extends PropertyConverter<T, String> {

		public DatabaseConverter(SecurityContext securityContext, GraphObject entity) {
			super(securityContext, entity);
		}

		@Override
		public T revert(String source) throws FrameworkException {

			if (source != null) {

				return (T)Enum.valueOf(enumType, source.toString());
			}

			return null;
			
		}
		
		@Override
		public String convert(T source) throws FrameworkException {

			if (source != null) {
				
				return source.toString();
			}
			
			return null;
		}
		
	}
	
	protected class InputConverter extends PropertyConverter<String, T> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}
		
		@Override
		public String revert(T source) throws FrameworkException {

			if (source != null) {
				
				return source.toString();
			}
			
			return null;
		}

		@Override
		public T convert(String source) throws FrameworkException {

			if (source != null) {

				try {
					return (T)Enum.valueOf(enumType, source.toString());
					
				} catch(Throwable t) {
					
					throw new FrameworkException(declaringClass.getSimpleName(), new ValueToken(EnumProperty.this, enumType.getEnumConstants()));
				}
			}

			return null;
			
		}
		
	}
}
