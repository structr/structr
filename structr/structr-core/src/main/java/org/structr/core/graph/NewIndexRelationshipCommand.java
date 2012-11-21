/*
 *  Copyright (C) 2012 Axel Morgner
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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.index.lucene.ValueContext;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.property.PropertyKey;

/**
 *
 * @author Christian Morgner
 */
public class NewIndexRelationshipCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(NewIndexRelationshipCommand.class.getName());
	
	private Map<String, Index> indices = new HashMap<String, Index>();
	private boolean initialized        = false;
	
	public void updateRelationship(AbstractRelationship relationship) {
		
		init();
		
		removeRelationship(relationship);
		addRelationship(relationship);
		
	}
	
	public void addRelationship(AbstractRelationship relationship) {
		
		init();
		
		try {

			String uuid = relationship.getProperty(AbstractRelationship.uuid);
			Relationship dbRelationship = relationship.getRelationship();
			long id     = relationship.getId();


			// Don't touch non-structr relationship
			if (uuid == null) {

				return;

			}


			String combinedKey = relationship.getProperty(AbstractRelationship.combinedType);
			if (combinedKey == null) {

				AbstractNode startNode = relationship.getStartNode();
				AbstractNode endNode   = relationship.getEndNode();

				if(startNode != null && endNode != null) {

					// add a special combinedType key, consisting of the relationship combinedType, the combinedType of the start node and the combinedType of the end node
					String tripleKey = EntityContext.createCombinedRelationshipType(startNode.getType(), relationship.getType(), endNode.getType());

					relationship.setProperty(AbstractRelationship.combinedType, Search.clean(tripleKey));
					
					addRelationshipPropertyToIndex(dbRelationship, AbstractRelationship.combinedType, tripleKey, NodeService.RelationshipIndex.rel_fulltext.name());
					addRelationshipPropertyToIndex(dbRelationship, AbstractRelationship.combinedType, tripleKey, NodeService.RelationshipIndex.rel_keyword.name());

				} else {

					logger.log(Level.WARNING, "Unable to create combined type key, startNode or endNode was null!");
				}
			}

			
			
			Map<String, Set<PropertyKey>> searchablePropertyIndexMap = EntityContext.getSearchablePropertyMapForType(relationship.getClass());
			for (Entry<String, Set<PropertyKey>> entry : searchablePropertyIndexMap.entrySet()) {
				
				Set<PropertyKey> searchableProperties = entry.getValue();
				String indexName = entry.getKey();
				
				for (PropertyKey key : searchableProperties) {
					
					boolean emptyKey = StringUtils.isEmpty(key.dbName());

					if (emptyKey) {

						logger.log(Level.SEVERE, "Relationship {0} has empty, not-null key, removing property", new Object[] { id });
						dbRelationship.removeProperty(key.dbName());

						return;
					}

					Object valueForIndexing = relationship.getPropertyForIndexing(key);
					Object value            = relationship.getProperty(key);

					if ((value == null && key.databaseConverter(securityContext, null) == null) || (value != null && value instanceof String && StringUtils.isEmpty((String) value))) {
						valueForIndexing = SearchNodeCommand.IMPROBABLE_SEARCH_VALUE;
					}

					if (valueForIndexing != null) {

						addRelationshipPropertyToIndex(dbRelationship, key, valueForIndexing, indexName);
						
						if (key.equals(AbstractRelationship.uuid)) {
							addRelationshipPropertyToIndex(dbRelationship, key, valueForIndexing, NodeService.RelationshipIndex.rel_uuid.name());
						}
					}
				}
			}
			
		} catch(Throwable t) {
			
			t.printStackTrace();
			
			logger.log(Level.WARNING, "Unable to index relationship {0}: {1}", new Object[] { relationship.getRelationship().getId(), t.getMessage() } );
			
		}
		
	}
	
	public void removeRelationship(AbstractRelationship relationship) {
		
		init();
		
		Map<String, Set<PropertyKey>> searchablePropertyIndexMap = EntityContext.getSearchablePropertyMapForType(relationship.getClass());
	
		for (String indexName : searchablePropertyIndexMap.keySet()) {
			
			Index<Relationship> index = indices.get(indexName);
			synchronized(index) {
				
				index.remove(relationship.getRelationship());
			}
		}
	}
	
	private void init() {

		if (!initialized) {

			for (Enum indexName : (NodeService.RelationshipIndex[]) arguments.get("relationshipIndices")) {
				indices.put(indexName.name(), (Index<Relationship>) arguments.get(indexName.name()));

			}
			
			initialized = true;
		}
	}
	
	private void addRelationshipPropertyToIndex(final Relationship relationship, final PropertyKey key, final Object value, final String indexName) {
		
		if (value == null) {
			return;
		}
		Index<Relationship> index = indices.get(indexName);
		synchronized(index) {

			if (value instanceof Number) {
				
				index.add(relationship, key.dbName(), ValueContext.numeric((Number) value));
				
			} else {
				
				index.add(relationship, key.dbName(), value);
				
			}
		}
	}
}
