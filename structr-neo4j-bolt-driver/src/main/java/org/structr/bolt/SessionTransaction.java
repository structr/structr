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
package org.structr.bolt;

import org.neo4j.driver.Record;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.exceptions.DatabaseException;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang.StringUtils;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Path.Segment;
import org.neo4j.driver.types.Relationship;
import org.structr.api.graph.Identity;
import org.structr.api.util.Iterables;

/**
 *
 */
abstract class SessionTransaction implements org.structr.api.Transaction {

	private static final Logger logger                = LoggerFactory.getLogger(SessionTransaction.class);
	protected static final AtomicLong ID_SOURCE       = new AtomicLong();
	protected final Set<EntityWrapper> accessedEntities = new HashSet<>();
	protected final Set<EntityWrapper> modifiedEntities = new HashSet<>();
	protected final Set<Long> deletedNodes              = new HashSet<>();
	protected final Set<Long> deletedRels               = new HashSet<>();
	protected final Object transactionKey               = new Object();
	protected BoltDatabaseService db                    = null;
	protected long transactionId                        = 0L;
	protected boolean success                           = false;
	protected boolean isPing                            = false;

	public SessionTransaction(final BoltDatabaseService db) {

		this.transactionId = ID_SOURCE.getAndIncrement();
		this.db            = db;
	}

	public abstract boolean isClosed();
	public abstract boolean getBoolean(final String statement);
	public abstract boolean getBoolean(final String statement, final Map<String, Object> map);
	public abstract long getLong(final String statement);
	public abstract long getLong(final String statement, final Map<String, Object> map);
	public abstract Object getObject(final String statement, final Map<String, Object> map);
	public abstract Entity getEntity(final String statement, final Map<String, Object> map);
	public abstract Node getNode(final String statement, final Map<String, Object> map);
	public abstract Relationship getRelationship(final String statement, final Map<String, Object> map);
	public abstract Object collectRecords(final String statement, final Map<String, Object> map, final Object consumer);
	public abstract Iterable<String> getStrings(final String statement, final Map<String, Object> map);
	public abstract Iterable<Map<String, Object>> run(final String statement, final Map<String, Object> map);
	public abstract void set(final String statement, final Map<String, Object> map);

	public abstract Iterable<Record> newIterable(final BoltDatabaseService db, final AdvancedCypherQuery query);

	public void deleted(final NodeWrapper wrapper) {
		deletedNodes.add(wrapper.getDatabaseId());
	}

	public void deleted(final RelationshipWrapper wrapper) {
		deletedRels.add(wrapper.getDatabaseId());
	}

	public boolean isDeleted(final EntityWrapper wrapper) {

		if (wrapper instanceof NodeWrapper) {
			return deletedNodes.contains(wrapper.getDatabaseId());
		}

		if (wrapper instanceof RelationshipWrapper) {
			return deletedRels.contains(wrapper.getDatabaseId());
		}

		return false;
	}

	public void modified(final EntityWrapper wrapper) {
		modifiedEntities.add(wrapper);
	}

	public void accessed(final EntityWrapper wrapper) {
		accessedEntities.add(wrapper);
	}

	public void setIsPing(final boolean isPing) {
		this.isPing = isPing;
	}

	@Override
	public long getTransactionId() {
		return this.transactionId;
	}

	public Object getTransactionKey() {
		// we need a simple object that can be used in a weak hash map
		return transactionKey;
	}

	// ----- public static methods -----
	public static RuntimeException translateClientException(final ClientException cex) {

		switch (cex.code()) {

			case "Neo.ClientError.Schema.ConstraintValidationFailed":
				throw new ConstraintViolationException(cex, cex.code(), cex.getMessage());
			case "Neo.ClientError.Statement.SyntaxError":
				throw new SyntaxErrorException(cex, cex.code(), cex.getMessage());
			case "N/A":
				if (cex.getCause() != null && cex.getCause() instanceof ClientException causeCex) {
					throw translateClientException(causeCex);
				}
				throw new UnknownClientException(cex, cex.code(), cex.getMessage());
			default:
				throw new UnknownClientException(cex, cex.code(), cex.getMessage());
		}
	}

	public static RuntimeException translateDatabaseException(final DatabaseException dex) {

		switch (dex.code()) {

			case "Neo.DatabaseError.General.UnknownError":
				throw new DataFormatException(dex, dex.code(), dex.getMessage());

			// add handlers / translated exceptions for DatabaseExceptions here..
		}

		// wrap exception if no other cause could be found
		throw new UnknownDatabaseException(dex, dex.code(), dex.getMessage());
	}

	// ----- protected methods -----
	protected void logQuery(final String statement) {
		logQuery(statement, null);
	}

	protected void logQuery(final String statement, final Map<String, Object> map) {

		if (db.logQueries()) {

			if (!isPing || db.logPingQueries()) {

				if (map != null && map.size() > 0) {

					if (statement.contains("extractedContent")) {
						logger.info("{}: {}\t\t SET on extractedContent - value suppressed", Thread.currentThread().getId(), statement);
					} else {
						logger.info("{}: {}\t\t Parameters: {}", Thread.currentThread().getId(), statement, map.toString());
					}

				} else {

					logger.info("{}: {}", Thread.currentThread().getId(), statement);
				}
			}
		}
	}

	protected void logSummary(final ResultSummary summary) {

		if (db.logQueries()) {

			final SummaryCounters counters = summary.counters();

			final int nodesDeleted = counters.nodesDeleted();
			final int nodesCreated = counters.nodesCreated();

			if (nodesDeleted + nodesCreated > 1) {

				final long availableAfter = summary.resultAvailableAfter(TimeUnit.MILLISECONDS);
				final long consumedAfter  = summary.resultConsumedAfter(TimeUnit.MILLISECONDS);

				logger.info("Query summary: {} nodes created, {} nodes deleted, result available after {} ms, consumed after {} ms, notifications: {}, query: {}",
					nodesCreated, nodesDeleted, availableAfter, consumedAfter, summary.notifications(), summary.query().text()
				);
			}
		}
	}
}
