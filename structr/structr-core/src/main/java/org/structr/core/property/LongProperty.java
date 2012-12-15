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
 * A property that stores and retrieves a simple Long value.
 * 
 * @author Christian Morgner
 */
public class LongProperty extends AbstractPrimitiveProperty<Long> {
	
	public LongProperty(String name) {
		super(name);
	}
	
	@Override
	public String typeName() {
		return "Long";
	}
	
	@Override
	public PropertyConverter<Long, Long> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, Long> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext, SortField.LONG);
	}
	
	protected class InputConverter extends PropertyConverter<Object, Long> {

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
		public Object revert(Long source) throws FrameworkException {
			return source;
		}

		@Override
		public Long convert(Object source) {
			
			// FIXME: be more strict when dealing with "wrong" input types
			if (source != null) {
				
				if (source instanceof Number) {

					return ((Number)source).longValue();
					
				}
				
				if (source instanceof String) {
					
					return Long.parseLong(source.toString());
				}
			}
			
			return null;
		}
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		
		if (value != null) {
			
			if (value instanceof Long) {
				return value;
			}
			
			if (value instanceof Number) {
				return ((Number)value).longValue();
			}
			
			try {
				
				return Long.parseLong(value.toString());
				
			} catch (Throwable t) {
				
				// no chance, give up..
			}
		}
		
		return null;
	}
}
