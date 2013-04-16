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
package org.structr.core.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.neo4j.gis.spatial.indexprovider.LayerNodeIndex;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.index.Index;
import org.neo4j.index.lucene.ValueContext;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Location;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.property.PropertyKey;

/**
 * Indexes nodes.
 * 
 * @author Christian Morgner
 */
public class NewIndexNodeCommand extends NodeServiceCommand {

	private static final Logger logger = Logger.getLogger(NewIndexNodeCommand.class.getName());
	
	private Map<String, Index> indices = new HashMap<String, Index>();
	private boolean initialized        = false;
	
	public void updateNode(AbstractNode node) {
		
		init();
		
		removeNode(node);
		addNode(node);
		
	}
	
	public void addNode(AbstractNode node) {
		
		init();
		
		try {

			String uuid = node.getProperty(AbstractNode.uuid);
			Node dbNode = node.getNode();
			long id     = node.getId();


			// Don't touch non-structr node
			if (uuid == null) {

				return;

			}

			Map<String, Set<PropertyKey>> searchablePropertyIndexMap = EntityContext.getSearchablePropertyMapForType(node.getClass());
			for (Entry<String, Set<PropertyKey>> entry : searchablePropertyIndexMap.entrySet()) {
				
				Set<PropertyKey> searchableProperties = entry.getValue();
				String indexName = entry.getKey();
				
				for (PropertyKey key : searchableProperties) {
					
					boolean emptyKey = StringUtils.isEmpty(key.dbName());

					if (emptyKey) {

						logger.log(Level.SEVERE, "Node {0} has empty, not-null key, removing property", new Object[] { id });
						dbNode.removeProperty(key.dbName());

						return;
					}

					Object valueForIndexing = node.getPropertyForIndexing(key);
					Object value            = node.getProperty(key);

					if ((value == null && key.databaseConverter(securityContext, null) == null) || (value != null && value instanceof String && StringUtils.isEmpty((String) value))) {
						valueForIndexing = SearchNodeCommand.IMPROBABLE_SEARCH_VALUE;
					}

					if (valueForIndexing != null) {

						addNodePropertyToIndex(dbNode, key, valueForIndexing, indexName);
						
						/*
						if ((node instanceof Principal) && (key.equals(AbstractNode.name) || key.equals(Person.email))) {
							addNodePropertyToIndex(dbNode, key, valueForIndexing, NodeService.NodeIndex.user.name());
						}

						if (key.equals(AbstractNode.uuid)) {
							addNodePropertyToIndex(dbNode, key, valueForIndexing, NodeService.NodeIndex.uuid.name());
						}
						*/
					}
				}
			}
			
			if ((dbNode.hasProperty(Location.latitude.dbName())) && (dbNode.hasProperty(Location.longitude.dbName()))) {
				
				// Before indexing, check properties for correct type
				Object lat = dbNode.getProperty(Location.latitude.dbName());
				Object lon = dbNode.getProperty(Location.longitude.dbName());
				
				if (lat instanceof Double && lon instanceof Double && !((Double) lat).isNaN() && !((Double) lon).isNaN()) {

					LayerNodeIndex layerIndex = (LayerNodeIndex) indices.get(NodeService.NodeIndex.layer.name());

					try {

						synchronized (layerIndex) {

							layerIndex.add(dbNode, "", "");
						}

						// If an exception is thrown here, the index was deleted
						// and has to be recreated.
					} catch (NotFoundException nfe) {

						logger.log(Level.SEVERE, "Could not add node to layer index because the db could not find the node", nfe);

					} catch (Throwable t) {

						logger.log(Level.SEVERE, "Could not add node to layer index", t);
					}
				
				}

			}
			
		} catch(Throwable t) {
			
			t.printStackTrace();
			
			logger.log(Level.WARNING, "Unable to index node {0}: {1}", new Object[] { node.getNode().getId(), t.getMessage() } );
			
		}
		
	}
	
	public void removeNode(AbstractNode node) {
		
		init();
		
		Map<String, Set<PropertyKey>> searchablePropertyIndexMap = EntityContext.getSearchablePropertyMapForType(node.getClass());
	
		for (String indexName : searchablePropertyIndexMap.keySet()) {
			
			Index<Node> index = indices.get(indexName);
			synchronized(index) {
				
				index.remove(node.getNode());
			}
		}
	}
	
	private void init() {

		if (!initialized) {

			for (Enum indexName : (NodeService.NodeIndex[]) arguments.get("indices")) {
				indices.put(indexName.name(), (Index<Node>) arguments.get(indexName.name()));

			}
			
			initialized = true;
		}
	}
	
	private void addNodePropertyToIndex(final Node node, final PropertyKey key, final Object value, final String indexName) {
		
		if (value == null) {
			return;
		}

		Index<Node> index = indices.get(indexName);
		synchronized(index) {

//			if (value instanceof Number) {
//				
//				index.add(node, key.dbName(), ValueContext.numeric((Number) value));
//				
//			} else {
				
				index.add(node, key.dbName(), value);
				
//			}
		}
	}
}
