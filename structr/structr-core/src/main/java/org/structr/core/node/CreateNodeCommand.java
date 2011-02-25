/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;

/**
 *
 * @author cmorgner
 */
public class CreateNodeCommand extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(CreateNodeCommand.class.getName());

    @Override
    public Object execute(Object... parameters) {

        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
        StructrNodeFactory nodeFactory = (StructrNodeFactory) arguments.get("nodeFactory");
//        IndexService index = (LuceneFulltextIndexService) arguments.get("index");

        AbstractNode node = null;
        User user = null;

        // Default is update index when creating a new node,
        // so the node is found immediately
        boolean updateIndex = true;

        // TODO: let the AbstractNode create itself, including all necessary db properties
        // example: a HtmlSource has to be created with mimeType=text/html

        if (graphDb != null) {

            Date now = new Date();

            Command createRel = Services.command(CreateRelationshipCommand.class);

            List<NodeAttribute> attrs = new LinkedList<NodeAttribute>();

            // initialize node from parameters...
            for (Object o : parameters) {

                if (o instanceof List) {

                    attrs = (List<NodeAttribute>) o;

                } else if (o instanceof NodeAttribute) {

                    NodeAttribute attr = (NodeAttribute) o;
                    attrs.add(attr);

                } else if (o instanceof User) {

                    user = (User) o;

                } else if (o instanceof Boolean) {

                    updateIndex = (Boolean) o;

                }
            }

            // Determine node type
            String nodeType = null;
            for (NodeAttribute attr : attrs) {
                if (AbstractNode.TYPE_KEY.equals(attr.getKey())) {
                    nodeType = (String) attr.getValue();
                }
            }

            // Create node with type
            node = nodeFactory.createNode(graphDb.createNode(), nodeType);
            logger.log(Level.FINE, "Node {0} created", node.getId());


            for (NodeAttribute attr : attrs) {
                // Don't update index now
                node.setProperty(attr.getKey(), attr.getValue(), false);
                logger.log(Level.FINEST, "Set node attribute {0} to {1}", new Object[]{attr.getKey(), attr.getValue()});
            }
            attrs.clear();

            if (user != null && !(user instanceof SuperUser)) {
                createRel.execute(user, node, RelType.OWNS);
                logger.log(Level.FINEST, "Relationship to owner {0} added", user.getName());

                StructrRelationship securityRel = (StructrRelationship) createRel.execute(user, node, RelType.SECURITY);
                securityRel.setAllowed(Arrays.asList(StructrRelationship.ALL_PERMISSIONS));
                logger.log(Level.FINEST, "All permissions given to user {0}", user.getName());

                node.setProperty(AbstractNode.CREATED_BY_KEY, user.getRealName() + " (" + user.getName() + ")", false);
            }
            node.setProperty(AbstractNode.CREATED_DATE_KEY, now, false);
            node.setProperty(AbstractNode.LAST_MODIFIED_DATE_KEY, now, false);

            if (updateIndex) {
                // index the database node we just created
                Services.command(IndexNodeCommand.class).execute(node);
                logger.log(Level.FINE, "Node {0} indexed.", node.getId());
            }

        }

        return node;
    }
//
//    private void indexNode(IndexService index, final AbstractNode node) {
//
//        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
//
//        Node n = graphDb.getNodeById(node.getId());
//        // TODO: move to service method
//        for (String propertyKey : n.getPropertyKeys()) {
//            index.index(n, propertyKey, n.getProperty(propertyKey));
//        }
//
//    }
}
