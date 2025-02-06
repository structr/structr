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
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.*;

/**
 * The SuperUser entity. Please note that this class is not persistent but will
 * be instantiated when needed.
 */
public class SuperUser implements Principal {

	@Override
	public boolean isAdmin() {
		return true;
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
		return null;
	}

	@Override
	public List<Group> getParents() {
		return Collections.emptyList();
	}

	@Override
	public List<Group> getParentsPrivileged() {
		return Collections.emptyList();
	}

	@Override
	public String getUuid() {
		return Principal.SUPERUSER_ID;
	}

	@Override
	public void clearCaches() {

	}

	@Override
	public void setSecurityContext(SecurityContext securityContext) {

	}

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
		return "superadmin";
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
	public Iterable<NodeInterface> getOwnedNodes() {
		return null;
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
	public String[] getRefreshTokens() {
		return new String[0];
	}

	@Override
	public void removeRefreshToken(String refreshToken) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public void clearTokens() {

	}

	@Override
	public boolean isValidPassword(final String password) {
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
	public String getEMail() {
		return null;
	}

	@Override
	public List<Group> getGroups() {
		return Collections.emptyList();
	}

	@Override
	public int compareTo(final NodeInterface o) {
		return 0;
	}
}
