/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.CypherQueryConverter;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.cypher.CypherQueryHandler;

import java.util.Map;

/**
 * A property that executes the given {@link CypherQueryHandler} when evaluated
 * and returns the results of the query.
 *
 *
 */
public class CypherProperty<T> extends AbstractPrimitiveProperty<T> {

	private CypherQueryHandler handler = null;

	public CypherProperty(final String name, final CypherQueryHandler handler) {
		super(name);

		this.handler = handler;
	}

	@Override
	public String typeName() {
		return ""; // read-only
	}

	@Override
	public String valueType() {
		return null;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(final SecurityContext securityContext) {
		return databaseConverter(securityContext, null);
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(final SecurityContext securityContext, final GraphObject entity) {
		return new CypherQueryConverter(securityContext, entity, handler);
	}

	@Override
	public PropertyConverter<?, T> inputConverter(final SecurityContext securityContext) {
		return null;
	}

	@Override
	public Object fixDatabaseProperty(final Object value) {
		return null;
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}
}
