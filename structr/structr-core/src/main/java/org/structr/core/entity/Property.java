package org.structr.core.entity;

import java.util.Date;
import org.neo4j.graphdb.*;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 * 
 * @author amorgner
 * 
 */
public class Property {

    private final Node dbNode;
    private String key;
    private Object value;

    /**
     * Create bean with reference to backend database and node
     *
     * @param graphDb
     * @param dbNode
     */
    public Property(final Node dbNode) {
        this.dbNode = dbNode;
        this.key = null;
        this.value = null;
    }

    /**
     * Create bean with reference to backend database and node,
     * get value for key from backend
     *
     * @param graphDb
     * @param dbNode
     * @param key
     */
    public Property(final Node dbNode, final String key) {
        this.dbNode = dbNode;
        this.key = key;
        this.value = getValue();
    }

    /**
     * Create bean and set key and value in backend
     *
     * @param graphDb
     * @param dbNode
     * @param key
     * @param value
     */
    public Property(final Node dbNode, final String key, final String value) {
        this.dbNode = dbNode;
        this.key = key;
        this.value = value;
        setPropertyInBackend();
    }

    public String getKey() {
        return key;
    }

    /**
     * Get value from underlying db node with given key
     *
     * TODO: support others than String values
     */
    public final Object getValue() {

        Object ret = null;

        if (dbNode.hasProperty(key)) {
            if (key.equals(AbstractNode.CREATED_DATE_KEY) ||
                    key.equals(AbstractNode.LAST_MODIFIED_DATE_KEY)) {

                ret = new Date((Long) dbNode.getProperty(key));

            } else {
                
                ret = dbNode.getProperty(key);

            }

        }

        return ret;
    }

    private void setPropertyInBackend() {
        Command transaction = Services.command(TransactionCommand.class);

        transaction.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {

                if (key.equals(AbstractNode.CREATED_DATE_KEY) ||
                    key.equals(AbstractNode.LAST_MODIFIED_DATE_KEY)) {

                    Date d = (Date) value;

                    // store date as long
                    dbNode.setProperty(key, d.getTime());

                } else {

                    dbNode.setProperty(key, value);
                    
                }

                return (null);
            }
        });
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public void setValue(final Object value) {
        this.value = value;
        setPropertyInBackend();
    }
}
