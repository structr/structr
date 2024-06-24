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
import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.*;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.NonIndexed;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

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
	public Iterable<Principal> getParents() {
		return Collections.EMPTY_LIST;
	}

	@Override
	public Iterable<Principal> getParentsPrivileged() {

		// if the list of reference IDs is set, we search for groups with the given IDs and associate this principal with them
		if (jwksReferenceIds != null) {

			if (groups == null) {

				groups = new LinkedList<>();

				try {

					for (final String id : jwksReferenceIds) {

						groups.addAll(StructrApp.getInstance().nodeQuery(Group.class).and(StructrApp.key(Group.class, "jwksReferenceId"), id).getAsList());
					}

				} catch (FrameworkException fex) {
					fex.printStackTrace();
				}
			}

			if (groups != null) {

				return (Iterable) groups;
			}
		}

		return Collections.EMPTY_LIST;
	}

	@Override
	public boolean isValidPassword(String password) {
		return false;
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
	public void init(SecurityContext securityContext, Node dbNode, Class type, long sourceTransactionId) {
		this.securityContext = securityContext;
	}

	@Override
	public void onNodeCreation() {
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
	public String getName() {
		return (String)data.get("name");
	}

	@Override
	public boolean hasRelationshipTo(RelationshipType type, NodeInterface targetNode) {
		return false;
	}

	@Override
	public <R extends AbstractRelationship> R getRelationshipTo(RelationshipType type, NodeInterface targetNode) {
		return null;
	}

	@Override
	public <R extends AbstractRelationship> Iterable<R> getRelationships() {
		return null;
	}

	@Override
	public <R extends AbstractRelationship> Iterable<R> getRelationshipsAsSuperUser() {
		return null;
	}

	@Override
	public <R extends AbstractRelationship> Iterable<R> getIncomingRelationships() {
		return null;
	}

	@Override
	public <R extends AbstractRelationship> Iterable<R> getOutgoingRelationships() {
		return null;
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target> boolean hasRelationship(Class<? extends Relation<A, B, S, T>> type) {
		return false;
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasIncomingRelationships(Class<R> type) {
		return false;
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasOutgoingRelationships(Class<R> type) {
		return false;
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<R> getRelationships(Class<R> type) {
		return null;
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> R getIncomingRelationship(Class<R> type) {
		return null;
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> R getIncomingRelationshipAsSuperUser(Class<R> type) {
		return null;
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<R> getIncomingRelationships(Class<R> type) {
		return null;
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> R getOutgoingRelationship(Class<R> type) {
		return null;
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> R getOutgoingRelationshipAsSuperUser(Class<R> type) {
		return null;
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, ManyEndpoint<B>>> Iterable<R> getOutgoingRelationships(Class<R> type) {
		return null;
	}

	@Override
	public void setRawPathSegmentId(Identity pathSegmentId) {
	}

	@Override
	public List<Security> getSecurityRelationships() {
		return List.of();
	}

	@Override
	public Map<String, Object> getTemporaryStorage() {
		return Map.of();
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

	@Override
	public Security getSecurityRelationship(Principal principal) {
		return null;
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
	public boolean isNotHidden() {
		return false;
	}

	@Override
	public boolean isHidden() {
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
	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	@Override
	public SecurityContext getSecurityContext() {
		return securityContext;
	}

	@Override
	public PropertyContainer getPropertyContainer() {
		return null;
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
		throw new UnsupportedOperationException("This object is read-only.");
	}

	@Override
	public <T> Object setProperty(PropertyKey<T> key, T value, boolean isCreation) throws FrameworkException {
		throw new UnsupportedOperationException("This object is read-only.");
	}

	@Override
	public void setProperties(SecurityContext securityContext, PropertyMap properties) throws FrameworkException {
		throw new UnsupportedOperationException("This object is read-only.");
	}

	@Override
	public void setProperties(SecurityContext securityContext, PropertyMap properties, boolean isCreation) throws FrameworkException {
		throw new UnsupportedOperationException("This object is read-only.");
	}

	@Override
	public <T> T getProperty(PropertyKey<T> propertyKey) {
		return (T)data.get(propertyKey.jsonName());
	}

	@Override
	public <T> T getProperty(PropertyKey<T> propertyKey, Predicate<GraphObject> filter) {
		return getProperty(propertyKey);
	}

	@Override
	public <T> Comparable getComparableProperty(PropertyKey<T> key) {
		return null;
	}

	@Override
	public void removeProperty(PropertyKey key) throws FrameworkException {
		throw new UnsupportedOperationException("This object is read-only.");
	}

	@Override
	public void unlockSystemPropertiesOnce() {
	}

	@Override
	public void unlockReadOnlyPropertiesOnce() {
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {
		return false;
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
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public Object evaluate(ActionContext actionContext, String key, String defaultValue, EvaluationHints hints, int row, int column) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public Class getEntityType() {
		return this.getClass();
	}

	@Override
	public List<GraphObject> getSyncData() throws FrameworkException {
		return List.of();
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
	public String getUuid() {
		return (String)data.get("id");
	}

	// ----- private methods -----
	private boolean recursivelyCheckForAdminPermissions(final Iterable<Principal> parents) {

		for (final Principal parent : parents) {

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
