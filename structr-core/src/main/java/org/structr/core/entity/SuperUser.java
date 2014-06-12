/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.action.ActionContext;

//~--- classes ----------------------------------------------------------------

/**
 * The SuperUser entity. Please note that this class is not persitent but will
 * be instantiated when needed.
 *
 * @author amorgner
 *
 */
public class SuperUser implements Principal, AccessControllable {

	@Override
	public void removeProperty(PropertyKey key) throws FrameworkException {}

	@Override
	public void grant(Permission permission, AbstractNode obj) {}

	@Override
	public void revoke(Permission permission, AbstractNode obj) {}

	@Override
	public void unlockReadOnlyPropertiesOnce() {}

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		return true;

	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		return true;

	}

	@Override
	public boolean onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {

		return true;

	}

	@Override
	public void afterCreation(SecurityContext securityContext) {}

	@Override
	public void afterModification(SecurityContext securityContext) {}

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

	//~--- get methods ----------------------------------------------------

	@Override
	public long getId() {

		return -1L;

	}

//      @Override
	public String getRealName() {

		return "Super User";

	}

	@Override
	public String getEncryptedPassword() {

		return null;

	}

//      @Override
	public Object getPropertyForIndexing(String key) {

		return null;

	}

//      @Override
	public String getPassword() {

		return null;

	}

//      @Override
	public String getConfirmationKey() {

		return null;

	}

//      @Override
	public String getSessionId() {

		return null;

	}

	@Override
	public String getType() {

		return null;

	}

	@Override
	public Iterable<PropertyKey> getPropertyKeys(String propertyView) {

		return null;

	}

	@Override
	public <T> T getProperty(PropertyKey<T> key) {

		return null;

	}

	@Override
	public <T> T getProperty(PropertyKey<T> key, org.neo4j.helpers.Predicate<GraphObject> predicate) {
		return null;
	}

	@Override
	public <T> Comparable getComparableProperty(PropertyKey<T> key) {

		return null;
	}

	@Override
	public PropertyKey getDefaultSortKey() {

		return null;

	}

	@Override
	public String getDefaultSortOrder() {

		return null;

	}

	@Override
	public List<Principal> getParents() {

		return Collections.emptyList();

	}

	@Override
	public String getUuid() {

		return null;

	}

//      @Override
	public boolean isFrontendUser() {

		return (true);

	}

//      @Override
	public boolean isBackendUser() {

		return (true);

	}

	//~--- set methods ----------------------------------------------------

//      @Override
	public void setPassword(final String passwordValue) {

		// not supported
	}

//      @Override
	public void setRealName(final String realName) {

		// not supported
	}

	public void setConfirmationKey(String value) throws FrameworkException {}

	public void setFrontendUser(boolean isFrontendUser) throws FrameworkException {}

	public void setBackendUser(boolean isBackendUser) throws FrameworkException {}

	@Override
	public void setProperty(PropertyKey key, Object value) throws FrameworkException {}

	@Override
	public Object getPropertyForIndexing(PropertyKey key) {
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
	public void updateInIndex() {
	}

	@Override
	public void removeFromIndex() {
	}

	@Override
	public void indexPassiveProperties() {
	}

	@Override
	public void init(SecurityContext securityContext, Node dbNode) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void setSecurityContext(SecurityContext securityContext) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void onNodeCreation() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void onNodeInstantiation() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void onNodeDeletion() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Node getNode() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String getName() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean isDeleted() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <R extends AbstractRelationship> Iterable<R> getRelationships() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <R extends AbstractRelationship> Iterable<R> getIncomingRelationships() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <R extends AbstractRelationship> Iterable<R> getOutgoingRelationships() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<R> getRelationships(Class<R> type) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> R getIncomingRelationship(Class<R> type) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<R> getIncomingRelationships(Class<R> type) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> R getOutgoingRelationship(Class<R> type) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, ManyEndpoint<B>>> Iterable<R> getOutgoingRelationships(Class<R> type) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public int compareTo(NodeInterface o) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Principal getOwnerNode() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean isGranted(Permission permission, Principal principal) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Security getSecurityRelationship(Principal principal) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean isVisibleToPublicUsers() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean isVisibleToAuthenticatedUsers() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean isNotHidden() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean isHidden() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Date getVisibilityStartDate() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Date getVisibilityEndDate() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Date getCreatedDate() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Date getLastModifiedDate() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void addSessionId(String sessionId) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void removeSessionId(String sessionId) {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public SecurityContext getSecurityContext() {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String getPropertyWithVariableReplacement(SecurityContext securityContext, ActionContext renderContext, PropertyKey<String> key) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String replaceVariables(SecurityContext securityContext, ActionContext actionContext, Object rawValue) throws FrameworkException {
		throw new UnsupportedOperationException("Not supported."); //To change body of generated methods, choose Tools | Templates.
	}
}
