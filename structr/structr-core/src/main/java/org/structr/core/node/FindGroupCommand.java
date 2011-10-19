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

import org.structr.core.node.search.SearchNodeCommand;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.structr.common.SecurityContext;
import org.structr.core.node.search.Search;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Group;
import org.structr.core.node.search.SearchAttribute;

/**
 * <p>Searches for a group node by its name in the database and returns the result.</p>
 *
 * <p>This command takes one or two parameters:</p>
 *
 * <ol>
 *  <li>first parameter: Group name
 *  <li>second parameter (optional): Top node, return groups beneath this node
 * </ol>
 *
 *
 * @author amorgner
 */
public class FindGroupCommand extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(FindGroupCommand.class.getName());

    @Override
    public Object execute(Object... parameters) {

        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
        Command searchNode = Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class);

        if (graphDb != null) {

            switch (parameters.length) {

                case 0:
                    // Return all groups
                    return (List<Group>) searchNode.execute(null, false, false, Search.andExactType(Group.class.getSimpleName()));

                case 1:

                    // we have only a simple group name
                    if (parameters[0] instanceof String) {

                        String groupName = (String) parameters[0];

                        List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();
                        searchAttrs.add(Search.andExactName(groupName));
                        searchAttrs.add(Search.andExactType(Group.class.getSimpleName()));

                        List<AbstractNode> groupsFound = (List<AbstractNode>) searchNode.execute(null, false, false, searchAttrs);

                        if (groupsFound != null && groupsFound.size() > 0) {
                            return groupsFound.get(0);
                        } else {
                            logger.log(Level.FINE, "No group with name {0} found.", groupName);
                            return null;
                        }

                    }
//                    break;

                case 2:

                    // Limit search to a top node, means: Return groups which are in the CHILD tree beneath a given node
                    if (parameters[0] instanceof String && parameters[1] instanceof AbstractNode) {

                        String groupName = (String) parameters[0];
                        AbstractNode topNode = (AbstractNode) parameters[1];

                        List<SearchAttribute> searchAttrs = new LinkedList<SearchAttribute>();
                        searchAttrs.add(Search.andExactName(groupName));
                        searchAttrs.add(Search.andExactType(Group.class.getSimpleName()));

                        List<AbstractNode> groupsFound = (List<AbstractNode>) searchNode.execute(topNode, false, false, searchAttrs);

                        if (groupsFound != null && groupsFound.size() > 0) {
                            return groupsFound.get(0);
                        } else {
                            logger.log(Level.FINE, "No group with name {0} found in {1}[{2}].", new Object[]{groupName, topNode.getName(), topNode.getId()});
                            return null;
                        }

                    }
                    break;

                default:
                    break;

            }
        }

        return null;
    }
}
