/**
 * Copyright (C) 2010-2017 Structr GmbH
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
package org.structr.neo4j;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Date;
import org.structr.neo4j.wrapper.TransactionWrapper;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.shell.ShellSettings;
import org.neo4j.tooling.GlobalGraphOperations;
import org.structr.api.DatabaseService;
import org.structr.api.graph.GraphProperties;
import org.structr.api.util.Iterables;
import org.structr.api.graph.Label;
import org.structr.api.NativeResult;
import org.structr.api.NotFoundException;
import org.structr.api.graph.Node;
import org.structr.api.NotInTransactionException;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.Transaction;
import org.structr.api.config.Structr;
import org.structr.api.index.Index;
import org.structr.api.index.IndexManager;
import org.structr.api.util.FixedSizeCache;
import org.structr.neo4j.mapper.NodeMapper;
import org.structr.neo4j.mapper.RelationshipMapper;
import org.structr.neo4j.wrapper.NodeWrapper;
import org.structr.neo4j.wrapper.RelationshipWrapper;
import org.structr.neo4j.wrapper.Neo4jResultWrapper;

/**
 *
 */
public class Neo4jDatabaseService implements DatabaseService, GraphProperties {

	private static final Logger logger = LoggerFactory.getLogger(Neo4jDatabaseService.class.getName());

	public static final String RELATIONSHIP_CACHE_SIZE  = "database.cache.relationship.size";
	public static final String NODE_CACHE_SIZE          = "database.cache.node.size";

	public static final String NEO4J_SHELL_ENABLED      = "neo4j.shell.enabled";
	public static final String NEO4J_SHELL_PORT         = "neo4j.shell.port";
	public static final String NEO4J_PAGE_CACHE_MEMORY  = "neo4j.pagecache.memory";


	private static final Map<String, RelationshipType> relTypeCache     = new ConcurrentHashMap<>();
	private static final Map<String, Label> labelCache                  = new ConcurrentHashMap<>();
	private FixedSizeCache<Long, RelationshipWrapper> relationshipCache = null;
	private FixedSizeCache<Long, NodeWrapper> nodeCache                 = null;
	private IndexManager relationshipIndexer                            = null;
	private IndexManager nodeIndexer                                    = null;
	private GraphDatabaseService graphDb                                = null;
	private String databasePath                                         = null;

	@Override
	public void initialize(final Properties config) {

		this.databasePath = config.getProperty(Structr.DATABASE_PATH);

		final int relationshipCacheSize = Integer.valueOf(config.getProperty(RELATIONSHIP_CACHE_SIZE, "10000"));
		if (relationshipCacheSize > 0) {

			logger.info("Relationship cache size set to {}", relationshipCacheSize);
			relationshipCache = new FixedSizeCache<>(relationshipCacheSize);

		} else {

			logger.info("Relationship cache disabled.");
		}

		final int nodeCacheSize = Integer.valueOf(config.getProperty(NODE_CACHE_SIZE, "10000"));
		if (nodeCacheSize > 0) {

			logger.info("Node cache size set to {}", nodeCacheSize);
			nodeCache = new FixedSizeCache<>(nodeCacheSize);

		} else {

			logger.info("Node cache disabled.");
		}

		final File confFile                = new File(databasePath + "/neo4j.conf");
		final GraphDatabaseBuilder builder = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(databasePath);

		// load additional settings
		if (confFile.exists()) {
			builder.loadPropertiesFromFile(confFile.getAbsolutePath());
		}

		// neo4j remote shell configuration
		builder.setConfig(ShellSettings.remote_shell_enabled, config.getProperty(NEO4J_SHELL_ENABLED, "false"));
		builder.setConfig(ShellSettings.remote_shell_port,    config.getProperty(NEO4J_SHELL_PORT,    "1337"));

		// Neo4j page cache memory, default 64m
		builder.setConfig(GraphDatabaseSettings.pagecache_memory, config.getProperty(NEO4J_PAGE_CACHE_MEMORY, "64m"));

		logger.info("Initializing database ({}) ...", databasePath);

		graphDb = builder.newGraphDatabase();
	}

	@Override
	public void shutdown() {

		graphDb.shutdown();
		nodeCache.clear();
		relationshipCache.clear();
	}

	@Override
	public Transaction beginTx() {
		return new TransactionWrapper(graphDb.beginTx());
	}

	@Override
	public Node createNode(final Set<String> labels, final Map<String, Object> properties) {

		try {
			return NodeWrapper.getWrapper(this, graphDb.createNode());

		} catch (org.neo4j.graphdb.NotInTransactionException t) {

			throw new NotInTransactionException(t);
		}
	}

	@Override
	public Node getNodeById(long id) {

		try {
			return NodeWrapper.getWrapper(this, graphDb.getNodeById(id));

		} catch (org.neo4j.graphdb.NotInTransactionException t) {

			throw new NotInTransactionException(t);

		} catch (org.neo4j.graphdb.NotFoundException t) {

			throw new NotFoundException(t);
		}
	}

	@Override
	public Relationship getRelationshipById(long id) {

		try {
			return RelationshipWrapper.getWrapper(this, graphDb.getRelationshipById(id));

		} catch (org.neo4j.graphdb.NotInTransactionException t) {

			throw new NotInTransactionException(t);

		} catch (org.neo4j.graphdb.NotFoundException t) {

			throw new NotFoundException(t);
		}
	}

	@Override
	public Iterable<Node> getAllNodes() {

		try {

			return Iterables.map(new NodeMapper(this), GlobalGraphOperations.at(graphDb).getAllNodes());

		} catch (org.neo4j.graphdb.NotInTransactionException t) {

			throw new NotInTransactionException(t);

		} catch (org.neo4j.graphdb.NotFoundException t) {

			throw new NotFoundException(t);
		}
	}

	@Override
	public Iterable<Relationship> getAllRelationships() {

		try {

			return Iterables.map(new RelationshipMapper(this), GlobalGraphOperations.at(graphDb).getAllRelationships());

		} catch (org.neo4j.graphdb.NotInTransactionException t) {

			throw new NotInTransactionException(t);

		} catch (org.neo4j.graphdb.NotFoundException t) {

			throw new NotFoundException(t);
		}
	}

	@Override
	public GraphProperties getGlobalProperties() {
		return this;
	}

	@Override
	public Index<Node> nodeIndex() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Index<Relationship> relationshipIndex() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public NativeResult execute(final String nativeQuery) {

		try {

			return new Neo4jResultWrapper<>(this, graphDb.execute(nativeQuery));

		} catch (org.neo4j.graphdb.NotInTransactionException t) {

			throw new NotInTransactionException(t);
		}
	}

	@Override
	public NativeResult execute(final String nativeQuery, final Map<String, Object> parameters) {

		try {

			return new Neo4jResultWrapper<>(this, graphDb.execute(nativeQuery, parameters));

		} catch (org.neo4j.graphdb.NotInTransactionException t) {

			throw new NotInTransactionException(t);
		}
	}

	@Override
	public <T> T forName(final Class<T> type, final String name) {

		if (Label.class.equals(type)) {

			return (T)getOrCreateLabel(name);
		}

		if (RelationshipType.class.equals(type)) {

			return (T)getOrCreateRelationshipType(name);
		}

		throw new RuntimeException("Cannot create object of type " + type);
	}

	public Label getOrCreateLabel(final String name) {

		Label label = labelCache.get(name);
		if (label == null) {

			label = new LabelImpl(name);
			labelCache.put(name, label);
		}

		return label;
	}

	public RelationshipType getOrCreateRelationshipType(final String name) {

		RelationshipType relType = relTypeCache.get(name);
		if (relType == null) {

			relType = new RelationshipTypeImpl(name);
			relTypeCache.put(name, relType);
		}

		return relType;
	}

	public GraphDatabaseService getGraphDb() {
		return graphDb;
	}

	public NodeWrapper getNodeFromCache(final long id) {

		if (nodeCache != null) {
			return nodeCache.get(id);
		}

		return null;
	}

	public void storeNodeInCache(final NodeWrapper node) {

		if (nodeCache != null) {
			nodeCache.put(node.getId(), node);
		}
	}

	public void removeNodeFromCache(final long id) {

		if (nodeCache != null) {
			nodeCache.remove(id);
		}
	}

	public RelationshipWrapper getRelationshipFromCache(final long id) {

		if (relationshipCache != null) {
			return relationshipCache.get(id);
		}

		return null;
	}

	public void storeRelationshipInCache(final RelationshipWrapper relationship) {

		if (relationshipCache != null) {
			relationshipCache.put(relationship.getId(), relationship);
		}
	}

	public void removeRelationshipFromCache(final long id) {

		if (relationshipCache != null) {
			relationshipCache.remove(id);
		}
	}

	// ----- interface GraphProperties -----
	@Override
	public void setProperty(final String name, final Object value) {

		final Properties properties = new Properties();
		final File propertiesFile   = new File(databasePath + "/graph.properties");

		try (final Reader reader = new FileReader(propertiesFile)) {
			properties.load(reader);
		} catch (IOException ioex) {}

		properties.setProperty(name, value.toString());

		try (final Writer writer = new FileWriter(propertiesFile)) {
			properties.store(writer, "Created by Structr at " + new Date());
		} catch (IOException ioex) {
			logger.warn("Unable to write properties file", ioex);
		}
	}

	@Override
	public Object getProperty(final String name) {

		final Properties properties = new Properties();
		final File propertiesFile   = new File(databasePath + "/graph.properties");

		try (final Reader reader = new FileReader(propertiesFile)) {
			properties.load(reader);
		} catch (IOException ioex) {}

		return properties.getProperty(name);
	}

	@Override
	public void invalidateCache() {

		if (nodeCache != null) {
			nodeCache.clear();
		}

		if (relationshipCache != null) {
			relationshipCache.clear();
		}

	}

	@Override
	public boolean needsIndexRebuild() {
		return false;
	}

	// ----- nested classes -----
	private static class LabelImpl implements Label {

		private String name = null;

		private LabelImpl(final String name) {
			this.name = name;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public boolean equals(final Object other) {

			if (other instanceof Label) {
				return other.hashCode() == hashCode();
			}

			return false;
		}
	}

	private static class RelationshipTypeImpl implements RelationshipType {

		private String name = null;

		private RelationshipTypeImpl(final String name) {
			this.name = name;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public boolean equals(final Object other) {

			if (other instanceof RelationshipType) {
				return other.hashCode() == hashCode();
			}

			return false;
		}
	}
}
