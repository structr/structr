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

import org.apache.commons.lang.StringUtils;

import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;

import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.NodeService.RelationshipIndex;
import org.structr.core.graph.search.Search;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.property.PropertyKey;
import org.structr.core.entity.AbstractNode;

//~--- classes ----------------------------------------------------------------

/**
 * Indexes relationships.
 *
 * @author Axel Morgner
 */
public class IndexRelationshipCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(IndexRelationshipCommand.class.getName());

	//~--- fields ---------------------------------------------------------

	private Map<String, Index> indices = new HashMap<String, Index>();

	//~--- methods --------------------------------------------------------
	
	public void execute(AbstractRelationship relationship, PropertyKey propertyKey) throws FrameworkException {

		init();
		indexProperty(relationship, propertyKey);
	}
	
	public void execute(AbstractRelationship relationship) throws FrameworkException {

		init();
		indexRelationship(relationship);
	}

	public void execute(List<AbstractRelationship> relationships) throws FrameworkException {

		init();
		indexRelationships(relationships);
	}

	private void indexRelationships(final List<AbstractRelationship> rels) throws FrameworkException {

		for (AbstractRelationship rel : rels) {

			indexRelationship(rel);

		}
	}

	private void init() {

		for (Enum indexName : (RelationshipIndex[]) arguments.get("relationshipIndices")) {
			indices.put(indexName.name(), (Index<Relationship>) arguments.get(indexName.name()));

		}
		
	}

	private void indexRelationship(final AbstractRelationship rel) throws FrameworkException {

		String uuid = rel.getProperty(AbstractRelationship.uuid);

		// Don't index non-structr relationship
		if (uuid == null) {

			return;

		}

		String combinedKey = rel.getProperty(AbstractRelationship.combinedType);

		if (combinedKey == null) {

			AbstractNode startNode = rel.getStartNode();
			AbstractNode endNode   = rel.getEndNode();

			if(startNode != null && endNode != null) {
				
				// add a special combinedType key, consisting of the relationship combinedType, the combinedType of the start node and the combinedType of the end node
				String tripleKey = EntityContext.createCombinedRelationshipType(startNode.getType(), rel.getType(), endNode.getType());

				rel.setProperty(AbstractRelationship.combinedType, Search.clean(tripleKey));
				indexProperty(rel, AbstractRelationship.combinedType);
				
			} else {
				
				logger.log(Level.WARNING, "Unable to create combined type key, startNode or endNode was null!");
			}
		}

		for (PropertyKey key : rel.getPropertyKeys()) {

			indexProperty(rel, key);

		}
	}

	private void indexProperty(final AbstractRelationship rel, final PropertyKey key) {

		// String combinedType = node.getClass().getSimpleName();
		Relationship dbRel = rel.getRelationship();
		long id            = rel.getId();

		if (key == null) {

			logger.log(Level.SEVERE, "Relationship {0} has null key", new Object[] { id });

			return;

		}

		boolean emptyKey = StringUtils.isEmpty(key.dbName());

		if (emptyKey) {

			logger.log(Level.SEVERE, "Relationship {0} has empty, not-null key, removing property", new Object[] { id });
			dbRel.removeProperty(key.dbName());

			return;

		}

		/*
		 * if (!(dbRel.hasProperty(key))) {
		 *
		 *       removeRelationshipPropertyFromAllIndices(dbRel, key);
		 *       logger.log(Level.FINE, "Relationship {0} has no key {1}, to be sure, it was removed from all indices", new Object[] { id, key });
		 *       return;
		 *
		 * }
		 */
		Object value            = rel.getProperty(key);    // dbRel.getProperty(key);
		Object valueForIndexing = rel.getPropertyForIndexing(key);
		boolean emptyValue      = ((value instanceof String) && StringUtils.isEmpty((String) value));

		/*
		logger.log(Level.INFO, "Indexing key {0} with value {1} of ID {2}", new Object[] {
			key,
			value != null ? value : "null",
			rel.getProperty("uuid")
		} );
		*/
		
		if (value == null) {

			logger.log(Level.FINE, "Node {0} has null value for key {1}, removing property", new Object[] { id, key });
			dbRel.removeProperty(key.dbName());
			removeRelationshipPropertyFromAllIndices(dbRel, key);

		} else if (emptyValue) {

			logger.log(Level.FINE, "Node {0} has empty, non-null value for key {1}, removing property", new Object[] { id, key });
			dbRel.removeProperty(key.dbName());
			removeRelationshipPropertyFromAllIndices(dbRel, key);

		} else {

			// index.remove(node, key, value);
			removeRelationshipPropertyFromAllIndices(dbRel, key);
			logger.log(Level.FINE, "Node {0}: Old value for key {1} removed from all indices", new Object[] { id, key });
			addRelationshipPropertyToFulltextIndex(dbRel, key, valueForIndexing);
			addRelationshipPropertyToKeywordIndex(dbRel, key, valueForIndexing);

			if (key.equals(AbstractRelationship.uuid)) {

				addRelationshipPropertyToUuidIndex(dbRel, key, valueForIndexing);

			}

			logger.log(Level.FINE, "Node {0}: New value {2} added for key {1}", new Object[] { id, key, value });
		}
	}

	private void removeRelationshipPropertyFromAllIndices(final Relationship rel, final PropertyKey key) {

		for (Enum indexName : (RelationshipIndex[]) arguments.get("relationshipIndices")) {

			indices.get(indexName.name()).remove(rel, key.dbName());

		}
	}

	private void addRelationshipPropertyToFulltextIndex(final Relationship rel, final PropertyKey key, final Object value) {
		Index<Relationship> index = indices.get(RelationshipIndex.rel_fulltext.name());
		synchronized(index) {
			index.add(rel, key.dbName(), value);
		}
	}

	private void addRelationshipPropertyToUuidIndex(final Relationship rel, final PropertyKey key, final Object value) {
		Index<Relationship> index = indices.get(RelationshipIndex.rel_uuid.name());
		synchronized(index) {
			index.add(rel, key.dbName(), value);
		}
	}

	private void addRelationshipPropertyToKeywordIndex(final Relationship rel, final PropertyKey key, final Object value) {
		Index<Relationship> index = indices.get(RelationshipIndex.rel_keyword.name());
		synchronized(index) {
			index.add(rel, key.dbName(), value);
		}
	}
}
