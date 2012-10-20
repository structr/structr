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
package org.structr.core.converter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.Value;
import org.structr.core.cypher.CypherQueryHandler;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.GraphDatabaseCommand;

/**
 *
 * @author Christian Morgner
 */
public class CypherQueryConverter extends PropertyConverter {

	private static final Logger logger = Logger.getLogger(CypherQueryConverter.class.getName());
	
	private GraphDatabaseService graphDb    = null;
	private ExecutionEngine engine          = null;

	public CypherQueryConverter() {
		
		try {
			graphDb = (GraphDatabaseService)Services.command(SecurityContext.getSuperUserInstance(), GraphDatabaseCommand.class).execute();
			engine  = new ExecutionEngine(graphDb);

		} catch(Throwable t) {
			
			logger.log(Level.WARNING, "Unable to create cypher execution engine.");
		}
	}
	
	@Override
	public Object convertForSetter(Object source, Value value) throws FrameworkException {

		throw new UnsupportedOperationException("This converter is read-only!");
	}

	@Override
	public Object convertForGetter(Object source, Value value) {
		
		if (currentObject != null && value != null) {

			Object valueObject             = value.get(securityContext);

			if (valueObject != null && valueObject instanceof CypherQueryHandler) {
				
				Map<String, Object> parameters = new LinkedHashMap<String, Object>();
				CypherQueryHandler handler     = (CypherQueryHandler)valueObject;
				String query                   = handler.getQuery();
				String name                    = currentObject.getStringProperty(AbstractNode.name);
				String uuid                    = currentObject.getStringProperty(AbstractNode.uuid);
				
				// initialize parameters
				parameters.put("id",   uuid);
				parameters.put("uuid", uuid);
				parameters.put("name", name);

				// initialize query handler with security context
				handler.setSecurityContext(securityContext);
				
				try {
					
					List<AbstractNode> nodes = (List<AbstractNode>)handler.handleQueryResults(engine.execute(query, parameters));
					
					return nodes;
					
					
				} catch(FrameworkException fex) {
					
					logger.log(Level.WARNING, "Exception while executing cypher query {0}: {1}", new Object[] { query, fex.getMessage() } );
				}
			}
		}
		
		return null;
	}
	
}
