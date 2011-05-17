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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.digest.DigestUtils;
import org.neo4j.graphdb.Direction;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.DeleteRelationshipCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 * 
 * @author amorgner
 * 
 */
public class User extends Person {

    private static final Logger logger = Logger.getLogger(User.class.getName());
    private final static String ICON_SRC = "/images/user.png";
    public final static String REAL_NAME_KEY = "realName";
    public final static String PASSWORD_KEY = "password";
    public final static String BLOCKED_KEY = "blocked";
    public final static String SESSION_ID_KEY = "sessionId";
    public final static String CONFIRMATION_KEY_KEY = "confirmationKey";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    /**
     * Return user's personal root node
     * 
     * @return
     */
    public AbstractNode getRootNode() {
        List<StructrRelationship> outRels = getRelationships(RelType.ROOT_NODE, Direction.OUTGOING);
        if (outRels != null) {
            for (StructrRelationship r : outRels) {
                return r.getEndNode();
            }
        }
        return null;
    }

    public void setPassword(final String passwordValue) {

        // store passwords always as SHA-512 hash
        setProperty(PASSWORD_KEY,
                DigestUtils.sha512Hex(passwordValue));
    }

    public String getPassword() {
        return getStringProperty(PASSWORD_KEY);
    }

    public String getRealName() {
        return getStringProperty(REAL_NAME_KEY);
    }

    public String getConfirmationKey() {
        return getStringProperty(CONFIRMATION_KEY_KEY);
    }

    public Boolean getBlocked() {
        return (Boolean) getProperty(BLOCKED_KEY);
    }

    public Boolean isBlocked() {
        return Boolean.TRUE.equals(getBlocked());
    }

    public String getSessionId() {
        return getStringProperty(SESSION_ID_KEY);
    }

    public void setRealName(final String realName) {
        setProperty(REAL_NAME_KEY, realName);
    }

    public void setBlocked(final Boolean blocked) {
        setProperty(BLOCKED_KEY, blocked);
    }

    public void setConfirmationKey(final String value) {
        setProperty(CONFIRMATION_KEY_KEY, value);
    }

    public void block() {
        setBlocked(Boolean.TRUE);
    }

    /**
     * Return all objects linked to the given category,
     * sorted
     *
     * @param sortDirection 
     * @param sortBy
     * @param categoryName
     * @return
     */
    public List<AbstractNode> getSortedObjectsOfCategory(final String categoryName, final String sortBy, final String sortDirection) {

        List<AbstractNode> objects = getObjectsOfCategory(categoryName);

        if (sortBy != null) {

            Collections.sort(objects, new Comparator<AbstractNode>() {

                @Override
                public int compare(AbstractNode nodeOne, AbstractNode nodeTwo) {

                    if (nodeOne instanceof Link) {
                        nodeOne = ((Link) nodeOne).getStructrNode();
                    }

                    if (nodeTwo instanceof Link) {
                        nodeTwo = ((Link) nodeTwo).getStructrNode();
                    }

                    Method getterOne = null;
                    try {
                        getterOne = nodeOne.getClass().getMethod(sortBy);
                    } catch (Exception ex) {
                        logger.log(Level.FINE, "Cannot invoke method {0}", sortBy);
                    }

                    Method getterTwo = null;
                    try {
                        getterTwo = nodeOne.getClass().getMethod(sortBy);
                    } catch (Exception ex) {
                        logger.log(Level.FINE, "Cannot invoke method {0}", sortBy);
                    }
                    int result = 0;

                    if (getterOne != null && getterTwo != null) {

                        Object valueOne = null;
                        try {
                            valueOne = getterOne.invoke(nodeOne);
                        } catch (Exception ex) {
                            logger.log(Level.FINE, "Cannot invoke method {0} on {1}", new Object[]{getterOne, nodeOne});
                        }
                        Object valueTwo = null;
                        try {
                            valueTwo = getterTwo.invoke(nodeTwo);
                        } catch (Exception ex) {
                            logger.log(Level.FINE, "Cannot invoke method {0} on {1}", new Object[]{getterTwo, nodeTwo});
                        }

                        if (valueOne != null && valueTwo != null) {
                            if (valueOne instanceof Comparable && valueTwo instanceof Comparable) {

                                if (sortDirection != null && sortDirection.equals("asc")) {
                                    result = ((Comparable) valueOne).compareTo((Comparable) valueTwo);
                                } else {
                                    result = ((Comparable) valueTwo).compareTo((Comparable) valueOne);
                                }


                            } else {

                                if (sortDirection != null && sortDirection.equals("asc")) {
                                    result = valueOne.toString().compareTo(valueTwo.toString());
                                } else {
                                    result = valueTwo.toString().compareTo(valueOne.toString());
                                }
                            }
                        }

                    }

                    return result;
                }
            });
        }

        return objects;

    }

    /**
     * Return all objects linked to the given category
     *
     * @param categoryName
     * @return
     */
    public List<AbstractNode> getObjectsOfCategory(final String categoryName) {

        if (categoryName == null) {
            logger.log(Level.SEVERE, "Empty category name!");
            return null;
        }

        List<AbstractNode> result = Collections.emptyList();

        final User user = this;
        Category cat = null;

        List<AbstractNode> children = this.getDirectChildNodes();
        for (AbstractNode child : children) {
            if (child instanceof Category && categoryName.equals(child.getName())) {
                cat = (Category) child;
            }
        }

        if (cat != null) {
            result = cat.getSortedLinkedNodes();
        }

        return result;

    }

    /**
     * Remove object with given id from a category with given name.
     * 
     * @param categoryName
     * @param objectId
     */
    public void removeFromCategory(final String categoryName, final String objectId) {
        final User user = this;

        if (categoryName == null) {
            logger.log(Level.SEVERE, "Empty category name!");
            return;
        }

        final AbstractNode object = (AbstractNode) Services.command(FindNodeCommand.class).execute(user, objectId);
        if (object == null) {
            logger.log(Level.SEVERE, "Object not found!");
            return;
        }

        Category cat = null;

        List<AbstractNode> children = this.getDirectChildNodes();
        for (AbstractNode child : children) {
            if (child instanceof Category && categoryName.equals(child.getName())) {
                cat = (Category) child;
            }
        }

        if (cat == null) {
            logger.log(Level.SEVERE, "Category not found!");
            return;
        }

        StructrRelationship relationshipToRemove = null;

        List<StructrRelationship> outgoingRelationships = cat.getOutgoingLinkRelationships();
        for (StructrRelationship rel : outgoingRelationships) {

            AbstractNode endNode = rel.getEndNode();
            if (endNode.equals(object)) {
                relationshipToRemove = rel;
            }
        }

        if (relationshipToRemove != null) {
            final StructrRelationship relToDel = relationshipToRemove;
            Services.command(TransactionCommand.class).execute(new StructrTransaction() {

                @Override
                public Object execute() {
                    // Delete relationship
                    Services.command(DeleteRelationshipCommand.class).execute(relToDel);
                    return null;
                }
            });
        }
    }

    /**
     * Add object with given id to a category with given name.
     *
     * If no category with the given name exists, create one.
     *
     * @param categoryName
     * @param objectId
     */
    public AbstractNode addToCategory(final String categoryName, final String objectId) {

        final User user = this;

        if (categoryName == null) {
            logger.log(Level.SEVERE, "Empty category name!");
            return null;
        }

        final AbstractNode object = (AbstractNode) Services.command(FindNodeCommand.class).execute(user, objectId);
        if (object == null) {
            logger.log(Level.SEVERE, "Object not found!");
            return null;
        }

        Category cat = null;

        List<AbstractNode> children = this.getDirectChildNodes();
        for (AbstractNode child : children) {
            if (child instanceof Category && categoryName.equals(child.getName())) {
                cat = (Category) child;
            }
        }

        final Category category = cat;

        final Command createRel = Services.command(CreateRelationshipCommand.class);

        Services.command(TransactionCommand.class).execute(new StructrTransaction() {

            Category cat = null;

            @Override
            public Object execute() {

                if (category == null) {

                    // Category with given name not found, create one!
                    cat = (Category) Services.command(CreateNodeCommand.class).execute(user,
                            new NodeAttribute(AbstractNode.NAME_KEY, categoryName),
                            new NodeAttribute(AbstractNode.TYPE_KEY, Category.class.getSimpleName()),
                            true);

                    // Link category to user
                    createRel.execute(user, cat, RelType.HAS_CHILD, true);


                } else {
                    cat = category;
                }

                // Create link between category and object
                createRel.execute(cat, object, RelType.LINK, true);

                return null;

            }
        });

        return object;

    }
}
