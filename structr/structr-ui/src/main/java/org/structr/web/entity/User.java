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



package org.structr.web.entity;

import org.neo4j.graphdb.Direction;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.converter.PasswordConverter;
import org.structr.core.entity.RelationClass.Cardinality;
import org.structr.core.node.NodeService.NodeIndex;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.logging.Logger;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Person;
import org.structr.core.entity.Principal;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class User extends Person implements Principal {

	private static final Logger logger = Logger.getLogger(User.class.getName());

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerPropertyConverter(User.class, Key.password.name(), PasswordConverter.class);
		EntityContext.registerPropertySet(User.class, PropertyView.All, Key.values());
		EntityContext.registerPropertySet(User.class, PropertyView.Public, Key.realName);

		EntityContext.registerEntityRelation(User.class, Group.class, RelType.MEMBER_OF, Direction.OUTGOING, Cardinality.ManyToMany);

		// EntityContext.registerEntityRelation(User.class, LogNodeList.class, RelType.OWNS, Direction.OUTGOING, Cardinality.OneToOne);
		EntityContext.registerSearchablePropertySet(User.class, NodeIndex.user.name(), UserIndexKey.values());
		EntityContext.registerSearchablePropertySet(User.class, NodeIndex.fulltext.name(), Key.values());
		EntityContext.registerSearchablePropertySet(User.class, NodeIndex.keyword.name(), Key.values());

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
