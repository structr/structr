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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.CreationContainer;
import org.structr.core.graph.RelationshipInterface;

import java.util.Collections;
import java.util.Map;

/**
 *
 *
 */
public class TargetId extends Property<String> {

	private static final Logger logger = LoggerFactory.getLogger(TargetId.class.getName());

	public TargetId(final String name) {
		super(name);
	}

	@Override
	public Class relatedType() {
		return null;
	}

	@Override
	public String getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public String getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final Predicate<GraphObject> predicate) {

		if (obj instanceof RelationshipInterface) {

			return ((RelationshipInterface)obj).getTargetNodeId();
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
	public Object setProperty(SecurityContext securityContext, GraphObject obj, String value) throws FrameworkException {

		if (obj instanceof RelationshipInterface) {

			try {
				((RelationshipInterface)obj).setTargetNodeId(value);

			} catch (FrameworkException fex) {

				throw fex;

			} catch (Throwable t) {

				logger.warn("", t);
			}

		} else if (obj instanceof CreationContainer) {

			((CreationContainer)obj).setProperty(jsonName, value);
		}

		return null;
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}

	@Override
	public String typeName() {
		return null;
	}

	@Override
	public Class valueType() {
		return String.class;
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

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(final String type, final String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputType(final String type, final String viewName, final int level) {
		return Collections.EMPTY_MAP;
	}

	@Override
	public Map<String, Object> describeOpenAPIInputType(final String type, final String viewName, final int level) {
		return Collections.EMPTY_MAP;
	}
}
