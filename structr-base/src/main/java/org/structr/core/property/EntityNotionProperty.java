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
import org.structr.api.search.Occurrence;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.Query;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.search.GraphSearchAttribute;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.notion.Notion;

import java.util.Collections;
import java.util.Map;

/**
* A property that uses the value of a related node property to create
* a relationship between two nodes. This property should only be used
* with related properties that uniquely identify a given node, as the
* value will be used to search for a matching node to which the
* relationship will be created.
 *
 *
 */
public class EntityNotionProperty<S extends NodeInterface, T> extends Property<T> {

	private static final Logger logger = LoggerFactory.getLogger(EntityNotionProperty.class.getName());

	private Property<S> entityProperty = null;
	private Notion<S, T> notion        = null;

	public EntityNotionProperty(final String name, final Property<S> base, final Notion<S, T> notion) {

		super(name);

		this.notion = notion;
		this.entityProperty   = base;

		notion.setType(base.relatedType());
	}

	@Override
	public Property<T> indexed() {
		return this;
	}

	@Override
	public Property<T> passivelyIndexed() {
		return this;
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}

	@Override
	public String typeName() {
		return "";
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(final SecurityContext securityContext) {
		return null;
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(final SecurityContext securityContext, final GraphObject entity) {
		return null;
	}

	@Override
	public PropertyConverter<?, T> inputConverter(final SecurityContext securityContext) {
		return null;
	}

	@Override
	public T getProperty(final SecurityContext securityContext, final GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public T getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter, final Predicate<NodeInterface> predicate) {

		try {
			return notion.getAdapterForGetter(securityContext).adapt(entityProperty.getProperty(securityContext, obj, applyConverter, predicate));

		} catch (FrameworkException fex) {

			logger.warn("Unable to apply notion of type {} to property {}", new Object[] { notion.getClass(), this } );
		}

		return null;
	}

	@Override
	public Object setProperty(final SecurityContext securityContext, final GraphObject obj, final T value) throws FrameworkException {

		if (value != null) {

			return entityProperty.setProperty(securityContext, obj, notion.getAdapterForSetter(securityContext).adapt(value));

		} else {

			return entityProperty.setProperty(securityContext, obj, null);
		}
	}

	@Override
	public String relatedType() {
		return entityProperty.relatedType();
	}

	@Override
	public String valueType() {
		return relatedType();
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public SearchAttribute getSearchAttribute(SecurityContext securityContext, Occurrence occur, T searchValue, boolean exactMatch, final Query query) {
		return new GraphSearchAttribute(notion.getPrimaryPropertyKey(), entityProperty, searchValue, occur, exactMatch);
	}

	@Override
	public boolean isIndexed() {
		return false;
	}

	@Override
	public boolean isPassivelyIndexed() {
		return false;
	}

	@Override
	public int getProcessingOrderPosition() {
		return 1000;
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
