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
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.NonIndexed;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.*;

/**
 * The SuperUser entity. Please note that this class is not persitent but will
 * be instantiated when needed.
 */
public class SuperUser implements Principal, AccessControllable, NonIndexed {

	@Override
	public void removeProperty(PropertyKey key) throws FrameworkException {}

	@Override
	public void grant(Permission permission, Principal obj) {}

	@Override
	public void grant(final Set<Permission> permissions, final Principal principal) {};

	@Override
	public void grant(final Set<Permission> permissions, final Principal principal, final SecurityContext ctx) {}

	@Override
	public void revoke(Permission permission, Principal obj) {}

	@Override
	public void revoke(final Set<Permission> permissions, final Principal principal) {}

	@Override
	public void revoke(final Set<Permission> permissions, final Principal principal, final SecurityContext ctx) {}

	@Override
	public void setAllowed(final Set<Permission> permissions, final Principal principal) throws FrameworkException {}

	@Override
	public void setAllowed(final Set<Permission> permissions, final Principal principal, final SecurityContext ctx) {}

	@Override
	public void unlockSystemPropertiesOnce() {}

	@Override
	public void unlockReadOnlyPropertiesOnce() {}

	@Override
	public void onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
	}

	@Override
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
	}

	@Override
	public void onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {
	}

	@Override
	public void afterCreation(SecurityContext securityContext) {}

	@Override
	public void afterModification(SecurityContext securityContext) throws FrameworkException {}

	@Override
	public void afterDeletion(SecurityContext securityContext, PropertyMap properties) {}

	@Override
	public void ownerModified(SecurityContext securityContext) {}

	@Override
	public void securityModified(SecurityContext securityContext) {}

	@Override
	public void locationModified(SecurityContext securityContext) {}

	@Override
	public void propagatedModification(SecurityContext securityContext) {}

	@Override
	public boolean isAdmin() {
		return true;
	}

	@Override
	public boolean isDeleted() {
		return false;
	}

	public String getRealName() {
		return "Super User";
	}

	public String getPassword() {
		return null;
	}

	public String getConfirmationKey() {
		return null;
	}

	public String getSessionId() {
		return null;
	}

	@Override
	public String getType() {
		return null;
	}

	@Override
	public Set<PropertyKey> getPropertyKeys(String propertyView) {
		return null;
	}

	@Override
	public <T> T getProperty(PropertyKey<T> key) {
		return null;
	}

	@Override
	public <T> T getProperty(PropertyKey<T> key, Predicate<GraphObject> predicate) {
		return null;
	}

	@Override
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {
	}

	@Override
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {
	}

	@Override
	public <T> Comparable getComparableProperty(PropertyKey<T> key) {
		return null;
	}

	@Override
	public List<Principal> getParents() {
		return Collections.emptyList();
	}

	@Override
	public List<Principal> getParentsPrivileged() {
		return Collections.emptyList();
	}

	@Override
	public String getUuid() {
		return Principal.SUPERUSER_ID;
	}

	@Override
	public boolean shouldSkipSecurityRelationships() {
		return true;
	}

	@Override
	public void setPassword(final String passwordValue) {
		// not supported
	}

	public void setRealName(final String realName) {
		// not supported
	}

	public void setConfirmationKey(String value) throws FrameworkException {}

	@Override
	public Object setProperty(PropertyKey key, Object value) throws FrameworkException {
		return null;
	}

	@Override
	public Object setProperty(PropertyKey key, Object value, final boolean isCreation) throws FrameworkException {
		return null;
	}

	@Override
	public PropertyContainer getPropertyContainer() {
		return null;
	}

	@Override
	public void addToIndex() {
	}

	@Override
	public void indexPassiveProperties() {
	}

	@Override
	public void init(SecurityContext securityContext, Node dbNode, final Class entityType, final long transactionId) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void setSecurityContext(SecurityContext securityContext) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void onNodeCreation(final SecurityContext securityContext) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void onNodeInstantiation(final boolean isCreation) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void onNodeDeletion(SecurityContext securityContext) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public Node getNode() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public String getName() {
		return "superadmin";
	}

	@Override
	public boolean hasRelationshipTo(final RelationshipType relType, final NodeInterface targetNode) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public <R extends AbstractRelationship> R getRelationshipTo(final RelationshipType relType, final NodeInterface targetNode) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public <R extends AbstractRelationship> Iterable<R> getRelationships() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public <R extends AbstractRelationship> Iterable<R> getIncomingRelationships() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public <R extends AbstractRelationship> Iterable<R> getOutgoingRelationships() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<R> getRelationships(Class<R> type) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> R getIncomingRelationship(Class<R> type) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<R> getIncomingRelationships(Class<R> type) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> R getOutgoingRelationship(Class<R> type) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, ManyEndpoint<B>>> Iterable<R> getOutgoingRelationships(Class<R> type) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public int compareTo(Object o) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public Principal getOwnerNode() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public boolean isGranted(Permission permission, SecurityContext context) {
		return true;
	}

	@Override
	public Security getSecurityRelationship(Principal principal) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public boolean isVisibleToPublicUsers() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public boolean isVisibleToAuthenticatedUsers() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public boolean isNotHidden() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public boolean isHidden() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public Date getCreatedDate() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public Date getLastModifiedDate() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public boolean addSessionId(String sessionId) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void removeSessionId(String sessionId) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public boolean addRefreshToken(String refreshToken) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void removeRefreshToken(String refreshToken) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public SecurityContext getSecurityContext() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public String getPropertyWithVariableReplacement(ActionContext renderContext, PropertyKey<String> key) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public Object evaluate(final ActionContext actionContext, final String key, final String defaultValue, EvaluationHints hints, final int row, final int column) throws FrameworkException {
		return null;
	}

	@Override
	public List<GraphObject> getSyncData() {
		return new ArrayList<>();
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
	public boolean isValidPassword(final String password) {
		return false;
	}

	@Override
	public void setRawPathSegmentId(final Identity rawSegmentId) {
		// nothing to do for SuperUser
	}

	@Override
	public <R extends AbstractRelationship> Iterable<R> getRelationshipsAsSuperUser() {
		return null;
	}

	@Override
	public Class getEntityType() {
		return SuperUser.class;
	}

	@Override
	public String getLocale() {
		return null;
	}

	@Override
	public String getSessionData() {
		return null;
	}

	@Override
	public void setSessionData(String sessionData) throws FrameworkException {
		// nothing to do for SuperUser
	}

	@Override
	public boolean isBlocked() {
		return false;
	}

	@Override
	public void setIsAdmin(boolean isAdmin) throws FrameworkException {
		// nothing to do
	}

	@Override
	public void setEMail(String eMail) throws FrameworkException {
		// nothing to do
	}

	@Override
	public void setSalt(String salt) throws FrameworkException {
		// nothing to do
	}

	@Override
	public List<Security> getSecurityRelationships() {
		return null;
	}

	@Override
	public List<Favoritable> getFavorites() {
		return null;
	}

	@Override
	public void setFavorites(final Iterable<Favoritable> favorites) throws FrameworkException {
		// nothing to do
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
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> R getOutgoingRelationshipAsSuperUser(Class<R> type) {
		return null;
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> R getIncomingRelationshipAsSuperUser(Class<R> type) {
		return null;
	}

	@Override
	public Map<String, Object> getTemporaryStorage() {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public String getEMail() {
		return null;
	}

	@Override
	public List<Group> getGroups() {
		return Collections.emptyList();
	}

	@Override
	public long getSourceTransactionId() {
		return TransactionCommand.getCurrentTransactionId();
	}

	@Override
	public boolean changelogEnabled() {
		return true;
	}
}
