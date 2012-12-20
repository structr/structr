/*
 *  Copyright (C) 2011-2012 Axel Morgner, structr <structr@structr.org>
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



package org.structr.core.graph;

import org.neo4j.graphdb.Relationship;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Adapter;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.property.PropertyMap;

//~--- classes ----------------------------------------------------------------

/**
 * A factory for structr relationships. This class exists because we need a fast
 * way to instantiate and initialize structr relationships, as this is the most-
 * used operation.
 *
 * @author amorgner
 */
public class RelationshipFactory<T extends AbstractRelationship> implements Adapter<Relationship, T> {

	private static final Logger logger = Logger.getLogger(RelationshipFactory.class.getName());

	//~--- fields ---------------------------------------------------------

	private SecurityContext securityContext = null;

	//~--- constructors ---------------------------------------------------

	// private Map<String, Class> nodeTypeCache = new ConcurrentHashMap<String, Class>();
	public RelationshipFactory() {}

	public RelationshipFactory(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	//~--- methods --------------------------------------------------------

	public T instantiateRelationship(final SecurityContext securityContext, final String combinedRelType) throws FrameworkException {

		Class<T> relClass = EntityContext.getNamedRelationClass(combinedRelType);
		T newRel          = null;

		if (relClass != null) {

			try {
				newRel = relClass.newInstance();				
				newRel.onRelationshipInstantiation();
				
			} catch (InstantiationException ex) {
				logger.log(Level.FINE, "Could not instantiate relationship", ex);
			} catch (IllegalAccessException ex) {
				logger.log(Level.SEVERE, "Could not access relationship", ex);
			}

		}


		return newRel;
	}

	public T instantiateRelationship(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {

		String combinedRelType = (String) properties.get(AbstractRelationship.combinedType);
		T newRel               = instantiateRelationship(securityContext, combinedRelType);

		newRel.setProperties(properties);

		return newRel;
	}

	public T instantiateRelationship(final SecurityContext securityContext, final Relationship relationship) throws FrameworkException {

		Class<T> relClass = null;
		T newRel          = null;

		try {

			relClass = findNamedRelation(relationship);
			if (relClass != null) {

				try {
					newRel = relClass.newInstance();
				} catch (Throwable t2) {
					newRel = null;
				}

			} else {

				if (relationship.hasProperty(AbstractRelationship.combinedType.dbName())) {

					String combinedRelType = (String) relationship.getProperty(AbstractRelationship.combinedType.dbName());

					relClass = EntityContext.getNamedRelationClass(combinedRelType);

					if (relClass != null) {

						newRel = instantiateRelationship(securityContext, combinedRelType);

					}

				}

			}

		} catch (Throwable t) {}

		if (newRel == null) {
			newRel = (T)EntityContext.getGenericFactory().createGenericRelationship();
		}

		newRel.init(securityContext, relationship);
		newRel.onRelationshipInstantiation();
			
		return newRel;
	}

	@Override
	public T adapt(Relationship s) {

		try {
			return instantiateRelationship(securityContext, s);
			
		} catch (FrameworkException fex) {
			
			logger.log(Level.WARNING, "Unable to adapt relationship", fex);
		}

		return null;
	}

	/**
	 * Create structr relationship from all given underlying database rels
	 *
	 * @param input
	 * @return
	 */
	public List<T> instantiateRelationships(final SecurityContext securityContext, final Iterable<Relationship> input) throws FrameworkException {

		List<T> rels = new LinkedList<T>();

		if ((input != null) && input.iterator().hasNext()) {

			for (Relationship rel : input) {

				T n = instantiateRelationship(securityContext, rel);

				rels.add(n);

			}

		}

		return rels;
	}
	
	private Class<T> findNamedRelation(Relationship relationship) {
		
		String sourceNodeType = (String) relationship.getStartNode().getProperty(AbstractNode.type.dbName());
		String destNodeType   = (String) relationship.getEndNode().getProperty(AbstractNode.type.dbName());

		
		Class sourceType = EntityContext.getEntityClassForRawType(sourceNodeType);
		Class destType   = EntityContext.getEntityClassForRawType(destNodeType);
		
		return EntityContext.getNamedRelationClass(sourceType, destType, relationship.getType());
	}
}
















































