/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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

import java.util.LinkedList;
import java.util.List;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Property;
import org.structr.core.entity.AbstractNode;

/**
 * Returns a List of Properties for the given node.
 * 
 * @param one or more AbstractNode instances to collect the properties from.
 * @return a list of Properties for the given nodes
 *
 * @author cmorgner
 */
public class NodePropertiesCommand extends NodeServiceCommand {

    @Override
    public Object execute(Object... parameters) throws FrameworkException {

        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");

        List<Property> ret = null;

        if (parameters.length > 0) {
            ret = new LinkedList<Property>();

            for (Object argument : parameters) {
                if (argument instanceof AbstractNode) {
                    AbstractNode structrNode = (AbstractNode) argument;
                    Node node = graphDb.getNodeById(structrNode.getId());

                    for (String key : node.getPropertyKeys()) {
                        // use constructor which gets value from database
                        Property p = new Property(securityContext, node, key);
                        ret.add(p);
                    }
                }
            }
        }

        return (ret);
    }
}
