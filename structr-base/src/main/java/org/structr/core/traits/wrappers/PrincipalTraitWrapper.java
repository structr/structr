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
package org.structr.core.traits.wrappers;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.graph.Node;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.HashHelper;
import org.structr.core.entity.Favoritable;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.AbstractTraitWrapper;
import org.structr.core.traits.Traits;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class PrincipalTraitWrapper extends AbstractTraitWrapper implements Principal {

	public PrincipalTraitWrapper(Traits traits, NodeInterface nodeInterface) {
		super(traits, nodeInterface);
	}

	@Override
	public Iterable<Favoritable> getFavorites() {
		return nodeInterface.getProperty(traits.key("favorites"));
	}

	@Override
	public Iterable<Group> getGroups() {
		return nodeInterface.getProperty(traits.key("groups"));
	}

	@Override
	public String getSessionData() {
		return nodeInterface.getProperty(traits.key("sessionDataProperty"));
	}

	@Override
	public String getEMail() {
		return nodeInterface.getProperty(traits.key("eMail"));
	}

	@Override
	public void setSessionData(final String sessionData) throws FrameworkException {
		nodeInterface.setProperty(traits.key("sessionData"), sessionData);
	}

	@Override
	public boolean isAdmin() {
		return nodeInterface.getProperty(traits.key("isAdmin"));
	}

	public boolean isBlocked() {
		return nodeInterface.getProperty(traits.key("blocked"));
	}

	@Override
	public void setFavorites(final Iterable<Favoritable> favorites) throws FrameworkException {
		nodeInterface.setProperty(traits.key("favorites"), favorites);
	}

	@Override
	public void setIsAdmin(final boolean isAdmin) throws FrameworkException {
		nodeInterface.setProperty(traits.key("isAdmin"), isAdmin);
	}

	@Override
	public void setPassword(final String password) throws FrameworkException {
		nodeInterface.setProperty(traits.key("password"), password);
	}

	@Override
	public void setEMail(final String eMail) throws FrameworkException {
		nodeInterface.setProperty(traits.key("eMail"), eMail);
	}

	@Override
	public void setSalt(final String salt) throws FrameworkException {
		nodeInterface.setProperty(traits.key("salt"), salt);
	}

	@Override
	public String getLocale() {
		return nodeInterface.getProperty(traits.key("locale"));
	}

	@Override
	public boolean shouldSkipSecurityRelationships() {
		// fixme: this should be overridable
		//return nodeInterface.getProperty(traits.key("skipSecurityRelationships"));
	}

	@Override
	public void setTwoFactorConfirmed(final boolean b) throws FrameworkException {
		nodeInterface.setProperty(traits.key("twoFactorConfirmed"), b);
	}

	@Override
	public void setTwoFactorToken(final String token) throws FrameworkException {
		nodeInterface.setProperty(traits.key("twoFactorToken"), token);
	}

	@Override
	public boolean isTwoFactorUser() {
		return nodeInterface.getProperty(traits.key("isTwoFactorUser"));
	}

	@Override
	public void setIsTwoFactorUser(final boolean b) throws FrameworkException {
		nodeInterface.setProperty(traits.key("isTwoFactorUser"), b);

	}

	@Override
	public boolean isTwoFactorConfirmed() {
		return nodeInterface.getProperty(traits.key("isTwoFactorConfirmed"));
	}

	@Override
	public Integer getPasswordAttempts() {
		return nodeInterface.getProperty(traits.key("passwordAttempts"));
	}

	@Override
	public Date getPasswordChangeDate() {
		return nodeInterface.getProperty(traits.key("passwordChangeDate"));
	}

	@Override
	public void setPasswordAttempts(int num) throws FrameworkException {
		nodeInterface.setProperty(traits.key("passwordAttempts"), num);
	}

	@Override
	public void setLastLoginDate(final Date date) throws FrameworkException {
		nodeInterface.setProperty(traits.key("lastLoginDate"), date);
	}

	@Override
	public String[] getSessionIds() {
		return nodeInterface.getProperty(traits.key("sessionIds"));
	}

	@Override
	public Iterable<Group> getParents() {
		return nodeInterface.getProperty(traits.key("groups"));
	}

	@Override
	public Iterable<Group> getParentsPrivileged() {

		try {

			final App app             = StructrApp.getInstance();
			final NodeInterface node  = app.getNodeById("Principal", nodeInterface.getUuid());
			final Principal principal = new PrincipalTraitWrapper(null, node);

			return principal.getGroups();

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);

			logger.warn("Caught exception while fetching groups for user '{}' ({})", nodeInterface.getName(), nodeInterface.getUuid());
			logger.warn(ExceptionUtils.getStackTrace(fex));

			return Collections.emptyList();
		}
	}

	@Override
	public boolean addSessionId(final String sessionId) {

		try {

			final PropertyKey<String[]> key = traits.key("sessionIds");
			final String[] ids              = getSessionIds();

			if (ids != null) {

				if (!ArrayUtils.contains(ids, sessionId)) {

					if (Settings.MaxSessionsPerUser.getValue() > 0 && ids.length >= Settings.MaxSessionsPerUser.getValue()) {

						final Logger logger = LoggerFactory.getLogger(Principal.class);

						final String errorMessage = "Not adding session id, limit " + Settings.MaxSessionsPerUser.getKey() + " exceeded.";
						logger.warn(errorMessage);

						return false;
					}

					nodeInterface.setProperty(key, (String[]) ArrayUtils.add(nodeInterface.getProperty(key), sessionId));
				}

			} else {

				nodeInterface.setProperty(key, new String[] {  sessionId } );
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

			final PropertyKey<String[]> key = traits.key("refreshTokens");
			final String[] refreshTokens    = nodeInterface.getProperty(key);

			if (refreshTokens != null) {

				if (!ArrayUtils.contains(refreshTokens, refreshToken)) {

					if (Settings.MaxSessionsPerUser.getValue() > 0 && refreshTokens.length >= Settings.MaxSessionsPerUser.getValue()) {

						final Logger logger = LoggerFactory.getLogger(Principal.class);

						final String errorMessage = "Not adding session id, limit " + Settings.MaxSessionsPerUser.getKey() + " exceeded.";
						logger.warn(errorMessage);

						return false;
					}

					nodeInterface.setProperty(key, (String[]) ArrayUtils.add(nodeInterface.getProperty(key), refreshToken));
				}

			} else {

				nodeInterface.setProperty(key, new String[] {  refreshToken } );
			}

			return true;

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.error("Could not add refreshToken " + refreshToken + " to array of refreshTokens", ex);

			return false;
		}
	}

	@Override
	public void removeSessionId(final String sessionId) {

		try {

			final PropertyKey<String[]> key = traits.key("sessionIds");
			final String[] ids              = nodeInterface.getProperty(key);

			if (ids != null) {

				final Set<String> sessionIds = new HashSet<>(Arrays.asList(ids));

				sessionIds.remove(sessionId);

				nodeInterface.setProperty(key, (String[]) sessionIds.toArray(new String[0]));
			}

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.error("Could not remove sessionId " + sessionId + " from array of sessionIds", ex);
		}
	}

	@Override
	public void removeRefreshToken(final String refreshToken) {

		try {

			final PropertyKey<String[]> key = traits.key("refreshTokens");
			final String[] refreshTokens    = nodeInterface.getProperty(key);

			if (refreshTokens != null) {

				final Set<String> refreshTokenSet = new HashSet<>(Arrays.asList(refreshTokens));

				refreshTokenSet.remove(refreshToken);

				nodeInterface.setProperty(key, (String[]) refreshTokenSet.toArray(new String[0]));
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
			final PropertyKey<String[]> refreshTokensKey = traits.key("refreshTokens");

			properties.put(refreshTokensKey, new String[0]);

			nodeInterface.setProperties(SecurityContext.getSuperUserInstance(), properties);

		} catch (FrameworkException ex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);
			logger.error("Could not clear refreshTokens of user {}", this, ex);
		}
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

		final Node dbNode = nodeInterface.getNode();
		if (dbNode.hasProperty("password")) {

			return (String)dbNode.getProperty("password");
		}

		return null;
	}

	@Override
	public String getSalt() {

		final Node dbNode = nodeInterface.getNode();
		if (dbNode.hasProperty("salt")) {

			return (String) dbNode.getProperty("salt");
		}

		return null;
	}

	@Override
	public String getTwoFactorSecret() {

		final Node dbNode = nodeInterface.getNode();
		if (dbNode.hasProperty("twoFactorSecret")) {

			return (String) dbNode.getProperty("twoFactorSecret");
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

		final String eMail = nodeInterface.getProperty(traits.key("eMail"));
		if (eMail != null) {
			path.append(":").append(eMail);
		} else {
			path.append(":").append(nodeInterface.getName());
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
