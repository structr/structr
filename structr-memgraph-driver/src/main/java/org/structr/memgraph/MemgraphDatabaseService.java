/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.memgraph;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.exceptions.AuthenticationException;
import org.neo4j.driver.v1.exceptions.ClientException;
import org.neo4j.driver.v1.exceptions.DatabaseException;
import org.neo4j.driver.v1.exceptions.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Transaction;
import org.structr.api.*;
import org.structr.api.config.Settings;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.index.Index;
import org.structr.api.index.IndexConfig;
import org.structr.api.search.*;
import org.structr.api.util.CountResult;
import org.structr.api.util.NodeWithOwnerResult;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class MemgraphDatabaseService extends AbstractDatabaseService {

	private static final Logger logger                                = LoggerFactory.getLogger(MemgraphDatabaseService.class.getName());
	private static final ThreadLocal<SessionTransaction> sessions     = new ThreadLocal<>();
	private final Set<String> supportedQueryLanguages                 = new LinkedHashSet<>();
	private Properties globalGraphProperties                          = null;
	private CypherRelationshipIndex relationshipIndex                 = null;
	private CypherNodeIndex nodeIndex                                 = null;
	private String errorMessage                                       = null;
	private String databaseUrl                                        = null;
	private Driver driver                                             = null;
	private boolean supportsANY                                       = false;

	@Override
	public boolean initialize(final String name, final String version, final String instance) {

		String serviceName = null;

		if (!"default".equals(name)) {

			serviceName = name;
		}

		databaseUrl              = Settings.ConnectionUrl.getPrefixedValue(serviceName);
		final String username    = Settings.ConnectionUser.getPrefixedValue(serviceName);
		final String password    = Settings.ConnectionPassword.getPrefixedValue(serviceName);
		String databaseDriverUrl = "bolt://" + databaseUrl;

		// build list of supported query languages
		supportedQueryLanguages.add("application/x-cypher-query");
		supportedQueryLanguages.add("application/cypher");
		supportedQueryLanguages.add("text/cypher");

		if (databaseUrl.length() >= 7 && databaseUrl.substring(0, 7).equalsIgnoreCase("bolt://")) {

			databaseDriverUrl = databaseUrl;

		} else if (databaseUrl.length() >= 15 && databaseUrl.substring(0, 15).equalsIgnoreCase("bolt+routing://")) {

			databaseDriverUrl = databaseUrl;
		}

		try {

			driver = GraphDatabase.driver(databaseDriverUrl,
				AuthTokens.basic(username, password),
				Config.build().withEncryption().toConfig()
			);

			final int relCacheSize  = Settings.RelationshipCacheSize.getPrefixedValue(serviceName);
			final int nodeCacheSize = Settings.NodeCacheSize.getPrefixedValue(serviceName);

			NodeWrapper.initialize(nodeCacheSize);
			logger.info("Node cache size set to {}", nodeCacheSize);

			RelationshipWrapper.initialize(relCacheSize);
			logger.info("Relationship cache size set to {}", relCacheSize);


			// auto-detect support for ANY
			try (final Transaction tx = beginTx()) {

				execute("MATCH (n) WHERE ANY (x in n.test WHERE x = 'test') RETURN n LIMIT 1");
				supportsANY = true;
				tx.success();

			} catch (Throwable t) {
				supportsANY = false;
			}

			// signal success
			return true;

		} catch (AuthenticationException auex) {
			errorMessage = auex.getMessage() + " If you are connecting to this Neo4j instance for the first time, you might need to change the default password in the Neo4j Browser.";
		} catch (ServiceUnavailableException ex) {
			errorMessage = ex.getMessage();
		}

		// service failed to initialize
		return false;
	}

	@Override
	public void shutdown() {

		clearCaches();
		driver.close();
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
	public Transaction beginTx(final int timeoutInSeconds) {

		SessionTransaction session = sessions.get();
		if (session == null || session.isClosed()) {

			try {
				session = new SessionTransaction(this, driver.session(), timeoutInSeconds);
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

		buf.append(" ");
		buf.append(createParameterMapStringFromMapAndInsertIntoQueryParameters("properties", properties, map));
		buf.append(") RETURN n");

		final NodeWrapper newNode = NodeWrapper.newInstance(this, getCurrentTransaction().getNode(buf.toString(), map));

		newNode.setModified();

		return newNode;
	}

	@Override
	public NodeWithOwnerResult createNodeWithOwner(final Identity userId, final String type, final Set<String> labels, final Map<String, Object> nodeProperties, final Map<String, Object> ownsProperties, final Map<String, Object> securityProperties) {

		final Map<String, Object> parameters = new HashMap<>();
		final StringBuilder buf              = new StringBuilder();
		final String tenantId                = getTenantIdentifier();

		buf.append("MATCH (u:NodeInterface:Principal");

		if (tenantId != null) {

			buf.append(":");
			buf.append(tenantId);
		}

		buf.append(") WHERE ID(u) = $userId");
		buf.append(" CREATE (u)-[o:OWNS ");
		buf.append(createParameterMapStringFromMapAndInsertIntoQueryParameters("ownsProperties", ownsProperties, parameters));
		buf.append("]->(n");

		if (tenantId != null) {

			buf.append(":");
			buf.append(tenantId);
		}

		for (final String label : labels) {

			buf.append(":");
			buf.append(label);
		}

		buf.append(" ");
		buf.append(createParameterMapStringFromMapAndInsertIntoQueryParameters("nodeProperties", nodeProperties, parameters));
		buf.append(")<-[s:SECURITY ");
		buf.append(createParameterMapStringFromMapAndInsertIntoQueryParameters("securityProperties", securityProperties, parameters));
		buf.append("]-(u)");
		buf.append(" RETURN n, s, o");

		// store properties in statement
		parameters.put("userId",             unwrap(userId));

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

	public static String createParameterMapStringFromMapAndInsertIntoQueryParameters(final String variableName, final Map<String, Object> variableContent, final Map<String, Object> baseParamerters) {

		final StringBuilder buf = new StringBuilder("{");

		final ArrayList<String> tmpList = new ArrayList();

		variableContent.forEach((key, value) -> {

			final String paramName = variableName + "_" + key;

			tmpList.add("`" + key + "`: $`" + paramName + "`");

			baseParamerters.put(paramName, value);
		});

		buf.append(StringUtils.join(tmpList.toArray(), ", "));

		buf.append("}");

		return buf.toString();
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
	public void updateIndexConfiguration(final Map<String, Map<String, IndexConfig>> schemaIndexConfigSource, final Map<String, Map<String, IndexConfig>> removedClassesSource, final boolean createOnly) {

		final ExecutorService executor              = Executors.newCachedThreadPool();
		final Map<String, String> existingDbIndexes = new HashMap<>();
		final int timeoutSeconds                    = 10;

		try {

			executor.submit(() -> {

				try (Session session = driver.session()) {

					for (final Map row : session.run("SHOW INDEX INFO").list(r -> r.asMap())) {

						if ("label+property".equals(row.get("index type"))) {

							final String description = "INDEX ON :" + row.get("label") + "(`" + row.get("property") + "`)";

							existingDbIndexes.put(description, "ONLINE");
						}
					}
				}

			}).get(timeoutSeconds, TimeUnit.SECONDS);

		} catch (Throwable t) {
			logger.error(ExceptionUtils.getStackTrace(t));
		}

		logger.debug("Found {} existing indexes", existingDbIndexes.size());

		final AtomicInteger createdIndexes = new AtomicInteger(0);
		final AtomicInteger droppedIndexes = new AtomicInteger(0);

		// create indices for properties of existing classes
		for (final Map.Entry<String, Map<String, IndexConfig>> entry : schemaIndexConfigSource.entrySet()) {

			final String typeName = entry.getKey();

			for (final Map.Entry<String, IndexConfig> propertyIndexConfig : entry.getValue().entrySet()) {

				final String indexDescription = "INDEX ON :" + typeName + "(`" + propertyIndexConfig.getKey() + "`)";
				final String state            = existingDbIndexes.get(indexDescription);
				final boolean alreadySet      = "ONLINE".equals(state);
				final IndexConfig indexConfig = propertyIndexConfig.getValue();

				if ("FAILED".equals(state)) {

					logger.warn("Index is in FAILED state - dropping the index before handling it further. {}. If this error is recurring, please verify that the data in the concerned property is indexable by the database", indexDescription);

					final AtomicBoolean retry = new AtomicBoolean(true);
					final AtomicInteger retryCount = new AtomicInteger(0);

					while (retry.get()) {

						retry.set(false);

						try {

							executor.submit(() -> {

								try (Session session = driver.session()) {

									session.run("DROP " + indexDescription);

								} catch (RetryException rex) {

									retry.set(retryCount.incrementAndGet() < 3);
									logger.info("DROP INDEX: retry {}", retryCount.get());

								} catch (Throwable t) {
									logger.warn("Unable to drop failed index: {}", t.getMessage());
								}

								return null;

							}).get(timeoutSeconds, TimeUnit.SECONDS);

						} catch (Throwable t) {
							logger.error(ExceptionUtils.getStackTrace(t));
						}
					}
				}

				final AtomicBoolean retry = new AtomicBoolean(true);
				final AtomicInteger retryCount = new AtomicInteger(0);

				while (retry.get()) {

					retry.set(false);

					try {

						executor.submit(() -> {

							try (Session session = driver.session()) {

								if (indexConfig.createOrDropIndex()) {

									if (!alreadySet) {

										try {

											session.run("CREATE " + indexDescription);
											createdIndexes.incrementAndGet();

										} catch (Throwable t) {
											logger.warn("Unable to create {}: {}", indexDescription, t.getMessage());
										}
									}

								} else if (alreadySet && !createOnly) {

									try {

										session.run("DROP " + indexDescription);
										droppedIndexes.incrementAndGet();

									} catch (Throwable t) {
										logger.warn("Unable to drop {}: {}", indexDescription, t.getMessage());
									}
								}

							} catch (RetryException rex) {

								retry.set(retryCount.incrementAndGet() < 3);
								logger.info("INDEX update: retry {}", retryCount.get());

							} catch (IllegalStateException i) {

								// if the driver instance is already closed, there is nothing we can do => exit
								return;

							} catch (Throwable t) {

								logger.warn("Unable to update index configuration: {}", t.getMessage());
							}

						}).get(timeoutSeconds, TimeUnit.SECONDS);

					} catch (Throwable t) {}
				}
			}
		}

		if (createdIndexes.get() > 0) {
			logger.debug("Created {} indexes", createdIndexes.get());
		}

		if (droppedIndexes.get() > 0) {
			logger.debug("Dropped {} indexes", droppedIndexes.get());
		}

		if (!createOnly) {

			final AtomicInteger droppedIndexesOfRemovedTypes = new AtomicInteger(0);
			final List removedTypes = new ArrayList();

			// drop indices for all indexed properties of removed classes
			for (final Map.Entry<String, Map<String, IndexConfig>> entry : removedClassesSource.entrySet()) {

				final String typeName = entry.getKey();
				removedTypes.add(typeName);

				for (final Map.Entry<String, IndexConfig> propertyIndexConfig : entry.getValue().entrySet()) {

					final String indexDescription = "INDEX ON :" + typeName + "(`" + propertyIndexConfig.getKey() + "`)";
					final boolean indexExists     = (existingDbIndexes.get(indexDescription) != null);
					final IndexConfig indexConfig = propertyIndexConfig.getValue();

					if (indexExists && indexConfig.createOrDropIndex()) {

						final AtomicBoolean retry = new AtomicBoolean(true);
						final AtomicInteger retryCount = new AtomicInteger(0);

						while (retry.get()) {

							retry.set(false);

							try {

								executor.submit(() -> {

									try (Session session = driver.session()) {

										// drop index
										session.run("DROP " + indexDescription);
										droppedIndexesOfRemovedTypes.incrementAndGet();

									} catch (RetryException rex) {

										retry.set(retryCount.incrementAndGet() < 3);
										logger.info("DROP INDEX: retry {}", retryCount.get());

									} catch (Throwable t) {
										logger.warn("Unable to drop {}: {}", indexDescription, t.getMessage());
									}

								}).get(timeoutSeconds, TimeUnit.SECONDS);

							} catch (Throwable t) {}
						}
					}
				}
			}

			if (droppedIndexesOfRemovedTypes.get() > 0) {
				logger.debug("Dropped {} indexes of deleted types ({})", droppedIndexesOfRemovedTypes.get(), StringUtils.join(removedTypes, ", "));
			}
		}
	}

	@Override
	public boolean isIndexUpdateFinished() {
		return true;
	}

	@Override
	public <T> T execute(final NativeQuery<T> nativeQuery) {

		if (nativeQuery instanceof AbstractNativeQuery) {

			return (T)((AbstractNativeQuery)nativeQuery).execute(getCurrentTransaction());
		}

		throw new IllegalArgumentException("Unsupported query type " + nativeQuery.getClass().getName() + ".");
	}

	@Override
	public <T> NativeQuery<T> query(final Object query, final Class<T> resultType) {

		if (!(query instanceof String)) {
			throw new IllegalArgumentException("Unsupported query type " + query.getClass().getName() + ", expected String.");
		}

		return createQuery((String)query, resultType);
	}

	@Override
	public void clearCaches() {

		NodeWrapper.clearCache();
		RelationshipWrapper.clearCache();
	}

	@Override
	public void removeNodeFromCache(final Identity id) {
		NodeWrapper.expunge(unwrap(id));
	}

	@Override
	public void removeRelationshipFromCache(final Identity id) {
		RelationshipWrapper.expunge(unwrap(id));
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

	boolean logQueries() {
		return Settings.CypherDebugLogging.getValue();
	}

	boolean logPingQueries() {
		return Settings.CypherDebugLoggingPing.getValue();
	}

	long unwrap(final Identity identity) {

		if (identity instanceof BoltIdentity) {

			return ((BoltIdentity)identity).getId();
		}

		throw new IllegalArgumentException("This implementation cannot handle Identity objects of type " + identity.getClass().getName() + ".");
	}

	Node getNodeById(final long id) {
		return NodeWrapper.newInstance(this, id);
	}

	Relationship getRelationshipById(final long id) {
		return RelationshipWrapper.newInstance(this, id);
	}

	Iterable<Map<String, Object>> execute(final String nativeQuery) {
		return execute(nativeQuery, Collections.EMPTY_MAP);
	}

	Iterable<Map<String, Object>> execute(final String nativeQuery, final Map<String, Object> parameters) {
		return getCurrentTransaction().run(nativeQuery, parameters);
	}

	TransactionConfig getTransactionConfig(final long id) {

		final Map<String, Object> metadata = new HashMap<>();

		metadata.put("id", id);

		return TransactionConfig
			.builder()
			.withMetadata(metadata)
			.build();
	}

	TransactionConfig getTransactionConfigForTimeout(final int seconds, final long id) {

		final Map<String, Object> metadata = new HashMap<>();

		metadata.put("id", id);

		return TransactionConfig
			.builder()
			.withTimeout(Duration.ofSeconds(seconds))
			.withMetadata(metadata)
			.build();
	}

	@Override
	public CountResult getNodeAndRelationshipCount() {

		final String tenantId = getTenantIdentifier();
		final String part     = tenantId != null ? ":" + tenantId : "";
		final long nodeCount  = getCount("MATCH (n" + part + ":NodeInterface) RETURN COUNT(n) AS count", "count");
		final long relCount   = getCount("MATCH (n" + part + ":NodeInterface)-[r]->() RETURN COUNT(r) AS count", "count");
		final long userCount  = getCount("MATCH (n" + part + ":User) RETURN COUNT(n) AS count", "count");

		return new CountResult(nodeCount, relCount, userCount);
	}

	@Override
	public boolean supportsFeature(final DatabaseFeature feature, final Object... parameters) {

		switch (feature) {

			case LargeStringIndexing:
				return false;

			case QueryLanguage:

				final String param = getStringParameter(parameters);
				if (param != null) {

					return supportedQueryLanguages.contains(param.toLowerCase());
				}

			case SpatialQueries:
				return true;

			case AuthenticationRequired:
				return false;
		}

		return false;
	}

	public String anyOrSingleFunction() {
		return (supportsANY ? "ANY" : "SINGLE");
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public Map<String, Map<String, Integer>> getCachesInfo() {
		return Map.of(
			"nodes", NodeWrapper.nodeCache.getCacheInfo(),
			"relationships", RelationshipWrapper.relationshipCache.getCacheInfo()
		);
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

	@Override
	public Identity identify(long id) {
		return new BoltIdentity(id);
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

	private <T> NativeQuery<T> createQuery(final String query, final Class<T> type) {

		if (Iterable.class.equals(type)) {
			return (NativeQuery<T>)new IterableQuery(query);
		}

		if (Boolean.class.equals(type)) {
			return (NativeQuery<T>)new BooleanQuery(query);
		}

		if (Long.class.equals(type)) {
			return (NativeQuery<T>)new LongQuery(query);
		}

		return null;
	}

	private String getStringParameter(final Object[] parameters) {

		if (parameters != null && parameters.length > 0) {

			final Object param = parameters[0];
			if (param instanceof String) {

				return (String)param;
			}
		}

		return null;
	}
}
