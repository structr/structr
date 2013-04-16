/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.web.entity;

import org.structr.core.property.BooleanProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.neo4j.graphdb.Direction;

import org.structr.core.property.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.web.common.RelType;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import static org.structr.core.GraphObject.uuid;
import org.structr.core.entity.Person;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeService.NodeIndex;

//~--- JDK imports ------------------------------------------------------------

import org.structr.core.notion.PropertyNotion;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.EntityProperty;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class User extends Person implements Principal {

	public static final Property<String>          confirmationKey = new StringProperty("confirmationKey");
	public static final Property<Boolean>         backendUser     = new BooleanProperty("backendUser");
	public static final Property<Boolean>         frontendUser    = new BooleanProperty("frontendUser");
	public static final Property<Image>           img             = new EntityProperty("img", Image.class, RelType.PICTURE_OF, Direction.INCOMING, false);
	public static final CollectionProperty<Group> groups          = new CollectionProperty<Group>("groups", Group.class, RelType.CONTAINS, Direction.INCOMING, new PropertyNotion(uuid), false);
	
	public static final org.structr.common.View uiView = new org.structr.common.View(User.class, PropertyView.Ui,
		type, name, password, blocked, sessionId, confirmationKey, backendUser, frontendUser, groups, img
	);
	
	public static final org.structr.common.View publicView = new org.structr.common.View(User.class, PropertyView.Public,
		type, name
	);
	
	static {

		EntityContext.registerSearchablePropertySet(User.class, NodeIndex.user.name(),     name, email);
		EntityContext.registerSearchablePropertySet(User.class, NodeIndex.fulltext.name(), uiView.properties());
		EntityContext.registerSearchablePropertySet(User.class, NodeIndex.keyword.name(),  uiView.properties());
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public Object getPropertyForIndexing(final PropertyKey key) {

		if (User.password.equals(key)) {

			return "";
			
		} else {

			return super.getPropertyForIndexing(key);
		}

	}

	/**
	 * Intentionally return null.
	 * @return
	 */
	public String getPassword() {

		return null;

	}

	public String getConfirmationKey() {

		return getProperty(User.confirmationKey);

	}

	public String getSessionId() {

		return getProperty(Principal.sessionId);

	}

	public boolean isBackendUser() {

		return getBooleanProperty(User.backendUser);

	}

	public boolean isFrontendUser() {

		return getBooleanProperty(User.frontendUser);

	}

	//~--- set methods ----------------------------------------------------

	public void setPassword(final String passwordValue) throws FrameworkException {

		setProperty(password, passwordValue);

	}

	public void setConfirmationKey(final String value) throws FrameworkException {

		setProperty(User.confirmationKey, value);

	}

	public void setFrontendUser(final boolean isFrontendUser) throws FrameworkException {

		setProperty(User.frontendUser, isFrontendUser);

	}

	public void setBackendUser(final boolean isBackendUser) throws FrameworkException {

		setProperty(User.backendUser, isBackendUser);

	}
}
