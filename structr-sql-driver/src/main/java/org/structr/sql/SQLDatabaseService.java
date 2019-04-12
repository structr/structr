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

import java.sql.SQLException;
import java.util.Map;
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
import org.structr.api.util.NodeWithOwnerResult;

/**
 */
public class SQLDatabaseService extends AbstractDatabaseService implements GraphProperties {

	private static final Logger logger                     = LoggerFactory.getLogger(SQLDatabaseService.class);

	private final ThreadLocal<SQLTransaction> transactions = new ThreadLocal<>();
	private BasicDataSource dataSource                     = null;

	@Override
	public boolean initialize(final String serviceName) {

		dataSource = new BasicDataSource();
		dataSource.setUrl("jdbc:mysql://localhost/structr");
		dataSource.setMaxTotal(20);
		dataSource.setMaxIdle(10);
		dataSource.setDefaultAutoCommit(false);
		dataSource.setUsername("structr");
		dataSource.setPassword("test");

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
	}

	@Override
	public void deleteNodesByLabel(final String label) {
	}

	@Override
	public Transaction beginTx() {

		SQLTransaction current = transactions.get();
		if (current == null) {

			try {

				current = new SQLTransaction(dataSource.getConnection());
				transactions.set(current);

			} catch (SQLException ex) {
				logger.warn("Unable to get connection: {}", ex.getMessage());
			}
		}

		return current;
	}

	@Override
	public Node createNode(final String type, final Set<String> labels, final Map<String, Object> properties) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Iterable<Node> getAllNodes() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Iterable<Node> getNodesByLabel(final String label) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Iterable<Node> getNodesByTypeProperty(final String type) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Iterable<Relationship> getAllRelationships() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Iterable<Relationship> getRelationshipsByType(final String type) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public GraphProperties getGlobalProperties() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
	public void updateIndexConfiguration(final Map<String, Map<String, Boolean>> schemaIndexConfig, final Map<String, Map<String, Boolean>> removedClasses) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public CountResult getNodeAndRelationshipCount() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <T> T execute(NativeQuery<T> nativeQuery) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <T> NativeQuery<T> query(final Object query, final Class<T> resultType) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean supportsFeature(final DatabaseFeature feature, final Object... parameters) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void setProperty(final String name, final Object value) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Object getProperty(final String name) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	// ----- package-private methods -----
	SQLNode getNodeById(final SQLIdentity identity) {
		return SQLNode.newInstance(this, identity);
	}

	SQLTransaction getCurrentTransaction() {

		final SQLTransaction current = transactions.get();
		if (current != null) {

			return current;
		}

		throw new NotInTransactionException("Not in transaction");
	}
}
