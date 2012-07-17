/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
