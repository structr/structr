/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.util.List;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.index.IndexService;
import org.neo4j.index.lucene.LuceneFulltextIndexService;
import org.neo4j.kernel.Traversal;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;

/**
 * Searches for a user node by her/his name in the database and returns the result.
 *
 * @author amorgner
 */
public class SearchUserCommand extends NodeServiceCommand {

    @Override
    public Object execute(Object... parameters) {
        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");
        LuceneFulltextIndexService index = (LuceneFulltextIndexService) arguments.get("index");

        Command findNode = Services.command(FindNodeCommand.class);

        if (graphDb != null) {

            switch (parameters.length) {

                case 1:

                    // we have only a simple user name
                    if (parameters[0] instanceof String) {

                        String userName = (String) parameters[0];

                        for (Node n : index.getNodes(AbstractNode.NAME_KEY, userName)) {

                            AbstractNode s = (AbstractNode) findNode.execute(n);

                            // FIXME: remove hardcoded reference to User class name
                            if (s.getType().equals("User")) {
                                return s;
                            }
                        }
                    }
                    break;

                case 2:

                    // we have user name and domain
                    if (parameters[0] instanceof String && parameters[1] instanceof String) {

                        String userName = (String) parameters[0];
                        String rootNodePath = (String) parameters[1];

                        List<AbstractNode> nodes = (List<AbstractNode>) findNode.execute(new XPath(rootNodePath));

                        // we take the first one
                        if (nodes != null && nodes.size() > 0) {

                            AbstractNode r = nodes.get(0);

                            Node startNode = null;

                            if (r != null) {
                                startNode = r.getNode();

                                if (startNode != null) {
                                    startNode = graphDb.getReferenceNode();
                                }

                                for (Node n : getSubnodes(startNode)) {

                                    AbstractNode s = (AbstractNode) findNode.execute(n);

                                    // FIXME: remove hardcoded reference to User class name
                                    // TODO: implement better algorithm for user retrieval
                                    if (s.getType().equals("User") && userName.equals(s.getName())) {
                                        return s;
                                    }
                                }
                            }
                        }

                    }
                    break;

                default:
                    break;

            }
        }

        return null;
    }

    private Iterable<Node> getSubnodes(Node rootNode) {
        return Traversal.description().breadthFirst().relationships(RelType.HAS_CHILD, Direction.OUTGOING).prune(Traversal.pruneAfterDepth(999)).traverse(rootNode).nodes();
    }
}
