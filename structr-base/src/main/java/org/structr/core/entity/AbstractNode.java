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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.graph.*;
import org.structr.api.util.FixedSizeCache;
import org.structr.common.*;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.Methods;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.*;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.accesscontrollable.*;
import org.structr.core.traits.operations.nodeinterface.*;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.schema.action.Function;

import java.util.*;

/**
 * Abstract base class for all node entities in Structr.
 */
public class AbstractNode extends AbstractGraphObject<Node> implements NodeInterface, AccessControllable {

	private static final Logger logger                                                                        = LoggerFactory.getLogger(AbstractNode.class.getName());

	/*
	public static final View defaultView = new View(AbstractNode.class, PropertyView.Public, id, type, name);

	public static final View uiView = new View(AbstractNode.class, PropertyView.Ui,
		id, name, owner, type, createdBy, hidden, createdDate, lastModifiedDate, visibleToPublicUsers, visibleToAuthenticatedUsers
	);

	 */

	private Identity rawPathSegmentId = null;

	@Override
	public String getType() {
		return getProperty(typeHandler.key("type"));
	}

	@Override
	public void init(final SecurityContext securityContext, final PropertyContainer dbObject, final String type, final long sourceTransactionId) {



	}

	@Override
	public PropertyContainer getPropertyContainer() {
		return TransactionCommand.getCurrentTransaction().getNode(id);
	}

	@Override
	public long getSourceTransactionId() {
		return sourceTransactionId;
	}

	@Override
	public boolean equals(final Object o) {

		if (o == null) {

			return false;
		}

		if (!(o instanceof AbstractNode)) {

			return false;
		}

		return (Integer.valueOf(this.hashCode()).equals(o.hashCode()));

	}

	@Override
	public int hashCode() {

		if (getNode() == null) {

			return (super.hashCode());
		}

		return getNode().getId().hashCode();

	}

	@Override
	public int compareTo(final Object other) {

		if (other instanceof AbstractNode) {

			final AbstractNode node = (AbstractNode)other;
			final String _name      = getName();

			if (_name == null) {
				return -1;
			}

			final String nodeName = node.getName();
			if (nodeName == null) {

				return -1;
			}

			return _name.compareTo(nodeName);
		}

		if (other instanceof String) {

			return getUuid().compareTo((String)other);

		}

		if (other == null) {
			throw new NullPointerException();
		}

		throw new IllegalStateException("Cannot compare " + this + " to " + other);
	}

	/**
	 * Implement standard toString() method
	 */
	@Override
	public String toString() {
		return getUuid();

	}

	/**
	 * Get name from underlying db node
	 *
	 * If name is null, return node id as fallback
	 */
	@Override
	public final String getName() {

		String _name = getProperty(typeHandler.key("name"));
		if (_name == null) {

			_name = getUuid();
		}

		return _name;
	}

	@Override
	public void onNodeCreation(final SecurityContext securityContext) throws FrameworkException {

		for (final OnNodeCreation callback : typeHandler.getMethods(OnNodeCreation.class)) {
			callback.onNodeCreation(this, securityContext);
		}
	}

	@Override
	public void onNodeInstantiation(final boolean isCreation) {

		this.cachedUuid = getProperty(typeHandler.key("id"));

		for (final OnNodeInstantiation callback : typeHandler.getMethods(OnNodeInstantiation.class)) {
			callback.onNodeInstantiation(this, isCreation);
		}
	}

	@Override
	public void onNodeDeletion(SecurityContext securityContext) throws FrameworkException {

		for (final OnNodeDeletion callback : typeHandler.getMethods(OnNodeDeletion.class)) {
			callback.onNodeDeletion(this, securityContext);
		}
	}

	@Override
	public final Node getNode() {
		return (Node) getPropertyContainer();
	}

	@Override
	public boolean isDeleted() {
		return TransactionCommand.getCurrentTransaction().isNodeDeleted(id.getId());
	}

	@Override
	public boolean hasRelationshipTo(final RelationshipType type, final NodeInterface targetNode) {
		return typeHandler.getMethod(GetRelationships.class).hasRelationshipTo(this, type, targetNode);
	}

	@Override
	public RelationshipInterface getRelationshipTo(final RelationshipType type, final NodeInterface targetNode) {
		return typeHandler.getMethod(GetRelationships.class).getRelationshipTo(this, type, targetNode);
	}

	@Override
	public Iterable<RelationshipInterface> getRelationships() {
		return typeHandler.getMethod(GetRelationships.class).getRelationships(this);
	}

	@Override
	public final Iterable<RelationshipInterface> getRelationships(final String type) {
		return typeHandler.getMethod(GetRelationships.class).getRelationships(this, type);
	}

	@Override
	public final RelationshipInterface getIncomingRelationship(final String type) {
		return typeHandler.getMethod(GetRelationships.class).getIncomingRelationship(this, type);
	}

	@Override
	public final RelationshipInterface getIncomingRelationshipAsSuperUser(final String type) {
		return typeHandler.getMethod(GetRelationships.class).getIncomingRelationshipAsSuperUser(this, type);
	}

	@Override
	public final Iterable<RelationshipInterface> getIncomingRelationships(final String type) {
		return typeHandler.getMethod(GetRelationships.class).getIncomingRelationships(this, type);
	}

	@Override
	public final RelationshipInterface getOutgoingRelationship(final String type) {
		return typeHandler.getMethod(GetRelationships.class).getOutgoingRelationship(this, type);
	}

	@Override
	public final Iterable<RelationshipInterface> getOutgoingRelationships(final String type) {
		return typeHandler.getMethod(GetRelationships.class).getOutgoingRelationships(this, type);
	}

	@Override
	public  Iterable<RelationshipInterface> getIncomingRelationships() {
		return typeHandler.getMethod(GetRelationships.class).getIncomingRelationships(this);
	}

	@Override
	public  Iterable<RelationshipInterface> getOutgoingRelationships() {
		return typeHandler.getMethod(GetRelationships.class).getOutgoingRelationships(this);
	}

	@Override
	public  Iterable<RelationshipInterface> getRelationshipsAsSuperUser() {
		return typeHandler.getMethod(GetRelationships.class).getRelationshipsAsSuperUser(this);
	}

	@Override
	public  Iterable<RelationshipInterface> getRelationshipsAsSuperUser(String type) {
		return typeHandler.getMethod(GetRelationships.class).getRelationshipsAsSuperUser(this);
	}

	@Override
	public final Iterable<RelationshipInterface> getIncomingRelationshipsAsSuperUser(final String type, final Predicate<NodeInterface> predicate) {
		return typeHandler.getMethod(GetRelationships.class).getIncomingRelationshipsAsSuperUser(this, type, predicate);
	}

	@Override
	public RelationshipInterface getOutgoingRelationshipAsSuperUser(final String type) {
		return typeHandler.getMethod(GetRelationships.class).getOutgoingRelationshipAsSuperUser(this, type);
	}

	public final boolean hasRelationship(final String type) {
		return typeHandler.getMethod(GetRelationships.class).hasRelationship(this, type);
	}

	@Override
	public final boolean hasIncomingRelationships(final String type) {
		return typeHandler.getMethod(GetRelationships.class).hasIncomingRelationships(this, type);
	}

	@Override
	public final boolean hasOutgoingRelationships(final String type) {
		return typeHandler.getMethod(GetRelationships.class).hasOutgoingRelationships(this, type);
	}

	@Override
	public final Principal getOwnerNode() {
		return typeHandler.getMethod(GetOwnerNode.class).getOwnerNode(this);
	}

	@Override
	public boolean allowedBySchema(Principal principal, Permission permission) {
		return typeHandler.getMethod(AllowedBySchema.class).allowedBySchema(this, principal, permission);
	}

	@Override
	public boolean isGranted(Permission permission, SecurityContext securityContext) {
		return false;
	}

	/**
	 * Returns the database ID of the owner node of this node.
	 *
	 * @return the database ID of the owner node of this node
	 */
	public final String getOwnerId() {
		return getOwnerNode().getUuid();
	}

	@Override
	public final Security getSecurityRelationship(final Principal p) {
		return typeHandler.getMethod(GetSecurityRelationships.class).getSecurityRelationship(this, p);
	}

	@Override
	public List<Security> getSecurityRelationships() {
		return typeHandler.getMethod(GetSecurityRelationships.class).getSecurityRelationships(this);
	}

	@Override
	public boolean isVisibleToPublicUsers() {
		return getVisibleToPublicUsers();
	}

	@Override
	public boolean isVisibleToAuthenticatedUsers() {
		return getVisibleToAuthenticatedUsers();
	}

	@Override
	public final boolean isHidden() {
		return getHidden();
	}

	@Override
	public final Date getCreatedDate() {
		return getProperty(typeHandler.key("createdDate"));
	}

	@Override
	public final Date getLastModifiedDate() {
		return getProperty(typeHandler.key("lastModifiedDate"));
	}

	public final Object getPath(final SecurityContext currentSecurityContext) {

		if (rawPathSegmentId != null) {

			final Relationship rel = StructrApp.getInstance(currentSecurityContext).getDatabaseService().getRelationshipById(rawPathSegmentId);
			if (rel != null) {

				final RelationshipFactory factory = new RelationshipFactory(currentSecurityContext);
				return factory.instantiate(rel);
			}
		}

		return null;
	}

	@Override
	public Object evaluate(final ActionContext actionContext, final String key, final String defaultValue, final EvaluationHints hints, final int row, final int column) throws FrameworkException {

		hints.reportUsedKey(key, row, column);

		switch (key) {

			case "owner":
				hints.reportExistingKey(key);
				return getOwnerNode();

			case "_path":
				hints.reportExistingKey(key);
				return getPath(actionContext.getSecurityContext());

			default:

				// evaluate object value or return default
				final PropertyKey propertyKey = typeHandler.key(key);
				if (propertyKey != null) {

					hints.reportExistingKey(key);

					final Object value = getProperty(propertyKey, actionContext.getPredicate());
					if (value != null) {

						return value;
					}
				}

				final AbstractMethod method = Methods.resolveMethod(typeHandler, key);
				if (method != null) {

					final ContextStore contextStore = actionContext.getContextStore();
					final Map<String, Object> temp  = contextStore.getTemporaryParameters();
					final Arguments arguments       = Arguments.fromMap(temp);

					return method.execute(actionContext.getSecurityContext(), this, arguments, hints);
				}

				return Function.numberOrString(defaultValue);
		}
	}

	@Override
	public final void grant(final Permission permission, final Principal principal) throws FrameworkException {
		grant(Collections.singleton(permission), principal, securityContext);
	}

	@Override
	public final void grant(final Set<Permission> permissions, final Principal principal) throws FrameworkException {
		grant(permissions, principal, securityContext);
	}

	@Override
	public final void grant(final Set<Permission> permissions, final Principal principal, SecurityContext ctx) throws FrameworkException {
		typeHandler.getMethod(Grant.class).grant(this, permissions, principal, ctx);
	}

	@Override
	public final void revoke(Permission permission, Principal principal) throws FrameworkException {
		revoke(Collections.singleton(permission), principal, securityContext);
	}

	@Override
	public final void revoke(final Set<Permission> permissions, Principal principal) throws FrameworkException {
		revoke(permissions, principal, securityContext);
	}

	@Override
	public final void revoke(Set<Permission> permissions, Principal principal, SecurityContext ctx) throws FrameworkException {
		typeHandler.getMethod(Revoke.class).revoke(this, permissions, principal, ctx);
	}

	@Override
	public final void setAllowed(Set<Permission> permissions, Principal principal) throws FrameworkException {
		setAllowed(permissions, principal, securityContext);
	}

	@Override
	public final void setAllowed(Set<Permission> permissions, Principal principal, SecurityContext ctx) throws FrameworkException {
		typeHandler.getMethod(SetAllowed.class).setAllowed(this, permissions, principal, ctx);
	}

	@Override
	public final void setRawPathSegmentId(final Identity rawPathSegmentId) {
		this.rawPathSegmentId = rawPathSegmentId;
	}

	@Override
	public boolean changelogEnabled() {
		return true;
	}

	// ----- Cloud synchronization and replication -----
	@Override
	public List<GraphObject> getSyncData() throws FrameworkException {
		return new ArrayList<>(); // provide a basis for super.getSyncData() calls
	}

	@Override
	public final NodeInterface getSyncNode() {
		return this;
	}

	@Override
	public final RelationshipInterface getSyncRelationship() {
		throw new ClassCastException(this.getClass() + " cannot be cast to org.structr.core.graph.RelationshipInterface");
	}

	@Override
	public synchronized Map<String, Object> getTemporaryStorage() {

		if (tmpStorageContainer == null) {
			tmpStorageContainer = new LinkedHashMap<>();
		}

		return tmpStorageContainer;
	}

	protected boolean isGenericNode() {
		return false;
	}
}
