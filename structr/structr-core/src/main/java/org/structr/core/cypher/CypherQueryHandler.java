/*
 *  Copyright (C) 2012 Axel Morgner
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.cypher;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Value;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.node.NodeFactory;
import org.structr.core.node.RelationshipFactory;

/**
 *
 * @author Christian Morgner
 */
public abstract class CypherQueryHandler implements Value<CypherQueryHandler> {

	private static final Logger logger = Logger.getLogger(CypherQueryHandler.class.getName());
	
	protected RelationshipFactory relFactory  = new RelationshipFactory();
	protected NodeFactory nodeFactory;
	protected SecurityContext securityContext = null;
	protected String query                    = null;
	
	public abstract Object handleQueryResults(Iterable<Map<String, Object>> rows) throws FrameworkException;
	
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
	
	protected AbstractNode getAsAbstractNode(Map<String, Object> row, String columnName) throws FrameworkException {
		return nodeFactory.createNode((Node)row.get(columnName));
	}
	
	protected AbstractRelationship getAsAbstractRelationship(Map<String, Object> row, String columnName) throws FrameworkException {
		return relFactory.createRelationship(securityContext, (Relationship)row.get(columnName));
	}
	
	protected GraphObject getAsGraphObject(Map<String, Object> row, String columnName) {
		
		Object obj = row.get(columnName);
		
		try {
			
			if (obj instanceof Node) {
				
				return nodeFactory.createNode((Node)obj);
			}
			
			if (obj instanceof Relationship) {
				
				return relFactory.createRelationship(securityContext, (Relationship)obj);
			}

		} catch(Throwable ignore) {

			// FIXME: ignore or throw??
			logger.log(Level.WARNING, "Unable to instantiate node {0}", obj);
		}
		
		return null;
	}
}
