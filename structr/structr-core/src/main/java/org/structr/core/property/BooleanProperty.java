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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

/**
* A property that stores and retrieves a simple Boolean value.
 *
 * @author Christian Morgner
 */
public class BooleanProperty extends AbstractPrimitiveProperty<Boolean> {
	
	private static final Set<String> TRUE_VALUES = new LinkedHashSet<String>(Arrays.asList(new String[] { "true", "1", "on" }));

	public BooleanProperty(String name) {
		super(name);
	}
	
	@Override
	public String typeName() {
		return "Boolean";
	}
	
	@Override
	public PropertyConverter<Boolean, Boolean> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return new DatabaseConverter(securityContext);
	}

	@Override
	public PropertyConverter<?, Boolean> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		
		if (value != null) {
			
			if (value instanceof Boolean) {
				return value;
			}
			
			if (value instanceof String) {
				
				return TRUE_VALUES.contains(value.toString().toLowerCase());
			}
		}
		
		return false;
	}
	
	protected class DatabaseConverter extends PropertyConverter<Boolean, Boolean> {

		public DatabaseConverter(SecurityContext securityContext) {
			this(securityContext, null, false);
		}
		
		public DatabaseConverter(SecurityContext securityContext, Integer sortKey) {
			this(securityContext, sortKey, false);
		}
		
		public DatabaseConverter(SecurityContext securityContext, boolean sortFinalResults) {
			this(securityContext, null, sortFinalResults);
		}
		
		public DatabaseConverter(SecurityContext securityContext, Integer sortKey, boolean sortFinalResults) {
			super(securityContext, null, sortKey, sortFinalResults);
		}
		
		@Override
		public Boolean revert(Boolean source) throws FrameworkException {
			
			if (source != null) {
				return source;
			}
			
			return false;
		}

		@Override
		public Boolean convert(Boolean source) {
			
			if (source != null) {
				return source;
			}
			
			return false;
		}
	}

	protected class InputConverter extends PropertyConverter<Object, Boolean> {

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
		public Object revert(Boolean source) throws FrameworkException {
			
			if (source != null) {
				return source;
			}
			
			return false;
		}

		@Override
		public Boolean convert(Object source) {
			
			// FIXME: be more strict when dealing with "wrong" input types
			if (source != null) {
				
				if (source instanceof Boolean) {
					return (Boolean)source;
				}
				
				if (source instanceof String) {
					
					return TRUE_VALUES.contains(source.toString().toLowerCase());
				}
			}
			
			return false;
		}
	}
}
