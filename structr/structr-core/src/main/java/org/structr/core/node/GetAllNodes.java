/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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
package org.structr.core.node;

import java.util.Collections;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.tooling.GlobalGraphOperations;
import org.structr.common.error.FrameworkException;

/**
 * Get all nodes in the database.
 *
 * This command takes no parameters.
 *
 * @author amorgner
 */
public class GetAllNodes extends NodeServiceCommand {

    @Override
    public Object execute(Object... parameters) throws FrameworkException {

        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
        NodeFactory nodeFactory = new NodeFactory(securityContext);

        if (graphDb != null) {
            return nodeFactory.createAllNodes(GlobalGraphOperations.at(graphDb).getAllNodes());
        }
        
        return Collections.emptyList();
    }
}
