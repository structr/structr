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

import org.structr.api.Predicate;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.graph.PropertyContainer;
import org.structr.api.graph.RelationshipType;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GroupTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

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
	public SecurityContext getSecurityContext() {
		return null;
	}

	@Override
	public PropertyContainer getPropertyContainer() {
		return null;
	}

	@Override
	public Set<PropertyKey> getFullPropertySet() {
		return Set.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys(String propertyView) {
		return Set.of();
	}

	@Override
	public long getSourceTransactionId() {
		return 0;
	}

	@Override
	public <T> Object setProperty(PropertyKey<T> key, T value) throws FrameworkException {
		return null;
	}

	@Override
	public <T> Object setProperty(PropertyKey<T> key, T value, boolean isCreation) throws FrameworkException {
		return null;
	}

	@Override
	public void setProperties(SecurityContext securityContext, PropertyMap properties) throws FrameworkException {

	}

	@Override
	public void setProperties(SecurityContext securityContext, PropertyMap properties, boolean isCreation) throws FrameworkException {

	}

	@Override
	public boolean isNode() {
		return false;
	}

	@Override
	public boolean isRelationship() {
		return false;
	}

	@Override
	public <V> V getProperty(PropertyKey<V> propertyKey) {
		return null;
	}

	@Override
	public <V> V getProperty(PropertyKey<V> propertyKey, Predicate<GraphObject> filter) {
		return null;
	}

	@Override
	public void removeProperty(PropertyKey key) throws FrameworkException {

	}

	@Override
	public boolean systemPropertiesUnlocked() {
		return false;
	}

	@Override
	public void unlockSystemPropertiesOnce() {

	}

	@Override
	public void lockSystemProperties() {

	}

	@Override
	public boolean readOnlyPropertiesUnlocked() {
		return false;
	}

	@Override
	public void unlockReadOnlyPropertiesOnce() {

	}

	@Override
	public void lockReadOnlyProperties() {

	}

	@Override
	public boolean isGranted(Permission permission, SecurityContext securityContext) {
		return false;
	}

	@Override
	public boolean isGranted(Permission permission, SecurityContext securityContext, boolean isCreation) {
		return false;
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {
		return false;
	}

	@Override
	public void indexPassiveProperties() {

	}

	@Override
	public void addToIndex() {

	}

	@Override
	public Traits getTraits() {
		return null;
	}

	@Override
	public <T> T as(Class<T> type) {
		return null;
	}

	@Override
	public boolean is(String type) {
		return false;
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

					final PropertyKey<String> jwksReferenceIdKey = Traits.of(StructrTraits.GROUP).key(GroupTraitDefinition.JWKS_REFERENCE_ID_PROPERTY);

					for (final String id : jwksReferenceIds) {

						for (final NodeInterface node : StructrApp.getInstance().nodeQuery(StructrTraits.GROUP).and(jwksReferenceIdKey, id).getResultStream()) {

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
	public String getProxyUsername() {
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
		return (String)data.get(PrincipalTraitDefinition.EMAIL_PROPERTY);
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
		data.put(PrincipalTraitDefinition.EMAIL_PROPERTY, eMail);
	}

	@Override
	public void setSalt(String salt) throws FrameworkException {
	}

	@Override
	public String getLocale() {
		return "en_EN";
	}

	@Override
	public void onNodeCreation(SecurityContext securityContext) throws FrameworkException {

	}

	@Override
	public void onNodeInstantiation(boolean isCreation) {

	}

	@Override
	public void onNodeDeletion(SecurityContext securityContext) throws FrameworkException {

	}

	@Override
	public Node getNode() {
		return null;
	}

	@Override
	public boolean isDeleted() {
		return false;
	}

	@Override
	public String getName() {
		return (String)data.get("name");
	}

	@Override
	public void setName(String name) throws FrameworkException {

	}

	@Override
	public Object getPath(SecurityContext securityContext) {
		return null;
	}

	@Override
	public boolean hasRelationshipTo(RelationshipType type, NodeInterface targetNode) {
		return false;
	}

	@Override
	public RelationshipInterface getRelationshipTo(RelationshipType type, NodeInterface targetNode) {
		return null;
	}

	@Override
	public Iterable<RelationshipInterface> getRelationships() {
		return null;
	}

	@Override
	public Iterable<RelationshipInterface> getRelationshipsAsSuperUser() {
		return null;
	}

	@Override
	public Iterable<RelationshipInterface> getRelationshipsAsSuperUser(String type) {
		return null;
	}

	@Override
	public Iterable<RelationshipInterface> getIncomingRelationships() {
		return null;
	}

	@Override
	public Iterable<RelationshipInterface> getOutgoingRelationships() {
		return null;
	}

	@Override
	public boolean hasRelationship(String type) {
		return false;
	}

	@Override
	public boolean hasIncomingRelationships(String type) {
		return false;
	}

	@Override
	public boolean hasOutgoingRelationships(String type) {
		return false;
	}

	@Override
	public Iterable<RelationshipInterface> getRelationships(String type) {
		return null;
	}

	@Override
	public RelationshipInterface getIncomingRelationship(String type) {
		return null;
	}

	@Override
	public RelationshipInterface getIncomingRelationshipAsSuperUser(String type) {
		return null;
	}

	@Override
	public Iterable<RelationshipInterface> getIncomingRelationships(String type) {
		return null;
	}

	@Override
	public Iterable<RelationshipInterface> getIncomingRelationshipsAsSuperUser(String type, Predicate<GraphObject> predicate) {
		return null;
	}

	@Override
	public RelationshipInterface getOutgoingRelationship(String type) {
		return null;
	}

	@Override
	public RelationshipInterface getOutgoingRelationshipAsSuperUser(String type) {
		return null;
	}

	@Override
	public Iterable<RelationshipInterface> getOutgoingRelationships(String type) {
		return null;
	}

	@Override
	public void setRawPathSegmentId(Identity pathSegmentId) {

	}

	@Override
	public Map<String, Object> getTemporaryStorage() {
		return Map.of();
	}

	@Override
	public void visitForUsage(Map<String, Object> data) {

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
	public boolean isHidden() {
		return false;
	}

	@Override
	public void setHidden(boolean hidden) throws FrameworkException {

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
	public void setLastModifiedDate(final Date date) throws FrameworkException {
	}

	@Override
	public void onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

	}

	@Override
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, ModificationQueue modificationQueue) throws FrameworkException {

	}

	@Override
	public void onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {

	}

	@Override
	public void afterCreation(SecurityContext securityContext) throws FrameworkException {

	}

	@Override
	public void afterModification(SecurityContext securityContext) throws FrameworkException {

	}

	@Override
	public void afterDeletion(SecurityContext securityContext, PropertyMap properties) {

	}

	@Override
	public void ownerModified(SecurityContext securityContext) {

	}

	@Override
	public void securityModified(SecurityContext securityContext) {

	}

	@Override
	public void locationModified(SecurityContext securityContext) {

	}

	@Override
	public void propagatedModification(SecurityContext securityContext) {

	}

	@Override
	public String getPropertyWithVariableReplacement(ActionContext renderContext, PropertyKey<String> key) throws FrameworkException {
		return "";
	}

	@Override
	public Object evaluate(ActionContext actionContext, String key, String defaultValue, EvaluationHints hints, int row, int column) throws FrameworkException {
		return null;
	}

	@Override
	public List<GraphObject> getSyncData() throws FrameworkException {
		return List.of();
	}

	@Override
	public NodeInterface getSyncNode() {
		return null;
	}

	@Override
	public RelationshipInterface getSyncRelationship() {
		return null;
	}

	@Override
	public boolean changelogEnabled() {
		return false;
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

	@Override
	public void clearCaches() {

	}

	@Override
	public void setSecurityContext(SecurityContext securityContext) {

	}

	@Override
	public int compareTo(final NodeInterface o) {
		return 0;
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
