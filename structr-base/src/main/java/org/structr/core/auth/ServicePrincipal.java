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
package org.structr.core.auth;

import org.structr.api.graph.PropertyContainer;
import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.*;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.schema.NonIndexed;

import java.util.*;

public class ServicePrincipal implements Principal, AccessControllable, NonIndexed {

	private final Map<String, Object> data  = new LinkedHashMap<>();
	private SecurityContext securityContext = null;
	private List<String> jwksReferenceIds   = null;
	private List<Group> groups              = null;
	private boolean isAdmin                 = false;

	public ServicePrincipal(final String id, final String name, final List<String> jwksReferenceIds, final boolean isAdmin) {

		data.put("id",   id);
		data.put("name", name);

		this.jwksReferenceIds = jwksReferenceIds;
		this.isAdmin          = isAdmin;
	}

	@Override
	public String getType() {
		return "ServicePrincipal";
	}

	@Override
	public int compareTo(final Object other) {

		// provoke ClassCastException
		final ServicePrincipal u = (ServicePrincipal)other;

		return getUuid().compareTo(u.getUuid());
	}

	@Override
	public Iterable<Favoritable> getFavorites() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public Iterable<Group> getGroups() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public Iterable<Group> getParents() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public Iterable<Group> getParentsPrivileged() {

		// if the list of reference IDs is set, we search for groups with the given IDs and associate this principal with them
		if (jwksReferenceIds != null) {

			if (groups == null) {

				groups = new LinkedList<>();

				try {

					for (final String id : jwksReferenceIds) {

						groups.addAll(StructrApp.getInstance().nodeQuery(GroupTraitDefinition.class).and(StructrApp.key(GroupTraitDefinition.class, "jwksReferenceId"), id).getAsList());
					}

				} catch (FrameworkException fex) {
					fex.printStackTrace();
				}
			}

			if (groups != null) {

				return groups;
			}
		}

		return Collections.EMPTY_LIST;
	}

	@Override
	public boolean isValidPassword(String password) {
		return false;
	}

	@Override
	public String getEncryptedPassword() {
		return null;
	}

	@Override
	public String getSalt() {
		return null;
	}

	@Override
	public String getTwoFactorSecret() {
		return null;
	}

	@Override
	public String getTwoFactorUrl() {
		return null;
	}

	@Override
	public boolean addSessionId(String sessionId) {
		return false;
	}

	@Override
	public void removeSessionId(String sessionId) {
	}

	@Override
	public boolean addRefreshToken(String refreshToken) {
		return false;
	}

	@Override
	public void removeRefreshToken(String refreshToken) {
	}

	@Override
	public void clearTokens() {
	}

	@Override
	public String getSessionData() {
		return null;
	}

	@Override
	public String getEMail() {
		return (String)data.get("eMail");
	}

	@Override
	public void setSessionData(String sessionData) throws FrameworkException {
	}

	@Override
	public boolean isAdmin() {
		return isAdmin || recursivelyCheckForAdminPermissions(getParentsPrivileged());
	}

	@Override
	public boolean isBlocked() {
		return false;
	}

	@Override
	public boolean shouldSkipSecurityRelationships() {
		return true;
	}

	@Override
	public void setFavorites(Iterable<Favoritable> favorites) throws FrameworkException {
	}

	@Override
	public void setIsAdmin(boolean isAdmin) throws FrameworkException {
	}

	@Override
	public void setPassword(String password) throws FrameworkException {
	}

	@Override
	public void setEMail(String eMail) throws FrameworkException {
		data.put("eMail", eMail);
	}

	@Override
	public void setSalt(String salt) throws FrameworkException {
	}

	@Override
	public String getLocale() {
		return "en_EN";
	}

	@Override
	public String getName() {
		return (String)data.get("name");
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getRelationshipsAsSuperUser(Class<R> type) {
		return null;
	}

	@Override
	public Principal getOwnerNode() {
		return null;
	}

	@Override
	public boolean isGranted(Permission permission, SecurityContext securityContext) {
		return false;
	}

	@Override
	public boolean isVisibleToPublicUsers() {
		return false;
	}

	@Override
	public boolean isVisibleToAuthenticatedUsers() {
		return false;
	}

	@Override
	public String getUuid() {
		return (String)data.get("id");
	}

	@Override
	public PropertyContainer getPropertyContainer() {
		return null;
	}

	// ----- private methods -----
	private boolean recursivelyCheckForAdminPermissions(final Iterable<Group> parents) {

		for (final GroupTraitDefinition parent : parents) {

			if (parent.isAdmin()) {
				return true;
			}

			// recurse
			final boolean isAdmin = recursivelyCheckForAdminPermissions(parent.getParentsPrivileged());
			if (isAdmin) {

				return true;
			}
		}

		return false;
	}
}
