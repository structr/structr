package org.structr.core.entity;

import java.util.List;
import org.neo4j.graphdb.*;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.DeleteRelationshipCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.NodeFactoryCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 *
 * @author amorgner
 *
 */
public class StructrRelationship {

    public final static String ALLOWED_KEY = "allowed";
    public final static String DENIED_KEY = "denied";
    public final static String READ_KEY = "read";
    public final static String SHOW_TREE_KEY = "showTree";
    public final static String WRITE_KEY = "write";
    public final static String EXECUTE_KEY = "execute";
    public final static String CREATE_SUBNODE_KEY = "createNode";
    public final static String DELETE_NODE_KEY = "deleteNode";
    public final static String EDIT_PROPERTIES_KEY = "editProperties";
    public final static String ADD_RELATIONSHIP_KEY = "addRelationship";
    public final static String REMOVE_RELATIONSHIP_KEY = "removeRelationship";
    public final static String ACCESS_CONTROL_KEY = "accessControl";
    public final static String[] ALL_PERMISSIONS = new String[]{
        READ_KEY, SHOW_TREE_KEY,
        CREATE_SUBNODE_KEY, DELETE_NODE_KEY,
        WRITE_KEY, EXECUTE_KEY,
        ADD_RELATIONSHIP_KEY, REMOVE_RELATIONSHIP_KEY,
        EDIT_PROPERTIES_KEY, ACCESS_CONTROL_KEY
    };

    ; // reference to database relationship
    protected Relationship dbRelationship;

    public StructrRelationship() {
    }

    public StructrRelationship(Relationship dbRelationship) {
        init(dbRelationship);
    }

    public void init(final Relationship dbRelationship) {
        this.dbRelationship = dbRelationship;
    }

    public long getId() {
        return getInternalId();
    }

    public long getRelationshipId() {
        return getInternalId();
    }

    public long getInternalId() {
        return dbRelationship.getId();
    }

    public Object getProperty(final String key) {
        return dbRelationship.getProperty(key, null);
    }

    public void setProperty(final String key, final Object value) {
        dbRelationship.setProperty(key, value);
    }

    /**
     * Return database relationship
     *
     * @return
     */
    public Relationship getRelationship() {
        return dbRelationship;
    }

    public StructrNode getEndNode() {
        Command nodeFactory = Services.createCommand(NodeFactoryCommand.class);
        return (StructrNode) nodeFactory.execute(dbRelationship.getEndNode());
    }

    public StructrNode getStartNode() {
        Command nodeFactory = Services.createCommand(NodeFactoryCommand.class);
        return (StructrNode) nodeFactory.execute(dbRelationship.getStartNode());
    }

    public RelationshipType getRelType() {
        return (dbRelationship.getType());
    }

    public boolean isType(RelType type) {
        return (type != null && type.equals(dbRelationship.getType()));
    }

    /**
     * Set node id of end node.
     *
     * Internally, this method deletes the old relationship
     * and creates a new one, start from the same start node,
     * but pointing to the node with endNodeId
     *
     */
    public void setEndNodeId(final User user, final long endNodeId) {
        Command transaction = Services.createCommand(TransactionCommand.class);

        transaction.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {
                Command findNode = Services.createCommand(FindNodeCommand.class);
                Command deleteRel = Services.createCommand(DeleteRelationshipCommand.class);
                Command createRel = Services.createCommand(CreateRelationshipCommand.class);
                Command nodeFactory = Services.createCommand(NodeFactoryCommand.class);

                StructrNode startNode = (StructrNode) nodeFactory.execute(getStartNode());
                StructrNode newEndNode = (StructrNode) findNode.execute(user, endNodeId);

                if (newEndNode != null) {
                    RelationshipType type = dbRelationship.getType();

                    deleteRel.execute(dbRelationship);
                    dbRelationship = ((StructrRelationship) createRel.execute(type, startNode, newEndNode)).getRelationship();
                }

                return (null);
            }
        });
    }

    /**
     * Set relationship type
     *
     * Internally, this method deletes the old relationship
     * and creates a new one, with the same start and end node,
     * but with another type
     *
     */
    public void setType(final String type) {
        if (type != null) {
            Command transacted = Services.createCommand(TransactionCommand.class);

            transacted.execute(new StructrTransaction() {

                @Override
                public Object execute() throws Throwable {
                    Command deleteRel = Services.createCommand(DeleteRelationshipCommand.class);
                    Command createRel = Services.createCommand(CreateRelationshipCommand.class);
                    Command nodeFactory = Services.createCommand(NodeFactoryCommand.class);

                    StructrNode startNode = (StructrNode) nodeFactory.execute(getStartNode());
                    StructrNode endNode = (StructrNode) nodeFactory.execute(getEndNode());

                    deleteRel.execute(dbRelationship);
                    dbRelationship = ((StructrRelationship) createRel.execute(type, startNode, endNode)).getRelationship();

                    return (null);
                }
            });
        }
    }

    public String getAllowed() {

        if (dbRelationship.hasProperty(StructrRelationship.ALLOWED_KEY)) {

            String result = "";

            String[] allowedProperties = (String[]) dbRelationship.getProperty(StructrRelationship.ALLOWED_KEY);

            if (allowedProperties != null) {
                for (String p : allowedProperties) {
                    result += p + "\n";
                }
            }
            return result;

        } else {
            return null;
        }
    }

    public void setAllowed(final List<String> allowed) {
        String[] allowedActions = (String[]) allowed.toArray(new String[allowed.size()]);
        dbRelationship.setProperty(StructrRelationship.ALLOWED_KEY, allowedActions);
    }

    public String getDenied() {

        if (dbRelationship.hasProperty(StructrRelationship.DENIED_KEY)) {

            String result = "";

            String[] deniedProperties = (String[]) dbRelationship.getProperty(StructrRelationship.DENIED_KEY);

            if (deniedProperties != null) {
                for (String p : deniedProperties) {
                    result += p + "\n";
                }
            }
            return result;

        } else {
            return null;
        }
    }

    public void setDenied(final List<String> denied) {
        if (dbRelationship.hasProperty(StructrRelationship.DENIED_KEY)) {
            dbRelationship.setProperty(StructrRelationship.DENIED_KEY, denied);
        }
    }

    public boolean isAllowed(final String action) {
        if (dbRelationship.hasProperty(StructrRelationship.ALLOWED_KEY)) {
            String[] allowedProperties = (String[]) dbRelationship.getProperty(StructrRelationship.ALLOWED_KEY);

            if (allowedProperties != null) {
                for (String p : allowedProperties) {
                    if (p.equals(action)) {
                        return true;
                    }
                }
            }

        }
        return false;
    }

    public boolean isDenied(final String action) {
        if (dbRelationship.hasProperty(StructrRelationship.DENIED_KEY)) {
            String[] deniedProperties = (String[]) dbRelationship.getProperty(StructrRelationship.DENIED_KEY);

            if (deniedProperties != null) {
                for (String p : deniedProperties) {
                    if (p.equals(action)) {
                        return true;
                    }
                }
            }

        }
        return false;
    }
}
