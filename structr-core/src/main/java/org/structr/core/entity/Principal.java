/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.structr.api.Predicate;
import org.structr.api.graph.Node;
import org.structr.common.AccessControllable;
import org.structr.common.EMailValidator;
import org.structr.common.LowercaseTransformator;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.HashHelper;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;

public interface Principal extends NodeInterface, AccessControllable {

	static class Impl { static {

		final JsonSchema schema          = SchemaService.getDynamicSchema();
		final JsonObjectType principal   = schema.addType("Principal");
		final JsonObjectType favoritable = (JsonObjectType)schema.getType("Favoritable");

		principal.setImplements(URI.create("https://structr.org/v1.1/definitions/Principal"));
		principal.setCategory("core");

		principal.addBooleanProperty("isAdmin").setIndexed(true).setReadOnly(true);
		principal.addBooleanProperty("blocked", PropertyView.Ui);

		// FIXME: indexedWhenEmpty() is not possible here, but needed?
		principal.addStringArrayProperty("sessionIds").setIndexed(true);

		principal.addStringProperty("sessionData");

		principal.addStringProperty("eMail")
			.setIndexed(true)
			.setUnique(true)
			.addValidator(EMailValidator.class.getName())
			.addTransformer(LowercaseTransformator.class.getName());

		principal.addPasswordProperty("password");
                
                // Two Factor Authentication Attributes
                principal.addStringProperty("twoFactorSecret");
                principal.addStringProperty("twoFactorToken");
                principal.addStringProperty("twoFactorUrl");
                principal.addBooleanProperty("twoFactorUser", PropertyView.Ui);
                
                principal.addPropertySetter("twoFactorUser", Boolean.TYPE);
                principal.addPropertyGetter("twoFactorUser", Boolean.TYPE);
                principal.addPropertySetter("twoFactorSecret", String.class);
                principal.addPropertyGetter("twoFactorSecret", String.class);
                principal.addPropertySetter("twoFactorToken", String.class);
                principal.addPropertyGetter("twoFactorToken", String.class);
                principal.addPropertySetter("twoFactorUrl", String.class);
                principal.addPropertyGetter("twoFactorUrl", String.class);
                
		principal.addStringProperty("salt");
		principal.addStringProperty("locale");
		principal.addStringProperty("publicKey");
		principal.addStringProperty("proxyUrl");
		principal.addStringProperty("proxyUsername");
		principal.addStringProperty("proxyPassword");

		//type.addStringArrayProperty("sessionIds");
		principal.addStringArrayProperty("publicKeys");

		principal.addStringProperty("customPermissionQueryRead");
		principal.addStringProperty("customPermissionQueryWrite");
		principal.addStringProperty("customPermissionQueryDelete");
		principal.addStringProperty("customPermissionQueryAccessControl");

		principal.addPropertyGetter("locale", String.class);
		principal.addPropertyGetter("sessionData", String.class);
		principal.addPropertyGetter("favorites", List.class);
		principal.addPropertyGetter("groups", List.class);
		principal.addPropertyGetter("eMail", String.class);

		principal.addPropertySetter("sessionData", String.class);
		principal.addPropertySetter("favorites", List.class);
		principal.addPropertySetter("password", String.class);
		principal.addPropertySetter("isAdmin", Boolean.TYPE);
		principal.addPropertySetter("eMail", String.class);
		principal.addPropertySetter("salt", String.class);

		principal.overrideMethod("shouldSkipSecurityRelationships", false, "return false;");
		principal.overrideMethod("isAdmin",                         false, "return getProperty(isAdminProperty);");
		principal.overrideMethod("isBlocked",                       false, "return getProperty(blockedProperty);");
		principal.overrideMethod("getParents",                      false, "return " + Principal.class.getName() + ".getParents(this);");
		principal.overrideMethod("isValidPassword",                 false, "return " + Principal.class.getName() + ".isValidPassword(this, arg0);");
		principal.overrideMethod("addSessionId",                    false, Principal.class.getName() + ".addSessionId(this, arg0);");
		principal.overrideMethod("removeSessionId",                 false, Principal.class.getName() + ".removeSessionId(this, arg0);");

		// override getProperty
		principal.addMethod("getProperty")
			.setReturnType("<T> T")
			.addParameter("arg0", PropertyKey.class.getName() + "<T>")
			.addParameter("arg1", Predicate.class.getName() + "<GraphObject>")
			.setSource("if (arg0.equals(passwordProperty) || arg0.equals(saltProperty)) { return (T)Principal.HIDDEN; } else { return super.getProperty(arg0, arg1); }");

		// create relationships
		principal.relate(favoritable, "FAVORITE", Relation.Cardinality.ManyToMany, "favoriteUsers", "favorites");
	}}

	public static final Object HIDDEN                            = "****** HIDDEN ******";
	public static final String SUPERUSER_ID                      = "00000000000000000000000000000000";
	public static final String ANONYMOUS                         = "anonymous";
	public static final String ANYONE                            = "anyone";

	public static final Property<List<NodeInterface>> ownedNodes = new EndNodes<>("ownedNodes", PrincipalOwnsNode.class);

	List<Favoritable> getFavorites();
	List<Principal> getParents();
	List<Group> getGroups();

	boolean isValidPassword(final String password);

	void addSessionId(final String sessionId);
	void removeSessionId(final String sessionId);

	String getSessionData();
	String getEMail();
	void setSessionData(final String sessionData) throws FrameworkException;

	boolean isAdmin();
	boolean isBlocked();
	boolean shouldSkipSecurityRelationships();
        
	void setFavorites(final List<Favoritable> favorites) throws FrameworkException;
	void setIsAdmin(final boolean isAdmin) throws FrameworkException;
	void setPassword(final String password) throws FrameworkException;
	void setEMail(final String eMail) throws FrameworkException;
	void setSalt(final String salt) throws FrameworkException;

	String getLocale();

	public static List<Principal> getParents(final Principal principal) {

		final List<Principal> parents          = new LinkedList<>();
		final PropertyKey<List<Principal>> key = StructrApp.key(Principal.class, "groups");
		final List<Principal> list             = principal.getProperty(key);

		for (final Principal parent : list) {

			if (parent != null) {
				parents.add(parent);
			}
		}

		return parents;
	}

	public static void addSessionId(final Principal principal, final String sessionId) {

		try {

			final PropertyKey<String[]> key = StructrApp.key(Principal.class, "sessionIds");
			final String[] ids              = principal.getProperty(key);

			if (ids != null) {

				if (!ArrayUtils.contains(ids, sessionId)) {

					principal.setProperty(key, (String[]) ArrayUtils.add(principal.getProperty(key), sessionId));
				}

			} else {

				principal.setProperty(key, new String[] {  sessionId } );
			}


		} catch (FrameworkException ex) {
			logger.error("Could not add sessionId " + sessionId + " to array of sessionIds", ex);
		}
	}

	public static void removeSessionId(final Principal principal, final String sessionId) {

		try {

			final PropertyKey<String[]> key = StructrApp.key(Principal.class, "sessionIds");
			final String[] ids              = principal.getProperty(key);
			List<String> newSessionIds      = new ArrayList<>();

			if (ids != null) {

				for (final String id : ids) {

					if (!id.equals(sessionId)) {

						newSessionIds.add(id);
					}
				}
			}

			principal.setProperty(key, (String[]) newSessionIds.toArray(new String[0]));

		} catch (FrameworkException ex) {
			logger.error("Could not remove sessionId " + sessionId + " from array of sessionIds", ex);
		}
	}

	public static boolean isValidPassword(final Principal principal, final String password) {

		final String encryptedPasswordFromDatabase = getEncryptedPassword(principal);
		if (encryptedPasswordFromDatabase != null) {

			final String encryptedPasswordToCheck = HashHelper.getHash(password, getSalt(principal));

			if (encryptedPasswordFromDatabase.equals(encryptedPasswordToCheck)) {
				return true;
			}
		}

		return false;
	}

	public static String getEncryptedPassword(final Principal principal) {

		final Node dbNode = principal.getNode();
		if (dbNode.hasProperty("password")) {

			return (String)dbNode.getProperty("password");
		}

		return null;
	}

	public static String getSalt(final Principal principal) {

		final Node dbNode = principal.getNode();
		if (dbNode.hasProperty("salt")) {

			return (String) dbNode.getProperty("salt");
		}

		return null;
	}
}


/*

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

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
