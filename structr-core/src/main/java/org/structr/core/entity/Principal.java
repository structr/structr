/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.util.List;
import java.util.Set;
import org.structr.common.AccessControllable;
import org.structr.common.ValidationHelper;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.SemanticErrorToken;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.ArrayProperty;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.LowercaseStringProperty;
import org.structr.core.property.PasswordProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.schema.SchemaService;

public interface Principal extends NodeInterface, AccessControllable {

	static class Impl { static { SchemaService.registerMixinType(Principal.class); }}

	public static final Object HIDDEN                            = "****** HIDDEN ******";
	public static final String SUPERUSER_ID                      = "00000000000000000000000000000000";
	public static final String ANONYMOUS                         = "anonymous";
	public static final String ANYONE                            = "anyone";

	public static final Property<String[]> sessionIds            = new ArrayProperty("sessionIds", String.class).indexedWhenEmpty();
	public static final Property<String> sessionData             = new StringProperty("sessionData");
	public static final Property<List<NodeInterface>> ownedNodes = new EndNodes<>("ownedNodes", PrincipalOwnsNode.class);
	public static final Property<Boolean> blocked                = new BooleanProperty("blocked");
	public static final Property<String> eMail                   = new LowercaseStringProperty("eMail").cmis().indexed();
	public static final Property<String> password                = new PasswordProperty("password");
	public static final Property<String> salt                    = new StringProperty("salt");
	public static final Property<Boolean> isAdmin                = new BooleanProperty("isAdmin").indexed().readOnly();
	public static final Property<String> locale                  = new StringProperty("locale");
	public static final Property<String> publicKey               = new StringProperty("publicKey");
	public static final Property<String[]> publicKeys            = new ArrayProperty("publicKeys", String.class);
	public static final Property<String> proxyUrl                = new StringProperty("proxyUrl");
	public static final Property<String> proxyUsername           = new StringProperty("proxyUsername");
	public static final Property<String> proxyPassword           = new StringProperty("proxyPassword");

	public List<Principal> getParents();

	public boolean isValidPassword(final String password);
	public String getEncryptedPassword();
	public String getSalt();

	public void addSessionId(final String sessionId);

	public void removeSessionId(final String sessionId);

	public boolean isAdmin();
	public boolean shouldSkipSecurityRelationships();

	public Set<String> getAllowedPermissions();
	public Set<String> getDeniedPermissions();

	@Override
	default public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = true;

		valid &= ValidationHelper.isValidStringNotBlank(this, name, errorBuffer);
		valid &= ValidationHelper.isValidUniqueProperty(this, eMail, errorBuffer);

		final String _eMail = getProperty(eMail);
		if (_eMail != null) {

			// verify that the address contains at least the @ character,
			// which is a requirement for it to be distinguishable from
			// a user name, so email addresses can less easily interfere
			// with user names.
			if (!_eMail.contains("@")) {

				valid = false;

				errorBuffer.add(new SemanticErrorToken(getClass().getSimpleName(), eMail, "must_contain_at_character", _eMail));
			}
		}

		return valid;
	}

}


/*
public abstract class AbstractUser extends AbstractNode implements Principal {

	private static final Logger logger = LoggerFactory.getLogger(AbstractUser.class.getName());
	private Boolean cachedIsAdminFlag  = null;

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean valid = true;

		// call default method of principal
		valid &= Principal.super.isValid(errorBuffer);
		valid &= super.isValid(errorBuffer);

		return valid;
	}

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

	@Override
	public boolean shouldSkipSecurityRelationships() {
		return false;
	}
}
*/