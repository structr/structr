/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.core.traits.wrappers;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.graph.Node;
import org.structr.api.util.Iterables;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.UserTraitDefinition;
import org.structr.core.traits.operations.principal.IsValidPassword;
import org.structr.core.traits.operations.principal.OnAuthenticate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class PrincipalTraitWrapper extends AbstractNodeTraitWrapper implements Principal {

	private Boolean cachedIsAdminFlag = null;

	public PrincipalTraitWrapper(final Traits traits, final NodeInterface nodeInterface) {
		super(traits, nodeInterface);
	}

	@Override
	public String getName() {
		return wrappedObject.getName();
	}

	// Not fixed
	@Override
	public Iterable<NodeInterface> getOwnedNodes() {
		return wrappedObject.getProperty(traits.key(PrincipalTraitDefinition.OWNED_NODES_PROPERTY));
	}

	@Override
	public Iterable<Group> getGroups() {

		final Iterable<NodeInterface> nodes = wrappedObject.getProperty(traits.key(PrincipalTraitDefinition.GROUPS_PROPERTY));

		return Iterables.map(n -> n.as(Group.class), nodes);
	}

	@Override
	public String getSessionData() {
		return wrappedObject.getProperty(traits.key(PrincipalTraitDefinition.SESSION_DATA_PROPERTY));
	}

	@Override
	public String getEMail() {
		return wrappedObject.getProperty(traits.key(PrincipalTraitDefinition.EMAIL_PROPERTY));
	}

	@Override
	public void setSessionData(final String sessionData) throws FrameworkException {
		wrappedObject.setProperty(traits.key(PrincipalTraitDefinition.SESSION_DATA_PROPERTY), sessionData);
	}

	@Override
	public boolean isAdmin() {

		if (cachedIsAdminFlag == null) {
			cachedIsAdminFlag = wrappedObject.getProperty(traits.key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY));
		}

		return cachedIsAdminFlag;
	}

	public boolean isBlocked() {
		return wrappedObject.getProperty(traits.key(PrincipalTraitDefinition.BLOCKED_PROPERTY));
	}

	@Override
	public void setIsAdmin(final boolean isAdmin) throws FrameworkException {

		cachedIsAdminFlag = isAdmin;

		wrappedObject.setProperty(traits.key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), isAdmin);
	}

	@Override
	public void setPassword(final String password) throws FrameworkException {
		wrappedObject.setProperty(traits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY), password);
	}

	@Override
	public void setEMail(final String eMail) throws FrameworkException {
		wrappedObject.setProperty(traits.key(PrincipalTraitDefinition.EMAIL_PROPERTY), eMail);
	}

	@Override
	public void setSalt(final String salt) throws FrameworkException {
		wrappedObject.setProperty(traits.key(PrincipalTraitDefinition.SALT_PROPERTY), salt);
	}

	@Override
	public String getLocale() {
		return wrappedObject.getProperty(traits.key(PrincipalTraitDefinition.LOCALE_PROPERTY));
	}

	@Override
	public boolean shouldSkipSecurityRelationships() {
		// fixme: this should be overridable
		return wrappedObject.getProperty(traits.key(UserTraitDefinition.SKIP_SECURITY_RELATIONSHIPS_PROPERTY));
	}

	@Override
	public void setTwoFactorConfirmed(final boolean b) throws FrameworkException {
		wrappedObject.setProperty(traits.key(PrincipalTraitDefinition.TWO_FACTOR_CONFIRMED_PROPERTY), b);
	}

	@Override
	public void setTwoFactorToken(final String token) throws FrameworkException {
		wrappedObject.setProperty(traits.key(PrincipalTraitDefinition.TWO_FACTOR_TOKEN_PROPERTY), token);
	}

	@Override
	public boolean isTwoFactorUser() {
		return wrappedObject.getProperty(traits.key(PrincipalTraitDefinition.IS_TWO_FACTOR_USER_PROPERTY));
	}

	@Override
	public void setIsTwoFactorUser(final boolean b) throws FrameworkException {
		wrappedObject.setProperty(traits.key(PrincipalTraitDefinition.IS_TWO_FACTOR_USER_PROPERTY), b);

	}

	@Override
	public boolean isTwoFactorConfirmed() {
		return wrappedObject.getProperty(traits.key(PrincipalTraitDefinition.TWO_FACTOR_CONFIRMED_PROPERTY));
	}

	@Override
	public Integer getPasswordAttempts() {
		return wrappedObject.getProperty(traits.key(PrincipalTraitDefinition.PASSWORD_ATTEMPTS_PROPERTY));
	}

	@Override
	public Date getPasswordChangeDate() {
		return wrappedObject.getProperty(traits.key(PrincipalTraitDefinition.PASSWORD_CHANGE_DATE_PROPERTY));
	}

	@Override
	public void setPasswordAttempts(int num) throws FrameworkException {
		wrappedObject.setProperty(traits.key(PrincipalTraitDefinition.PASSWORD_ATTEMPTS_PROPERTY), num);
	}

	@Override
	public void setLastLoginDate(final Date date) throws FrameworkException {
		wrappedObject.setProperty(traits.key(PrincipalTraitDefinition.LAST_LOGIN_DATE_PROPERTY), date);
	}

	@Override
	public String[] getSessionIds() {
		return wrappedObject.getProperty(traits.key(PrincipalTraitDefinition.SESSION_IDS_PROPERTY));
	}

	@Override
	public String getProxyUrl() {
		return wrappedObject.getProperty(traits.key(PrincipalTraitDefinition.PROXY_URL_PROPERTY));
	}

	@Override
	public String getProxyUsername() {
		return wrappedObject.getProperty(traits.key(PrincipalTraitDefinition.PROXY_USERNAME_PROPERTY));
	}

	@Override
	public String getProxyPassword() {
		return wrappedObject.getProperty(traits.key(PrincipalTraitDefinition.PROXY_PASSWORD_PROPERTY));
	}

	@Override
	public void onAuthenticate() {

		for (final OnAuthenticate method : traits.getMethods(OnAuthenticate.class)) {
			method.onAuthenticate(this);
		}
	}

	@Override
	public Iterable<Group> getParents() {

		final Iterable<NodeInterface> nodes = wrappedObject.getProperty(traits.key(PrincipalTraitDefinition.GROUPS_PROPERTY));

		return Iterables.map(n -> n.as(Group.class), nodes);
	}

	@Override
	public Iterable<Group> getParentsPrivileged() {

		try {

			final App app            = StructrApp.getInstance();
			final NodeInterface node = app.getNodeById(StructrTraits.PRINCIPAL, wrappedObject.getUuid());

			if (node != null) {

				return node.as(Principal.class).getGroups();
			}

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);

			logger.warn("Caught exception while fetching groups for user '{}' ({})", wrappedObject.getName(), wrappedObject.getUuid());
			logger.warn(ExceptionUtils.getStackTrace(fex));

			return Collections.emptyList();
		}

		return Collections.emptyList();
	}

	@Override
	public boolean addSessionId(final String sessionId) {

		try {

			final PropertyKey<String[]> key = traits.key(PrincipalTraitDefinition.SESSION_IDS_PROPERTY);
			final String[] ids              = getSessionIds();

			if (ids != null) {

				if (!ArrayUtils.contains(ids, sessionId)) {

					if (Settings.MaxSessionsPerUser.getValue() > 0 && ids.length >= Settings.MaxSessionsPerUser.getValue()) {

						final Logger logger = LoggerFactory.getLogger(Principal.class);

						final String errorMessage = "Not adding session id, limit " + Settings.MaxSessionsPerUser.getKey() + " exceeded.";
						logger.warn(errorMessage);

						return false;
					}

					wrappedObject.setProperty(key, (String[]) ArrayUtils.add(wrappedObject.getProperty(key), sessionId));
				}

			} else {

				wrappedObject.setProperty(key, new String[] {  sessionId } );
			}

			return true;

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.error("Could not add sessionId " + sessionId + " to array of sessionIds", ex);

			return false;
		}
	}

	@Override
	public boolean addRefreshToken(final String refreshToken) {

		try {

			final PropertyKey<String[]> key = traits.key(PrincipalTraitDefinition.REFRESH_TOKENS_PROPERTY);
			final String[] refreshTokens    = wrappedObject.getProperty(key);

			if (refreshTokens != null) {

				if (!ArrayUtils.contains(refreshTokens, refreshToken)) {

					if (Settings.MaxSessionsPerUser.getValue() > 0 && refreshTokens.length >= Settings.MaxSessionsPerUser.getValue()) {

						final Logger logger = LoggerFactory.getLogger(Principal.class);

						final String errorMessage = "Not adding session id, limit " + Settings.MaxSessionsPerUser.getKey() + " exceeded.";
						logger.warn(errorMessage);

						return false;
					}

					wrappedObject.setProperty(key, (String[]) ArrayUtils.add(wrappedObject.getProperty(key), refreshToken));
				}

			} else {

				wrappedObject.setProperty(key, new String[] {  refreshToken } );
			}

			return true;

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.error("Could not add refreshToken " + refreshToken + " to array of refreshTokens", ex);

			return false;
		}
	}

	@Override
	public String[] getRefreshTokens() {
		return wrappedObject.getProperty(traits.key(PrincipalTraitDefinition.REFRESH_TOKENS_PROPERTY));
	}

	@Override
	public void removeSessionId(final String sessionId) {

		try {

			final PropertyKey<String[]> key = traits.key(PrincipalTraitDefinition.SESSION_IDS_PROPERTY);
			final String[] ids              = wrappedObject.getProperty(key);

			if (ids != null) {

				final Set<String> sessionIds = new HashSet<>(Arrays.asList(ids));

				sessionIds.remove(sessionId);

				wrappedObject.setProperty(key, (String[]) sessionIds.toArray(new String[0]));
			}

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.error("Could not remove sessionId " + sessionId + " from array of sessionIds", ex);
		}
	}

	@Override
	public void removeRefreshToken(final String refreshToken) {

		try {

			final PropertyKey<String[]> key = traits.key(PrincipalTraitDefinition.REFRESH_TOKENS_PROPERTY);
			final String[] refreshTokens    = wrappedObject.getProperty(key);

			if (refreshTokens != null) {

				final Set<String> refreshTokenSet = new HashSet<>(Arrays.asList(refreshTokens));

				refreshTokenSet.remove(refreshToken);

				wrappedObject.setProperty(key, (String[]) refreshTokenSet.toArray(new String[0]));
			}

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.error("Could not remove refreshToken " + refreshToken + " from array of refreshTokens", ex);
		}
	}

	@Override
	public void clearTokens() {

		try {

			PropertyMap properties = new PropertyMap();
			final PropertyKey<String[]> refreshTokensKey = traits.key(PrincipalTraitDefinition.REFRESH_TOKENS_PROPERTY);

			properties.put(refreshTokensKey, new String[0]);

			wrappedObject.setProperties(SecurityContext.getSuperUserInstance(), properties);

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.error("Could not clear refreshTokens of user {}", this, ex);
		}
	}

	@Override
	public final boolean isValidPassword(final String password) {

		final IsValidPassword method = traits.getMethod(IsValidPassword.class);
		if (method != null) {

			return method.isValidPassword(this, password);
		}

		return false;
	}

	@Override
	public String getEncryptedPassword() {

		final Node dbNode = wrappedObject.getNode();
		if (dbNode.hasProperty(PrincipalTraitDefinition.PASSWORD_PROPERTY)) {

			return (String)dbNode.getProperty(PrincipalTraitDefinition.PASSWORD_PROPERTY);
		}

		return null;
	}

	@Override
	public String getSalt() {

		final Node dbNode = wrappedObject.getNode();
		if (dbNode.hasProperty(PrincipalTraitDefinition.SALT_PROPERTY)) {

			return (String) dbNode.getProperty(PrincipalTraitDefinition.SALT_PROPERTY);
		}

		return null;
	}

	@Override
	public String getTwoFactorSecret() {

		final Node dbNode = wrappedObject.getNode();
		if (dbNode.hasProperty(PrincipalTraitDefinition.TWO_FACTOR_SECRET_PROPERTY)) {

			return (String) dbNode.getProperty(PrincipalTraitDefinition.TWO_FACTOR_SECRET_PROPERTY);
		}

		return null;
	}

	@Override
	public String getTwoFactorUrl() {

		final String twoFactorIssuer    = Settings.TwoFactorIssuer.getValue();
		final String twoFactorAlgorithm = Settings.TwoFactorAlgorithm.getValue();
		final Integer twoFactorDigits   = Settings.TwoFactorDigits.getValue();
		final Integer twoFactorPeriod   = Settings.TwoFactorPeriod.getValue();

		final StringBuilder path = new StringBuilder("/").append(twoFactorIssuer);

		final String eMail = wrappedObject.getProperty(traits.key(PrincipalTraitDefinition.EMAIL_PROPERTY));
		if (eMail != null) {
			path.append(":").append(eMail);
		} else {
			path.append(":").append(wrappedObject.getName());
		}

		final StringBuilder query = new StringBuilder("secret=").append(getTwoFactorSecret())
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
