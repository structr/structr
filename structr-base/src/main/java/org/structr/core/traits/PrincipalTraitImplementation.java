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
package org.structr.core.traits;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.config.Settings;
import org.structr.api.graph.Node;
import org.structr.common.EMailValidator;
import org.structr.common.LowercaseTransformator;
import org.structr.common.SecurityContext;
import org.structr.common.TrimTransformator;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.HashHelper;
import org.structr.core.entity.Favoritable;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.entity.Security;
import org.structr.core.entity.relationship.GroupCONTAINSPrincipal;
import org.structr.core.entity.relationship.PrincipalFAVORITEFavoritable;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.operations.ComposableOperation;
import org.structr.core.traits.operations.OverwritableOperation;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.propertycontainer.GetProperty;
import org.structr.core.traits.operations.propertycontainer.SetProperty;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class PrincipalTraitImplementation extends AbstractTraitImplementation {

	Property<Iterable<Favoritable>> favoritesProperty = new EndNodes<>("favorites", PrincipalFAVORITEFavoritable.class);
	Property<Iterable<Group>> groupsProperty          = new StartNodes<>("groups", GroupCONTAINSPrincipal.class);

	Property<Iterable<NodeInterface>> ownedNodes   = new EndNodes<>("ownedNodes", PrincipalOwnsNode.class).partOfBuiltInSchema();
	Property<Iterable<NodeInterface>> grantedNodes = new EndNodes<>("grantedNodes", Security.class).partOfBuiltInSchema();

	Property<Boolean> isAdminProperty                           = new BooleanProperty("isAdmin").indexed().readOnly();
	Property<Boolean> blockedProperty                           = new BooleanProperty("blocked");
	Property<String> sessionIdsProperty                         = new ArrayProperty("sessionIds", String.class).indexed();
	Property<String> refreshTokensProperty                      = new ArrayProperty("refreshTokens", String.class).indexed();
	Property<String> sessionDataProperty                        = new StringProperty("sessionData");
	Property<String> eMailProperty                              = new StringProperty("eMail").indexed().unique().transformators(LowercaseTransformator.class.getName(), TrimTransformator.class.getName());
	Property<String> passwordProperty                           = new PasswordProperty("password");
	Property<Date> passwordChangeDateProperty                   = new DateProperty("passwordChangeDate");
	Property<Integer> passwordAttemptsProperty                  = new IntProperty("passwordAttempts");
	Property<Date> lastLoginDateProperty                        = new DateProperty("lastLoginDate");
	Property<String> twoFactorSecretProperty                    = new StringProperty("twoFactorSecret");
	Property<String> twoFactorTokenProperty                     = new StringProperty("twoFactorToken").indexed();
	Property<Boolean> isTwoFactorUserProperty                   = new BooleanProperty("isTwoFactorUser");
	Property<Boolean> twoFactorConfirmedProperty                = new BooleanProperty("twoFactorConfirmed");
	Property<String> saltProperty                               = new StringProperty("salt");
	Property<String> localeProperty                             = new StringProperty("locale");
	Property<String> publicKeyProperty                          = new StringProperty("publicKey");
	Property<String> proxyUrlProperty                           = new StringProperty("proxyUrl");
	Property<String> proxyUsernameProperty                      = new StringProperty("proxyUsername");
	Property<String> proxyPasswordProperty                      = new StringProperty("proxyPassword");
	Property<String> publicKeysProperty                         = new ArrayProperty("publicKeys", String.class);

	@Override
	public Set<ComposableOperation> getComposableOperations() {

		return Set.of(

			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					boolean valid = true;

					valid &= ValidationHelper.isValidUniqueProperty(obj, eMailProperty, errorBuffer);
					valid &= new EMailValidator().isValid(obj, errorBuffer);

					return valid;
				}
			}
		);
	}

	@Override
	public Set<OverwritableOperation> getOverwritableOperations() {

		return Set.of(

			new GetProperty() {

				@Override
				public <V> V getProperty(final GraphObject graphObject, final PropertyKey<V> key, final Predicate<GraphObject> predicate) {

					if (key.equals(passwordProperty) || key.equals(saltProperty) || key.equals(twoFactorSecretProperty)) {

						return (V) Principal.HIDDEN;

					} else {

						return this.getSuper().getProperty(graphObject, key, predicate);
					}
				}
			},

			new SetProperty() {

				@Override
				public <T> Object setProperty(final GraphObject graphObject, final PropertyKey<T> key, final T value, final boolean isCreation) throws FrameworkException {

					AbstractNode.clearCaches();

					return getSuper().setProperty(graphObject, key, value, isCreation);
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {
		return Set.of();
	}

	@Override
	public void onAuthenticate(final Principal principal) {
	}

	@Override
	public Iterable<Favoritable> getFavorites(final Principal principal) {
		return principal.getProperty(traits.key("favorites"));
	}

	@Override
	public Iterable<Group> getGroups(final Principal principal) {
		return principal.getProperty(traits.key("groups"));
	}

	@Override
	public String getSessionData(final Principal principal) {
		return principal.getProperty(traits.key("sessionDataProperty"));
	}

	@Override
	public String getEMail(final Principal principal) {
		return principal.getProperty(traits.key("eMail"));
	}

	@Override
	public void setSessionData(final Principal principal, final String sessionData) throws FrameworkException {
		principal.setProperty(traits.key("sessionData"), sessionData);
	}

	@Override
	public boolean isAdmin(final Principal principal) {
		return principal.getProperty(traits.key("isAdmin"));
	}

	public boolean isBlocked(final Principal principal) {
		return principal.getProperty(traits.key("blocked"));
	}

	@Override
	public void setFavorites(final Principal principal, final Iterable<Favoritable> favorites) throws FrameworkException {
		principal.setProperty(traits.key("favorites"), favorites);
	}

	@Override
	public void setIsAdmin(final Principal principal, final boolean isAdmin) throws FrameworkException {
		principal.setProperty(traits.key("isAdmin"), isAdmin);
	}

	@Override
	public void setPassword(final Principal principal, final String password) throws FrameworkException {
		principal.setProperty(traits.key("password"), password);
	}

	@Override
	public void setEMail(final Principal principal, final String eMail) throws FrameworkException {
		principal.setProperty(traits.key("eMail"), eMail);
	}

	@Override
	public void setSalt(final Principal principal, final String salt) throws FrameworkException {
		principal.setProperty(traits.key("salt"), salt);
	}

	@Override
	public String getLocale(final Principal principal) {
		return principal.getProperty(traits.key("locale"));
	}

	@Override
	public boolean shouldSkipSecurityRelationships(final Principal principal) {
		return false;
	}

	@Override
	public void setTwoFactorConfirmed(final Principal principal, final boolean b) throws FrameworkException {
		principal.setProperty(traits.key("twoFactorConfirmed"), b);
	}

	@Override
	public void setTwoFactorToken(final Principal principal, final String token) throws FrameworkException {
		principal.setProperty(traits.key("twoFactorToken"), token);
	}

	@Override
	public boolean isTwoFactorUser(final Principal principal) {
		return principal.getProperty(traits.key("isTwoFactorUser"));
	}

	@Override
	public void setIsTwoFactorUser(final Principal principal, final boolean b) throws FrameworkException {
		principal.setProperty(traits.key("isTwoFactorUser"), b);

	}

	@Override
	public boolean isTwoFactorConfirmed(final Principal principal) {
		return principal.getProperty(traits.key("isTwoFactorConfirmed"));
	}

	@Override
	public Integer getPasswordAttempts(final Principal principal) {
		return principal.getProperty(traits.key("passwordAttempts"));
	}

	@Override
	public Date getPasswordChangeDate(final Principal principal) {
		return principal.getProperty(traits.key("passwordChangeDate"));
	}

	@Override
	public void setPasswordAttempts(final Principal principal, int num) throws FrameworkException {
		principal.setProperty(traits.key("passwordAttempts"), num);
	}

	@Override
	public void setLastLoginDate(final Principal principal, final Date date) throws FrameworkException {
		principal.setProperty(traits.key("lastLoginDate"), date);
	}

	@Override
	public String[] getSessionIds(final Principal principal) {
		return principal.getProperty(traits.key("sessionIds"));
	}

	@Override
	public Iterable<Group> getParents(final Principal principal) {
		return principal.getProperty(traits.key("groups"));
	}

	@Override
	public Iterable<Group> getParentsPrivileged(final Principal principal) {

		try {

			final App app                       = StructrApp.getInstance();
			final Principal privilegedPrincipal = app.getNodeById(Principal.class, principal.getUuid());

			return privilegedPrincipal.getGroups();

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);

			logger.warn("Caught exception while fetching groups for user '{}' ({})", principal.getName(), principal.getUuid());
			logger.warn(ExceptionUtils.getStackTrace(fex));

			return Collections.emptyList();
		}
	}

	@Override
	public boolean addSessionId(final Principal principal, final String sessionId) {

		try {

			final PropertyKey<String[]> key = traits.key("sessionIds");
			final String[] ids              = getSessionIds(principal);

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

	@Override
	public boolean addRefreshToken(final Principal principal, final String refreshToken) {

		try {

			final PropertyKey<String[]> key = traits.key("refreshTokens");
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

	@Override
	public void removeSessionId(final Principal principal, final String sessionId) {

		try {

			final PropertyKey<String[]> key = traits.key("sessionIds");
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

	@Override
	public void removeRefreshToken(final Principal principal, final String refreshToken) {

		try {

			final PropertyKey<String[]> key = traits.key("refreshTokens");
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

	@Override
	public void clearTokens(final Principal principal) {

		try {

			PropertyMap properties = new PropertyMap();
			final PropertyKey<String[]> refreshTokensKey = traits.key("refreshTokens");

			properties.put(refreshTokensKey, new String[0]);

			principal.setProperties(SecurityContext.getSuperUserInstance(), properties);

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.error("Could not clear refreshTokens of user {}", this, ex);
		}
	}

	@Override
	public boolean isValidPassword(final Principal principal, final String password) {

		final String encryptedPasswordFromDatabase = getEncryptedPassword(principal);
		if (encryptedPasswordFromDatabase != null) {

			final String encryptedPasswordToCheck = HashHelper.getHash(password, getSalt(principal));

			if (encryptedPasswordFromDatabase.equals(encryptedPasswordToCheck)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String getEncryptedPassword(final Principal principal) {

		final Node dbNode = principal.getNode();
		if (dbNode.hasProperty("password")) {

			return (String)dbNode.getProperty("password");
		}

		return null;
	}

	@Override
	public String getSalt(final Principal principal) {

		final Node dbNode = principal.getNode();
		if (dbNode.hasProperty("salt")) {

			return (String) dbNode.getProperty("salt");
		}

		return null;
	}

	@Override
	public String getTwoFactorSecret(final Principal principal) {

		final Node dbNode = principal.getNode();
		if (dbNode.hasProperty("twoFactorSecret")) {

			return (String) dbNode.getProperty("twoFactorSecret");
		}

		return null;
	}

	@Override
	public String getTwoFactorUrl(final Principal principal) {

		final String twoFactorIssuer    = Settings.TwoFactorIssuer.getValue();
		final String twoFactorAlgorithm = Settings.TwoFactorAlgorithm.getValue();
		final Integer twoFactorDigits   = Settings.TwoFactorDigits.getValue();
		final Integer twoFactorPeriod   = Settings.TwoFactorPeriod.getValue();

		final StringBuilder path = new StringBuilder("/").append(twoFactorIssuer);

		final String eMail = principal.getProperty(traits.key("eMail"));
		if (eMail != null) {
			path.append(":").append(eMail);
		} else {
			path.append(":").append(principal.getName());
		}

		final StringBuilder query = new StringBuilder("secret=").append(getTwoFactorSecret(principal))
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
