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
package org.structr.web.property;

import org.structr.api.Predicate;
import org.structr.api.search.SortType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.AbstractReadOnlyCollectionProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.web.entity.dom.DOMNode;

import java.util.Map;

public class DOMNodeSortedChildrenProperty extends AbstractReadOnlyCollectionProperty<DOMNode> {

	public DOMNodeSortedChildrenProperty(final String name) {
		super(name);
	}

	@Override
	public Class valueType() {
		return NodeInterface.class;
	}

	@Override
	public String relatedType() {
		return null;
	}

	@Override
	public Iterable<DOMNode> getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public Iterable<DOMNode> getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter, final Predicate<GraphObject> predicate) {

		if (obj != null && obj.is(StructrTraits.DOM_NODE)) {

			try {
				return obj.as(DOMNode.class).getChildNodes();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}
		}

		return null;
	}

	@Override
	public boolean isCollection() {
		return true;
	}

	@Override
	public boolean isArray() {
		return true;
	}

	@Override
	public SortType getSortType() {
		return SortType.Default;
	}

	@Override
	public Object getExampleValue(String type, String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return Map.of();
	}
}
