/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.common;

import org.structr.api.Predicate;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.graph.RelationshipType;
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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A container that is used internally to combine a related node (determined
 * by IdDeserializationStrategy) and additional properties that are meant to
 * be set on the newly created relationship.
 */
public class EntityAndPropertiesContainer implements NodeInterface {

	private Map<String, Object> properties = null;
	private NodeInterface entity           = null;

	public EntityAndPropertiesContainer(final NodeInterface entity, final Map<String, Object> properties) {
		this.properties = properties;
		this.entity     = entity;
	}

	public NodeInterface getEntity() {
		return entity;
	}

	public Map<String, Object> getProperties() throws FrameworkException {
		return properties;
	}

	// ----- implement hashCode contract to make this class comparable to GraphObject -----
	@Override
	public int hashCode() {
		return entity.hashCode();
	}

	@Override
	public boolean equals(final Object other) {
		return other.hashCode() == hashCode();
	}

	@Override
	public boolean is(final String type) {
		return false;
	}

	@Override
	public void onNodeCreation(final SecurityContext securityContext) {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public void onNodeInstantiation(boolean isCreation) {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public void onNodeDeletion(SecurityContext securityContext) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public Node getNode() {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public boolean isDeleted() {
		return false;
	}

	@Override
	public String getName() {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public void setName(final String name) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public Object getPath(final SecurityContext securityContext) {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public boolean hasRelationshipTo(final RelationshipType relType, final NodeInterface targetNode) {
		throw new UnsupportedOperationException("Not supported by this container.");
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
	public void setRawPathSegmentId(final Identity pathSegmentId) {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public String getUuid() {
		return entity.getUuid();
	}

	@Override
	public void clearCaches() {
	}

	@Override
	public Traits getTraits() {
		return entity.getTraits();
	}

	@Override
	public <T> T as(final Class<T> type) {
		return entity.as(type);
	}

	@Override
	public String getType() {
		return entity.getType();
	}

	@Override
	public void setSecurityContext(SecurityContext securityContext) {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public SecurityContext getSecurityContext() {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public Node getPropertyContainer() {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public Set<PropertyKey> getFullPropertySet() {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public Set<PropertyKey> getPropertyKeys(final String propertyView) {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public <T> Object setProperty(final PropertyKey<T> key, final T value) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public <T> Object setProperty(final PropertyKey<T> key, T value, final boolean isCreation) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public <T> T getProperty(final PropertyKey<T> propertyKey) {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public <T> T getProperty(final PropertyKey<T> propertyKey, final Predicate<GraphObject> filter) {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public void setProperties(final SecurityContext securityContext, final PropertyMap properties, final boolean isCreation) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {
		return false;
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
	public void removeProperty(final PropertyKey key) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public boolean systemPropertiesUnlocked() {
		return false;
	}

	@Override
	public void unlockSystemPropertiesOnce() {
		throw new UnsupportedOperationException("Not supported by this container.");
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
		throw new UnsupportedOperationException("Not supported by this container.");
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
	public void indexPassiveProperties() {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public void addToIndex() {
		throw new UnsupportedOperationException("Not supported by this container.");
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
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public Object evaluate(ActionContext actionContext, String key, String defaultValue, EvaluationHints hints, final int row, final int column) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public List<GraphObject> getSyncData() throws FrameworkException {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public NodeInterface getSyncNode() {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public RelationshipInterface getSyncRelationship() {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public boolean isVisibleToPublicUsers() {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public boolean isVisibleToAuthenticatedUsers() {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public boolean isHidden() {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public void setHidden(boolean hidden) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public Date getCreatedDate() {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public Date getLastModifiedDate() {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public void setLastModifiedDate(Date date) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public Map<String, Object> getTemporaryStorage() {
		throw new UnsupportedOperationException("Not supported by this container.");
	}

	@Override
	public void visitForUsage(Map<String, Object> data) {

	}

	@Override
	public long getSourceTransactionId() {
		return entity.getSourceTransactionId();
	}

	@Override
	public boolean changelogEnabled() {
		return entity.changelogEnabled();
	}

	@Override
	public int compareTo(final NodeInterface o) {
		return 0;
	}
}
