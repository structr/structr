/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.core.converter;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.structr.common.SecurityContext;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.notion.Notion;
import org.structr.core.property.AggregatorProperty;
import org.structr.core.property.CollectionProperty;

/**
 * Encapsulates a sorted collection of related nodes of different types, to be
 * used by an {@link AggregatorProperty}.
 *
 * @author Christian Morgner
 */
public class Aggregation implements Value<Aggregation> {

	private Set<CollectionProperty<? extends AbstractNode>> aggregationProperties = new LinkedHashSet<CollectionProperty<? extends AbstractNode>>();
	private Map<Class, Notion> notions                                            = new LinkedHashMap<Class, Notion>();
	private Comparator<AbstractNode> comparator                                   = null;

	public Aggregation(Comparator<AbstractNode> comparator, CollectionProperty<? extends AbstractNode>... properties) {
		
		for(CollectionProperty<? extends AbstractNode> property : properties) {
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
	
	public Comparator<AbstractNode> getComparator() {
		return comparator;
	}

	public Set<CollectionProperty<? extends AbstractNode>> getAggregationProperties() {
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
