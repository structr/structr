/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.neo4j.graphdb.*;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.GraphObject;
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
public class StructrRelationship implements GraphObject {

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

    @Override
    public long getId() {
        return getInternalId();
    }

    public long getRelationshipId() {
        return getInternalId();
    }

    public long getInternalId() {
        return dbRelationship.getId();
    }

    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();

        for (String key : dbRelationship.getPropertyKeys()) {
            properties.put(key, dbRelationship.getProperty(key));
        }

        return properties;
    }

    public Object getProperty(final PropertyKey propertyKey) {
        return dbRelationship.getProperty(propertyKey.name(), null);
    }

    @Override
    public Object getProperty(final String key) {
        return dbRelationship.getProperty(key, null);
    }

    public void setProperty(final PropertyKey propertyKey, final Object value) {
        dbRelationship.setProperty(propertyKey.name(), value);
    }

    @Override
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

    public AbstractNode getEndNode() {
        Command nodeFactory = Services.command(NodeFactoryCommand.class);
        return (AbstractNode) nodeFactory.execute(dbRelationship.getEndNode());
    }

    public AbstractNode getStartNode() {
        Command nodeFactory = Services.command(NodeFactoryCommand.class);
        return (AbstractNode) nodeFactory.execute(dbRelationship.getStartNode());
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
        Command transaction = Services.command(TransactionCommand.class);

        transaction.execute(new StructrTransaction() {

            @Override
            public Object execute() throws Throwable {
                Command findNode = Services.command(FindNodeCommand.class);
                Command deleteRel = Services.command(DeleteRelationshipCommand.class);
                Command createRel = Services.command(CreateRelationshipCommand.class);
                Command nodeFactory = Services.command(NodeFactoryCommand.class);

                AbstractNode startNode = (AbstractNode) nodeFactory.execute(getStartNode());
                AbstractNode newEndNode = (AbstractNode) findNode.execute(user, endNodeId);

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
            Command transacted = Services.command(TransactionCommand.class);

            transacted.execute(new StructrTransaction() {

                @Override
                public Object execute() throws Throwable {
                    Command deleteRel = Services.command(DeleteRelationshipCommand.class);
                    Command createRel = Services.command(CreateRelationshipCommand.class);
                    Command nodeFactory = Services.command(NodeFactoryCommand.class);

                    AbstractNode startNode = (AbstractNode) nodeFactory.execute(getStartNode());
                    AbstractNode endNode = (AbstractNode) nodeFactory.execute(getEndNode());

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

    @Override
    public boolean equals(final Object o) {
	    return(new Integer(this.hashCode()).equals(new Integer(o.hashCode())));
    }

    @Override
    public int hashCode()
    {
	if(this.dbRelationship == null)
	{
		return(super.hashCode());
	}

	return(new Long(dbRelationship.getId()).hashCode());
    }

    // ----- interface GraphObject -----
	@Override
	public Iterable<String> getPropertyKeys(PropertyView propertyView) {
		return getProperties().keySet();
	}

	@Override
	public Map<RelationshipType, Long> getRelationshipInfo(Direction direction) {
		return null;
	}

	@Override
	public List<StructrRelationship> getRelationships(RelationshipType type, Direction dir) {
		return null;
	}

	@Override
	public String getType() {
		return this.getRelType().name();
	}

	@Override
	public Long getStartNodeId() {
		return this.getStartNode().getId();
	}

	@Override
	public Long getEndNodeId() {
		return this.getEndNode().getId();
	}

	@Override
	public boolean delete() {
		dbRelationship.delete();
		return true;
	}
}
