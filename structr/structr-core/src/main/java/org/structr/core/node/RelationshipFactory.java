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


import java.util.LinkedList;
import java.util.List;
import org.structr.common.SecurityContext;
import org.structr.core.Adapter;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.module.GetEntityClassCommand;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.cloud.RelationshipDataContainer;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.GenericRelationship;

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
	private SecurityContext securityContext = null;
	//~--- constructors ---------------------------------------------------

	// private Map<String, Class> nodeTypeCache = new ConcurrentHashMap<String, Class>();
	public RelationshipFactory() {}

	public RelationshipFactory(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	//~--- methods --------------------------------------------------------

	public AbstractRelationship createRelationship(SecurityContext securityContext, final Relationship relationship) throws FrameworkException {

		return createRelationship(securityContext, relationship, relationship.getType().name());
	}

	public AbstractRelationship createRelationship(final SecurityContext securityContext, final Relationship relationship, final String relType) throws FrameworkException {

		AbstractRelationship newRel = null;

		try {
			String sourceNodeType = (String)relationship.getStartNode().getProperty(AbstractNode.Key.type.name());
			String destNodeType = (String)relationship.getStartNode().getProperty(AbstractNode.Key.type.name());

			Class relClass      = EntityContext.getNamedRelationClass(sourceNodeType, destNodeType, relType);

			if (relClass != null) {
				newRel = (AbstractRelationship) relClass.newInstance();
			}

		} catch (Throwable t) {
			newRel = null;
		}

		if (newRel == null) {
			newRel = new GenericRelationship();
		}

		newRel.init(securityContext, relationship);

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
		} catch(FrameworkException fex) {
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

	public AbstractRelationship createRelationship(final SecurityContext securityContext, final RelationshipDataContainer data) throws FrameworkException {

		if (data == null) {

			logger.log(Level.SEVERE, "Could not create relationship: Empty data container.");

			return null;
		}

		Map properties       = data.getProperties();
		String nodeType      = properties.containsKey(AbstractNode.Key.type.name())
				       ? (String) properties.get(AbstractNode.Key.type.name())
				       : null;
		Class nodeClass      = (Class) Services.command(securityContext, GetEntityClassCommand.class).execute(nodeType);
		AbstractRelationship newRel = null;

		if (nodeClass != null) {

			try {
				newRel = (AbstractRelationship) nodeClass.newInstance();
			} catch (Throwable t) {
				newRel = null;
			}
		}

		if (newRel == null) {
			newRel = new GenericRelationship();
		}

		newRel.init(securityContext, data);
		newRel.commit();

		return newRel;
	}
}
