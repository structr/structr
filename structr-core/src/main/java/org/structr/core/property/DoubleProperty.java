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

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.NumericUtils;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.StringSearchAttribute;

/**
* A property that stores and retrieves a simple Double value.
 *
 * @author Christian Morgner
 */
public class DoubleProperty extends AbstractPrimitiveProperty<Double> {
	
	public DoubleProperty(String name) {
		super(name);
	}
	
	@Override
	public String typeName() {
		return "Double";
	}

	@Override
	public Integer getSortType() {
		return SortField.DOUBLE;
	}
	
	@Override
	public PropertyConverter<Double, ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<Double, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, Double> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
	}
	
	protected class InputConverter extends PropertyConverter<Object, Double> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext);
		}
		
		@Override
		public Object revert(Double source) throws FrameworkException {
			return source;
		}

		@Override
		public Double convert(Object source) {
			
			// FIXME: be more strict when dealing with "wrong" input types
			if (source != null) {
				
				if (source instanceof Number) {

					return ((Number)source).doubleValue();
					
				}
				
				if (source instanceof String) {
					
					return Double.parseDouble(source.toString());
				}
			}
			
			return null;
		}
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		
		if (value != null) {
			
			if (value instanceof Double) {
				return value;
			}
			
			if (value instanceof Number) {
				return ((Number)value).doubleValue();
			}
			
			try {
				
				return Double.parseDouble(value.toString());
				
			} catch (Throwable t) {
				
				// no chance, give up..
			}
		}
		
		return null;
	}
	
	@Override
	public SearchAttribute getSearchAttribute(BooleanClause.Occur occur, Double searchValue, boolean exactMatch) {
		
		String value = "";
		
		if (searchValue != null) {
			
			value = NumericUtils.doubleToPrefixCoded(searchValue);
		}
		
		return new StringSearchAttribute(this, value, occur, exactMatch);
	}
}
