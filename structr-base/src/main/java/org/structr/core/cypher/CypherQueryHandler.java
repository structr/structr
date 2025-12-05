/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.core.cypher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Node;
import org.structr.api.graph.Relationship;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Value;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.RelationshipInterface;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class for Cypher queries. Extend this class and implement the
 * abstract method to transform the raw Cypher query result into a meaningful
 * object.
 *
 *
 */
public abstract class CypherQueryHandler implements Value<CypherQueryHandler> {

	private static final Logger logger = LoggerFactory.getLogger(CypherQueryHandler.class.getName());

	protected RelationshipFactory relFactory  = null;
	protected NodeFactory nodeFactory         = null;
	protected SecurityContext securityContext = null;
	protected String query                    = null;

	public abstract Object handleQueryResults(final Iterable<Map<String, Object>> result) throws FrameworkException;

	public CypherQueryHandler(Object... query) {

		// construct query from varargs
		StringBuilder buffer = new StringBuilder();
		for(Object obj : query) {
			buffer.append(obj);
		}

		this.query = buffer.toString();
	}

	public String getQuery() {
		return query;
	}

	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
		this.nodeFactory     = new NodeFactory(securityContext);
		this.relFactory      = new RelationshipFactory(securityContext);
	}

	// ----- interface Value<CypherQueryHandler> -----
	@Override
	public void set(SecurityContext securityContext, CypherQueryHandler value) throws FrameworkException {
	}

	@Override
	public CypherQueryHandler get(SecurityContext securityContext) {
		return this;
	}

	// ----- protected methods -----
	protected List getAsList(Map<String, Object> row, String columnName) {
		return (List)row.get(columnName);
	}

	protected Set getAsSet(Map<String, Object> row, String columnName) {
		return (Set)row.get(columnName);
	}

	protected Collection getAsCollection(Map<String, Object> row, String columnName) {
		return (Collection)row.get(columnName);
	}

	protected Node getAsNode(Map<String, Object> row, String columnName) {
		return (Node)row.get(columnName);
	}

	protected Relationship getAsRelationship(Map<String, Object> row, String columnName) {
		return (Relationship)row.get(columnName);
	}

	protected NodeInterface getAsNodeInterface(Map<String, Object> row, String columnName) throws FrameworkException {
		return nodeFactory.instantiate((Node)row.get(columnName));
	}

	protected RelationshipInterface getAsAbstractRelationship(Map<String, Object> row, String columnName) throws FrameworkException {
		return relFactory.instantiate((Relationship)row.get(columnName));
	}

	protected GraphObject getAsGraphObject(Map<String, Object> row, String columnName) {

		Object obj = row.get(columnName);

		try {

			if (obj instanceof Node) {

				return nodeFactory.instantiate((Node)obj);
			}

			if (obj instanceof Relationship) {

				return relFactory.instantiate((Relationship)obj);
			}

		} catch(Throwable ignore) {

			// FIXME: ignore or throw??
			logger.warn("Unable to instantiate node {}", obj);
		}

		return null;
	}
}
