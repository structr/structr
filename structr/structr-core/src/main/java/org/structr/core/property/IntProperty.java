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

import org.apache.lucene.search.SortField;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

/**
* A property that stores and retrieves a simple Integer value.
 *
 * @author Christian Morgner
 */
public class IntProperty extends AbstractPrimitiveProperty<Integer> {

	public IntProperty(String name) {
		this(name, name, null);
	}
	
	public IntProperty(String name, Integer defaultValue) {
		this(name, name, defaultValue);
	}
	
	public IntProperty(String jsonName, String dbName, Integer defaultValue) {
		super(jsonName, dbName, defaultValue);
	}
	
	@Override
	public String typeName() {
		return "Integer";
	}
	
	@Override
	public PropertyConverter<Integer, Integer> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, Integer> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext, SortField.INT);
	}
	
	protected class InputConverter extends PropertyConverter<Object, Integer> {

		public InputConverter(SecurityContext securityContext) {
			this(securityContext, null, false);
		}
		
		public InputConverter(SecurityContext securityContext, Integer sortKey) {
			this(securityContext, sortKey, false);
		}
		
		public InputConverter(SecurityContext securityContext, boolean sortFinalResults) {
			this(securityContext, null, sortFinalResults);
		}
		
		public InputConverter(SecurityContext securityContext, Integer sortKey, boolean sortFinalResults) {
			super(securityContext, null, sortKey, sortFinalResults);
		}
		
		@Override
		public Object revert(Integer source) throws FrameworkException {
			return source;
		}

		@Override
		public Integer convert(Object source) {
			
			// FIXME: be more strict when dealing with "wrong" input types
			if (source != null) {
				
				if (source instanceof Number) {

					return ((Number)source).intValue();
					
				}
				
				if (source instanceof String) {
					
					return Integer.parseInt(source.toString());
				}
			}
			
			return null;
		}
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		
		if (value != null) {
			
			if (value instanceof Integer) {
				return value;
			}
			
			if (value instanceof Number) {
				return ((Number)value).intValue();
			}
			
			try {
				
				return Integer.parseInt(value.toString());
				
			} catch (Throwable t) {
				
				// no chance, give up..
			}
		}
		
		return null;
	}
}
