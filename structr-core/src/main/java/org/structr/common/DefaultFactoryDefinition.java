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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.GenericNode;
import org.structr.core.entity.GenericRelationship;
import org.structr.core.entity.Relation;
import org.structr.core.experimental.NodeExtender;
import org.structr.core.module.ModuleService;

/**
 * The default factory for unknown types in structr. When structr needs to
 * instantiate a node with an unknown / unregistered type, this class is
 * used.
 *
 * @author Christian Morgner
 */
public class DefaultFactoryDefinition implements FactoryDefinition {
	
	private static final Logger logger = Logger.getLogger(DefaultFactoryDefinition.class.getName());

	private static final String COMBINED_RELATIONSHIP_KEY_SEP = " ";
	
	public static final NodeExtender genericNodeExtender = new NodeExtender(GenericNode.class, "org.structr.core.entity.dynamic");
	public static final Class GENERIC_NODE_TYPE          = GenericNode.class;
	public static final Class GENERIC_REL_TYPE           = GenericRelationship.class;
	
	private ModuleService moduleService = null;
	private String externalNodeTypeName = null;

	@Override
	public AbstractRelationship createGenericRelationship() {
		return new GenericRelationship();
	}

	@Override
	public Class getGenericRelationshipType() {
		return GENERIC_REL_TYPE;
	}

	@Override
	public AbstractNode createGenericNode() {
		return new GenericNode();
	}

	@Override
	public Class getGenericNodeType() {
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
	public Class determineNodeType(Node node) {
		
		final String type = GraphObject.type.dbName();
		if (node.hasProperty(type)) {
			
			final Object obj =  node.getProperty(type);
			if (obj != null) {
				
				return getModuleService().getNodeEntityClass(obj.toString());
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
					return moduleService.getNodeEntityClass(typeObj.toString());
				}
			}
			
			
		}
		
		return getGenericNodeType();
	}

	@Override
	public Class determineRelationshipType(Relationship relationship) {

		final String type = GraphObject.type.dbName();
		
		
		if (relationship.hasProperty(type)) {
			
			Object obj =  relationship.getProperty(type);
			
			logger.log(Level.FINEST, "Type property: {0}", obj);
			
			if (obj != null) {
				
				return getModuleService().getRelationshipEntityClass(obj.toString());
			}
		}

		// fallback to old type
		final String combinedTypeName = "combinedType";
		if (relationship.hasProperty(combinedTypeName)) {
			
			Object obj =  relationship.getProperty(combinedTypeName);
			
			logger.log(Level.FINE, "Combined type property: {0}", obj);
			
			if (obj != null) {
				
				final String combinedType = obj.toString();
				final Class entityType = getClassForCombinedType(combinedType);
				
				if (entityType != null) {
					
					return entityType;
				}
				
			}
		}
		
		// last chance: source type, target type and relationship type
		final String sourceType = relationship.getStartNode().hasProperty(type) ? relationship.getStartNode().getProperty(type).toString() : null;
		final String targetType = relationship.getEndNode().hasProperty(type) ? relationship.getEndNode().getProperty(type).toString() : null;
		final String relType    = relationship.getType().name();
		final Class entityType  = getClassForCombinedType(sourceType, relType, targetType);

		
		if (entityType != null) {
			logger.log(Level.FINE, "Class for assembled combined {0}", entityType.getName());
			return entityType;
		}
		
		logger.log(Level.WARNING, "No instantiable class for relationship found for {0} {1} {2}, returning generic relationship class.", new Object[] { sourceType, relType, targetType });
		
		return getGenericRelationshipType();
	}

	private Class getClassForCombinedType(final String combinedType) {
		
		final String[] parts = StringUtils.split(combinedType, COMBINED_RELATIONSHIP_KEY_SEP);
		final String sourceType = parts[0];
		final String relType    = parts[1];
		final String targetType = parts[2];

		return getClassForCombinedType(sourceType, relType, targetType);
	}

	private Class getClassForCombinedType(final String sourceType, final String relType, final String targetType) {
		
		if (sourceType == null || relType == null || targetType == null) {
			return null;
		}
		
		logger.log(Level.FINE, "Need class for relationship {0}-[:{1}]->{2}", new Object[]{ sourceType, relType, targetType });
		
		for (final Class candidate : getModuleService().getCachedRelationshipEntities().values()) {

			logger.log(Level.FINEST, "Relation class candidate: {0}", candidate.getName());
			
			final Relation rel = instantiate(candidate);
			if (rel != null) {
				
				final String sourceTypeName = rel.getSourceType().getSimpleName();
				final String relTypeName    = rel.name();
				final String targetTypeName = rel.getTargetType().getSimpleName();

				logger.log(Level.FINE, "Checking relationship {0}-[:{1}]->{2}", new Object[]{ sourceTypeName, relTypeName, targetTypeName });

				if (sourceType.equals(sourceTypeName) && relType.equals(relTypeName) && targetType.equals(targetTypeName)) {
					
					logger.log(Level.INFO, "--> Found matching relation class: {0}", candidate.getName());
					return candidate;
				}
			}
		}
		
		return null;
	}
	
	private Relation instantiate(final Class clazz) {
		
		try {
			
			return (Relation)clazz.newInstance();
			
		} catch (Throwable t) {
			
		}
		
		return null;
	}
	
	private ModuleService getModuleService() {
		return Services.getService(ModuleService.class);
	}
}
