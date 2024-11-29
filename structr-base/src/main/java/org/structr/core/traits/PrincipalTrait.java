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

import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Favoritable;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;

import java.util.Date;

public interface PrincipalTrait {

	void onAuthenticate(final Principal principal);
	Iterable<Favoritable> getFavorites(final Principal principal);
	Iterable<Group> getGroups(final Principal principal);
	String getSessionData(final Principal principal);
	String getEMail(final Principal principal);
	void setSessionData(final Principal principal, final String sessionData) throws FrameworkException;
	boolean isAdmin(final Principal principal);
	boolean isBlocked(final Principal principal);
	void setFavorites(final Principal principal, final Iterable<Favoritable> favorites) throws FrameworkException;
	void setIsAdmin(final Principal principal, final boolean isAdmin) throws FrameworkException;
	void setPassword(final Principal principal, final String password) throws FrameworkException;
	void setEMail(final Principal principal, final String eMail) throws FrameworkException;
	void setSalt(final Principal principal, final String salt) throws FrameworkException;
	String getLocale(final Principal principal);
	boolean shouldSkipSecurityRelationships(final Principal principal);
	void setTwoFactorConfirmed(final Principal principal, final boolean b) throws FrameworkException;
	void setTwoFactorToken(final Principal principal, final String token) throws FrameworkException;
	boolean isTwoFactorUser(final Principal principal);
	void setIsTwoFactorUser(final Principal principal, final boolean b) throws FrameworkException;
	boolean isTwoFactorConfirmed(final Principal principal);
	Integer getPasswordAttempts(final Principal principal);
	Date getPasswordChangeDate(final Principal principal);
	void setPasswordAttempts(final Principal principal, final int num) throws FrameworkException;
	void setLastLoginDate(final Principal principal, final Date date) throws FrameworkException;
	String[] getSessionIds(final Principal principal);
	Iterable<Group> getParents(final Principal principal);
	Iterable<Group> getParentsPrivileged(final Principal principal);
	boolean addSessionId(final Principal principal, final String sessionId);
	boolean addRefreshToken(final Principal principal, final String refreshToken);
	void removeSessionId(final Principal principal, final String sessionId);
	void removeRefreshToken(final Principal principal, final String refreshToken);
	void clearTokens(final Principal principal);
	boolean isValidPassword(final Principal principal, final String password);
	String getEncryptedPassword(final Principal principal);
	String getSalt(final Principal principal);
	String getTwoFactorSecret(final Principal principal);
	String getTwoFactorUrl(final Principal principal);
}
