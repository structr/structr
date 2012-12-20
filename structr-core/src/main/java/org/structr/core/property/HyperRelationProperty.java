/*
 *  Copyright (C) 2012 Axel Morgner
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
package org.structr.core.property;

import java.util.LinkedList;
import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.core.EntityContext;
import org.structr.core.GraphObject;
import org.structr.core.entity.AbstractNode;

/**
 * A property that returns the end node of a hyperrelationship.
 *
 * @author Christian Morgner
 */
public class HyperRelationProperty<S extends AbstractNode, T extends AbstractNode> extends AbstractReadOnlyProperty<List<T>> {
	
	CollectionProperty<S> step1 = null;
	EntityProperty<T> step2     = null;
	
	public HyperRelationProperty(String name, CollectionProperty<S> step1, EntityProperty<T> step2) {
		
		super(name);
		
		this.step1 = step1;
		this.step2 = step2;
		
		// make us known to the Collection context
		EntityContext.registerConvertedProperty(this);
	}
	
	@Override
	public List<T> getProperty(SecurityContext securityContext, GraphObject obj, boolean applyConverter) {

		List<S> connectors = obj.getProperty(step1);
		List<T> endNodes   = new LinkedList<T>();
		
		if (connectors != null) {

			for (AbstractNode node : connectors) {
				
				endNodes.add(node.getProperty(step2));
			}
		}
		
		return endNodes;
	}
	
	@Override
	public Class relatedType() {
		return step2.relatedType();
	}
	
	@Override
	public boolean isCollection() {
		return true;
	}
}
