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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.converter.PropertyConverter;

/**
 *
 * @author Christian Morgner
 */
public class Property<JavaType> implements PropertyKey<JavaType> {

	protected JavaType defaultValue = null;
	protected String name           = null;
	
	public Property(String name) {
		this.name = name;
	}
	
	public Property(String name, JavaType defaultValue) {
		this.name = name;
		this.defaultValue = defaultValue;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public String name() {
		return name;
	}
	
	@Override
	public JavaType defaultValue() {
		return defaultValue;
	}
	
	@Override
	public int hashCode() {
		
		if (name != null) {
			return name.hashCode();
		}
		
		// TODO: check if it's ok if null key is not unique
		return super.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		
		if (o instanceof Property) {
		
			return o.hashCode() == hashCode();
		}
		
		return false;
	}
	
	@Override
	public PropertyConverter<?, JavaType> databaseConverter(SecurityContext securityContext) {
		return new Identitiy<Object, JavaType>(securityContext);
	}

	@Override
	public PropertyConverter<?, JavaType> inputConverter(SecurityContext securityContext) {
		return new Identitiy<Object, JavaType>(securityContext);
	}
	
	protected class Identitiy<S, T> extends PropertyConverter<S, T> {

		public Identitiy(SecurityContext securityContext) {
			this(securityContext, null, false);
		}
		
		public Identitiy(SecurityContext securityContext, Integer sortKey) {
			this(securityContext, sortKey, false);
		}
		
		public Identitiy(SecurityContext securityContext, boolean sortFinalResults) {
			this(securityContext, null, sortFinalResults);
		}
		
		public Identitiy(SecurityContext securityContext, Integer sortKey, boolean sortFinalResults) {
			super(securityContext, sortKey, sortFinalResults);
		}
		
		@Override
		public S convertForSetter(T source) throws FrameworkException {
			return (S)source;
		}

		@Override
		public T convertForGetter(S source) {
			return (T)source;
		}
		
	}
}
