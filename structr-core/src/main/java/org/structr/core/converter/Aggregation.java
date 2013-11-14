/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.converter;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.structr.common.SecurityContext;
import org.structr.core.Value;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.Notion;
import org.structr.core.property.AggregatorProperty;
import org.structr.core.property.EndNodes;

/**
 * Encapsulates a sorted collection of related nodes of different types, to be
 * used by an {@link AggregatorProperty}.
 *
 * @author Christian Morgner
 */
public class Aggregation implements Value<Aggregation> {

	private Set<EndNodes<?, ?>> aggregationProperties = new LinkedHashSet<EndNodes<?, ?>>();
	private Map<Class, Notion> notions                 = new LinkedHashMap<Class, Notion>();
	private Comparator<NodeInterface> comparator       = null;

	public Aggregation(Comparator<NodeInterface> comparator, EndNodes<?, ?>... properties) {
		
		for(EndNodes<?, ?> property : properties) {
			this.aggregationProperties.add(property);
		}
		
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

	public Set<EndNodes<?, ?>> getAggregationProperties() {
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
