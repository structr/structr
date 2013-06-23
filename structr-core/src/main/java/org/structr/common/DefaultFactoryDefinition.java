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
package org.structr.common;

import org.neo4j.graphdb.Node;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.GenericNode;
import org.structr.core.entity.GenericRelationship;
import org.structr.core.experimental.NodeExtender;

/**
 * The default factory for unknown types in structr. When structr needs to
 * instantiate a node with an unknown / unregistered type, this class is
 * used.
 *
 * @author Christian Morgner
 */
public class DefaultFactoryDefinition implements FactoryDefinition {

	public static final String GENERIC_NODE_TYPE = GenericNode.class.getSimpleName();
	public static final String GENERIC_REL_TYPE  = GenericRelationship.class.getSimpleName();
	
	public static final NodeExtender genericNodeExtender = new NodeExtender(GenericNode.class, "org.structr.core.entity.dynamic");
	
	private String externalNodeTypeName = null;

	@Override
	public AbstractRelationship createGenericRelationship() {
		return new GenericRelationship();
	}

	@Override
	public String getGenericRelationshiType() {
		return GENERIC_REL_TYPE;
	}

	@Override
	public AbstractNode createGenericNode() {
		return new GenericNode();
	}

	@Override
	public String getGenericNodeType() {
		return GENERIC_NODE_TYPE;
	}

	@Override
	public boolean isGeneric(Class<?> entityClass) {
		
		return
		    GenericRelationship.class.isAssignableFrom(entityClass)
		    ||
		    GenericNode.class.isAssignableFrom(entityClass);
	}

	@Override
	public String determineNodeType(Node node) {
		
		String type = GraphObject.type.dbName();
		if (node.hasProperty(type)) {
			
			Object obj =  node.getProperty(type);
			if (obj != null) {
				return obj.toString();
			}
			
		} else {
			
			if (externalNodeTypeName == null) {
				
				// try to determine external node
				// type name from configuration
				externalNodeTypeName = Services.getConfigurationValue(Services.FOREIGN_TYPE);
			}
			
			if (externalNodeTypeName != null && node.hasProperty(externalNodeTypeName)) {
				
				Object typeObj = node.getProperty(externalNodeTypeName);
				if (typeObj != null) {
					
					String externalNodeType = typeObj.toString();
					
					// initialize dynamic type
					genericNodeExtender.getType(externalNodeType);
					
					// return dynamic type
					return typeObj.toString();
				}
			}
			
			
		}
		
		return getGenericNodeType();
	}
}
