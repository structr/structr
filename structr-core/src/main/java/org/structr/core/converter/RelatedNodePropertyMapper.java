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

import org.structr.core.property.PropertyKey;
import org.structr.common.error.FrameworkException;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.End;

//~--- classes ----------------------------------------------------------------

/**
 * Maps the given target property key to a related node.
 * 
 * @author Christian Morgner
 */
public class RelatedNodePropertyMapper extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(RelatedNodePropertyMapper.class.getName());

	private End<?> sourceKey  = null;
	private PropertyKey targetKey = null;
	
	public RelatedNodePropertyMapper(SecurityContext securityContext, GraphObject currentObject, End<?> sourceKey, PropertyKey targetKey) {
		
		super(securityContext, currentObject);
		
		this.sourceKey = sourceKey;
		this.targetKey = targetKey;
	}
	
	//~--- methods --------------------------------------------------------

	@Override
	public Object convert(Object source) {

		NodeInterface relatedNode = getRelatedNode(true);
		if (relatedNode != null) {

			try {
				
				relatedNode.setProperty(targetKey, source);
				
			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Unable to set remote node property {0}", targetKey);
			}
		}
		
		return null;
	}

	@Override
	public Object revert(Object source) {

		NodeInterface relatedNode = getRelatedNode(false);
		if (relatedNode != null) {

			return relatedNode.getProperty(targetKey);
		}

		return null;
	}

	//~--- get methods ----------------------------------------------------

	private NodeInterface getRelatedNode(boolean add) {

		NodeInterface relatedNode = null;

		if ((currentObject != null) && (currentObject instanceof NodeInterface)) {

			NodeInterface localNode = (NodeInterface) currentObject;
			relatedNode = localNode.getProperty(sourceKey);
			
			if (relatedNode == null && add) {
				
				try {
					relatedNode = sourceKey.createRelatedNode(securityContext, localNode);
					
				} catch (FrameworkException fex) {
					
					logger.log(Level.WARNING, "Unable to create related node from property {0}", sourceKey);
				}
			}
		}

		return relatedNode;
	}
}
