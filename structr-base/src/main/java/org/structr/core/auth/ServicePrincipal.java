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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;

import java.util.*;

public class ServicePrincipal implements Principal {

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
	public NodeInterface getWrappedNode() {
		return null;
	}

	@Override
	public SecurityContext getSecurityContext() {
		return null;
	}

	@Override
	public String getType() {
		return "ServicePrincipal";
	}

	@Override
	public Iterable<NodeInterface> getOwnedNodes() {
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

					final PropertyKey<String> jwksReferenceIdKey = Traits.of("Group").key("jwksReferenceId");

					for (final String id : jwksReferenceIds) {

						for (final NodeInterface node : StructrApp.getInstance().nodeQuery("Group").and(jwksReferenceIdKey, id).getResultStream()) {

							groups.add(node.as(Group.class));
						}
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
	public void setTwoFactorConfirmed(boolean b) throws FrameworkException {

	}

	@Override
	public void setTwoFactorToken(String token) throws FrameworkException {

	}

	@Override
	public boolean isTwoFactorUser() {
		return false;
	}

	@Override
	public void setIsTwoFactorUser(boolean b) throws FrameworkException {

	}

	@Override
	public boolean isTwoFactorConfirmed() {
		return false;
	}

	@Override
	public Integer getPasswordAttempts() {
		return 0;
	}

	@Override
	public Date getPasswordChangeDate() {
		return null;
	}

	@Override
	public void setPasswordAttempts(int num) throws FrameworkException {

	}

	@Override
	public void setLastLoginDate(Date date) throws FrameworkException {

	}

	@Override
	public String[] getSessionIds() {
		return new String[0];
	}

	@Override
	public String getProxyUrl() {
		return "";
	}

	@Override
	public String getProxUsername() {
		return "";
	}

	@Override
	public String getProxyPassword() {
		return "";
	}

	@Override
	public void onAuthenticate() {
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
	public String[] getRefreshTokens() {
		return new String[0];
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
	public void setName(String name) throws FrameworkException {

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
	public Date getCreatedDate() {
		return null;
	}

	@Override
	public Date getLastModifiedDate() {
		return null;
	}

	@Override
	public void setVisibleToAuthenticatedUsers(boolean visible) throws FrameworkException {

	}

	@Override
	public void setVisibleToPublicUsers(boolean visible) throws FrameworkException {

	}

	/*
	@Override
	public Principal getOwnerNode() {
		return null;
	}

	@Override
	public boolean allowedBySchema(Principal principal, Permission permission) {
		return false;
	}

	@Override
	public boolean isGranted(Permission permission, SecurityContext securityContext) {
		return false;
	}

	@Override
	public void grant(Permission permission, Principal principal) throws FrameworkException {
	}

	@Override
	public void grant(Set<Permission> permissions, Principal principal) throws FrameworkException {
	}

	@Override
	public void grant(Set<Permission> permissions, Principal principal, SecurityContext ctx) throws FrameworkException {
	}

	@Override
	public void revoke(Permission permission, Principal principal) throws FrameworkException {
	}

	@Override
	public void revoke(Set<Permission> permissions, Principal principal) throws FrameworkException {
	}

	@Override
	public void revoke(Set<Permission> permissions, Principal principal, SecurityContext ctx) throws FrameworkException {
	}

	@Override
	public void setAllowed(Set<Permission> permissions, Principal principal) throws FrameworkException {
	}

	@Override
	public void setAllowed(Set<Permission> permissions, Principal principal, SecurityContext ctx) throws FrameworkException {
	}
	*/

	@Override
	public String getUuid() {
		return (String)data.get("id");
	}

	// ----- private methods -----
	private boolean recursivelyCheckForAdminPermissions(final Iterable<Group> parents) {

		for (final Group parent : parents) {

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
