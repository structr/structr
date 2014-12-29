/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.property;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.PropertyValidator;
import org.structr.core.converter.PropertyConverter;

/**
 * A property that stores and retrieves a simple String value.
 *
 * @author Christian Morgner
 */
public class StringProperty extends AbstractPrimitiveProperty<String> {
	
	public StringProperty(String jsonName) {
		this(jsonName, jsonName, new PropertyValidator[0]);
	}

	public StringProperty(String jsonName, String defaultValue) {
		super(jsonName);
		this.defaultValue = defaultValue;
	}

	public StringProperty(String name, String dbName, String defaultValue) {
		super(name, dbName, defaultValue);
	}
	
	public StringProperty(String name, String dbName, String defaultValue, String pattern) {
		this(name, dbName, defaultValue);
		this.format = pattern;
	}

	public StringProperty(String name, PropertyValidator<String>... validators) {
		this(name, name, validators);
	}
	
	public StringProperty(String jsonName, String dbName, PropertyValidator<String>... validators) {
		this(jsonName, dbName, null, validators);
	}
	
	public StringProperty(String jsonName, String dbName, String defaultValue, PropertyValidator<String>... validators) {

		super(jsonName, dbName, defaultValue);
		
		for (PropertyValidator<String> validator : validators) {
			addValidator(validator);
		}
	}
	
	@Override
	public String typeName() {
		return "String";
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		
		if (value != null) {
			
			if (value instanceof String) {
				return value;
			}
			
			return value.toString();
		}
		
		return null;
	}

	@Override
	public Integer getSortType() {
		return null;
	}

	@Override
	public PropertyConverter<String, ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<String, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, String> inputConverter(SecurityContext securityContext) {
		return null;
	}
}
