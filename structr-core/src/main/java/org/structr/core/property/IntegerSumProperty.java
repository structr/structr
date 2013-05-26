package org.structr.core.property;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.lucene.search.SortField;

/**
 *
 * @author Christian Morgner
 */
public class IntegerSumProperty extends AbstractReadOnlyProperty<Integer> {

	private List<Property<Integer>> sumProperties = new LinkedList<Property<Integer>>();
	
	public IntegerSumProperty(String name, Property<Integer>... properties) {
		
		super(name);
		
		this.sumProperties = Arrays.asList(properties);
	}
	
	@Override
	public Class relatedType() {
		return null;
	}

	@Override
	public Integer getSortType() {
		return SortField.INT;
	}

	@Override
	public Integer getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		
		int sum = 0;
		
		for (Property<Integer> prop : sumProperties) {
			
			Integer value = obj.getProperty(prop);
			
			if (value != null) {

				sum = sum + value.intValue();
			}
		}
		
		return sum;
	}

	@Override
	public boolean isCollection() {
		return false;
	}
}
