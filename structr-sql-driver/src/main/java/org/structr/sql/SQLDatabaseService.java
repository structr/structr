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
package org.structr.sql;

import com.google.gson.Gson;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.AbstractDatabaseService;
import org.structr.api.DatabaseFeature;
import org.structr.api.NativeQuery;
import org.structr.api.NotInTransactionException;
import org.structr.api.Transaction;
import org.structr.api.graph.GraphProperties;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.api.index.Index;
import org.structr.api.schema.JsonProperty;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.api.util.CountResult;
import org.structr.api.util.Iterables;
import org.structr.api.util.NodeWithOwnerResult;

/**
 */
public class SQLDatabaseService extends AbstractDatabaseService implements GraphProperties {

	private static final Logger logger                     = LoggerFactory.getLogger(SQLDatabaseService.class);
	private static final Set<String> uuidFields            = new LinkedHashSet<>(Arrays.asList("id", "createdBy", "lastModifiedBy"));
	private static final Set<String> varcharFields         = new LinkedHashSet<>(Arrays.asList("type"));
	private static final Set<String> varcharTypes          = new LinkedHashSet<>(Arrays.asList("string"));
	private static final Set<String> bigintTypes           = new LinkedHashSet<>(Arrays.asList("long"));
	private static final Set<String> intTypes              = new LinkedHashSet<>(Arrays.asList("integer"));

	private final ThreadLocal<SQLTransaction> transactions = new ThreadLocal<>();
	private final Map<String, Object> properties           = new LinkedHashMap<>();
	private BasicDataSource dataSource                     = null;
	private SQLRelationshipIndex relIndex                  = null;
	private SQLNodeIndex nodeIndex                         = null;

	@Override
	public boolean initialize(final String serviceName) {

		dataSource = new BasicDataSource();
		dataSource.setUrl("jdbc:mysql://localhost/structr?serverTimezone=UTC&useSSL=false");
		dataSource.setMaxTotal(20);
		dataSource.setMaxIdle(10);
		dataSource.setDefaultAutoCommit(false);
		dataSource.setUsername("structr");
		dataSource.setPassword("test");

		SQLNode.initialize(1000);
		SQLRelationship.initialize(1000);

		return true;
	}

	@Override
	public void shutdown() {
	}

	@Override
	public void clearCaches() {
	}

	@Override
	public void cleanDatabase() {

		/*
		try {

			final SQLTransaction tx = getCurrentTransaction();

		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		*/
	}

	@Override
	public void deleteNodesByLabel(final String label) {
	}

	@Override
	public Transaction beginTx() {

		SQLTransaction current = transactions.get();
		if (current == null) {

			try {

				current = new SQLTransaction(this, dataSource.getConnection());
				transactions.set(current);

			} catch (SQLException ex) {
				ex.printStackTrace();
				logger.warn("Unable to get connection: {}", ex.getMessage());
			}
		}

		return current;
	}

	@Override
	public Node createNode(final String type, final Set<String> labels, final Map<String, Object> properties) {

		try {

			final SQLTransaction tx = getCurrentTransaction();
			final StringBuilder buf = new StringBuilder("INSERT INTO ");
			int index               = 1;

			buf.append(type);
			buf.append("(`");
			buf.append(StringUtils.join(properties.keySet(), "`, `"));
			buf.append("`) VALUES(");
			buf.append(StringUtils.repeat("?", ", ", properties.size()));
			buf.append(")");

			System.out.println(buf.toString());

			final PreparedStatement createNode = tx.prepareStatement(buf.toString());
			final String uuid                  = (String)properties.get("id");

			// fill in values
			for (final Object value : properties.values()) {

				createNode.setObject(index++, convertValue(value));
			}

			final int createNodeResultCount = createNode.executeUpdate();
			if (createNodeResultCount == 1) {

				final SQLIdentity identity = SQLIdentity.getInstance(uuid, type);

				return SQLNode.newInstance(this, new NodeResult(identity, properties));
			}

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	@Override
	public NodeWithOwnerResult createNodeWithOwner(final Identity ownerId, final String type, final Set<String> labels, final Map<String, Object> nodeProperties, final Map<String, Object> ownsProperties, final Map<String, Object> securityProperties) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Node getNodeById(final Identity id) {
		return getNodeById((SQLIdentity)id);
	}

	@Override
	public Relationship getRelationshipById(final Identity id) {
		return getRelationshipById((SQLIdentity)id);
	}

	@Override
	public Iterable<Node> getAllNodes() {

		/*
	}

		try {

			final SQLTransaction tx     = getCurrentTransaction();
			final PreparedStatement stm = tx.prepareStatement("SELECT id FROM Node ORDER BY id");

			return Iterables.map(r -> SQLNode.newInstance(this, r), new IdentityStream(stm.executeQuery()));

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		*/

		return null;
	}

	@Override
	public Iterable<Node> getNodesByLabel(final String label) {

		/*

		try {

			final SQLTransaction tx     = getCurrentTransaction();
			final PreparedStatement stm = tx.prepareStatement("SELECT DISTINCT nodeId FROM Label l WHERE l.name = ? ORDER BY p.nodeId");

			stm.setString(1, label);

			return Iterables.map(r -> SQLNode.newInstance(this, r), new IdentityStream(stm.executeQuery()));

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		*/

		return null;
	}

	@Override
	public Iterable<Node> getNodesByTypeProperty(final String type) {

		try {

			final SQLTransaction tx     = getCurrentTransaction();
			final PreparedStatement stm = tx.prepareStatement("SELECT DISTINCT * FROM " + type);

			return Iterables.map(r -> SQLNode.newInstance(this, r), new IdentityStream(type, stm.executeQuery()));

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	@Override
	public Iterable<Relationship> getAllRelationships() {

		/*

		try {

			final SQLTransaction tx     = getCurrentTransaction();
			final PreparedStatement stm = tx.prepareStatement("SELECT id FROM Relationship");

			return Iterables.map(r -> SQLRelationship.newInstance(this, r), new IdentityStream(stm.executeQuery()));

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		*/

		return null;
	}

	@Override
	public Iterable<Relationship> getRelationshipsByType(final String type) {

		try {

			final SQLTransaction tx     = getCurrentTransaction();
			final PreparedStatement stm = tx.prepareStatement("SELECT * FROM " + type);

			stm.setString(1, type);

			return Iterables.map(r -> SQLRelationship.newInstance(this, r), new IdentityStream(type, stm.executeQuery()));

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	@Override
	public GraphProperties getGlobalProperties() {
		return this;
	}

	@Override
	public Index<Node> nodeIndex() {

		if (nodeIndex == null) {

			nodeIndex = new SQLNodeIndex(this);
		}

		return nodeIndex;
	}

	@Override
	public Index<Relationship> relationshipIndex() {

		if (relIndex == null) {

			relIndex = new SQLRelationshipIndex(this);
		}

		return relIndex;
	}

	@Override
	public void updateIndexConfiguration(final Map<String, Map<String, Boolean>> schemaIndexConfig, final Map<String, Map<String, Boolean>> removedClasses) {
	}

	@Override
	public CountResult getNodeAndRelationshipCount() {
		return new CountResult(0, 0);
	}

	@Override
	public <T> T execute(NativeQuery<T> nativeQuery) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public <T> NativeQuery<T> query(final Object query, final Class<T> resultType) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public boolean supportsFeature(final DatabaseFeature feature, final Object... parameters) {
		return false;
	}

	@Override
	public void setProperty(final String name, final Object value) {
		properties.put(name, value);
	}

	@Override
	public Object getProperty(final String name) {
		return properties.get(name);
	}

	@Override
	public void initializeSchema(final JsonSchema schema) {

		try (final Transaction tx = beginTx()) {

			for (final JsonType type : schema.getTypes()) {

				if (!type.isAbstract() && !type.isInterface()) {

					final StringBuilder buf = new StringBuilder();
					final List<String> cols = new LinkedList<>();

					for (final JsonProperty property : type.getProperties()) {

						final String sqlType = getSQLTypeForProperty(property);
						if (sqlType != null) {

							final StringBuilder col = new StringBuilder();

							col.append("`");
							col.append(property.getName());
							col.append("` ");
							col.append(sqlType);

							cols.add(col.toString());
						}
					}

					// build actual statement
					buf.append("CREATE TABLE IF NOT EXISTS `");
					buf.append(type.getName());
					buf.append("` (");
					buf.append(StringUtils.join(cols, ", "));
					buf.append(", PRIMARY KEY (id)");
					buf.append(")");

					System.out.println(buf.toString());

					// create table
					getCurrentTransaction().prepareStatement(buf.toString()).executeUpdate();
				}
			}

			tx.success();

		} catch (SQLException e) {

			System.out.println("Error code: " + e.getErrorCode());
			e.printStackTrace();

		} catch (Throwable t) {

			t.printStackTrace();
		}
	}

	// ----- package-private methods -----
	SQLNode getNodeById(final SQLIdentity identity) {
		return SQLNode.newInstance(this, identity);
	}

	SQLRelationship getRelationshipById(final SQLIdentity identity) {
		return SQLRelationship.newInstance(this, identity);
	}

	SQLTransaction getCurrentTransaction() {

		final SQLTransaction current = transactions.get();
		if (current != null) {

			return current;
		}

		throw new NotInTransactionException("Not in transaction");
	}

	// ----- private methods -----
	private String getSQLTypeForProperty(final JsonProperty property) {

		final StringBuilder buf = new StringBuilder();
		final String name       = property.getName();
		final String type       = property.getType();

		if (uuidFields.contains(name)) {

			buf.append("char(32)");

		} else if (varcharTypes.contains(type) || varcharFields.contains(name)) {

			buf.append("text");

		} else if (intTypes.contains(type)) {

			buf.append("int");

		} else if (bigintTypes.contains(type)) {

			buf.append("bigint");

		} else if ("array".equals(type)) {

			buf.append("json");

		} else {

			buf.append(type);
		}

		return buf.toString();
	}

	private Object convertValue(final Object value) {

		if (value != null && value.getClass().isArray()) {

			final Gson gson = new Gson();

			return gson.toJson(value);
		}

		return value;
	}
}
