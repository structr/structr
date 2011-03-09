/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import org.structr.core.node.search.SearchNodeCommand;
import org.structr.core.node.search.TextualSearchAttribute;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.structr.core.node.search.Search;
import org.structr.core.node.search.SearchOperator;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.node.search.SearchAttribute;

/**
 * Searches for a user node by her/his name in the database and returns the result.
 *
 * @author amorgner
 */
public class FindUserCommand extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(FindUserCommand.class.getName());

    @Override
    public Object execute(Object... parameters) {

        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
        //IndexService index = (LuceneFulltextIndexService) arguments.get("index");

        Command findNode = Services.command(FindNodeCommand.class);
        Command searchNode = Services.command(SearchNodeCommand.class);

        String userXPath = null;

        if (graphDb != null) {

            switch (parameters.length) {

                case 0:

                    //userXPath = "//User";
//                    break;

                    return (List<User>) searchNode.execute(null, null, false, false, Search.andExactType(User.class.getSimpleName()));


                case 1:

                    // we have only a simple user name
                    if (parameters[0] instanceof String) {

                        String userName = (String) parameters[0];
//                        userXPath = "//User[@name='" + userName + "']";

                        List<SearchAttribute> searchAttrs = new ArrayList<SearchAttribute>();
                        searchAttrs.add(Search.andExactName(userName));
                        searchAttrs.add(Search.andExactType(User.class.getSimpleName()));

                        List<AbstractNode> usersFound = (List<AbstractNode>) searchNode.execute(null, null, false, false, searchAttrs);

                        if (usersFound != null && usersFound.size() > 0) {
                            return usersFound.get(0);
                        } else {
                            logger.log(Level.SEVERE, "No user with name {0} found. Contact superadmin", userName);
                            return null;
                        }

                    }
//                    break;

                case 2:

                    // we have user name and domain
                    if (parameters[0] instanceof String && parameters[1] instanceof String) {

                        String userName = (String) parameters[0];
                        String domainName = (String) parameters[1];

                        userXPath = "//Domain[@name='" + domainName + "']/*/User[@name='" + userName + "']";
                        //userXPath = "//User[@name='" + userName + "']";

                    }
                    break;

                default:
                    break;

            }
        }

        // search for user nodes with super user permissions
        List<AbstractNode> nodes = (List<AbstractNode>) findNode.execute(new SuperUser(), new XPath(userXPath));

        if (nodes != null) {

            if (nodes.size() == 1) {

                AbstractNode r = nodes.get(0);

                if (r instanceof User) {
                    return (User) r;
                } else {
                    logger.log(Level.SEVERE, "XPath search {0} for User, but returned class was not User: {1}", new Object[]{userXPath, r.getType()});
                    return null;
                }

            } else if (nodes.size() > 1) {

                return nodes;

            } else {
//                Long nodeId = r.getId();
//                String name = r.getName();
//                String type = r.getType();
//                String className = r.getClass().getCanonicalName();

                logger.log(Level.SEVERE, "XPath search {0} for User, but returned class was not User!", userXPath);
            }
        }

        return null;
    }
}
