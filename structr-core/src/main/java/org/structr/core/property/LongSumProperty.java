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

import java.util.List;
import org.apache.lucene.search.SortField;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;

/**
 *
 * @author Christian Morgner
 */
public class LongSumProperty extends AbstractReadOnlyProperty<Long> {

	private CollectionProperty<?, ?> collectionProperty = null;
	private Property<Long> valueProperty                = null;
	
	public LongSumProperty(String name, CollectionProperty<?, ?> collectionProperty, Property<Long> valueProperty, Long defaultValue) {
		
		super(name, defaultValue);
		
		this.collectionProperty = collectionProperty;
		this.valueProperty = valueProperty;
		this.defaultValue = defaultValue;
	}
	
	@Override
	public Class relatedType() {
		return null;
	}

	@Override
	public Long getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {
		
		List<? extends GraphObject> collection = obj.getProperty(collectionProperty);
		if (collection != null) {
			
			long sum = 0L;
			
			for (GraphObject element : collection) {
				
				Long value = element.getProperty(valueProperty);
				if (value != null) {
				
					sum += value.longValue();
				}
				
			}
			
			return Long.valueOf(sum);
		}
		
		return defaultValue();
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public Integer getSortType() {
		
		return SortField.LONG;
		
	}
}
