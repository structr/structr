/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.util.Arrays;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.StructrNode;
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

        StructrNode node = null;
        User user = null;

        // TODO: let the StructrNode create itself, including all necessary db properties
        // example: a HtmlSource has to be created with mimeType=text/html

        if (graphDb != null) {

            // create empty node (has no type yet)
            node = nodeFactory.createNode(graphDb.createNode());
            Date now = new Date();

            // initialize node from parameters...
            for (Object o : parameters) {
                if (o instanceof NodeAttribute) {
                    NodeAttribute attr = (NodeAttribute) o;
                    node.setProperty(attr.getKey(), attr.getValue());
                } else if (o instanceof User) {
                    user = (User) o;

                    if (!(user instanceof SuperUser)) {

                        Command createRel = Services.createCommand(CreateRelationshipCommand.class);
                        createRel.execute(user, node, RelType.OWNS);

                        StructrRelationship securityRel = (StructrRelationship) createRel.execute(user, node, RelType.SECURITY);
                        securityRel.setAllowed(Arrays.asList(StructrRelationship.ALL_PERMISSIONS));
                    }

                }
            }

            if (!(user instanceof SuperUser)) {
                node.setProperty(StructrNode.CREATED_BY_KEY, user.getRealName() + " (" + user.getName() + ")");
            }
            node.setProperty(StructrNode.CREATED_DATE_KEY, now);
            node.setProperty(StructrNode.LAST_MODIFIED_DATE_KEY, now);

            // index the database node we just created
            Command indexNode = Services.createCommand(IndexNodeCommand.class);
            indexNode.execute(node.getId());

        }

        return node;
    }
//
//    private void indexNode(IndexService index, final StructrNode node) {
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
