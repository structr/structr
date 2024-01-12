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
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.RelationshipInterface;

/**
 *
 *
 */
public class RelationshipTypeProperty extends StringProperty {

	public RelationshipTypeProperty() {

		super("relType");

		systemInternal();
		readOnly();
		passivelyIndexed();
		writeOnce();

	}

	@Override
	public Class relatedType() {
		return null;
	}

	@Override
	public Class valueType() {
		return String.class;
	}

	@Override
	public String getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public String getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final Predicate<GraphObject> predicate) {

		if (obj instanceof RelationshipInterface) {

			return ((RelationshipInterface)obj).getRelType().name();
		}

		return null;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public String typeName() {
		return ""; // read-only
	}

	@Override
	public Object fixDatabaseProperty(final Object value) {
		return value;
	}

	@Override
	public PropertyConverter<String, ?> databaseConverter(final SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<String, ?> databaseConverter(final SecurityContext securityContext, final GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, String> inputConverter(final SecurityContext securityContext) {
		return null;
	}
}
