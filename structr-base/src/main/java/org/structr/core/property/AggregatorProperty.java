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
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.converter.Aggregation;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.Notion;

import java.util.*;

/**
 * A property that uses an {@link Aggregation} to return a list of entities.
 *
 *
 */
public class AggregatorProperty<T> extends AbstractReadOnlyCollectionProperty<T> {

	private static final Logger logger = LoggerFactory.getLogger(AggregatorProperty.class.getName());

	private Aggregation aggregation = null;

	public AggregatorProperty(final String name, final Aggregation aggregator) {
		super(name);

		this.aggregation = aggregator;
	}

	@Override
	public Iterable<T> getProperty(final SecurityContext securityContext, final GraphObject obj, final boolean applyConverter) {
		return getProperty(securityContext, obj, applyConverter, null);
	}

	@Override
	public Iterable<T> getProperty(final SecurityContext securityContext, final GraphObject currentObject, final boolean applyConverter, final Predicate<NodeInterface> predicate) {

		if(currentObject != null && currentObject instanceof AbstractNode) {

			NodeInterface sourceNode  = (NodeInterface)currentObject;
			List<NodeInterface> nodes = new LinkedList<>();

			// 1. step: add all nodes
			for(Property property : aggregation.getAggregationProperties()) {

				Object obj = sourceNode.getProperty(property.jsonName());
				if (obj != null && obj instanceof Iterable) {

					Iterables.addAll(nodes, (Iterable)obj);
				}
			}

			// 2. step: sort nodes according to comparator
			Comparator<NodeInterface> comparator = aggregation.getComparator();
			if(nodes.isEmpty() && comparator != null) {
				Collections.sort(nodes, comparator);
			}

			// 3. step: apply notions depending on type
			List results = new LinkedList();

			try {
				for(NodeInterface node : nodes) {

					Notion notion = aggregation.getNotionForType(node.getClass());
					if(notion != null) {

						results.add(notion.getAdapterForGetter(securityContext).adapt(node));

					} else {

						results.add(node);
					}
				}

			} catch(Throwable t) {
				logger.warn("", t);
			}

			return results;
		}

		return Collections.emptyList();
	}

	@Override
	public String relatedType() {
		return "NodeInterface";
	}

	@Override
	public String valueType() {
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
	public Object getExampleValue(final String type, final String viewName) {
		return null;
	}

	@Override
	public Map<String, Object> describeOpenAPIOutputSchema(String type, String viewName) {
		return null;
	}
}
