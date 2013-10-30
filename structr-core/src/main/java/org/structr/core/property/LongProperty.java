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

import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.NumericUtils;
import org.neo4j.index.lucene.ValueContext;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.PropertySearchAttribute;
import org.structr.core.graph.search.SearchCommand;

/**
 * A property that stores and retrieves a simple Long value.
 * 
 * @author Christian Morgner
 */
public class LongProperty extends AbstractPrimitiveProperty<Long> {
	
	private static final Logger logger = Logger.getLogger(LongProperty.class.getName());
	
	public LongProperty(String name) {
		super(name);
	}
	
	@Override
	public String typeName() {
		return "Long";
	}

	@Override
	public Integer getSortType() {
		return SortField.LONG;
	}
	
	@Override
	public PropertyConverter<Long, Long> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<Long, Long> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, Long> inputConverter(SecurityContext securityContext) {
		return new InputConverter(securityContext);
	}
	
	protected class InputConverter extends PropertyConverter<Object, Long> {

		public InputConverter(SecurityContext securityContext) {
			super(securityContext);
		}
		
		@Override
		public Object revert(Long source) throws FrameworkException {
			return source;
		}

		@Override
		public Long convert(Object source) {
			
			if (source == null) return null;
			
			if (source instanceof Number) {

				return ((Number)source).longValue();

			}

			if (source instanceof String && StringUtils.isNotBlank((String) source)) {

				return Long.parseLong(source.toString());
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
	
	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, BooleanClause.Occur occur, Long searchValue, boolean exactMatch) {
		
		String value = "";
		
		if (searchValue != null) {
			
			value = NumericUtils.longToPrefixCoded(searchValue);
		}
		
		return new PropertySearchAttribute(this, value, occur, exactMatch);
	}

	@Override
	public void index(GraphObject entity, Object value) {
		super.index(entity, value != null ? ValueContext.numeric((Number)value) : value);
	}

}
