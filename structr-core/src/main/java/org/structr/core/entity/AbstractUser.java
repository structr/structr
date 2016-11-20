/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.auth.HashHelper;
import org.structr.core.entity.relationship.Groups;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

//~--- interfaces -------------------------------------------------------------

/**
 *
 *
 *
 */
public abstract class AbstractUser extends AbstractNode implements Principal {

	private static final Logger logger = LoggerFactory.getLogger(AbstractUser.class.getName());
	private Boolean cachedIsAdminFlag  = null;
	public static final Object HIDDEN = "****** HIDDEN ******";

	@Override
	public void addSessionId(final String sessionId) {

		try {

			final String[] ids = getProperty(Principal.sessionIds);
			if (ids != null) {

				if (!ArrayUtils.contains(ids, sessionId)) {

					setProperty(Principal.sessionIds, (String[]) ArrayUtils.add(getProperty(Principal.sessionIds), sessionId));

				}

			} else {

				setProperty(Principal.sessionIds, new String[] {  sessionId } );
			}


		} catch (FrameworkException ex) {
			logger.error("Could not add sessionId " + sessionId + " to array of sessionIds", ex);
		}
	}

	@Override
	public void removeSessionId(final String sessionId) {

		try {

			final String[] ids = getProperty(Principal.sessionIds);
			List<String> newSessionIds = new ArrayList<>();

			if (ids != null) {

				for (final String id : ids) {

					if (!id.equals(sessionId)) {

						newSessionIds.add(id);

					}

				}
			}

			setProperties(securityContext, new PropertyMap(Principal.sessionIds, (String[]) newSessionIds.toArray(new String[newSessionIds.size()])));

		} catch (FrameworkException ex) {
			logger.error("Could not remove sessionId " + sessionId + " from array of sessionIds", ex);
		}
	}

	@Override
	public boolean isAdmin() {

		if (cachedIsAdminFlag == null) {

			cachedIsAdminFlag = getProperty(Principal.isAdmin);
			if (cachedIsAdminFlag == null) {

				cachedIsAdminFlag = false;
			}
		}

		return cachedIsAdminFlag;
	}

	@Override
	public List<Principal> getParents() {

		List<Principal> parents         = new LinkedList<>();

		for (Groups rel : getIncomingRelationships(Groups.class)) {

			if (rel != null && rel.getSourceNode() != null) {

				parents.add(rel.getSourceNode());

			}
		}

		return parents;
	}

	@Override
	public boolean isValidPassword(final String password) {

		final String encryptedPasswordFromDatabase = getEncryptedPassword();
		if (encryptedPasswordFromDatabase != null) {

			final String encryptedPasswordToCheck = HashHelper.getHash(password, getSalt());

			if (encryptedPasswordFromDatabase.equals(encryptedPasswordToCheck)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String getEncryptedPassword() {

		boolean dbNodeHasProperty = dbNode.hasProperty(password.dbName());

		if (dbNodeHasProperty) {

			Object dbValue = dbNode.getProperty(password.dbName());

			return (String) dbValue;

		} else {

			return null;
		}
	}

	@Override
	public String getSalt() {

		boolean dbNodeHasProperty = dbNode.hasProperty(salt.dbName());

		if (dbNodeHasProperty) {

			Object dbValue = dbNode.getProperty(salt.dbName());

			return (String) dbValue;

		} else {

			return null;
		}
	}

	/**
	 * Intentionally return a special value indicating that the real value is not being disclosed.
	 *
	 * @param key
	 * @param predicate
	 * @return null for password
	 */
	@Override
	public <T> T getProperty(final PropertyKey<T> key, final Predicate<GraphObject> predicate) {

		if (password.equals(key) || salt.equals(key)) {

			return (T) HIDDEN;

		} else {

			return super.getProperty(key, predicate);

		}

	}

	@Override
	public Set<String> getAllowedPermissions() {
		return null;
	}

	@Override
	public Set<String> getDeniedPermissions() {
		return null;
	}

}
