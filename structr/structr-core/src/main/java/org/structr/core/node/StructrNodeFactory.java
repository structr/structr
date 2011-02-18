/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.neo4j.graphdb.Node;
import org.structr.core.Adapter;
import org.structr.core.Services;
import org.structr.core.entity.EmptyNode;
import org.structr.core.entity.StructrNode;
import org.structr.core.module.GetEntityClassCommand;

/**
 * A factory for structr nodes. This class exists because we need a fast
 * way to instantiate and initialize structr nodes, as this is the most-
 * used operation.
 *
 * @author cmorgner
 */
public class StructrNodeFactory<T extends StructrNode> implements Adapter<Node, T> {

    private static final Logger logger = Logger.getLogger(StructrNodeFactory.class.getName());
    //private Map<String, Class> nodeTypeCache = new ConcurrentHashMap<String, Class>();

    public StructrNodeFactory() {
    }

    public StructrNode createNode(final Node node) {

        String nodeType = node.hasProperty(StructrNode.TYPE_KEY) ? (String) node.getProperty(StructrNode.TYPE_KEY) : "";
        return createNode(node, nodeType);

    }

    public StructrNode createNode(final Node node, final String nodeType) {

        Class nodeClass = (Class) Services.createCommand(GetEntityClassCommand.class).execute(nodeType);
        StructrNode ret = null;

        if (nodeClass != null) {
            try {
                ret = (StructrNode) nodeClass.newInstance();

            } catch (Throwable t) {
                ret = null;
            }

        }

        if (ret == null) {
            ret = new EmptyNode();
        }

        ret.init(node);

        return ret;
    }

    public List<StructrNode> createNodes(Iterable<Node> input) {

        List<StructrNode> nodes = new ArrayList<StructrNode>();
        if (input != null && input.iterator().hasNext()) {

            for (Node node : input) {

                StructrNode structrNode = createNode(node);
                //structrNode.init(node);

                nodes.add(structrNode);
            }
        }
        return nodes;
    }

//    @Override
//    protected void finalize() throws Throwable {
//        nodeTypeCache.clear();
//    }

    @Override
    public T adapt(Node s) {
        return ((T) createNode(s));
    }
}
