/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.common.xpath;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.jxpath.ri.QName;
import org.apache.commons.jxpath.ri.compiler.NodeNameTest;
import org.apache.commons.jxpath.ri.compiler.NodeTest;
import org.apache.commons.jxpath.ri.compiler.NodeTypeTest;
import org.apache.commons.jxpath.ri.model.NodeIterator;
import org.apache.commons.jxpath.ri.model.NodePointer;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.helpers.Predicate;
import org.neo4j.kernel.Traversal;
import org.structr.common.RelType;
import org.structr.core.entity.StructrNode;

/**
 * FIXME: this does not work right now, they are changing the API ever so often....
 *
 *
 * @author cmorgner
 */
public class Neo4jNodeIterator implements NodeIterator {

    private static final Logger logger = Logger.getLogger(Neo4jNodeIterator.class.getName());
    NodePointer parent = null;
    private int position = 0;
    Node[] nodes = null;

    public Neo4jNodeIterator(NodePointer parent, NodeTest nodeTest, boolean reverse, NodePointer startWith) {
        this.parent = parent;

        if (parent != null) {

//        // debug section start
//        Object parentNode = null;
//        if (parent != null) {
//            parentNode = parent.getNode();
//        }
//
//        Object startWithNode = null;
//        if (startWith != null) {
//            startWithNode = startWith.getNode();
//        }
//
//
//        System.out.println("Neo4jNodeIterator, parentNode: " + parentNode
//                + ", nodeTest: " + nodeTest
//                + ", reverse: " + reverse
//                + ", startWith: " + startWithNode);

            long t0 = System.currentTimeMillis();

            // create traverser and retrieve collection
            Node node = null;

            if (startWith != null) {
                node = (Node) startWith.getNode();

            } else {
                node = (Node) parent.getNode();
            }

            String testString = "";
            if (nodeTest instanceof NodeTypeTest) {
                testString = "type:" + ((NodeTypeTest) nodeTest).toString();
            } else if (nodeTest instanceof NodeNameTest) {
                testString = "name:" + ((NodeNameTest) nodeTest).getNodeName().toString();
            } else {
                testString = nodeTest.toString();
            }


            logger.log(Level.FINE, "Starting traversal from node: {0} ({1}), test for {2}", new Object[]{node.getProperty("name"), node.getId(), testString});

            //long t1 = System.currentTimeMillis();
            //System.out.println("Neo4jNodeIterator mark 1: " + (t1-t0) + " ms");

//        if (nodeTest instanceof NodeNameTest) {
//            NodeNameTest nameTest = (NodeNameTest) nodeTest;
//            QName name = nameTest.getNodeName();
//
//            final String shortName = name.getName();
//            final boolean isWildcard = nameTest.isWildcard();
//
//            if (node.hasProperty(StructrNode.TYPE_KEY)
//                            && (isWildcard
//                            || shortName.equals(
//                            //                            XPathEncoder.encode((String) node.getProperty(StructrNode.TYPE_KEY)))));
//                            (String) node.getProperty(StructrNode.TYPE_KEY)))) {
//
//                nodes = new Node[1];
//                nodes[0] = node;
//
//
//            }
//
//        }

//        TraversalDescription descr = Traversal.description().breadthFirst().expand(Traversal.expanderForTypes(
//                RelType.HAS_CHILD, Direction.OUTGOING,
//                RelType.SECURITY, Direction.OUTGOING)).filter(getPredicateForTest(nodeTest));
//
//        TraversalDescription descr = Traversal.description().depthFirst().relationships(RelType.HAS_CHILD, Direction.OUTGOING)
//                .prune(Traversal.pruneAfterDepth(1)) // most important statement, avoids fetching ALL nodes!!!
//                .filter(getPredicateForTest(nodeTest));

            // include LINK relationships!
            //TraversalDescription descr = Traversal.description().depthFirst()
        TraversalDescription descr = Traversal.description().breadthFirst()
//                .prune(Traversal.pruneAfterDepth(1)) // most important statement, avoids fetching ALL nodes!!!
                .relationships(RelType.HAS_CHILD, Direction.OUTGOING)
                .relationships(RelType.LINK, Direction.OUTGOING)
                .evaluator(Evaluators.toDepth(0)) // most important statement, avoids fetching ALL nodes!!!
                .filter(getPredicateForTest(nodeTest));

            long t2 = System.currentTimeMillis();
            //System.out.println("Neo4jNodeIterator mark 2: " + (t2-t1) + " ms");

            try {
            Traverser traverser = descr.traverse(node);
//            List<StructrNode> nodeList = new LinkedList<StructrNode>();
                // use a set to avoid duplicate entries
                Set<Node> nodeSet = new HashSet<Node>();

            for (Node n : traverser.nodes()) {
                nodeSet.add(n);
            }



//                Node parentNode = (Node) parent.getNode();
//
//                if (parentNode != null) {
//
//                    for (Relationship r : parentNode.getRelationships(RelType.HAS_CHILD, Direction.OUTGOING)) {
//
//                        Node endNode = r.getEndNode();
//
//                        if (endNode != null) {
//                            NodeNameTest nameTest = null;
//
//                            if (nodeTest instanceof NodeNameTest) {
//                                nameTest = (NodeNameTest) nodeTest;
//                            }
//
//                            if (nameTest != null && (nameTest.isWildcard() || (endNode.hasProperty("type") && endNode.getProperty("type").equals(nameTest.toString())))) {
//                                nodeSet.add(endNode);
//                            }
//                        }
//                    }
//
//                    for (Relationship r : parentNode.getRelationships(RelType.LINK, Direction.OUTGOING)) {
//
//                        Node endNode = r.getEndNode();
//
//                        if (endNode != null) {
//                            NodeNameTest nameTest = null;
//
//                            if (nodeTest instanceof NodeNameTest) {
//                                nameTest = (NodeNameTest) nodeTest;
//                            }
//
//                            if (nameTest != null && (nameTest.isWildcard() || (endNode.hasProperty("type") && endNode.getProperty("type").equals(nameTest.toString())))) {
//                                nodeSet.add(endNode);
//                            }
//                        }
//                    }
//                }

                    nodes = nodeSet.toArray(new Node[0]);

                //System.out.println("Neo4jNodeIterator, found " + nodes.length + " nodes");

                logger.log(Level.FINE, "Got node set with {0} entries in {1} ms", new Object[]{nodes.length, t2 - t0});

                //long t3 = System.currentTimeMillis();
                //System.out.println("Neo4jNodeIterator mark 3: " + (t3-t2) + " ms");

            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Error while traversing: {0}", t.getMessage());
            }

        }
    }

    @Override
    public int getPosition() {
        return (position);
    }

    @Override
    public boolean setPosition(int position) {

        //System.out.println("Neo4jNodeIterator.setPosition(" + position + ")");
        if (position < 1 || position > nodes.length) {
            return (false);

        } else {
            this.position = position;

            return (true);
        }
    }

    @Override
    public NodePointer getNodePointer() {
        try {
            // position-1 to fix one-off error (why does position start at 1??)
            return (new Neo4jNodePointer(parent, nodes[position - 1]));

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return (null);
    }

    private Predicate<Path> getPredicateForTest(NodeTest test) {
        Predicate<Path> ret = new Predicate<Path>() {

            @Override
            public boolean accept(Path path) {
                return (path.length() > 0); // TODO check if this equivalent to the former !pos.atStartNode()?
            }
        };

        if (test instanceof NodeNameTest) {
            NodeNameTest nameTest = (NodeNameTest) test;
            QName name = nameTest.getNodeName();

            final String shortName = name.getName();
            final boolean isWildcard = nameTest.isWildcard();

            ret = new Predicate<Path>() {

                @Override
                public boolean accept(Path path) {
                    Node node = path.endNode();

                    return (node.hasProperty(StructrNode.TYPE_KEY)
                            && (isWildcard
                            || shortName.equals(
                            //                            XPathEncoder.encode((String) node.getProperty(StructrNode.TYPE_KEY)))));
                            (String) node.getProperty(StructrNode.TYPE_KEY))));
                }
            };
        }

        return (ret);
    }
}
