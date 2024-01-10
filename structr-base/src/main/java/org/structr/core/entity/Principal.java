/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.config.Settings;
import org.structr.api.graph.Cardinality;
import org.structr.api.graph.Node;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.*;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.HashHelper;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.SchemaService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

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
		principal.addStringArrayProperty("refreshTokens").setIndexed(true);

		principal.addStringProperty("sessionData");

		principal.addStringProperty("eMail")
			.setIndexed(true)
			.setUnique(true)
			.addValidator(EMailValidator.class.getName())
			.addTransformer(LowercaseTransformator.class.getName())
			.addTransformer(TrimTransformator.class.getName());

		principal.addPasswordProperty("password");

		// Password Policy
		principal.addDateProperty("passwordChangeDate");
		principal.addPropertySetter("passwordChangeDate", Date.class);
		principal.addPropertyGetter("passwordChangeDate", Date.class);

		principal.addIntegerProperty("passwordAttempts");
		principal.addPropertySetter("passwordAttempts", Integer.class);
		principal.addPropertyGetter("passwordAttempts", Integer.class);

		principal.addDateProperty("lastLoginDate");
		principal.addPropertySetter("lastLoginDate", Date.class);
		principal.addPropertyGetter("lastLoginDate", Date.class);

		// Two Factor Authentication
		principal.addStringProperty("twoFactorSecret");

		principal.addStringProperty("twoFactorToken").setIndexed(true);
		principal.addPropertySetter("twoFactorToken", String.class);
		principal.addPropertyGetter("twoFactorToken", String.class);

		principal.addBooleanProperty("isTwoFactorUser");
		principal.addPropertySetter("isTwoFactorUser", Boolean.TYPE);
		principal.addPropertyGetter("isTwoFactorUser", Boolean.TYPE);

		principal.addBooleanProperty("twoFactorConfirmed");
		principal.addPropertySetter("twoFactorConfirmed", Boolean.TYPE);
		principal.addPropertyGetter("twoFactorConfirmed", Boolean.TYPE);


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
		principal.addPropertyGetter("favorites", Iterable.class);
		principal.addPropertyGetter("groups", Iterable.class);
		principal.addPropertyGetter("eMail", String.class);

		principal.addPropertySetter("sessionData", String.class);
		principal.addPropertySetter("favorites", Iterable.class);
		principal.addPropertySetter("password", String.class);
		principal.addPropertySetter("isAdmin", Boolean.TYPE);
		principal.addPropertySetter("eMail", String.class);
		principal.addPropertySetter("salt", String.class);

		principal.overrideMethod("shouldSkipSecurityRelationships", false, "return false;");
		principal.overrideMethod("isAdmin",                         false, "return getProperty(isAdminProperty);");
		principal.overrideMethod("isBlocked",                       false, "return getProperty(blockedProperty);");
		principal.overrideMethod("getParents",                      false, "return " + Principal.class.getName() + ".getParents(this);");
		principal.overrideMethod("getParentsPrivileged",            false, "return " + Principal.class.getName() + ".getParentsPrivileged(this);");
		principal.overrideMethod("isValidPassword",                 false, "return " + Principal.class.getName() + ".isValidPassword(this, arg0);");

		principal.overrideMethod("addSessionId",                    false, "return " + Principal.class.getName() + ".addSessionId(this, arg0);");
		principal.overrideMethod("addRefreshToken",                 false, "return " + Principal.class.getName() + ".addRefreshToken(this, arg0);");

		principal.overrideMethod("removeSessionId",                 false, Principal.class.getName() + ".removeSessionId(this, arg0);");
		principal.overrideMethod("removeRefreshToken",              false, Principal.class.getName() + ".removeRefreshToken(this, arg0);");

		principal.overrideMethod("onAuthenticate",                  false, "");

		// override getProperty
		principal.addMethod("getProperty")
			.setReturnType("<T> T")
			.addParameter("arg0", PropertyKey.class.getName() + "<T>")
			.addParameter("arg1", Predicate.class.getName() + "<GraphObject>")
			.setSource("if (arg0.equals(passwordProperty) || arg0.equals(saltProperty) || arg0.equals(twoFactorSecretProperty)) { return (T) Principal.HIDDEN; } else { return super.getProperty(arg0, arg1); }");

		// override setProperty final PropertyKey<T> key, final T value)
		principal.addMethod("setProperty")
			.setReturnType("<T> java.lang.Object")
			.addParameter("arg0", PropertyKey.class.getName() + "<T>")
			.addParameter("arg1", "T")
			.addException(FrameworkException.class.getName())
			.setSource("AbstractNode.clearCaches(); return super.setProperty(arg0, arg1);");

		// create relationships
		principal.relate(favoritable, "FAVORITE", Cardinality.ManyToMany, "favoriteUsers", "favorites");
	}}

	public static final Object HIDDEN                            = "****** HIDDEN ******";
	public static final String SUPERUSER_ID                      = "00000000000000000000000000000000";
	public static final String ANONYMOUS                         = "anonymous";
	public static final String ANYONE                            = "anyone";

	public static final Property<Iterable<NodeInterface>> ownedNodes   = new EndNodes<>("ownedNodes", PrincipalOwnsNode.class).partOfBuiltInSchema();
	public static final Property<Iterable<NodeInterface>> grantedNodes = new EndNodes<>("grantedNodes", Security.class).partOfBuiltInSchema();

	Iterable<Favoritable> getFavorites();
	Iterable<Group> getGroups();

	Iterable<Principal> getParents();
	Iterable<Principal> getParentsPrivileged();

	boolean isValidPassword(final String password);

	boolean addSessionId(final String sessionId);
	void removeSessionId(final String sessionId);

	boolean addRefreshToken(final String refreshToken);
	void removeRefreshToken(final String refreshToken);

	String getSessionData();
	String getEMail();
	void setSessionData(final String sessionData) throws FrameworkException;

	boolean isAdmin();
	boolean isBlocked();
	boolean shouldSkipSecurityRelationships();

	default void onAuthenticate() {}

	void setFavorites(final Iterable<Favoritable> favorites) throws FrameworkException;
	void setIsAdmin(final boolean isAdmin) throws FrameworkException;
	void setPassword(final String password) throws FrameworkException;
	void setEMail(final String eMail) throws FrameworkException;
	void setSalt(final String salt) throws FrameworkException;

	String getLocale();

	public static Iterable<Principal> getParents(final Principal principal) {
		return principal.getProperty(StructrApp.key(Principal.class, "groups"));
	}

	public static Iterable<Principal> getParentsPrivileged(final Principal principal) {

		try {

			final App app                       = StructrApp.getInstance();
			final Principal privilegedPrincipal = app.get(Principal.class, principal.getUuid());

			return privilegedPrincipal.getProperty(StructrApp.key(Principal.class, "groups"));

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);

			logger.warn("Caught exception while fetching groups for user '{}' ({})", principal.getName(), principal.getUuid());
			logger.warn(ExceptionUtils.getStackTrace(fex));

			return Collections.emptyList();
		}
	}

	public static boolean addSessionId(final Principal principal, final String sessionId) {

		try {

			final PropertyKey<String[]> key = StructrApp.key(Principal.class, "sessionIds");
			final String[] ids              = principal.getProperty(key);

			if (ids != null) {

				if (!ArrayUtils.contains(ids, sessionId)) {

					if (Settings.MaxSessionsPerUser.getValue() > 0 && ids.length >= Settings.MaxSessionsPerUser.getValue()) {

						final Logger logger = LoggerFactory.getLogger(Principal.class);

						final String errorMessage = "Not adding session id, limit " + Settings.MaxSessionsPerUser.getKey() + " exceeded.";
						logger.warn(errorMessage);

						return false;
					}

					principal.setProperty(key, (String[]) ArrayUtils.add(principal.getProperty(key), sessionId));
				}

			} else {

				principal.setProperty(key, new String[] {  sessionId } );
			}

			return true;

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.error("Could not add sessionId " + sessionId + " to array of sessionIds", ex);

			return false;
		}
	}

	public static boolean addRefreshToken(final Principal principal, final String refreshToken) {
		try {

			final PropertyKey<String[]> key = StructrApp.key(Principal.class, "refreshTokens");
			final String[] refreshTokens    = principal.getProperty(key);

			if (refreshTokens != null) {

				if (!ArrayUtils.contains(refreshTokens, refreshToken)) {

					if (Settings.MaxSessionsPerUser.getValue() > 0 && refreshTokens.length >= Settings.MaxSessionsPerUser.getValue()) {

						final Logger logger = LoggerFactory.getLogger(Principal.class);

						final String errorMessage = "Not adding session id, limit " + Settings.MaxSessionsPerUser.getKey() + " exceeded.";
						logger.warn(errorMessage);

						return false;
					}

					principal.setProperty(key, (String[]) ArrayUtils.add(principal.getProperty(key), refreshToken));
				}

			} else {

				principal.setProperty(key, new String[] {  refreshToken } );
			}

			return true;

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.error("Could not add refreshToken " + refreshToken + " to array of refreshTokens", ex);

			return false;
		}
	}

	public static void removeSessionId(final Principal principal, final String sessionId) {

		try {

			final PropertyKey<String[]> key = StructrApp.key(Principal.class, "sessionIds");
			final String[] ids              = principal.getProperty(key);

			if (ids != null) {

				final Set<String> sessionIds = new HashSet<>(Arrays.asList(ids));

				sessionIds.remove(sessionId);

				principal.setProperty(key, (String[]) sessionIds.toArray(new String[0]));
			}

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.error("Could not remove sessionId " + sessionId + " from array of sessionIds", ex);
		}
	}

	public static void removeRefreshToken(final Principal principal, final String refreshToken) {

		try {

			final PropertyKey<String[]> key = StructrApp.key(Principal.class, "refreshTokens");
			final String[] refreshTokens    = principal.getProperty(key);

			if (refreshTokens != null) {

				final Set<String> refreshTokenSet = new HashSet<>(Arrays.asList(refreshTokens));

				refreshTokenSet.remove(refreshToken);

				principal.setProperty(key, (String[]) refreshTokenSet.toArray(new String[0]));
			}

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.error("Could not remove refreshToken " + refreshToken + " from array of refreshTokens", ex);
		}
	}

	public static void clearTokens(final Principal principal) {

		try {

			PropertyMap properties = new PropertyMap();
			final PropertyKey<String[]> refreshTokensKey = StructrApp.key(Principal.class, "refreshTokens");

			properties.put(refreshTokensKey, new String[0]);

			principal.setProperties(SecurityContext.getSuperUserInstance(), properties);

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.error("Could not clear refreshTokens of user {}", principal, ex);
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

	public static String getTwoFactorSecret(final Principal principal) {

		final Node dbNode = principal.getNode();
		if (dbNode.hasProperty("twoFactorSecret")) {

			return (String) dbNode.getProperty("twoFactorSecret");
		}

		return null;
	}

	public static String getTwoFactorUrl(final Principal principal) {

		if (principal == null) {

			LoggerFactory.getLogger(Principal.class).warn("Cannot fetch twofactor URL from user, user is null!");

			return null;
		}

		final String twoFactorIssuer    = Settings.TwoFactorIssuer.getValue();
		final String twoFactorAlgorithm = Settings.TwoFactorAlgorithm.getValue();
		final Integer twoFactorDigits   = Settings.TwoFactorDigits.getValue();
		final Integer twoFactorPeriod   = Settings.TwoFactorPeriod.getValue();

		final StringBuilder path = new StringBuilder("/").append(twoFactorIssuer);

		final String eMail = principal.getProperty(StructrApp.key(Principal.class, "eMail"));
		if (eMail != null) {
			path.append(":").append(eMail);
		} else {
			path.append(":").append(principal.getName());
		}

		final StringBuilder query = new StringBuilder("secret=").append(Principal.getTwoFactorSecret(principal))
				.append("&issuer=").append(twoFactorIssuer)
				.append("&algorithm=").append(twoFactorAlgorithm)
				.append("&digits=").append(twoFactorDigits)
				.append("&period=").append(twoFactorPeriod);

		try {

			return new URI("otpauth", null, "totp", -1, path.toString(), query.toString(), null).toString();

		} catch (URISyntaxException use) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.warn("two_factor_url(): URISyntaxException for {}?{}", path, query, use);

			return "URISyntaxException for " + path + "?" + query;
		}
	}
}