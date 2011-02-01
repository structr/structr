/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

/**
 * A parameterized node attribute to identify a node attribute
 * in {@see FindNodeCommand}.
 *
 * @author cmorgner
 */
public class NodeAttribute {

    private String key = null;
    private Object value = null;

    public NodeAttribute(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * @return the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(Object value) {
        this.value = value;
    }
}
