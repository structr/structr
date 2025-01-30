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
import org.structr.api.graph.Node;
import org.structr.common.AccessControllable;
import org.structr.common.EMailValidator;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.HashHelper;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class Principal extends AbstractNode implements PrincipalInterface, AccessControllable {

	public Iterable<Favoritable> getFavorites() {
		return getProperty(favoritesProperty);
	}

	public Iterable<Group> getGroups() {
		return getProperty(groupsProperty);
	}

	public String getSessionData() {
		return getProperty(sessionDataProperty);
	}

	public String getEMail() {
		return getProperty(eMailProperty);
	}

	public void setSessionData(final String sessionData) throws FrameworkException {
		setProperty(sessionDataProperty, sessionData);
	}

	public boolean isAdmin() {
		return getProperty(isAdminProperty);
	}

	public boolean isBlocked() {
		return getProperty(blockedProperty);
	}

	public void setFavorites(final Iterable<Favoritable> favorites) throws FrameworkException {
		setProperty(favoritesProperty, favorites);
	}

	public void setIsAdmin(final boolean isAdmin) throws FrameworkException {
		setProperty(isAdminProperty, isAdmin);
	}

	public void setPassword(final String password) throws FrameworkException {
		setProperty(passwordProperty, password);
	}

	public void setEMail(final String eMail) throws FrameworkException {
		setProperty(eMailProperty, eMail);
	}

	public void setSalt(final String salt) throws FrameworkException {
		setProperty(saltProperty, salt);
	}

	public String getLocale() {
		return getProperty(localeProperty);
	}

	public boolean shouldSkipSecurityRelationship() {
		return false;
	}

	public void onAuthenticate() {
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidUniqueProperty(this, Principal.eMailProperty, errorBuffer);
		valid &= new EMailValidator().isValid(this, errorBuffer);

		return valid;
	}

	@Override
	public <T> T getProperty(PropertyKey<T> key, Predicate<GraphObject> predicate) {

		if (key.equals(passwordProperty) || key.equals(saltProperty) || key.equals(twoFactorSecretProperty)) {

			return (T) Principal.HIDDEN;

		} else {

			return super.getProperty(key, predicate);
		}
	}

	@Override
	public <T> Object setProperty(PropertyKey<T> key, T value) throws FrameworkException {

		AbstractNode.clearCaches();

		return super.setProperty(key, value);
	}

	public Iterable<PrincipalInterface> getParents() {
		return (Iterable)getProperty(Principal.groupsProperty);
	}

	public Iterable<PrincipalInterface> getParentsPrivileged() {

		try {

			final App app                       = StructrApp.getInstance();
			final Principal privilegedPrincipal = app.get(Principal.class, getUuid());

			return (Iterable)privilegedPrincipal.getProperty(Principal.groupsProperty);

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);

			logger.warn("Caught exception while fetching groups for user '{}' ({})", getName(), getUuid());
			logger.warn(ExceptionUtils.getStackTrace(fex));

			return Collections.emptyList();
		}
	}

	public boolean addSessionId(final String sessionId) {

		try {

			final PropertyKey<String[]> key = StructrApp.key(Principal.class, "sessionIds");
			final String[] ids              = getProperty(key);

			if (ids != null) {

				if (!ArrayUtils.contains(ids, sessionId)) {

					if (Settings.MaxSessionsPerUser.getValue() > 0 && ids.length >= Settings.MaxSessionsPerUser.getValue()) {

						final Logger logger = LoggerFactory.getLogger(Principal.class);

						final String errorMessage = "Not adding session id, limit " + Settings.MaxSessionsPerUser.getKey() + " exceeded.";
						logger.warn(errorMessage);

						return false;
					}

					setProperty(key, (String[]) ArrayUtils.add(getProperty(key), sessionId));
				}

			} else {

				setProperty(key, new String[] {  sessionId } );
			}

			return true;

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.error("Could not add sessionId " + sessionId + " to array of sessionIds", ex);

			return false;
		}
	}

	public boolean addRefreshToken(final String refreshToken) {

		try {

			final PropertyKey<String[]> key = StructrApp.key(Principal.class, "refreshTokens");
			final String[] refreshTokens    = getProperty(key);

			if (refreshTokens != null) {

				if (!ArrayUtils.contains(refreshTokens, refreshToken)) {

					if (Settings.MaxSessionsPerUser.getValue() > 0 && refreshTokens.length >= Settings.MaxSessionsPerUser.getValue()) {

						final Logger logger = LoggerFactory.getLogger(Principal.class);

						final String errorMessage = "Not adding session id, limit " + Settings.MaxSessionsPerUser.getKey() + " exceeded.";
						logger.warn(errorMessage);

						return false;
					}

					setProperty(key, (String[]) ArrayUtils.add(getProperty(key), refreshToken));
				}

			} else {

				setProperty(key, new String[] {  refreshToken } );
			}

			return true;

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.error("Could not add refreshToken " + refreshToken + " to array of refreshTokens", ex);

			return false;
		}
	}

	public void removeSessionId(final String sessionId) {

		try {

			final PropertyKey<String[]> key = StructrApp.key(Principal.class, "sessionIds");
			final String[] ids              = getProperty(key);

			if (ids != null) {

				final Set<String> sessionIds = new HashSet<>(Arrays.asList(ids));

				sessionIds.remove(sessionId);

				setProperty(key, (String[]) sessionIds.toArray(new String[0]));
			}

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.error("Could not remove sessionId " + sessionId + " from array of sessionIds", ex);
		}
	}

	public void removeRefreshToken(final String refreshToken) {

		try {

			final PropertyKey<String[]> key = StructrApp.key(Principal.class, "refreshTokens");
			final String[] refreshTokens    = getProperty(key);

			if (refreshTokens != null) {

				final Set<String> refreshTokenSet = new HashSet<>(Arrays.asList(refreshTokens));

				refreshTokenSet.remove(refreshToken);

				setProperty(key, (String[]) refreshTokenSet.toArray(new String[0]));
			}

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.error("Could not remove refreshToken " + refreshToken + " from array of refreshTokens", ex);
		}
	}

	public void clearTokens() {

		try {

			PropertyMap properties = new PropertyMap();
			final PropertyKey<String[]> refreshTokensKey = StructrApp.key(Principal.class, "refreshTokens");

			properties.put(refreshTokensKey, new String[0]);

			setProperties(SecurityContext.getSuperUserInstance(), properties);

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.error("Could not clear refreshTokens of user {}", this, ex);
		}
	}

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

	public String getEncryptedPassword() {

		final Node dbNode = getNode();
		if (dbNode.hasProperty("password")) {

			return (String)dbNode.getProperty("password");
		}

		return null;
	}

	public String getSalt() {

		final Node dbNode = getNode();
		if (dbNode.hasProperty("salt")) {

			return (String) dbNode.getProperty("salt");
		}

		return null;
	}

	public String getTwoFactorSecret() {

		final Node dbNode = getNode();
		if (dbNode.hasProperty("twoFactorSecret")) {

			return (String) dbNode.getProperty("twoFactorSecret");
		}

		return null;
	}

	public String getTwoFactorUrl() {

		final String twoFactorIssuer    = Settings.TwoFactorIssuer.getValue();
		final String twoFactorAlgorithm = Settings.TwoFactorAlgorithm.getValue();
		final Integer twoFactorDigits   = Settings.TwoFactorDigits.getValue();
		final Integer twoFactorPeriod   = Settings.TwoFactorPeriod.getValue();

		final StringBuilder path = new StringBuilder("/").append(twoFactorIssuer);

		final String eMail = getProperty(StructrApp.key(Principal.class, "eMail"));
		if (eMail != null) {
			path.append(":").append(eMail);
		} else {
			path.append(":").append(getName());
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
