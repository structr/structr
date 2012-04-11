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

import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.EntityContext;
import org.structr.core.Services;
import org.structr.core.converter.PasswordConverter;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.DeleteRelationshipCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.NodeService.NodeIndex;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

//~--- JDK imports ------------------------------------------------------------

import java.lang.reflect.Method;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.error.FrameworkException;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class User extends Person {

	private static final Logger logger = Logger.getLogger(User.class.getName());

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertyConverter(User.class, Key.password.name(), PasswordConverter.class);
		EntityContext.registerPropertySet(User.class, PropertyView.All, Key.values());
		EntityContext.registerPropertySet(User.class, PropertyView.Public, Key.realName);
		EntityContext.registerPropertyRelation(User.class, Key.groups, Group.class, RelType.HAS_CHILD, Direction.INCOMING, Cardinality.ManyToMany);
		EntityContext.registerSearchablePropertySet(User.class, NodeIndex.user.name(), UserIndexKey.values());

	}

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey {
		realName, password, blocked, sessionId, confirmationKey, backendUser, frontendUser, groups
	}

	public enum UserIndexKey implements PropertyKey{ name, email; }

	//~--- methods --------------------------------------------------------

	public void block() throws FrameworkException {
		setBlocked(Boolean.TRUE);
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

		try {
			final AbstractNode object = (AbstractNode) Services.command(securityContext, FindNodeCommand.class).execute(objectId);
			if (object == null) {

				logger.log(Level.SEVERE, "Object not found!");

				return;

			}

			Category cat                = null;
			List<AbstractNode> children = this.getDirectChildNodes();

			for (AbstractNode child : children) {

				if ((child instanceof Category) && categoryName.equals(child.getName())) {

					cat = (Category) child;

				}

			}

			if (cat == null) {

				logger.log(Level.SEVERE, "Category not found!");

				return;

			}

			AbstractRelationship relationshipToRemove        = null;
			List<AbstractRelationship> outgoingRelationships = cat.getOutgoingLinkRelationships();

			for (AbstractRelationship rel : outgoingRelationships) {

				AbstractNode endNode = rel.getEndNode();

				if (endNode.equals(object)) {

					relationshipToRemove = rel;

				}

			}

			if (relationshipToRemove != null) {

				final AbstractRelationship relToDel = relationshipToRemove;

				Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

					@Override
					public Object execute() throws FrameworkException {

						// Delete relationship
						Services.command(securityContext, DeleteRelationshipCommand.class).execute(relToDel);

						return null;
					}

				});

			}

		} catch(FrameworkException fex) {
			logger.log(Level.WARNING, "Unable to remove node from category", fex);
			return;
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

		try {
			final AbstractNode object = (AbstractNode) Services.command(securityContext, FindNodeCommand.class).execute(objectId);

			if (object == null) {

				logger.log(Level.SEVERE, "Object not found!");

				return null;

			}

			Category cat                = null;
			List<AbstractNode> children = this.getDirectChildNodes();

			for (AbstractNode child : children) {

				if ((child instanceof Category) && categoryName.equals(child.getName())) {

					cat = (Category) child;

				}

			}

			final Category category = cat;
			final Command createRel = Services.command(securityContext, CreateRelationshipCommand.class);

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				Category cat = null;
				@Override
				public Object execute() throws FrameworkException {

					if (category == null) {

						// Category with given name not found, create one!
						cat = (Category) Services.command(securityContext, CreateNodeCommand.class).execute(user,
										  new NodeAttribute(AbstractNode.Key.name.name(), categoryName),
										  new NodeAttribute(AbstractNode.Key.type.name(), Category.class.getSimpleName()),
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

		} catch(FrameworkException fex) {
			logger.log(Level.WARNING, "Unable to add node to category", fex);
		}

		return null;
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/user.png";
	}

	/**
	 * Return user's personal root node
	 *
	 * @return
	 */
	public AbstractNode getRootNode() {

		List<AbstractRelationship> outRels = getRelationships(RelType.ROOT_NODE, Direction.OUTGOING);

		if (outRels != null) {

			for (AbstractRelationship r : outRels) {

				return r.getEndNode();

			}

		}

		return null;
	}

	/**
	 * Return group node this user is in (if any)
	 */
	public Group getGroupNode() {

		AbstractNode parentNode = getParentNode();

		if ((parentNode != null) && (parentNode instanceof Group)) {

			return (Group) parentNode;

		}

		return null;
	}

	public String getEncryptedPassword() {

		boolean dbNodeHasProperty = dbNode.hasProperty(Key.password.name());

		if (dbNodeHasProperty) {

			Object dbValue = dbNode.getProperty(Key.password.name());

			return (String) dbValue;

		} else {

			return null;

		}
	}

	@Override
	public Object getPropertyForIndexing(final String key) {

		if (Key.password.name().equals(key)) {

			return "";

		} else {

			return getProperty(key);

		}
	}

	/**
	 * Intentionally return null.
	 * @return
	 */
	public String getPassword() {
		return null;
	}

	public String getRealName() {
		return getStringProperty(Key.realName);
	}

	public String getConfirmationKey() {
		return getStringProperty(Key.confirmationKey);
	}

	public Boolean getBlocked() {
		return (Boolean) getProperty(Key.blocked);
	}

	public String getSessionId() {
		return getStringProperty(Key.sessionId);
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

					if ((getterOne != null) && (getterTwo != null)) {

						Object valueOne = null;

						try {
							valueOne = getterOne.invoke(nodeOne);
						} catch (Exception ex) {
							logger.log(Level.FINE, "Cannot invoke method {0} on {1}", new Object[] { getterOne, nodeOne });
						}

						Object valueTwo = null;

						try {
							valueTwo = getterTwo.invoke(nodeTwo);
						} catch (Exception ex) {
							logger.log(Level.FINE, "Cannot invoke method {0} on {1}", new Object[] { getterTwo, nodeTwo });
						}

						if ((valueOne != null) && (valueTwo != null)) {

							if ((valueOne instanceof Comparable) && (valueTwo instanceof Comparable)) {

								if ((sortDirection != null) && sortDirection.equals("asc")) {

									result = ((Comparable) valueOne).compareTo((Comparable) valueTwo);

								} else {

									result = ((Comparable) valueTwo).compareTo((Comparable) valueOne);

								}

							} else {

								if ((sortDirection != null) && sortDirection.equals("asc")) {

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

		List<AbstractNode> result   = Collections.emptyList();
		final User user             = this;
		Category cat                = null;
		List<AbstractNode> children = this.getDirectChildNodes();

		for (AbstractNode child : children) {

			if ((child instanceof Category) && categoryName.equals(child.getName())) {

				cat = (Category) child;

			}

		}

		if (cat != null) {

			result = cat.getSortedLinkedNodes();

		}

		return result;
	}

	public Boolean isBlocked() {
		return Boolean.TRUE.equals(getBlocked());
	}

	public boolean isBackendUser() {
		return (getBooleanProperty(Key.backendUser));
	}

	public boolean isFrontendUser() {
		return (getBooleanProperty(Key.frontendUser));
	}

	//~--- set methods ----------------------------------------------------

	public void setPassword(final String passwordValue) throws FrameworkException {
		setProperty(Key.password, passwordValue);
	}

	public void setRealName(final String realName) throws FrameworkException {
		setProperty(Key.realName, realName);
	}

	public void setBlocked(final Boolean blocked) throws FrameworkException {
		setProperty(Key.blocked, blocked);
	}

	public void setConfirmationKey(final String value) throws FrameworkException {
		setProperty(Key.confirmationKey, value);
	}

	public void setFrontendUser(final boolean isFrontendUser) throws FrameworkException {
		setProperty(Key.frontendUser, isFrontendUser);
	}

	public void setBackendUser(final boolean isBackendUser) throws FrameworkException {
		setProperty(Key.backendUser, isBackendUser);
	}
}
