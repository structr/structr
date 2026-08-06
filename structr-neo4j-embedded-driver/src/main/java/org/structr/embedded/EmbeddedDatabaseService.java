/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.embedded;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.exceptions.CypherExecutionException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.*;
import org.structr.api.config.Settings;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.index.Index;
import org.structr.api.index.NewIndexConfig;
import org.structr.api.search.*;
import org.structr.api.util.CountResult;
import org.structr.api.util.NodeWithOwnerResult;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class EmbeddedDatabaseService extends AbstractDatabaseService<String> {

	private static final Logger                           logger                  = LoggerFactory.getLogger(EmbeddedDatabaseService.class.getName());
	private static final ThreadLocal<EmbeddedTransaction> transactions            = new ThreadLocal<>();
	private final Set<String>                             supportedQueryLanguages = new LinkedHashSet<>();
	private DatabaseManagementService managementService                           = null;
	private GraphDatabaseService      graphDb                                     = null;
	private CypherRelationshipIndex   relationshipIndex                           = null;
	private CypherNodeIndex nodeIndex                                             = null;
	private boolean supportsRelationshipIndexes                                   = false;
	private boolean supportsIdempotentIndexCreation                               = false;
	private int neo4jMajorVersion                                                 = -1;
	private int neo4jMinorVersion                                                 = -1;
	private int neo4jPatchVersion                                                 = -1;
	private String errorMessage                                                   = null;
	private String databaseName                                                   = null;
	private Path databasePath                                                     = null;
	private IndexUpdater indexUpdater                                             = null;

	@Override
	public boolean initialize(final String name, final String version, final String instance) {

		String serviceName = null;

		if (!"default".equals(name)) {

			serviceName = name;
		}

		databaseName = Settings.ConnectionDatabaseName.getPrefixedValue(serviceName);
		databasePath = Path.of(Settings.DatabasePath.getValue("db"));

		managementService = new DatabaseManagementServiceBuilder( databasePath ).build();
		graphDb = managementService.database(databaseName);

		// build list of supported query languages
		supportedQueryLanguages.add("application/x-cypher-query");
		supportedQueryLanguages.add("application/cypher");
		supportedQueryLanguages.add("text/cypher");

		configureVersionDependentFeatures();

		if (!databaseExists()) {

			errorMessage = "Database " + databaseName + " does not exist.";

			throw new RuntimeException(errorMessage);
		}

		// signal success

		return true;
	}

	@Override
	public void shutdown() {

		managementService.close();
	}

	@Override
	public Transaction<String> beginTx(boolean forceNew) {

		if (!forceNew) {

			return beginTx();

		} else {

			return new EmbeddedTransaction(this, graphDb.beginTx());
		}
	}

	@Override
	public Transaction<String> beginTx() {

		EmbeddedTransaction transaction = transactions.get();
		if (transaction == null || transaction.isClosed() || transaction.isRolledBack()) {

			transaction = new EmbeddedTransaction(this, graphDb.beginTx());

			transactions.set(transaction);
		}

		return transaction;
	}

	@Override
	public Transaction<String> beginTx(final int timeoutInSeconds) {

		EmbeddedTransaction transaction = transactions.get();
		if (transaction == null || transaction.isClosed()) {

			transaction = new EmbeddedTransaction(this, graphDb.beginTx(timeoutInSeconds, TimeUnit.SECONDS));

			transactions.set(transaction);
		}

		return transaction;
	}

	@Override
	public Node<String> createNode(final String type, final Set<String> labels, final Map<String, Object> input) {

		final EmbeddedTransaction tx   = getCurrentTransaction();
		final Map<String, Object> data = new LinkedHashMap<>(input);

		data.put("type", type);

		final NodeWrapper nodeWrapper = tx.getNodeWrapper(tx.createNode(labels));

		nodeWrapper.setProperties(input);

		return nodeWrapper;
	}

	@Override
	public NodeWithOwnerResult createNodeWithOwner(final Identity<String> userId, final String type, final Set<String> labels, final Map<String, Object> input, final Map<String, Object> ownsProperties, final Map<String, Object> securityProperties) {

		final Map<String, Object> nodeProperties = new LinkedHashMap<>(input);
		final Map<String, Object> parameters     = new HashMap<>();
		final StringBuilder buf                  = new StringBuilder();
		final String tenantId                    = getTenantIdentifier();

		buf.append("MATCH (u:NodeInterface:Principal");

		if (tenantId != null) {

			buf.append(":");
			buf.append(tenantId);
		}

		buf.append(") WHERE elementId(u) = $userId");
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

		// set type
		nodeProperties.put("type", type);

		for (final Map<String, Object> data : execute(buf.toString(), parameters)) {

			final NodeWrapper newNode             = (NodeWrapper) data.get("n");
			final RelationshipWrapper securityRel = (RelationshipWrapper) data.get("s");
			final RelationshipWrapper ownsRel     = (RelationshipWrapper) data.get("o");

			return new NodeWithOwnerResult(newNode, securityRel, ownsRel);
		}

		return null;
	}

	@Override
	public Node<String> getNodeById(final Identity<String> id) {

		return getNodeById(unwrap(id));
	}

	@Override
	public Relationship<String> getRelationshipById(final Identity<String> id) {

		return getRelationshipById(unwrap(id));
	}

	@Override
	public Iterable<Node<String>> getAllNodes() {

		final QueryContext context     = new QueryContext(true);
		final QueryPredicate predicate = new TypePredicate();
		final Index<Node<String>> index        = nodeIndex();

		return index.query(context, predicate, Integer.MAX_VALUE, 1);
	}

	@Override
	public Iterable<Node<String>> getNodesByLabel(final String type) {

		if (type == null) {

			return getAllNodes();
		}

		final QueryContext context     = new QueryContext(true);
		final QueryPredicate predicate = new TypePredicate(type);
		final Index<Node<String>> index  = nodeIndex();

		return index.query(context, predicate, Integer.MAX_VALUE, 1);
	}

	@Override
	public Iterable<Node<String>> getNodesByTypeProperty(final String type) {

		if (type == null) {

			return getAllNodes();
		}

		final QueryContext context     = new QueryContext(true);
		final QueryPredicate predicate = new TypePropertyPredicate(type);
		final Index<Node<String>> index  = nodeIndex();

		return index.query(context, predicate, Integer.MAX_VALUE, 1);
	}

	@Override
	public Iterable<Relationship<String>> getAllRelationships() {

		final Index<Relationship<String>> index = relationshipIndex();
		final QueryPredicate predicate  = new TypePredicate();
		final QueryContext context      = new QueryContext(true);

		return index.query(context, predicate, Integer.MAX_VALUE, 1);
	}

	@Override
	public Iterable<Relationship<String>> getRelationshipsByType(final String type) {

		if (type == null) {

			return getAllRelationships();
		}

		final Index<Relationship<String>> index = relationshipIndex();
		final QueryPredicate predicate        = new TypePredicate(type);
		final QueryContext context            = new QueryContext(true);

		return index.query(context, predicate, Integer.MAX_VALUE, 1);
	}

	@Override
	public Index<Node<String>> nodeIndex() {

		if (nodeIndex == null) {

			nodeIndex = new CypherNodeIndex(this);
		}

		return nodeIndex;
	}

	@Override
	public Index<Relationship<String>> relationshipIndex() {

		if (relationshipIndex == null) {

			relationshipIndex = new CypherRelationshipIndex(this);
		}

		return relationshipIndex;
	}

	@Override
	public void updateIndexConfiguration(final List<NewIndexConfig> schemaIndexConfigSource) {

		switch (neo4jMajorVersion) {

			// Cheers to date-based versioning.....
			case 2025:
			case 2026:
			case 2027:
			case 2028:
			case 2029:
			case 2030:
			case 2031:
			case 2032:
			case 2033:
			case 2034:
			case 2035:
			case 2036:
			case 2037:
			case 2038:
			case 2039:
			case 2040:
			case 2041:
			case 2042:
			case 2043:
			case 2044:
			case 2045:
			case 2046:
			case 2047:
			case 2048:
			case 2049:
			case 2050:
			case 5:
				// cannot use db.indexes(), replaced by SHOW INDEXES call
				indexUpdater = new Neo5IndexUpdater(this, supportsRelationshipIndexes);
				break;

			case 4:

				if (supportsIdempotentIndexCreation) {

					// idempotent index update, no need to check for existance first
					indexUpdater = new Neo4IndexUpdater(this, supportsRelationshipIndexes);

				} else {

					logger.warn("This driver does not support index creation on Neo4j 4.0.x databases. Performance will be impacted.");
				}

				break;

			case 3:

				// non-idempotent index update, need to check for existance first
				indexUpdater = new Neo3IndexUpdater(this, supportsRelationshipIndexes);
				break;

			default:

				// not supported
				logger.warn("This driver does not support index creation on Neo4j " + neo4jMajorVersion + ".x databases. Performance will be impacted.");
				break;
		}

		if (indexUpdater != null) {

			indexUpdater.updateIndexConfiguration(schemaIndexConfigSource);
		}
	}

	@Override
	public boolean isIndexUpdateFinished() {

		if (indexUpdater != null) {

			return indexUpdater.isFinished();
		}

		// no updater, no update in progress

		return true;
	}

	@Override
	public <T> T execute(final NativeQuery<T> nativeQuery) {

		return execute(nativeQuery, getCurrentTransaction());
	}

	@Override
	public <T> T execute(final NativeQuery<T> nativeQuery, final Transaction<String> tx) {

		if (!(tx instanceof EmbeddedTransaction)) {

			throw new IllegalArgumentException("Unsupported transaction type " + tx.toString());
		}

		if (nativeQuery instanceof AbstractNativeQuery) {

			return (T)((AbstractNativeQuery)nativeQuery).execute((EmbeddedTransaction) tx);
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
	public void cleanDatabase() {

		final String tenantId = getTenantIdentifier();
		if (tenantId != null) {

			consume("MATCH (n:" + tenantId + ") DETACH DELETE n", Collections.emptyMap());

		} else {

			consume("MATCH (n) DETACH DELETE n", Collections.emptyMap());
		}
	}

	public EmbeddedTransaction getCurrentTransaction() {

		return getCurrentTransaction(true);
	}

	public EmbeddedTransaction getCurrentTransaction(final boolean throwNotInTransactionException) {

		final EmbeddedTransaction tx = transactions.get();
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

	String unwrap(final Identity<String> identity) {

		if (identity instanceof EmbeddedIdentity b) {

			return b.getId();
		}

		throw new IllegalArgumentException("This implementation cannot handle Identity objects of type " + identity.getClass().getName() + ".");
	}

	Node<String> getNodeById(final String id) {

		return getCurrentTransaction().getNodeWrapper(id);
	}

	Relationship<String> getRelationshipById(final String id) {

		return getCurrentTransaction().getRelationshipWrapper(id);
	}

	void consume(final String nativeQuery) {

		consume(nativeQuery, Collections.EMPTY_MAP);
	}

	void consume(final String nativeQuery, final Map<String, Object> parameters) {

		getCurrentTransaction().set(nativeQuery, parameters);
	}

	Iterable<Map<String, Object>> execute(final String nativeQuery) {

		return execute(nativeQuery, Collections.EMPTY_MAP);
	}

	Iterable<Map<String, Object>> execute(final String nativeQuery, final Map<String, Object> parameters) {

		return getCurrentTransaction().run(new SimpleCypherQuery(nativeQuery, parameters));
	}

	@Override
	public CountResult getNodeAndRelationshipCount() {

		final String tenantId = getTenantIdentifier();
		final String part     = tenantId != null ? ":" + tenantId : "";
		final Long nodeCount  = getCount("MATCH (n" + part + ":NodeInterface) RETURN COUNT(n) AS count", "count");
		final Long relCount   = getCount("MATCH (n" + part + ":NodeInterface)-[r]->() RETURN COUNT(r) AS count", "count");
		final Long userCount  = getCount("MATCH (n" + part + ":User) RETURN COUNT(n) AS count", "count");

		if (nodeCount == null || relCount == null || userCount == null) {

			throw new RuntimeException("Unable to fetch database counts.");
		}

		return new CountResult(nodeCount, relCount, userCount);
	}

	@Override
	public List<Map<String, Object>> globalSearch(final Set<String> types, final String searchString) {

		final boolean supportsTypePredicateExpressions = supportsFeature(DatabaseFeature.TypePredicateExpressions);
		final Map<String, Object> parameters           = Map.of("searchString", supportsTypePredicateExpressions ? searchString.toLowerCase() : searchString);

		if (!types.isEmpty()) {

			final String searchLabels = String.join(" OR ", types);
			final String tenantId     = getTenantIdentifier();
			final String labelsClause = (tenantId == null) ? searchLabels : String.format("n:`%s` AND (%s)", tenantId, searchLabels);

			final String cypherQuery = (supportsTypePredicateExpressions) ? """
				MATCH (n)
					WHERE (%s)
				WITH
					n, $searchString as searchString
				WITH
					n,
					searchString,
					[prop IN keys(n)
						WHERE
							CASE
								WHEN n[prop] IS NULL THEN false
								WHEN n[prop] IS :: STRING THEN toLower(n[prop]) CONTAINS searchString
								WHEN n[prop] IS :: LIST<STRING> THEN ANY (v IN n[prop] WHERE toLower(v) CONTAINS searchString)
								ELSE toLower(toString(n[prop])) CONTAINS searchString
							END
						| prop] AS matchedKeys,
					labels(n) as labels
				WHERE
					size(matchedKeys) > 0
				RETURN {
					id:              n.id,
					type:            n.type,
					name:            n.name,
					keys:            matchedKeys,
					values:          [key IN matchedKeys |
					   CASE
						 WHEN n[key] IS :: LIST<STRING> THEN
						   head([v IN n[key] WHERE toLower(v) CONTAINS searchString |
							 {
							   before: right(substring(v, 0, size(split(toLower(v), searchString)[0])), 24),
							   match:  substring(v, size(split(toLower(v), searchString)[0]), size(searchString)),
							   after:  left(substring(v, size(split(toLower(v), searchString)[0]) + size(searchString)), 24)
							 }
						   ])
						 ELSE
						   {
							 before: right(substring(toString(n[key]), 0, size(split(toLower(toString(n[key])), searchString)[0])), 24),
							 match:  substring(toString(n[key]), size(split(toLower(toString(n[key])), searchString)[0]), size(searchString)),
							 after:  left(substring(toString(n[key]), size(split(toLower(toString(n[key])), searchString)[0]) + size(searchString)), 24)
						   }
					   END
				   ],
					labels:          labels
				} AS searchResult
				""".formatted(labelsClause) : """
					MATCH (n)
						WHERE (%s)
					WITH
						n, $searchString as searchString
					WITH
						n,
						searchString,
						[prop IN keys(n) WHERE n[prop] CONTAINS searchString | prop] AS matchedKeys,
						labels(n) as labels
					WHERE
						size(matchedKeys) > 0
					RETURN {
						id:              n.id,
						type:            n.type,
						name:            n.name,
						keys:            matchedKeys,
						values:          [key IN matchedKeys |
						   {
							 before: right(substring(toString(n[key]), 0, size(split(toLower(toString(n[key])), searchString)[0])), 24),
							 match:  substring(toString(n[key]), size(split(toLower(toString(n[key])), searchString)[0]), size(searchString)),
							 after:  left(substring(toString(n[key]), size(split(toLower(toString(n[key])), searchString)[0]) + size(searchString)), 24)
						   }
					   ],
						labels:          labels
					} AS searchResult
					""".formatted(labelsClause);

			final Iterable<Map<String, Object>> rawResults = getCurrentTransaction().run(new SimpleCypherQuery(cypherQuery, parameters));
			final List<Map<String, Object>> results        = new LinkedList<>();

			for (final Map<String, Object> result : rawResults) {

				results.add((Map) result.get("searchResult"));
			}

			return results;
		}

		return List.of();
	}

	@Override
	public boolean supportsFeature(final DatabaseFeature feature, final Object... parameters) {

		switch (feature) {

			case LargeStringIndexing:

				return false;

			case FulltextIndexing:

				return true;

			case QueryLanguage:

				final String param = getStringParameter(parameters);
				if (param != null) {

					return supportedQueryLanguages.contains(param.toLowerCase());
				}

			case SpatialQueries:

				return true;

			case NewDistanceFunction:

				return neo4jMajorVersion >= 5;

			case AuthenticationRequired:

				return true;

			case RelationshipIndexes:

				return supportsRelationshipIndexes;

			case NewDBIndexesFormat:
				// New db.indexes() format can be used for Neo4j versions >= 4,
				// which is identical to the version for the reactive flag.

				return neo4jMajorVersion >= 4;

			case ShowIndexesQuery:

				return neo4jMajorVersion >= 5;

			case TypePredicateExpressions:
				// see https://development.neo4j.dev/blog/developer/data-quality-type-constraints-functions/

				return neo4jMajorVersion >= 5 && neo4jMinorVersion >= 9;

			case RangeIndexes:

				return neo4jMajorVersion >= 5;
		}

		return false;
	}

	@Override
	public String getErrorMessage() {

		return errorMessage;
	}

	@Override
	public Map<String, Map<String, Integer>> getCachesInfo() {

		return Map.of();
	}

	@Override
	public void flushCaches() {

		EmbeddedTransaction.flushCaches();
	}

	// ----- private methods -----
	private String getNeo4jVersion() {

		return "2026.04.0";
	}

	private Long getCount(final String query, final String resultKey) {

		for (final Map<String, Object> row : execute(query)) {

			if (row.containsKey(resultKey)) {

				final Object value = row.get(resultKey);
				if (value != null && value instanceof Number n) {

					return n.longValue();
				}
			}
		}

		return null;
	}

	private void configureVersionDependentFeatures() {

		final String version      = getNeo4jVersion();
		final String[] parts      = version.replaceAll("[^0-9.]", "").split("\\.");
		final String majorVersion = stringOrDefault(parts, 0, "0");
		final String minorVersion = stringOrDefault(parts, 1, "0");
		final String patchVersion = stringOrDefault(parts, 2, "0");

		logger.info("Neo4j version is {}", version);

		neo4jMajorVersion = Integer.valueOf(majorVersion);
		neo4jMinorVersion = Integer.valueOf(minorVersion);
		neo4jPatchVersion = Integer.valueOf(patchVersion);

		// all versions >= 5 support the below flags
		this.supportsRelationshipIndexes     = neo4jMajorVersion >= 5 || (neo4jMajorVersion >= 4 && neo4jMinorVersion >= 3);
		this.supportsIdempotentIndexCreation = neo4jMajorVersion >= 5 || (neo4jMajorVersion >= 4 && neo4jMinorVersion >= 1 && neo4jPatchVersion >= 3);
	}

	private boolean databaseExists() {

		return graphDb.isAvailable();
	}

	private String stringOrDefault(final String[] source, final int index, final String defaultValue) {

		if (index >= source.length) {

			return defaultValue;
		}

		return source[index];
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
		public String getSourceType() {

			return null;
		}

		@Override
		public String getTargetType() {

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
