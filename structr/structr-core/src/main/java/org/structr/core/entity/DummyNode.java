package org.structr.core.entity;

import org.neo4j.graphdb.Node;

/**
 * Dummy node with no connection to database node
 *
 * Any property getter will return null
 *
 * @author amorgner
 * 
 */
public class DummyNode extends DefaultNode {

    private final static String ICON_SRC = "/images/error.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    public DummyNode() {
    }

    public DummyNode(Node dbNode) {
    }

    @Override
    public long getId() {
        return -1;
    }

    @Override
    public Object getProperty(final String key) {
        return null;
    }
}
