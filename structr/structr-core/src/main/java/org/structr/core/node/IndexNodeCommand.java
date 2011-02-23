/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.index.IndexService;
import org.neo4j.index.lucene.LuceneFulltextIndexService;

/**
 * Command for indexing a node's property
 *
 * @author axel
 */
public class IndexNodeCommand extends NodeServiceCommand {

    private static final Logger logger = Logger.getLogger(IndexNodeCommand.class.getName());

    @Override
    public Object execute(Object... parameters) {

        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");

        if (graphDb != null) {
            long id = 0;
            Node node = null;

            String key = null;
            Object value = null;

            switch (parameters.length) {

                case 1:

                    // index all properties of this node
                    if (parameters[0] instanceof Long) {
                        id = ((Long) parameters[0]).longValue();
                    } else if (parameters[0] instanceof String) {
                        id = Long.parseLong((String) parameters[0]);
                    }

                    node = graphDb.getNodeById(id);

                    indexNode(node);

                    break;

                case 3:

                    // index a certain property

                    if (parameters[0] instanceof Long) {
                        id = ((Long) parameters[0]).longValue();
                    } else if (parameters[0] instanceof String) {
                        id = Long.parseLong((String) parameters[0]);
                    }

                    node = graphDb.getNodeById(id);

                    if (parameters[1] instanceof String) {
                        key = (String) parameters[1];
                    }

                    value = parameters[2];

                    index(node, key, value);

                    break;


                default:

                    if (parameters.length != 3) {
                        logger.log(Level.SEVERE, "The index property command takes 1 or 3 parameter");
                        return null;
                    }
                    break;

            }
        }

        return null;

    }

    private void indexNode(final Node node) {

        for (String key : node.getPropertyKeys()) {
            index(node, key, node.getProperty(key));
        }

    }

    private void index(final Node node, final String key, final Object value) {

        IndexService index = (LuceneFulltextIndexService) arguments.get("index");

        // Remove key/value pair from index
        index.removeIndex(node, key);
        if (value != null && node.hasProperty(key)) {
            index.index(node, key, node.getProperty(key));
        }

    }
}
