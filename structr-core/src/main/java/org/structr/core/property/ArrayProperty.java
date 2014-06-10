/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.property;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.converter.PropertyConverter;

/**
* A property that stores and retrieves an array of the given type.
 *
 * @author Christian Morgner
 */
public class ArrayProperty<T> extends AbstractPrimitiveProperty<T[]> {

	private Class<T> componentType = null;
	
	public ArrayProperty(String name, Class<T> componentType, final PropertyValidator<T[]>... validators) {
		
		super(name);
		
		this.componentType = componentType;
		
		for (final PropertyValidator<T[]> validator : validators) {
			addValidator(validator);
		}
	}
	
	@Override
	public Object fixDatabaseProperty(Object value) {
		return value;
	}

	@Override
	public String typeName() {
		return "";
	}

	@Override
	public Integer getSortType() {
		return null;
	}
	
	@Override
	public PropertyConverter<T[], ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<T[], ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, T[]> inputConverter(SecurityContext securityContext) {
		return new ArrayInputConverter(securityContext);
	}
	
	private class ArrayInputConverter extends PropertyConverter<Object, T[]> {

		public ArrayInputConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}
		
		@Override
		public Object revert(Object[] source) throws FrameworkException {
			return source != null ? Arrays.asList(source) : null;
		}

		@Override
		public T[] convert(Object source) throws FrameworkException {
			
			if (source instanceof List) {

				T[] dummyValue = (T[])Array.newInstance(componentType, 0);
				return (T[])((List<T>)source).toArray(dummyValue);
			}
			
			if (source.getClass().isArray()) {
				return (T[])source;
			}
			
			return null;
		}
		
	}
}
