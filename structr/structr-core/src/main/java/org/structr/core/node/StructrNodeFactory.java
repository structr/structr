/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.neo4j.graphdb.Node;
import org.structr.core.Adapter;
import org.structr.core.Services;
import org.structr.core.cloud.NodeDataContainer;
import org.structr.core.entity.EmptyNode;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.module.GetEntityClassCommand;

/**
 * A factory for structr nodes. This class exists because we need a fast
 * way to instantiate and initialize structr nodes, as this is the most-
 * used operation.
 *
 * @author cmorgner
 */
public class StructrNodeFactory<T extends AbstractNode> implements Adapter<Node, T> {

    private static final Logger logger = Logger.getLogger(StructrNodeFactory.class.getName());
    //private Map<String, Class> nodeTypeCache = new ConcurrentHashMap<String, Class>();

    public StructrNodeFactory() {
    }

    public AbstractNode createNode(final Node node) {

        String nodeType = node.hasProperty(AbstractNode.TYPE_KEY) ? (String) node.getProperty(AbstractNode.TYPE_KEY) : "";
        return createNode(node, nodeType);

    }

    public AbstractNode createNode(final Node node, final String nodeType) {

        Class nodeClass = (Class) Services.command(GetEntityClassCommand.class).execute(nodeType);
        AbstractNode ret = null;

        if (nodeClass != null) {
            try {
                ret = (AbstractNode) nodeClass.newInstance();

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

    /**
     * Create structr nodes from the underlying database nodes
     *
     * If user is given, include only nodes which are readable by given user
     * If includeDeleted is true, include nodes with 'deleted' flag
     * If publicOnly is true, filter by 'public' flag
     *
     * @param input
     * @param user
     * @param includeDeleted
     * @param publicOnly
     * @return
     */
    public List<AbstractNode> createNodes(final Iterable<Node> input, final User user, final boolean includeDeleted, final boolean publicOnly) {

        List<AbstractNode> nodes = new ArrayList<AbstractNode>();
        if (input != null && input.iterator().hasNext()) {

            for (Node node : input) {

                AbstractNode n = createNode(node);

                if ((user == null || n.readAllowed(user)) && (includeDeleted || !(n.isDeleted())) && (!publicOnly || n.isPublic())) {
                    nodes.add(n);
                }
            }
        }
        return nodes;
    }

    /**
     * Create structr nodes from the underlying database nodes
     *
     * If user is given, include only nodes which are readable by given user
     * If includeDeleted is true, include nodes with 'deleted' flag
     *
     * @param input
     * @param user
     * @param includeDeleted
     * @return
     */
    public List<AbstractNode> createNodes(final Iterable<Node> input, final User user, final boolean includeDeleted) {
        return createNodes(input, null, includeDeleted, false);
    }

    /**
     * Create structr nodes from the underlying database nodes
     *
     * If includeDeleted is true, include nodes with 'deleted' flag
     * 
     * @param input
     * @param includeDeleted
     * @return
     */
    public List<AbstractNode> createNodes(final Iterable<Node> input, final boolean includeDeleted) {
        return createNodes(input, null, includeDeleted);
    }

    /**
     * Create structr nodes from all given underlying database nodes
     * including nodes with 'deleted' flag
     * 
     * @param input
     * @return
     */
    public List<AbstractNode> createNodes(final Iterable<Node> input) {
        return createNodes(input, true);
    }

//    @Override
//    protected void finalize() throws Throwable {
//        nodeTypeCache.clear();
//    }
    @Override
    public T adapt(Node s) {
        return ((T) createNode(s));
    }

    public AbstractNode createNode(final NodeDataContainer data) {

        if (data == null) {
            logger.log(Level.SEVERE, "Could not create node: Empty data container.");
            return null;
        }

        Map properties = data.getProperties();

        String nodeType = properties.containsKey(AbstractNode.TYPE_KEY) ? (String) properties.get(AbstractNode.TYPE_KEY) : null;

        Class nodeClass = (Class) Services.command(GetEntityClassCommand.class).execute(nodeType);
        AbstractNode newNode = null;

        if (nodeClass != null) {
            try {
                newNode = (AbstractNode) nodeClass.newInstance();

            } catch (Throwable t) {
                newNode = null;
            }

        }

        if (newNode == null) {
            newNode = new EmptyNode();
        }

        // ATTENTION: Initialisation with a NodeDataContainer leaves the node "dirty"!
        // Has to be commited later!
        newNode.init(data);

        return newNode;


    }
}
