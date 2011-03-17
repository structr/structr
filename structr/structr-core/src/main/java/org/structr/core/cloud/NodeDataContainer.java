/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.cloud;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.structr.core.entity.AbstractNode;

/**
 * Serializable data container for a node to be transported over network.
 *
 * To be initialized with {@link AbstractNode} in constructor.
 *
 * @author axel
 */
public class NodeDataContainer extends DataContainer {

    protected long sourceNodeId;

    public NodeDataContainer() {};

    public NodeDataContainer(final AbstractNode node) {

        sourceNodeId = node.getId();
        //Map properties = new HashMap<String, Object>();

        for (String key : node.getPropertyKeys()) {
            Object value = node.getProperty(key);
            properties.put(key, value);
        }

    }


    /**
     * Return id of node in source instance
     *
     * @return
     */
    public long getSourceNodeId() {
        return sourceNodeId;
    }

}
