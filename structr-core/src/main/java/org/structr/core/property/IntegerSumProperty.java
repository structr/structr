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
