/**
 * Copyright (C) 2010-2019 Structr GmbH
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang.StringUtils;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.exceptions.ClientException;
import org.neo4j.driver.v1.exceptions.DatabaseException;
import org.neo4j.driver.v1.exceptions.ServiceUnavailableException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.kernel.configuration.BoltConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.AbstractDatabaseService;
import org.structr.api.NetworkException;
import org.structr.api.NotInTransactionException;
import org.structr.api.Transaction;
import org.structr.api.config.Settings;
import org.structr.api.graph.GraphProperties;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Label;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.api.index.Index;
import org.structr.api.search.ExactQuery;
import org.structr.api.search.Occurrence;
import org.structr.api.search.QueryContext;
import org.structr.api.search.QueryPredicate;
import org.structr.api.search.SortOrder;
import org.structr.api.search.TypeQuery;
import org.structr.api.util.CountResult;
import org.structr.api.util.NodeWithOwnerResult;
import org.structr.bolt.index.CypherNodeIndex;
import org.structr.bolt.index.CypherRelationshipIndex;
import org.structr.bolt.wrapper.BoltIdentity;
import org.structr.bolt.wrapper.NodeWrapper;
import org.structr.bolt.wrapper.RelationshipWrapper;

/**
 *
 */
public class BoltDatabaseService extends AbstractDatabaseService implements GraphProperties {

	private static final Logger logger                                = LoggerFactory.getLogger(BoltDatabaseService.class.getName());
	private static final Map<String, RelationshipType> relTypeCache   = new ConcurrentHashMap<>();
	private static final Map<String, Label> labelCache                = new ConcurrentHashMap<>();
	private static final ThreadLocal<SessionTransaction> sessions     = new ThreadLocal<>();
	private static final long nanoEpoch                               = System.nanoTime();
	private Properties globalGraphProperties                          = null;
	private CypherRelationshipIndex relationshipIndex                 = null;
	private CypherNodeIndex nodeIndex                                 = null;
	private GraphDatabaseService graphDb                              = null;
	private String databaseUrl                                        = null;
	private String databasePath                                       = null;
	private Driver driver                                             = null;

	@Override
	public boolean initialize() {

		this.databasePath = Settings.DatabasePath.getValue();

		final BoltConnector bolt = new BoltConnector("0");
		databaseUrl              = Settings.ConnectionUrl.getValue();
		final String username    = Settings.ConnectionUser.getValue();
		final String password    = Settings.ConnectionPassword.getValue();
		final String driverMode  = Settings.DatabaseDriverMode.getValue();
		final String confPath    = databasePath + "/neo4j.conf";
		final File confFile      = new File(confPath);

		// see https://github.com/neo4j/neo4j-java-driver/issues/364 for an explanation
		final String databaseServerUrl;
		final String databaseDriverUrl;

		if (databaseUrl.length() >= 7 && databaseUrl.substring(0, 7).equalsIgnoreCase("bolt://")) {
			databaseServerUrl = databaseUrl.substring(7);
			databaseDriverUrl = databaseUrl;
		} else if (databaseUrl.length() >= 15 && databaseUrl.substring(0, 15).equalsIgnoreCase("bolt+routing://")) {
			databaseServerUrl = databaseUrl.substring(15);
			databaseDriverUrl = databaseUrl;
		} else {
			databaseServerUrl = databaseUrl;
			databaseDriverUrl = "bolt://" + databaseUrl;
		}

		// create db directory if it does not exist
		new File(databasePath).mkdirs();

		if (!"remote".equals(driverMode)) {

			final GraphDatabaseBuilder builder = new GraphDatabaseFactory()
				.newEmbeddedDatabaseBuilder(new File(databasePath))
				.setConfig( GraphDatabaseSettings.allow_upgrade, "true")
				.setConfig( bolt.type, "BOLT" )
				.setConfig( bolt.enabled, "true" )
				.setConfig( bolt.listen_address, databaseServerUrl);

			if (confFile.exists()) {
				builder.loadPropertiesFromFile(confPath);
			}

			graphDb  = builder.newGraphDatabase();
		}

		try {

			driver = GraphDatabase.driver(databaseDriverUrl,
				AuthTokens.basic(username, password),
				Config.build().withEncryption().toConfig()
			);

			final int relCacheSize  = Settings.RelationshipCacheSize.getValue();
			final int nodeCacheSize = Settings.NodeCacheSize.getValue();

			NodeWrapper.initialize(nodeCacheSize);
			logger.info("Node cache size set to {}", nodeCacheSize);

			RelationshipWrapper.initialize(relCacheSize);
			logger.info("Relationship cache size set to {}", relCacheSize);

			// drop :NodeInterface index and create uniqueness constraint
			// disabled, planned for Structr 2.4
			//createUUIDConstraint();

			createGlobalSchemaLockNode();

			// signal success
			return true;

		} catch (ServiceUnavailableException ex) {
			logger.error("Neo4j service is not available.");
		}

		// service failed to initialize
		return false;
	}

	@Override
	public void shutdown() {

		clearCaches();
		driver.close();

		if (graphDb != null) {
			graphDb.shutdown();
		}
	}

	@Override
	public Transaction beginTx() {

		SessionTransaction session = sessions.get();
		if (session == null || session.isClosed()) {

			try {
				session = new SessionTransaction(this, driver.session());
				sessions.set(session);

			} catch (ServiceUnavailableException ex) {
				throw new NetworkException(ex.getMessage(), ex);
			} catch (ClientException cex) {
				logger.warn("Cannot connect to Neo4j database server at {}: {}", databaseUrl, cex.getMessage());
			}
		}

		return session;
	}

	@Override
	public Node createNode(final String type, final Set<String> labels, final Map<String, Object> properties) {

		final StringBuilder buf       = new StringBuilder("CREATE (n");
		final Map<String, Object> map = new HashMap<>();
		final String tenantId         = getTenantIdentifier();

		if (tenantId != null) {

			buf.append(":");
			buf.append(tenantId);
		}

		for (final String label : labels) {

			buf.append(":");
			buf.append(label);
		}

		buf.append(" $properties) RETURN n");

		// make properties available to Cypher statement
		map.put("properties", properties);

		final NodeWrapper newNode = NodeWrapper.newInstance(this, getCurrentTransaction().getNode(buf.toString(), map));

		newNode.setModified();

		return newNode;
	}

	@Override
	public NodeWithOwnerResult createNodeWithOwner(final Identity userId, final String type, final Set<String> labels, final Map<String, Object> nodeProperties, final Map<String, Object> ownsProperties, final Map<String, Object> securityProperties) {

		final Map<String, Object> parameters = new HashMap<>();
		final StringBuilder buf              = new StringBuilder();
		final String tenantId         = getTenantIdentifier();

		buf.append("MATCH (u:NodeInterface:Principal");

		if (tenantId != null) {

			buf.append(":");
			buf.append(tenantId);
		}

		buf.append(") WHERE ID(u) = $userId");
		buf.append(" CREATE (u)-[o:OWNS $ownsProperties]->(n");

		if (tenantId != null) {

			buf.append(":");
			buf.append(tenantId);
		}

		for (final String label : labels) {

			buf.append(":");
			buf.append(label);
		}

		buf.append(" $nodeProperties)<-[s:SECURITY $securityProperties]-(u)");
		buf.append(" RETURN n, s, o");

		// store properties in statement
		parameters.put("userId",             unwrap(userId));
		parameters.put("ownsProperties",     ownsProperties);
		parameters.put("securityProperties", securityProperties);
		parameters.put("nodeProperties",     nodeProperties);

		try {

			for (final Map<String, Object> data : execute(buf.toString(), parameters)) {

				final NodeWrapper newNode             = (NodeWrapper)         data.get("n");
				final RelationshipWrapper securityRel = (RelationshipWrapper) data.get("s");
				final RelationshipWrapper ownsRel     = (RelationshipWrapper) data.get("o");

				newNode.setModified();

				securityRel.setModified();
				securityRel.stale();

				ownsRel.setModified();
				ownsRel.stale();

				((NodeWrapper)ownsRel.getStartNode()).setModified();

				return new NodeWithOwnerResult(newNode, securityRel, ownsRel);
			}

		} catch (ClientException dex) {
			throw SessionTransaction.translateClientException(dex);
		} catch (DatabaseException dex) {
			throw SessionTransaction.translateDatabaseException(dex);
		}

		return null;
	}

	@Override
	public Node getNodeById(final Identity id) {
		return getNodeById(unwrap(id));
	}

	@Override
	public Relationship getRelationshipById(final Identity id) {
		return getRelationshipById(unwrap(id));
	}

	@Override
	public Iterable<Node> getAllNodes() {

		final QueryContext context     = new QueryContext(true);
		final QueryPredicate predicate = new TypePredicate();
		final Index<Node> index        = nodeIndex();

		return index.query(context, predicate, Integer.MAX_VALUE, 1);
	}

	@Override
	public Iterable<Node> getNodesByLabel(final String type) {

		if (type == null) {
			return getAllNodes();
		}

		final QueryContext context     = new QueryContext(true);
		final QueryPredicate predicate = new TypePredicate(type);
		final Index<Node> index        = nodeIndex();

		return index.query(context, predicate, Integer.MAX_VALUE, 1);
	}

	@Override
	public Iterable<Node> getNodesByTypeProperty(final String type) {

		if (type == null) {
			return getAllNodes();
		}

		final QueryContext context     = new QueryContext(true);
		final QueryPredicate predicate = new TypePropertyPredicate(type);
		final Index<Node> index        = nodeIndex();

		return index.query(context, predicate, Integer.MAX_VALUE, 1);
	}

	@Override
	public void deleteNodesByLabel(final String label) {

		final StringBuilder buf = new StringBuilder();
		final String tenantId   = getTenantIdentifier();

		buf.append("MATCH (n");

		if (tenantId != null) {
			buf.append(":");
			buf.append(tenantId);
		}

		buf.append(":");
		buf.append(label);
		buf.append(") DETACH DELETE n");

		execute(buf.toString());
	}

	@Override
	public Iterable<Relationship> getAllRelationships() {

		final Index<Relationship> index = relationshipIndex();
		final QueryPredicate predicate  = new TypePredicate();
		final QueryContext context      = new QueryContext(true);

		return index.query(context, predicate, Integer.MAX_VALUE, 1);
	}

	@Override
	public Iterable<Relationship> getRelationshipsByType(final String type) {

		if (type == null) {
			return getAllRelationships();
		}

		final Index<Relationship> index = relationshipIndex();
		final QueryPredicate predicate  = new TypePredicate(type);
		final QueryContext context      = new QueryContext(true);

		return index.query(context, predicate, Integer.MAX_VALUE, 1);
	}

	@Override
	public GraphProperties getGlobalProperties() {
		return this;
	}

	@Override
	public Index<Node> nodeIndex() {

		if (nodeIndex == null) {
			nodeIndex = new CypherNodeIndex(this);
		}

		return nodeIndex;
	}

	@Override
	public Index<Relationship> relationshipIndex() {

		if (relationshipIndex == null) {
			relationshipIndex = new CypherRelationshipIndex(this);
		}

		return relationshipIndex;
	}

	@Override
	public void updateIndexConfiguration(final Map<String, Map<String, Boolean>> schemaIndexConfigSource, final Map<String, Map<String, Boolean>> removedClassesSource, final boolean createOnly) {

		final Map<String, Map<String, Boolean>> schemaIndexConfig = new TreeMap<>(schemaIndexConfigSource);
		final Map<String, Map<String, Boolean>> removedClasses    = new TreeMap<>(removedClassesSource);
		final Map<String, String> existingDbIndexes               = new TreeMap<>();

		try (final Session session = driver.session()) {

			for (final Record record : session.run("CALL db.indexes() YIELD description, state, type WHERE type = 'node_label_property' RETURN {description: description, state: state}").list()) {

				for (final Object value : record.asMap().values()) {

					final Map<String, String> valueMap = (Map<String, String>)value;

					existingDbIndexes.put(valueMap.get("description"), valueMap.get("state"));
				}
			}
		}

		logger.debug("Found {} existing indexes", existingDbIndexes.size());

		Integer createdIndexes = 0;
		Integer droppedIndexes = 0;

		// create indices for properties of existing classes
		for (final Map.Entry<String, Map<String, Boolean>> entry : schemaIndexConfig.entrySet()) {

			final Map<String, Boolean> values = new TreeMap<>(entry.getValue());
			final String typeName             = entry.getKey();

			for (final Map.Entry<String, Boolean> propertyIndexConfig : values.entrySet()) {

				final String indexDescription = "INDEX ON :" + typeName + "(" + propertyIndexConfig.getKey() + ")";
				final String state            = existingDbIndexes.get(indexDescription);
				final boolean alreadySet      = Boolean.TRUE.equals("ONLINE".equals(state));
				final boolean createIndex     = propertyIndexConfig.getValue();

				if ("FAILED".equals(state)) {

					logger.warn("Index is in FAILED state - dropping the index before handling it further. {}. If this error is recurring, please verify that the data in the concerned property is indexable by Neo4j", indexDescription);

					try (final Session session = driver.session()) {
						if (hasIndex(session, indexDescription)) {
							session.run("DROP " + indexDescription);
						}
					} catch (Throwable ignore) {}
				}

				if (createIndex) {

					if (!alreadySet) {

						try (final Session session = driver.session()) {

							session.run("CREATE " + indexDescription);
							createdIndexes++;
						} catch (Throwable ignore) {}
					}

				} else if (alreadySet && !createOnly) {

					try (final Session session = driver.session()) {
						if (hasIndex(session, indexDescription)) {
							session.run("DROP " + indexDescription);
							droppedIndexes++;
						}
					} catch (Throwable ignore) {}
				}
			}
		}

		if (createdIndexes > 0) {
			logger.debug("Created {} indexes", createdIndexes);
		}

		if (droppedIndexes > 0) {
			logger.debug("Dropped {} indexes", droppedIndexes);
		}

		if (!createOnly) {

			Integer droppedIndexesOfRemovedTypes = 0;
			final List removedTypes = new LinkedList();

			// drop indices for all indexed properties of removed classes
			for (final Map.Entry<String, Map<String, Boolean>> entry : removedClasses.entrySet()) {

				final Map<String, Boolean> values = new TreeMap<>(entry.getValue());
				final String typeName             = entry.getKey();

				removedTypes.add(typeName);

				for (final Map.Entry<String, Boolean> propertyIndexConfig : values.entrySet()) {

					final String indexDescription = "INDEX ON :" + typeName + "(" + propertyIndexConfig.getKey() + ")";
					final boolean indexExists     = Boolean.TRUE.equals(existingDbIndexes.get(indexDescription));
					final boolean dropIndex       = propertyIndexConfig.getValue();

					if (indexExists && dropIndex) {

						try (final Session session = driver.session()) {
							if (hasIndex(session, indexDescription)) {
								session.run("DROP " + indexDescription);
								droppedIndexesOfRemovedTypes++;
							}
						} catch (Throwable ignore) {}
					}
				}
			}

			if (droppedIndexesOfRemovedTypes > 0) {
				logger.debug("Dropped {} indexes of deleted types ({})", droppedIndexesOfRemovedTypes, StringUtils.join(removedTypes, ", "));
			}
		}

		// create final index that indicates that index generation is complete (this one will never be dropped)
		try (final Session session = driver.session()) {
			session.run("CREATE INDEX ON :StructrIndexCreationFinished(name)");
		} catch (Throwable ignore) {}
	}

	@Override
	public boolean isIndexUpdateFinished() {

		try (final Session session = driver.session()) {

			final StatementResult result = session.run("CALL db.indexes() YIELD state, description WHERE description = 'INDEX ON :StructrIndexCreationFinished(name)' RETURN state");
			if (result.hasNext()) {

				final Record record                = result.next();
				final Map<String, Object> valueMap = record.asMap();

				return "ONLINE".equals(valueMap.get("state"));
			}

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return false;
	}

	@Override
	public Iterable<Map<String, Object>> execute(final String nativeQuery) {
		return execute(nativeQuery, Collections.EMPTY_MAP);
	}

	@Override
	public Iterable<Map<String, Object>> execute(final String nativeQuery, final Map<String, Object> parameters) {
		return getCurrentTransaction().run(nativeQuery, parameters);
	}

	@Override
	public void clearCaches() {

		NodeCacheAccess.clearAllCaches();
		RelationshipCacheAccess.clearAllCaches();
	}

	@Override
	public void cleanDatabase() {

		final String tenantId = getTenantIdentifier();
		if (tenantId != null) {

			execute("MATCH (n:" + tenantId + ") DETACH DELETE n", Collections.emptyMap());

		} else {

			execute("MATCH (n) DETACH DELETE n", Collections.emptyMap());
		}
	}

	public SessionTransaction getCurrentTransaction() {
		return getCurrentTransaction(true);
	}

	public SessionTransaction getCurrentTransaction(final boolean throwNotInTransactionException) {

		final SessionTransaction tx = sessions.get();
		if (throwNotInTransactionException && (tx == null || tx.isClosed())) {

			throw new NotInTransactionException("Not in transaction");
		}

		return tx;
	}

	public boolean logQueries() {
		return Settings.CypherDebugLogging.getValue();
	}

	public boolean logPingQueries() {
		return Settings.CypherDebugLoggingPing.getValue();
	}

	public long unwrap(final Identity identity) {

		if (identity instanceof BoltIdentity) {

			return ((BoltIdentity)identity).getId();
		}

		throw new IllegalArgumentException("This implementation cannot handle Identity objects of type " + identity.getClass().getName() + ".");
	}

	public Node getNodeById(final long id) {
		return NodeWrapper.newInstance(this, id);
	}

	public Relationship getRelationshipById(final long id) {
		return RelationshipWrapper.newInstance(this, id);
	}

	// ----- interface GraphProperties -----
	@Override
	public void setProperty(final String name, final Object value) {

		final Properties properties = getProperties();
		boolean hasChanges          = false;

		if (value == null) {

			if (properties.containsKey(name)) {

				properties.remove(name);
				hasChanges = true;
			}

		} else {

			properties.setProperty(name, value.toString());
			hasChanges = true;
		}

		if (hasChanges) {

			final File propertiesFile   = new File(databasePath + "/graph.properties");

			try (final Writer writer = new FileWriter(propertiesFile)) {

				properties.store(writer, "Created by Structr at " + new Date());

			} catch (IOException ioex) {

				logger.warn("Unable to write properties file", ioex);
			}
		}
	}

	@Override
	public Object getProperty(final String name) {
		return getProperties().getProperty(name);
	}

	@Override
	public CountResult getNodeAndRelationshipCount() {

		final String tenantId = getTenantIdentifier();
		final String part     = tenantId != null ? ":" + tenantId : "";
		final long nodeCount  = getCount("MATCH (n" + part + ":NodeInterface) RETURN COUNT(n) AS count", "count");
		final long relCount   = getCount("MATCH (n" + part + ":NodeInterface)-[r]->() RETURN count(r) AS count", "count");

		return new CountResult(nodeCount, relCount);
	}

	// ----- private methods -----
	private void createUUIDConstraint() {

		// add UUID uniqueness constraint
		try (final Session session = driver.session()) {

			// this call may fail silently (e.g. if the index does not exist yet)
			try (final org.neo4j.driver.v1.Transaction tx = session.beginTransaction()) {

				tx.run("DROP INDEX ON :NodeInterface(id)");
				tx.success();

			} catch (Throwable t) { }

			// this call may NOT fail silently, hence we don't catch any exceptions
			try (final org.neo4j.driver.v1.Transaction tx = session.beginTransaction()) {

				tx.run("CREATE CONSTRAINT ON (node:NodeInterface) ASSERT node.id IS UNIQUE");
				tx.success();
			}
		}
	}

	private void createGlobalSchemaLockNode() {

		try (final Session session = driver.session()) {

			try (final org.neo4j.driver.v1.Transaction tx = session.beginTransaction()) {

				tx.run("MERGE (n:StructrGlobalSchemaLock { name: \"StructrGlobalSchemaLock\" })");
				tx.success();

			} catch (Throwable t) { }
		}
	}

	private Properties getProperties() {

		if (globalGraphProperties == null) {

			globalGraphProperties = new Properties();
			final File propertiesFile   = new File(databasePath + "/graph.properties");

			try (final Reader reader = new FileReader(propertiesFile)) {

				globalGraphProperties.load(reader);

			} catch (IOException ioex) {}
		}

		return globalGraphProperties;
	}

	private long getCount(final String query, final String resultKey) {

		for (final Map<String, Object> row : execute(query)) {

			if (row.containsKey(resultKey)) {

				final Object value = row.get(resultKey);
				if (value != null && value instanceof Number) {

					final Number number = (Number)value;
					return number.intValue();
				}
			}
		}

		return 0;
	}

	private boolean hasIndex(final Session session, final String index) {

		final Map<String, Object> data = new LinkedHashMap<>();
		data.put("name", index);

		final StatementResult result   = session.run("CALL db.indexes() YIELD state, description WHERE description = $name RETURN state", data);
		if (result.hasNext()) {

			final Record record                = result.next();
			final Map<String, Object> valueMap = record.asMap();

			return "ONLINE".equals(valueMap.get("state"));
		}

		return false;
	}

	// ----- nested classes -----
	private static class TypePredicate implements TypeQuery {

		protected String mainType = null;
		protected String name     = null;

		public TypePredicate() {
		}

		public TypePredicate(final String mainType) {
			this.mainType = mainType;
		}

		@Override
		public Class getSourceType() {
			return null;
		}

		@Override
		public Class getTargetType() {
			return null;
		}

		@Override
		public Class getQueryType() {
			return TypeQuery.class;
		}

		@Override
		public String getName() {
			return "type";
		}

		@Override
		public Class getType() {
			return String.class;
		}

		@Override
		public Object getValue() {
			return mainType;
		}

		@Override
		public String getLabel() {
			return null;
		}

		@Override
		public Occurrence getOccurrence() {
			return Occurrence.REQUIRED;
		}

		@Override
		public boolean isExactMatch() {
			return true;
		}

		@Override
		public SortOrder getSortOrder() {
			return null;
		}
	}

	private static class TypePropertyPredicate implements ExactQuery {

		protected String type = null;

		public TypePropertyPredicate(final String type) {
			this.type = type;
		}

		@Override
		public Class getQueryType() {
			return ExactQuery.class;
		}

		@Override
		public String getName() {
			return "type";
		}

		@Override
		public Class getType() {
			return String.class;
		}

		@Override
		public Object getValue() {
			return type;
		}

		@Override
		public String getLabel() {
			return null;
		}

		@Override
		public Occurrence getOccurrence() {
			return Occurrence.REQUIRED;
		}

		@Override
		public boolean isExactMatch() {
			return true;
		}

		@Override
		public SortOrder getSortOrder() {
			return null;
		}
	}
}
