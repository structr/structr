/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.core.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.RelationProperty;

/**
 * Maps the given target property key to a related node.
 */
public class RelatedNodePropertyMapper<T extends NodeInterface> extends PropertyConverter {

	private static final Logger logger = LoggerFactory.getLogger(RelatedNodePropertyMapper.class.getName());

	private PropertyKey<T> sourceKey  = null;
	private PropertyKey targetKey = null;
	
	public RelatedNodePropertyMapper(final SecurityContext securityContext, final GraphObject currentObject, final PropertyKey<T> sourceKey, final PropertyKey targetKey) {
		
		super(securityContext, currentObject);
		
		this.sourceKey = sourceKey;
		this.targetKey = targetKey;
	}

	@Override
	public Object convert(final Object source) {

		NodeInterface relatedNode = getRelatedNode(true);
		if (relatedNode != null) {

			try {
				
				relatedNode.setProperty(targetKey, source);
				
			} catch (FrameworkException fex) {

				logger.warn("Unable to set remote node property {}", targetKey);
			}
		}
		
		return null;
	}

	@Override
	public Object revert(final Object source) {

		NodeInterface relatedNode = getRelatedNode(false);
		if (relatedNode != null) {

			return relatedNode.getProperty(targetKey);
		}

		return null;
	}

	private NodeInterface getRelatedNode(final boolean add) {

		T relatedNode = null;

		if ((currentObject != null) && (currentObject instanceof NodeInterface)) {

			NodeInterface localNode = (NodeInterface) currentObject;
			relatedNode = localNode.getProperty(sourceKey);
			
			if (relatedNode == null && add && sourceKey instanceof RelationProperty) {

				final RelationProperty relationProperty = (RelationProperty)sourceKey;
				final App app                           = StructrApp.getInstance();
				final String relatedType                = relationProperty.getTargetType();

				if (relatedType != null) {
					
					try {
						relatedNode = (T)app.create(relatedType);
						relationProperty.addSingleElement(securityContext, localNode, relatedNode);

					} catch (FrameworkException fex) {

						logger.warn("", fex);
					}
					
				} else {
					
					logger.error("Related type was null for {}", currentObject);
				}
			}
		}

		return relatedNode;
	}
}
