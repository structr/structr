/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.predicate;

import org.neo4j.graphdb.Node;
import org.structr.common.SecurityContext;
import org.structr.core.Predicate;

/**
 * A node predicate that evaluates to <b>true</b> if the given node has a
 * property value that matches the given value.
 *
 *
 */
public class NodePropertyValuePredicate implements Predicate<Node> {

	private String propertyKey = null;
	private Object value = null;
	
	public NodePropertyValuePredicate(String propertyKey, Object value) {
		this.propertyKey = propertyKey;
		this.value = value;
	}
	
	@Override
	public boolean evaluate(SecurityContext securityContext, Node... nodes) {
		
		if(nodes.length > 0) {
			
			Node node = nodes[0];
			
			return node.hasProperty(propertyKey) && node.getProperty(propertyKey).equals(value);
		}
		
		return false;
	}
}
