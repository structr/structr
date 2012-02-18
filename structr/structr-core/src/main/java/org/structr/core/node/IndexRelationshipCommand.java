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

import org.apache.commons.lang.StringUtils;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;

import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Relationship;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.NodeService.RelationshipIndex;

//~--- classes ----------------------------------------------------------------

/**
 * Command for indexing relationships
 *
 * @author axel
 */
public class IndexRelationshipCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(IndexRelationshipCommand.class.getName());

	//~--- fields ---------------------------------------------------------

	private Map<String, Index> indices = new HashMap<String, Index>();

	//~--- methods --------------------------------------------------------

	@Override
	public Object execute(Object... parameters) throws FrameworkException {

		Command findRel = Services.command(securityContext, FindRelationshipCommand.class);
		
		for (Enum indexName : (RelationshipIndex[]) arguments.get("relationshipIndices")) {

			indices.put(indexName.name(), (Index<Node>) arguments.get(indexName.name()));

		}

		long id           = 0;
		AbstractRelationship rel = null;
		String key        = null;

		switch (parameters.length) {

			case 1 :

				// index all properties of this relationship
				if (parameters[0] instanceof Long) {

					id = ((Long) parameters[0]).longValue();

					rel = (AbstractRelationship) findRel.execute(id);

					indexRelationship(rel);

				} else if (parameters[0] instanceof String) {

					id = Long.parseLong((String) parameters[0]);

					rel = (AbstractRelationship) findRel.execute(id);

					indexRelationship(rel);

				} else if (parameters[0] instanceof AbstractRelationship) {

					rel = (AbstractRelationship) parameters[0];

					indexRelationship(rel);

				} else if (parameters[0] instanceof List) {

					indexRelationships((List<AbstractRelationship>) parameters[0]);

				}

				break;

			case 2 :

				// index a certain property
				if (parameters[0] instanceof Long) {

					id = ((Long) parameters[0]).longValue();

					rel = (AbstractRelationship) findRel.execute(id);

				} else if (parameters[0] instanceof String) {

					id = Long.parseLong((String) parameters[0]);

					rel = (AbstractRelationship) findRel.execute(id);

				} else if (parameters[0] instanceof AbstractNode) {

					rel = (AbstractRelationship) parameters[0];

					// id   = node.getId();

				}

				if (parameters[1] instanceof String) {

					key = (String) parameters[1];

				}

				indexProperty(rel, key);

				break;

			default :
				logger.log(Level.SEVERE, "Wrong number of parameters for the index relationship command: {0}", parameters);

				return null;

		}

		return null;
	}

	private void indexRelationships(final List<AbstractRelationship> rels)  throws FrameworkException {

		for (AbstractRelationship rel : rels) {

			indexRelationship(rel);

		}
	}

	private void indexRelationship(final AbstractRelationship rel) throws FrameworkException {

		// add a special type key, consisting of the relationship type, the type of the start node and the type of the end node
		String tripleKey = EntityContext.createTripleKey(rel.getStartNode().getType(), rel.getType(), rel.getEndNode().getType());
		rel.setProperty(AbstractRelationship.HiddenKey.type.name(), tripleKey);
		indexProperty(rel, AbstractRelationship.HiddenKey.type.name());

		for (String key : rel.getPropertyKeys()) {

			indexProperty(rel, key);

		}


	}

	private void indexProperty(final AbstractRelationship rel, final String key) {

		// String type = node.getClass().getSimpleName();
		Relationship dbRel = rel.getRelationship();
		long id     = rel.getId();

		if (key == null) {

			logger.log(Level.SEVERE, "Relationship {0} has null key", new Object[] { id });

			return;

		}

		boolean emptyKey = StringUtils.isEmpty((String) key);

		if (emptyKey) {

			logger.log(Level.SEVERE, "Relationship {0} has empty, not-null key, removing property", new Object[] { id });
			dbRel.removeProperty(key);

			return;

		}

		if (!(dbRel.hasProperty(key))) {

			removeRelationshipPropertyFromAllIndices(dbRel, key);
			logger.log(Level.FINE, "Relationship {0} has no key {1}, to be sure, it was removed from all indices", new Object[] { id, key });
			return;

		}

		Object value            = dbRel.getProperty(key);
		Object valueForIndexing = rel.getPropertyForIndexing(key);
		boolean emptyValue      = ((value instanceof String) && StringUtils.isEmpty((String) value));

		if (value == null) {

			logger.log(Level.FINE, "Node {0} has null value for key {1}, removing property", new Object[] { id, key });
			dbRel.removeProperty(key);
			removeRelationshipPropertyFromAllIndices(dbRel, key);

		} else if (emptyValue) {

			logger.log(Level.FINE, "Node {0} has empty, non-null value for key {1}, removing property", new Object[] { id, key });
			dbRel.removeProperty(key);
			removeRelationshipPropertyFromAllIndices(dbRel, key);

		} else {

			// index.remove(node, key, value);
			removeRelationshipPropertyFromAllIndices(dbRel, key);
			logger.log(Level.FINE, "Node {0}: Old value for key {1} removed from all indices", new Object[] { id, key });
			addRelationshipPropertyToFulltextIndex(dbRel, key, valueForIndexing);
			addRelationshipPropertyToKeywordIndex(dbRel, key, valueForIndexing);

			if (key.equals(AbstractRelationship.Key.uuid.name())) {

				addRelationshipPropertyToUuidIndex(dbRel, key, valueForIndexing);

			}

			logger.log(Level.FINE, "Node {0}: New value {2} added for key {1}", new Object[] { id, key, value });
		}
	}

	private void removeRelationshipPropertyFromAllIndices(final Relationship rel, final String key) {

		for (Enum indexName : (RelationshipIndex[]) arguments.get("relationshipIndices")) {

			indices.get(indexName.name()).remove(rel, key);

		}
	}

	private void addRelationshipPropertyToFulltextIndex(final Relationship rel, final String key, final Object value) {
		indices.get(RelationshipIndex.rel_fulltext.name()).add(rel, key, value);
	}

	private void addRelationshipPropertyToUuidIndex(final Relationship rel, final String key, final Object value) {
		indices.get(RelationshipIndex.rel_uuid.name()).add(rel, key, value);
	}

	private void addRelationshipPropertyToKeywordIndex(final Relationship rel, final String key, final Object value) {
		indices.get(RelationshipIndex.rel_keyword.name()).add(rel, key, value);
	}

}
