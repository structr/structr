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

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.NodeTrait;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

public interface Principal extends NodeTrait {

	String HIDDEN                            = "****** HIDDEN ******";
	String SUPERUSER_ID                      = "00000000000000000000000000000000";
	String ANONYMOUS                         = "anonymous";

	String getUuid();
	String getName();

	Iterable<NodeInterface> getOwnedNodes();
	Iterable<Group> getGroups();
	String getSessionData();
	String getEMail();
	void setSessionData(final String sessionData) throws FrameworkException;
	boolean isAdmin();
	boolean isBlocked();
	void setIsAdmin(final boolean isAdmin) throws FrameworkException;
	void setPassword(final String password) throws FrameworkException;
	void setEMail(final String eMail) throws FrameworkException;
	void setSalt(final String salt) throws FrameworkException;
	String getLocale();
	boolean shouldSkipSecurityRelationships();
	Iterable<Group> getParents();
	Iterable<Group> getParentsPrivileged();
	boolean addSessionId(final String sessionId);
	boolean addRefreshToken(final String refreshToken);
	String[] getRefreshTokens();
	void removeSessionId(final String sessionId);
	void removeRefreshToken(final String refreshToken);
	void clearTokens();
	boolean isValidPassword(final String password);
	String getEncryptedPassword();
	String getSalt();
	String getTwoFactorSecret();
	String getTwoFactorUrl();
	void setTwoFactorConfirmed(final boolean b) throws FrameworkException;
	void setTwoFactorToken(final String token) throws FrameworkException;
	boolean isTwoFactorUser();
	void setIsTwoFactorUser(final boolean b) throws FrameworkException;
	boolean isTwoFactorConfirmed();
	Integer getPasswordAttempts();
	Date getPasswordChangeDate();
	void setPasswordAttempts(int num) throws FrameworkException;
	void setLastLoginDate(final Date date) throws FrameworkException;
	String[] getSessionIds();
	String getProxyUrl();
	String getProxUsername();
	String getProxyPassword();
	void onAuthenticate();

	default Set<String> getOwnAndRecursiveParentsUuids() {

		final Set<String> uuids = new LinkedHashSet<>();

		recursiveCollectParentUuids(getWrappedNode(), uuids);

		return uuids;
	}

	default void recursiveCollectParentUuids(final NodeInterface node, final Set<String> uuids) {

		final Principal principal = node.as(Principal.class);

		uuids.add(principal.getUuid());

		for (final Group parent : principal.getParentsPrivileged()) {

			recursiveCollectParentUuids(parent.getWrappedNode(), uuids);
		}
	}
}
