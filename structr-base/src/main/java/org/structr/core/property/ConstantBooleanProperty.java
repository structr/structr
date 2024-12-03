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

import org.structr.api.Predicate;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.ReadOnlyPropertyToken;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.NodeInterface;

import java.util.Map;

/**
* A property that returns a constant Boolean value.
 */
public class ConstantBooleanProperty extends AbstractPrimitiveProperty<Boolean>	 {

	private boolean constantValue;

	public ConstantBooleanProperty(final String name, final boolean constantValue) {

		super(name);
		readOnly();

		this.constantValue = constantValue;
	}

	public ConstantBooleanProperty(final String jsonName, final String dbName, final boolean constantValue) {
		super(jsonName, dbName);
	}

	@Override
	public Boolean getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public Boolean getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter, final Predicate<NodeInterface> predicate) {

		if (obj.getTraits().contains(declaringTrait.getName())) {
			return this.constantValue;
		}

		return false; // null = false
	}

	@Override
	public Object setProperty(final SecurityContext securityContext, final GraphObject obj, final Boolean value) throws FrameworkException {
		throw new FrameworkException(422, "Unable to change value of constant property ‛" + jsonName() + "‛", new ReadOnlyPropertyToken(obj.getType(), jsonName()));
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String typeName() {
		return "Boolean";
	}

	@Override
	public String valueType() {
		return "Boolean";
	}

	@Override
	public PropertyConverter<Boolean, ?> databaseConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<Boolean, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, Boolean> inputConverter(SecurityContext securityContext) {
		return null;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return constantValue;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}
}
