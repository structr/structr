/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.core.converter.PropertyConverter;
import org.structr.core.converter.RelationshipEndNodeConverter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.docs.DocumentableType;

import java.util.Map;

/**
 * A property that returns the end node of a relationship.
 */
public class EndNodeProperty<T> extends AbstractPrimitiveProperty<T> {

	public EndNodeProperty(final String name) {
		super(name);
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(final SecurityContext securityContext) {
		return databaseConverter(securityContext, null);
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(final SecurityContext securityContext, final GraphObject entity) {
		return new RelationshipEndNodeConverter(securityContext, entity);
	}

	@Override
	public PropertyConverter<?, T> inputConverter(final SecurityContext securityContext, boolean fromString) {
		return null;
	}

	@Override
	public Object fixDatabaseProperty(final Object value) {
		return null;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public String typeName() {
		return StructrTraits.NODE_INTERFACE;
	}

	@Override
	public Class valueType() {
		return NodeInterface.class;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	// ----- interface Documentable -----
	@Override
	public DocumentableType getDocumentableType() {
		return DocumentableType.Hidden;
	}

	@Override
	public String getShortDescription() {
		return null;
	}

	@Override
	public String getLongDescription() {
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
