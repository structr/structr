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

/**
 *
 * @author Christian Morgner
 */
public class Aggregation implements Value<Aggregation> {

	private Map<Class, Notion> notions          = new LinkedHashMap<Class, Notion>();
	private Set<Class> aggregationTypes         = new LinkedHashSet<Class>();
	private Comparator<AbstractNode> comparator = null;

	public Aggregation(Comparator<AbstractNode> comparator, Class... types) {
		this.comparator = comparator;
		for(Class type : types) {
			this.aggregationTypes.add(type);
		}
	}

	public void setNotionForType(Class type, Notion notion) {
		notions.put(type, notion);
	}
	
	public Notion getNotionForType(Class type) {
		return notions.get(type);
	}
	
	public Comparator<AbstractNode> getComparator() {
		return comparator;
	}

	public Set<Class> getAggregationTypes() {
		return aggregationTypes;
	}

	@Override
	public void set(SecurityContext securityContext, Aggregation value) {
	}

	@Override
	public Aggregation get(SecurityContext securityContext) {
		return this;
	}
}
