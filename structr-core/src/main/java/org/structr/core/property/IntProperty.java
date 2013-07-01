/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.property;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.NumericUtils;
import org.neo4j.index.lucene.ValueContext;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.search.IntegerSearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.PropertySearchAttribute;

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
	public Integer getSortType() {
		return SortField.INT;
	}

	@Override
	public PropertyConverter<Integer, Integer> databaseConverter(SecurityContext securityContext) {
		return null;
	}
	
	@Override
	public PropertyConverter<Integer, Integer> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, Integer> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
	}
	
	protected class InputConverter extends PropertyConverter<Object, Integer> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext, null);
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
					
					if (StringUtils.isBlank((String) source)) {
						return null;
					}
					
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
	
	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, BooleanClause.Occur occur, Integer searchValue, boolean exactMatch) {
		return new IntegerSearchAttribute(this, searchValue, occur, exactMatch);
	}

	@Override
	public void index(GraphObject entity, Object value) {
		super.index(entity, value != null ? ValueContext.numeric((Number)value) : value);
	}
}