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

import org.apache.commons.lang.StringUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;

/**
 *
 * @Deprecated This property is not working correctly, please do not use.
 * 
 * @author Christian Morgner
 */
public class ListArrayProperty extends StringProperty {

	public static final String SEP = ",";
	
	public ListArrayProperty(String name) {
		super(name);
	}
	
	@Override
	public String typeName() {
		return "String";
	}
	
	@Override
	public PropertyConverter<String, String[]> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return new ListArrayConverter(securityContext);
	}

	@Override
	public PropertyConverter<String[], String> inputConverter(SecurityContext securityContext) {
		return new ArrayListConverter(securityContext);
	}

	public class ListArrayConverter extends PropertyConverter<String, String[]> {

		public ListArrayConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}

		@Override
		public String revert(String[] source) {
			return StringUtils.join((String[]) source, SEP);
		}

		@Override
		public String[] convert(String source) throws FrameworkException {
			return StringUtils.split((String) source, SEP);
		}
	}
	
	public class ArrayListConverter extends PropertyConverter<String[], String> {

		public ArrayListConverter(SecurityContext securityContext) {
			super(securityContext, null);
		}

		@Override
		public String[] revert(String source) throws FrameworkException {
			return StringUtils.split((String) source, SEP);
		}

		@Override
		public String convert(String[] source) {
			return StringUtils.join((String[]) source, SEP);
		}
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}
}
