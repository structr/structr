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
import org.structr.api.config.Settings;
import org.structr.api.graph.Node;
import org.structr.api.graph.PropertyContainer;
import org.structr.common.EMailValidator;
import org.structr.common.LowercaseTransformator;
import org.structr.common.SecurityContext;
import org.structr.common.TrimTransformator;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.HashHelper;
import org.structr.core.entity.*;
import org.structr.core.entity.relationship.GroupCONTAINSPrincipal;
import org.structr.core.entity.relationship.PrincipalFAVORITEFavoritable;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.property.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class PrincipalTrait extends NodeTraitImpl<Principal> implements Principal {

	static {

		final Trait<Principal> trait = Trait.create(Principal.class, n -> new PrincipalTrait(n));

		trait.registerProperty(new EndNodes<>("favorites", Trait.of(PrincipalFAVORITEFavoritable.class)));
		trait.registerProperty(new StartNodes<>("groups", Trait.of(GroupCONTAINSPrincipal.class)));

		trait.registerProperty(new EndNodes<>("ownedNodes", Trait.of(PrincipalOwnsNode.class)).partOfBuiltInSchema());
		trait.registerProperty(new EndNodes<>("grantedNodes", Trait.of(Security.class)).partOfBuiltInSchema());

		trait.registerProperty(new BooleanProperty("isAdmin").indexed().readOnly());
		trait.registerProperty(new BooleanProperty("blocked"));
		trait.registerProperty(new ArrayProperty("sessionIds", String.class).indexed());
		trait.registerProperty(new ArrayProperty("refreshTokens", String.class).indexed());
		trait.registerProperty(new StringProperty("sessionData"));
		trait.registerProperty(new StringProperty("eMail").indexed().unique().transformators(LowercaseTransformator.class.getName(), TrimTransformator.class.getName()));
		trait.registerProperty(new PasswordProperty("password"));
		trait.registerProperty(new DateProperty("passwordChangeDate"));
		trait.registerProperty(new IntProperty("passwordAttempts"));
		trait.registerProperty(new DateProperty("lastLoginDate"));
		trait.registerProperty(new StringProperty("twoFactorSecret"));
		trait.registerProperty(new StringProperty("twoFactorToken").indexed());
		trait.registerProperty(new BooleanProperty("isTwoFactorUser"));
		trait.registerProperty(new BooleanProperty("twoFactorConfirmed"));
		trait.registerProperty(new StringProperty("salt"));
		trait.registerProperty(new StringProperty("locale"));
		trait.registerProperty(new StringProperty("publicKey"));
		trait.registerProperty(new StringProperty("proxyUrl"));
		trait.registerProperty(new StringProperty("proxyUsername"));
		trait.registerProperty(new StringProperty("proxyPassword"));
		trait.registerProperty(new ArrayProperty("publicKeys", String.class));
	}

	public PrincipalTrait(final PropertyContainer obj) {
		super(obj);
	}

	@Override
	public void onAuthenticate() {
	}

	public Iterable<Favoritable> getFavorites() {
		return getProperty(traits.key("favorites"));
	}

	public Iterable<Group> getGroups() {
		return getProperty(key("groups"));
	}

	public String getSessionData() {
		return getProperty(key("sessionDataProperty"));
	}

	public String getEMail() {
		return getProperty(key("eMail"));
	}

	public void setSessionData(final String sessionData) throws FrameworkException {
		setProperty(key("sessionData"), sessionData);
	}

	public boolean isAdmin() {
		return getProperty(key("isAdmin"));
	}

	public boolean isBlocked() {
		return getProperty(key("blocked"));
	}

	public void setFavorites(final Iterable<Favoritable> favorites) throws FrameworkException {
		setProperty(key("favorites"), favorites);
	}

	public void setIsAdmin(final boolean isAdmin) throws FrameworkException {
		setProperty(key("isAdmin"), isAdmin);
	}

	public void setPassword(final String password) throws FrameworkException {
		setProperty(key("password"), password);
	}

	public void setEMail(final String eMail) throws FrameworkException {
		setProperty(key("eMail"), eMail);
	}

	public void setSalt(final String salt) throws FrameworkException {
		setProperty(key("salt"), salt);
	}

	public String getLocale() {
		return getProperty(key("locale"));
	}

	@Override
	public boolean shouldSkipSecurityRelationships() {
		return false;
	}

	@Override
	public void setTwoFactorConfirmed(final boolean b) throws FrameworkException {
		setProperty(key("twoFactorConfirmed"), b);
	}

	@Override
	public void setTwoFactorToken(final String token) throws FrameworkException {
		setProperty(key("twoFactorToken"), token);
	}

	@Override
	public boolean isTwoFactorUser() {
		return getProperty(key("isTwoFactorUser"));
	}

	@Override
	public void setIsTwoFactorUser(final boolean b) throws FrameworkException {
		setProperty(key("isTwoFactorUser"), b);

	}

	@Override
	public boolean isTwoFactorConfirmed() {
		return getProperty(key("isTwoFactorConfirmed"));
	}

	@Override
	public Integer getPasswordAttempts() {
		return getProperty(key("passwordAttempts"));
	}

	@Override
	public Date getPasswordChangeDate() {
		return getProperty(key("passwordChangeDate"));
	}

	@Override
	public void setPasswordAttempts(int num) throws FrameworkException {
		setProperty(key("passwordAttempts"), num);
	}

	@Override
	public void setLastLoginDate(final Date date) throws FrameworkException {
		setProperty(key("lastLoginDate"), date);
	}

	@Override
	public String[] getSessionIds() {
		return getProperty(key("sessionIds"));
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidUniqueProperty(this, key("eMail"), errorBuffer);
		valid &= new EMailValidator().isValid(this, errorBuffer);

		return valid;
	}

	public Iterable<Group> getParents() {
		return getProperty(key("groups"));
	}

	public Iterable<Group> getParentsPrivileged() {

		try {

			final App app                       = StructrApp.getInstance();
			final Principal privilegedPrincipal = app.getNodeById(Trait.of(Principal.class), getUuid());

			return privilegedPrincipal.getGroups();

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(Principal.class);

			logger.warn("Caught exception while fetching groups for user '{}' ({})", getName(), getUuid());
			logger.warn(ExceptionUtils.getStackTrace(fex));

			return Collections.emptyList();
		}
	}

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

			final PropertyKey<String[]> key = key("refreshTokens");
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

			final PropertyKey<String[]> key = key("sessionIds");
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

			final PropertyKey<String[]> key = key("refreshTokens");
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
			final PropertyKey<String[]> refreshTokensKey = key("refreshTokens");

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

		final String eMail = getProperty(key("eMail"));
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
