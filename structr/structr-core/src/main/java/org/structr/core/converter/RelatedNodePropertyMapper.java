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

import org.structr.common.property.PropertyKey;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.RelationClass;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.SecurityContext;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class RelatedNodePropertyMapper extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(RelatedNodePropertyMapper.class.getName());

	private ParameterHolder holder = null;
	
	public RelatedNodePropertyMapper(SecurityContext securityContext, ParameterHolder holder) {
		
		super(securityContext);
		
		this.holder = holder;
	}
	
	//~--- methods --------------------------------------------------------

	@Override
	public Object convertForSetter(Object source) {

		PropertyKey targetKey    = holder.getTargetKey();
		Class targetType         = holder.getTargetType();
		AbstractNode relatedNode = getRelatedNode(targetType);

		if (relatedNode != null) {

			try {
				relatedNode.setProperty(targetKey, source);
			} catch (FrameworkException fex) {

				logger.log(Level.WARNING, "Unable to set remote node property {0} on type {1}", new Object[] { targetKey.name(),
					targetType });
			}
		}
		
		return null;
	}

	@Override
	public Object convertForGetter(Object source) {

		PropertyKey targetKey    = holder.getTargetKey();
		Class targetType         = holder.getTargetType();
		AbstractNode relatedNode = getRelatedNode(targetType);

		if (relatedNode != null) {

			return relatedNode.getProperty(targetKey);

		}

		return null;
	}

	//~--- get methods ----------------------------------------------------

	private AbstractNode getRelatedNode(Class targetType) {

		AbstractNode relatedNode = null;

		if ((currentObject != null) && (currentObject instanceof AbstractNode)) {

			AbstractNode localNode = (AbstractNode) currentObject;
			RelationClass rel   = EntityContext.getRelationClass(localNode.getClass(), targetType);

			if (rel != null) {

				relatedNode = rel.getRelatedNode(securityContext, localNode);

				if (relatedNode == null) {

					try {
						relatedNode = rel.addRelatedNode(securityContext, localNode);
					} catch (FrameworkException ex) {
						logger.log(Level.WARNING, "Could not add related node", ex);
					}

				}

			}

		}

		return relatedNode;
	}

	//~--- inner classes --------------------------------------------------

	public static class ParameterHolder {

		private PropertyKey targetKey = null;
		private Class targetType      = null;

		//~--- constructors -------------------------------------------

		public ParameterHolder(PropertyKey targetKey, Class targetType) {

			this.targetKey  = targetKey;
			this.targetType = targetType;
		}

		//~--- get methods --------------------------------------------

		public PropertyKey getTargetKey() {
			return targetKey;
		}

		public Class getTargetType() {
			return targetType;
		}
	}
}
