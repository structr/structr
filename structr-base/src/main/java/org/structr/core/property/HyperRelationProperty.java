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
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * A property that returns the end node of a hyperrelationship.
 *
 *
 */
public class HyperRelationProperty<S extends AbstractNode, T extends AbstractNode> extends AbstractReadOnlyProperty<Iterable<T>> {

	Property<Iterable<S>> step1 = null;
	Property<T> step2           = null;

	public HyperRelationProperty(String name, Property<Iterable<S>> step1, Property<T> step2) {

		super(name);

		this.step1 = step1;
		this.step2 = step2;

		// make us known to the Collection context
		StructrApp.getConfiguration().registerConvertedProperty(this);
	}

	@Override
	public Iterable<T> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public Iterable<T> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final Predicate<GraphObject> predicate) {

		Iterable<S> connectors = obj.getProperty(step1);
		List<T> endNodes       = new LinkedList<>();

		if (connectors != null) {

			for (AbstractNode node : connectors) {

				endNodes.add(node.getProperty(step2));
			}
		}

		return endNodes;
	}

	@Override
	public Class relatedType() {
		return step2.relatedType();
	}

	@Override
	public Class valueType() {
		return relatedType();
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	// ----- OpenAPI -----
	@Override
	public Object getExampleValue(java.lang.String type, final String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}
}
