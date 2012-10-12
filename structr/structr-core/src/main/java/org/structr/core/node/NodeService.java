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



package org.structr.core.node;

import org.apache.commons.collections.map.LRUMap;

import org.neo4j.gis.spatial.indexprovider.LayerNodeIndex;
import org.neo4j.gis.spatial.indexprovider.SpatialIndexProvider;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.impl.lucene.LuceneIndexImplementation;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.RunnableService;
import org.structr.core.Services;
import org.structr.core.SingletonService;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Location;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author cmorgner
 */
public class NodeService implements SingletonService {

	private static final Logger logger                       = Logger.getLogger(NodeService.class.getName());
	private static final Map<String, AbstractNode> nodeCache = (Map<String, AbstractNode>) Collections.synchronizedMap(new LRUMap(100000));

	//~--- fields ---------------------------------------------------------

	private Index<Node> fulltextIndex               = null;
	private GraphDatabaseService graphDb            = null;
	private Index<Node> keywordIndex                = null;
	private Index<Node> layerIndex                  = null;
	private NodeFactory nodeFactory                 = null;
	private Index<Relationship> relFulltextIndex    = null;
	private Index<Relationship> relKeywordIndex     = null;
	private Index<Relationship> relUuidIndex        = null;
	private RelationshipFactory relationshipFactory = null;
	private Index<Node> userIndex                   = null;
	private Index<Node> caseInsensitiveUserIndex               = null;
	private Index<Node> uuidIndex                   = null;

	/** Dependent services */
	private Set<RunnableService> registeredServices = new HashSet<RunnableService>();
	private boolean isInitialized                   = false;

	//~--- constant enums -------------------------------------------------

	public static enum NodeIndex { uuid, user, caseInsensitiveUser, keyword, fulltext, layer }

	public static enum RelationshipIndex { rel_uuid, rel_keyword, rel_fulltext }

	//~--- methods --------------------------------------------------------

	// <editor-fold defaultstate="collapsed" desc="interface SingletonService">
	@Override
	public void injectArguments(Command command) {

		if (command != null) {

			command.setArgument("graphDb", graphDb);
			command.setArgument(NodeIndex.uuid.name(), uuidIndex);
			command.setArgument(NodeIndex.fulltext.name(), fulltextIndex);
			command.setArgument(NodeIndex.user.name(), userIndex);
			command.setArgument(NodeIndex.caseInsensitiveUser.name(), caseInsensitiveUserIndex);
			command.setArgument(NodeIndex.keyword.name(), keywordIndex);
			command.setArgument(NodeIndex.layer.name(), layerIndex);
			command.setArgument(RelationshipIndex.rel_uuid.name(), relUuidIndex);
			command.setArgument(RelationshipIndex.rel_fulltext.name(), relFulltextIndex);
			command.setArgument(RelationshipIndex.rel_keyword.name(), relKeywordIndex);
			//command.setArgument("nodeFactory", nodeFactory);
			command.setArgument("relationshipFactory", relationshipFactory);
			command.setArgument("filesPath", Services.getFilesPath());
			command.setArgument("indices", NodeIndex.values());
			command.setArgument("relationshipIndices", RelationshipIndex.values());

		}

	}

	@Override
	public void initialize(Map<String, String> context) {

//              String dbPath = (String) context.get(Services.DATABASE_PATH);
		String dbPath = Services.getDatabasePath();

		logger.log(Level.INFO, "Initializing database ({0}) ...", dbPath);

		if (graphDb != null) {

			logger.log(Level.INFO, "Database already running ({0}) ...", dbPath);

			return;

		}

		try {

			graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(dbPath).loadPropertiesFromFile(dbPath + "/neo4j.conf").newGraphDatabase();

		} catch (Throwable t) {

			logger.log(Level.INFO, "Database config {0}/neo4j.conf not found", dbPath);

			graphDb = new EmbeddedGraphDatabase(dbPath);

		}

		if (graphDb != null) {

			graphDb.registerTransactionEventHandler(EntityContext.getTransactionEventHandler());
		}

		if (graphDb == null) {

			logger.log(Level.SEVERE, "Database could not be started ({0}) ...", dbPath);

			return;

		}

		String filesPath = Services.getFilesPath();

		// check existence of files path
		File files = new File(filesPath);

		if (!files.exists()) {

			files.mkdir();
		}

		logger.log(Level.INFO, "Database ready.");
		logger.log(Level.FINE, "Initializing UUID index...");

		uuidIndex = graphDb.index().forNodes("uuidAllNodes", LuceneIndexImplementation.EXACT_CONFIG);

		logger.log(Level.FINE, "UUID index ready.");
		logger.log(Level.FINE, "Initializing user index...");

		userIndex = graphDb.index().forNodes("nameEmailAllUsers", LuceneIndexImplementation.EXACT_CONFIG);

		logger.log(Level.FINE, "Node Email index ready.");
		logger.log(Level.FINE, "Initializing exact email index...");

		caseInsensitiveUserIndex = graphDb.index().forNodes("caseInsensitiveAllUsers", MapUtil.stringMap( "provider", "lucene", "type", "exact", "to_lower_case", "true" ));

		logger.log(Level.FINE, "Node case insensitive node index ready.");
		logger.log(Level.FINE, "Initializing case insensitive fulltext node index...");

		fulltextIndex = graphDb.index().forNodes("fulltextAllNodes", LuceneIndexImplementation.FULLTEXT_CONFIG);

		logger.log(Level.FINE, "Fulltext node index ready.");
		logger.log(Level.FINE, "Initializing keyword node index...");

		keywordIndex = graphDb.index().forNodes("keywordAllNodes", LuceneIndexImplementation.EXACT_CONFIG);

		logger.log(Level.FINE, "Keyword node index ready.");
		logger.log(Level.FINE, "Initializing layer index...");

		final Map<String, String> config = new HashMap<String, String>();

		config.put(LayerNodeIndex.LAT_PROPERTY_KEY, Location.Key.latitude.name());
		config.put(LayerNodeIndex.LON_PROPERTY_KEY, Location.Key.longitude.name());
		config.put(SpatialIndexProvider.GEOMETRY_TYPE, LayerNodeIndex.POINT_PARAMETER);

		layerIndex = new LayerNodeIndex("layerIndex", graphDb, config);

		logger.log(Level.FINE, "Layer index ready.");
		logger.log(Level.FINE, "Initializing node factory...");

//		nodeFactory = new NodeFactory();
//
//		logger.log(Level.FINE, "Node factory ready.");
//		logger.log(Level.FINE, "Initializing relationship UUID index...");

		relUuidIndex = graphDb.index().forRelationships("uuidAllRelationships", LuceneIndexImplementation.EXACT_CONFIG);

		logger.log(Level.FINE, "Relationship UUID index ready.");
		logger.log(Level.FINE, "Initializing relationship index...");

		relFulltextIndex = graphDb.index().forRelationships("fulltextAllRelationships", LuceneIndexImplementation.FULLTEXT_CONFIG);

		logger.log(Level.FINE, "Relationship fulltext index ready.");
		logger.log(Level.FINE, "Initializing keyword relationship index...");

		relKeywordIndex = graphDb.index().forRelationships("keywordAllRelationships", LuceneIndexImplementation.EXACT_CONFIG);

		logger.log(Level.FINE, "Relationship numeric index ready.");
		logger.log(Level.FINE, "Initializing relationship factory...");

		relationshipFactory = new RelationshipFactory();

		logger.log(Level.FINE, "Relationship factory ready.");

		isInitialized = true;
	}

	@Override
	public void shutdown() {

		if (isRunning()) {

			for (RunnableService s : registeredServices) {

				s.stopService();
			}

			// Wait for all registered services to end
			waitFor(registeredServices.isEmpty());
			graphDb.shutdown();

			graphDb       = null;
			isInitialized = false;

		}

	}

	public void registerService(final RunnableService service) {

		registeredServices.add(service);

	}

	public void unregisterService(final RunnableService service) {

		registeredServices.remove(service);

	}

	private void waitFor(final boolean condition) {

		while (!condition) {

			try {

				Thread.sleep(10);

			} catch (Throwable t) {}

		}

	}

	public static void addNodeToCache(String uuid, AbstractNode node) {

		nodeCache.put(uuid, node);

	}

	public static void removeNodeFromCache(String uuid) {

		nodeCache.remove(uuid);

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getName() {

		return NodeService.class.getSimpleName();

	}

	public static AbstractNode getNodeFromCache(String uuid) {

		return nodeCache.get(uuid);

	}

	// </editor-fold>
	
	public GraphDatabaseService getGraphDb() {
		return graphDb;
	}
	
	@Override
	public boolean isRunning() {

		return ((graphDb != null) && isInitialized);

	}

}
