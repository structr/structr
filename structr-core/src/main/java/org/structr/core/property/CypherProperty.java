/**
 * Copyright (C) 2010-2014 Structr, c/o Morgner UG (haftungsbeschränkt) <structr@structr.org>
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

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.CypherQueryConverter;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.cypher.CypherQueryHandler;

/**
 * A property that executes the given {@link CypherQueryHandler} when evaluated
 * and returns the results of the query.
 *
 * @author Christian Morgner
 */
public class CypherProperty<T> extends AbstractPrimitiveProperty<T> {
	
	private CypherQueryHandler handler = null;
	
	public CypherProperty(String name, CypherQueryHandler handler) {
		super(name);
		
		this.handler = handler;
		
		// make us known to the entity context
		StructrApp.getConfiguration().registerConvertedProperty(this);

	}
	
	@Override
	public String typeName() {
		return ""; // read-only
	}

	@Override
	public Integer getSortType() {
		return null;
	}
	
	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext) {
		return databaseConverter(securityContext, null);
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return new CypherQueryConverter(securityContext, entity, handler);
	}

	@Override
	public PropertyConverter<?, T> inputConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}
}
