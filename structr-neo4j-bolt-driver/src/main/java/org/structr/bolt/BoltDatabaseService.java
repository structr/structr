/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.bolt;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.structr.api.DatabaseService;
import org.structr.api.NativeResult;
import org.structr.api.NotInTransactionException;
import org.structr.api.Transaction;
import org.structr.api.config.Structr;
import org.structr.api.graph.GraphProperties;
import org.structr.api.graph.Label;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.index.IndexManager;

/**
 *
 * @author Christian Morgner
 */
public class BoltDatabaseService implements DatabaseService, GraphProperties {

	private static final Logger logger                                = Logger.getLogger(BoltDatabaseService.class.getName());
	private static final Map<String, RelationshipType> relTypeCache   = new ConcurrentHashMap<>();
	private static final Map<String, Label> labelCache                = new ConcurrentHashMap<>();
	private static final ThreadLocal<SessionTransaction> sessions     = new ThreadLocal<>();
	private BoltRelationshipIndexer relationshipIndexer               = null;
	private BoltNodeIndexer nodeIndexer                               = null;
	private GraphDatabaseService graphDb                              = null;
	private String databasePath                                       = null;
	private Driver driver                                             = null;

	@Override
	public void initialize(final Properties configuration) {

		this.databasePath = configuration.getProperty(Structr.DATABASE_PATH);

		GraphDatabaseSettings.BoltConnector bolt = GraphDatabaseSettings.boltConnector( "0" );

		graphDb = new GraphDatabaseFactory()
		        .newEmbeddedDatabaseBuilder(new File(databasePath))
		        .setConfig( bolt.enabled, "true" )
		        .setConfig( bolt.address, "localhost:7689" )
		        .newGraphDatabase();

		driver = GraphDatabase.driver("bolt://localhost:7689",
			AuthTokens.basic("neo4j", "test"),
			Config.build().withEncryptionLevel(Config.EncryptionLevel.NONE).toConfig()
		);
	}

	@Override
	public void shutdown() {

		RelationshipWrapper.shutdownCache();
		NodeWrapper.shutdownCache();

		driver.close();
		graphDb.shutdown();
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


	@Override
	public Transaction beginTx() {

		SessionTransaction session = sessions.get();
		if (session == null || session.isClosed()) {

			session = new SessionTransaction(this, driver.session());
			sessions.set(session);
		}

		return session;
	}

	@Override
	public Node createNode() {
		return NodeWrapper.newInstance(this, getCurrentTransaction().getNode("CREATE (n) RETURN n", Collections.EMPTY_MAP));
	}

	@Override
	public Node getNodeById(final long id) {
		return NodeWrapper.newInstance(this, id);
	}

	@Override
	public Relationship getRelationshipById(final long id) {

		final SessionTransaction tx   = getCurrentTransaction();
		final Map<String, Object> map = new HashMap<>();

		map.put("id", id);

		final org.neo4j.driver.v1.types.Relationship rel = tx.getRelationship("CYPHER planner=rule MATCH ()-[r]-() WHERE ID(r) = {id} RETURN r", map);

		return RelationshipWrapper.newInstance(this, rel);

	}

	@Override
	public Iterable<Node> getAllNodes() {

		final SessionTransaction tx = getCurrentTransaction();
		final List<Node> nodes      = new LinkedList<>();

		for (final org.neo4j.driver.v1.types.Node node : tx.getNodes("MATCH (n) RETURN n", Collections.emptyMap())) {
			nodes.add(NodeWrapper.newInstance(this, node));
		}

		return nodes;
	}

	@Override
	public Iterable<Relationship> getAllRelationships() {

		final SessionTransaction tx   = getCurrentTransaction();
		final List<Relationship> rels = new LinkedList<>();

		for (final org.neo4j.driver.v1.types.Relationship rel : tx.getRelationships("MATCH ()-[r]-() RETURN r", Collections.emptyMap())) {
			rels.add(RelationshipWrapper.newInstance(this, rel));
		}

		return rels;
	}

	@Override
	public GraphProperties getGlobalProperties() {
		return this;
	}

	@Override
	public IndexManager<Node> nodeIndexer() {

		if (nodeIndexer == null) {
			nodeIndexer = new BoltNodeIndexer(this);
		}

		return nodeIndexer;
	}

	@Override
	public IndexManager<Relationship> relationshipIndexer() {

		if (relationshipIndexer == null) {
			relationshipIndexer = new BoltRelationshipIndexer(this);
		}

		return relationshipIndexer;
	}

	@Override
	public NativeResult execute(final String nativeQuery, final Map<String, Object> parameters) {
		return getCurrentTransaction().run(nativeQuery, parameters);
	}

	@Override
	public NativeResult execute(final String nativeQuery) {
		return execute(nativeQuery, Collections.EMPTY_MAP);
	}

	@Override
	public void invalidateCache() {
		// noop
	}

	public SessionTransaction getCurrentTransaction() {

		final SessionTransaction tx = sessions.get();
		if (tx == null || tx.isClosed()) {

			throw new NotInTransactionException("Not in transaction");
		}

		return tx;
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
			ioex.printStackTrace();
			logger.log(Level.WARNING, "Unable to write properties file");
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

	// ----- private methods -----
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
