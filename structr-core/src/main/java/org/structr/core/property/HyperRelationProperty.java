/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.LinkedList;
import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;

/**
 * A property that returns the end node of a hyperrelationship.
 *
 *
 */
public class HyperRelationProperty<S extends AbstractNode, T extends AbstractNode> extends AbstractReadOnlyProperty<List<T>> {

	Property<List<S>> step1 = null;
	Property<T> step2       = null;

	public HyperRelationProperty(String name, Property<List<S>> step1, Property<T> step2) {

		super(name);

		this.step1 = step1;
		this.step2 = step2;

		// make us known to the Collection context
		StructrApp.getConfiguration().registerConvertedProperty(this);
	}

	@Override
	public List<T> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public List<T> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter, final org.neo4j.helpers.Predicate<GraphObject> predicate) {

		List<S> connectors = obj.getProperty(step1);
		List<T> endNodes   = new LinkedList<>();

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
	public Integer getSortType() {
		return null;
	}
}
