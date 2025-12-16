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
import org.structr.core.converter.PropertyMapper;
import org.structr.docs.DocumentableType;

import java.util.Map;

/**
 * A property that maps another (local) property of the same node. This class can be used
 * to establish name mappings between different properties.
 *
 *
 */
public class MappedProperty<T> extends AbstractPrimitiveProperty<T> {

	private PropertyKey<T> mappedKey = null;

	public MappedProperty(String name, PropertyKey<T> mappedKey) {
		super(name);

		this.mappedKey = mappedKey;
	}

	public PropertyKey<T> mappedKey() {
		return mappedKey;
	}

	@Override
	public String typeName() {
		return mappedKey.typeName();
	}

	@Override
	public Class valueType() {
		return mappedKey.valueType();
	}

	@Override
	public SortType getSortType() {
		return mappedKey.getSortType();
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext) {
		return databaseConverter(securityContext, null);
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext, GraphObject entity) {
		return new PropertyMapper(securityContext, entity, mappedKey);
	}

	@Override
	public PropertyConverter<?, T> inputConverter(SecurityContext securityContext, boolean fromString) {
		return mappedKey.inputConverter(securityContext, false);
	}


	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}

	@Override
	public boolean isArray() {
		return mappedKey.isArray();
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return mappedKey.getExampleValue(type, viewName);
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
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
}
