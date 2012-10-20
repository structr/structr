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



package org.structr.core.node;

import org.neo4j.graphdb.Relationship;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Adapter;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.GenericRelationship;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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

	public AbstractRelationship createRelationship(final SecurityContext securityContext, final String combinedRelType) throws FrameworkException {

		AbstractRelationship newRel = null;
		Class relClass              = EntityContext.getNamedRelationClass(combinedRelType);

		if (relClass != null) {

			try {
				newRel = (AbstractRelationship) relClass.newInstance();				
				newRel.onRelationshipInstantiation();
			} catch (InstantiationException ex) {
				logger.log(Level.FINE, "Could not instantiate relationship", ex);
			} catch (IllegalAccessException ex) {
				logger.log(Level.SEVERE, "Could not access relationship", ex);
			}

		}


		return newRel;
	}

	public AbstractRelationship createRelationship(final SecurityContext securityContext, final Map properties) throws FrameworkException {

		String combinedRelType      = (String) properties.get(AbstractRelationship.combinedType.name());
		AbstractRelationship newRel = createRelationship(securityContext, combinedRelType);

		newRel.setProperties(properties);

		return newRel;
	}

	public AbstractRelationship createRelationship(final SecurityContext securityContext, final Relationship relationship) throws FrameworkException {

		AbstractRelationship newRel = null;
		Class relClass;

		try {

			relClass = findNamedRelation(relationship);
			if (relClass != null) {

				try {
					newRel = (AbstractRelationship) relClass.newInstance();
				} catch (Throwable t2) {
					newRel = null;
				}

			} else {

				if (relationship.hasProperty(AbstractRelationship.combinedType.name())) {

					String combinedRelType = (String) relationship.getProperty(AbstractRelationship.combinedType.name());

					relClass = EntityContext.getNamedRelationClass(combinedRelType);

					if (relClass != null) {

						newRel = createRelationship(securityContext, combinedRelType);

					}

				}

			}

		} catch (Throwable t) {}

		if (newRel == null) {

			newRel = new GenericRelationship();

		}

		newRel.init(securityContext, relationship);
		newRel.onRelationshipInstantiation();

		return newRel;
	}

//      @Override
//      protected void finalize() throws Throwable {
//          nodeTypeCache.clear();
//      }
	@Override
	public T adapt(Relationship s) {

		try {
			return ((T) createRelationship(securityContext, s));
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
	public List<AbstractRelationship> createRelationships(final SecurityContext securityContext, final Iterable<Relationship> input) throws FrameworkException {

		List<AbstractRelationship> rels = new LinkedList<AbstractRelationship>();

		if ((input != null) && input.iterator().hasNext()) {

			for (Relationship rel : input) {

				AbstractRelationship n = createRelationship(securityContext, rel);

				rels.add(n);

			}

		}

		return rels;
	}
//
//	public AbstractRelationship createRelationship(final SecurityContext securityContext, final RelationshipDataContainer data) throws FrameworkException {
//
//		if (data == null) {
//
//			logger.log(Level.SEVERE, "Could not create relationship: Empty data container.");
//
//			return null;
//
//		}
//
//		Map properties              = data.getProperties();
//		String combinedRelType      = properties.containsKey(AbstractRelationship.HiddenKey.combinedType.name())
//					      ? (String) properties.get(AbstractRelationship.HiddenKey.combinedType.name())
//					      : null;
//		Class relClass              = EntityContext.getNamedRelationClass(combinedRelType);
//		AbstractRelationship newRel = null;
//
//		if (relClass != null) {
//
//			try {
//				newRel = (AbstractRelationship) relClass.newInstance();
//			} catch (Throwable t) {
//				newRel = null;
//			}
//
//		}
//
//		if (newRel == null) {
//
//			newRel = new GenericRelationship();
//
//		}
//
//		newRel.init(securityContext, data);
//		newRel.commit();
//		newRel.onRelationshipInstantiation();
//
//		return newRel;
//	}
	
	private Class findNamedRelation(Relationship relationship) {
		
		String sourceNodeType = (String) relationship.getStartNode().getProperty(AbstractNode.type.name());
		String destNodeType   = (String) relationship.getEndNode().getProperty(AbstractNode.type.name());

		
		Class sourceType = EntityContext.getEntityClassForRawType(sourceNodeType);
		Class destType   = EntityContext.getEntityClassForRawType(destNodeType);
		
		return EntityContext.getNamedRelationClass(sourceType, destType, relationship.getType());
	}
}
















































