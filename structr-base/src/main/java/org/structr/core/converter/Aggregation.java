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
package org.structr.core.converter;

import org.structr.common.SecurityContext;
import org.structr.core.Value;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.Notion;
import org.structr.core.property.AggregatorProperty;
import org.structr.core.property.Property;

import java.util.*;

/**
 * Encapsulates a sorted collection of related nodes of different types, to be
 * used by an {@link AggregatorProperty}.
 *
 *
 */
public class Aggregation implements Value<Aggregation> {

	private Set<Property> aggregationProperties  = new LinkedHashSet<>();
	private Map<Class, Notion> notions           = new LinkedHashMap<>();
	private Comparator<NodeInterface> comparator = null;

	public Aggregation(Comparator<NodeInterface> comparator, Property... properties) {
		
		this.aggregationProperties.addAll(Arrays.asList(properties));
		this.comparator = comparator;
	}

	public void setNotionForProperty(Class type, Notion notion) {
		notions.put(type, notion);
	}
	
	public Notion getNotionForType(Class type) {
		return notions.get(type);
	}
	
	public Comparator<NodeInterface> getComparator() {
		return comparator;
	}

	public Set<Property> getAggregationProperties() {
		return aggregationProperties;
	}

	@Override
	public void set(SecurityContext securityContext, Aggregation value) {
	}

	@Override
	public Aggregation get(SecurityContext securityContext) {
		return this;
	}
}
