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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.dbcp2.BasicDataSource;
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
import org.structr.api.util.CountResult;
import org.structr.api.util.Iterables;
import org.structr.api.util.NodeWithOwnerResult;

/**
 */
public class SQLDatabaseService extends AbstractDatabaseService implements GraphProperties {

	private static final Logger logger                     = LoggerFactory.getLogger(SQLDatabaseService.class);

	private final ThreadLocal<SQLTransaction> transactions = new ThreadLocal<>();
	private final Map<String, Object> properties           = new LinkedHashMap<>();
	private BasicDataSource dataSource                     = null;
	private SQLRelationshipIndex relIndex                  = null;
	private SQLNodeIndex nodeIndex                         = null;

	@Override
	public boolean initialize(final String serviceName) {

		dataSource = new BasicDataSource();
		dataSource.setUrl("jdbc:mysql://localhost/structr?serverTimezone=UTC");
		dataSource.setMaxTotal(20);
		dataSource.setMaxIdle(10);
		dataSource.setDefaultAutoCommit(false);
		dataSource.setUsername("structr");
		dataSource.setPassword("test");

		SQLNode.initialize(1000);

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

		try {

			final SQLTransaction tx = getCurrentTransaction();

			tx.prepareStatement("DELETE FROM Node").execute();
			tx.prepareStatement("DELETE FROM Label").execute();
			tx.prepareStatement("DELETE FROM Relationship").execute();
			tx.prepareStatement("DELETE FROM NodeProperty").execute();
			tx.prepareStatement("DELETE FROM RelationshipProperty").execute();

		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void deleteNodesByLabel(final String label) {

		try {

			final SQLTransaction tx     = getCurrentTransaction();
			final PreparedStatement stm = tx.prepareStatement("DELETE n, l, p FROM Label l JOIN Node n ON n.id = l.nodeId JOIN NodeProperty p ON o.nodeId = l.nodeId WHERE l.name = ?");

			stm.setString(1, label);

			stm.execute();

		} catch (SQLException ex) {
			ex.printStackTrace();
		}
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

			final SQLTransaction tx            = getCurrentTransaction();
			final PreparedStatement createNode = tx.prepareStatement("INSERT INTO Node values()");
			final int createNodeResultCount    = createNode.executeUpdate();

			if (createNodeResultCount == 1) {

				final ResultSet generatedKeys = createNode.getGeneratedKeys();
				if (generatedKeys.next()) {

					final PreparedStatement createProperty = tx.prepareStatement("INSERT INTO NodeProperty(nodeId, name, type, stringValue, intValue) values(?, ?, ?, ?, ?)");
					final PreparedStatement createLabel    = tx.prepareStatement("INSERT INTO Label(nodeId, name) values(?, ?)");
					final long newNodeId                   = generatedKeys.getLong(1);

					for (final String label : labels) {

						createLabel.setLong(1, newNodeId);
						createLabel.setString(2, label);

						createLabel.executeUpdate();
					}

					for (final Entry<String, Object> entry : properties.entrySet()) {

						final Object value = entry.getValue();

						createProperty.setLong(1, newNodeId);
						createProperty.setString(2, entry.getKey());
						createProperty.setInt(3, SQLEntity.getInsertTypeForValue(value));

						if (value != null) {

							if (value instanceof String) {

								createProperty.setString(4, (String)value);
								createProperty.setNull(5, Types.INTEGER);
							}

							if (value instanceof Integer) {

								createProperty.setNull(4, Types.VARCHAR);
								createProperty.setInt(5, (Integer)value);
							}

						} else {

							createProperty.setNull(4, Types.VARCHAR);
							createProperty.setNull(5, Types.INTEGER);
						}

						createProperty.executeUpdate();
					}

					return SQLNode.newInstance(this, new NodeResult(SQLIdentity.forId(newNodeId), properties));
				}
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

		try {

			final SQLTransaction tx     = getCurrentTransaction();
			final PreparedStatement stm = tx.prepareStatement("SELECT * FROM NodeProperty ORDER BY nodeId");

			return Iterables.map(r -> SQLNode.newInstance(this, r), new PropertyStream(stm.executeQuery()));

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	@Override
	public Iterable<Node> getNodesByLabel(final String label) {

		try {

			final SQLTransaction tx     = getCurrentTransaction();
			final PreparedStatement stm = tx.prepareStatement("SELECT p.* FROM Label l JOIN NodeProperty p ON p.nodeId = l.nodeId WHERE l.name = ? ORDER BY p.nodeId");

			stm.setString(1, label);

			return Iterables.map(r -> SQLNode.newInstance(this, r), new PropertyStream(stm.executeQuery()));

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	@Override
	public Iterable<Node> getNodesByTypeProperty(final String type) {

		try {

			final SQLTransaction tx     = getCurrentTransaction();
			final PreparedStatement stm = tx.prepareStatement("SELECT p.* FROM NodeProperty x JOIN NodeProperty p ON p.nodeId = x.nodeId WHERE x.name = ? AND x.stringValue = ? ORDER BY p.nodeId");

			stm.setString(1, "type");
			stm.setString(2, type);

			return Iterables.map(r -> SQLNode.newInstance(this, r), new PropertyStream(stm.executeQuery()));

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	@Override
	public Iterable<Relationship> getAllRelationships() {

		try {

			final SQLTransaction tx     = getCurrentTransaction();
			final PreparedStatement stm = tx.prepareStatement("SELECT id FROM Relationship");

			return Iterables.map(r -> SQLRelationship.newInstance(this, r), new IdentityStream(stm.executeQuery()));

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	@Override
	public Iterable<Relationship> getRelationshipsByType(final String type) {

		try {

			final SQLTransaction tx     = getCurrentTransaction();
			final PreparedStatement stm = tx.prepareStatement("SELECT id FROM Relationship WHERE type = ?");

			stm.setString(1, type);

			return Iterables.map(r -> SQLRelationship.newInstance(this, r), new IdentityStream(stm.executeQuery()));

		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	@Override
	public GraphProperties getGlobalProperties() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
}
