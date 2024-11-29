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
import org.structr.core.traits.NodeInterfaceTrait;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.schema.action.Function;

import java.util.*;

/**
 * Abstract base class for all node entities in Structr.
 */
public abstract class AbstractNode extends AbstractGraphObject<Node> implements NodeInterface, AccessControllable {

	private static final Logger logger                                                                        = LoggerFactory.getLogger(AbstractNode.class.getName());
	private static final FixedSizeCache<String, Object> relationshipTemplateInstanceCache                     = new FixedSizeCache<>("Relationship template cache", 1000);

	/*
	public static final View defaultView = new View(AbstractNode.class, PropertyView.Public, id, type, name);

	public static final View uiView = new View(AbstractNode.class, PropertyView.Ui,
		id, name, owner, type, createdBy, hidden, createdDate, lastModifiedDate, visibleToPublicUsers, visibleToAuthenticatedUsers
	);

	 */

	private Identity rawPathSegmentId                         = null;
	protected NodeInterfaceTrait nodeInterfaceTrait           = null;

	@Override
	public void init(final SecurityContext securityContext, final PropertyContainer propertyContainer, final Class entityType, final long sourceTransactionId) {

		super.init(securityContext, propertyContainer, entityType, sourceTransactionId);

		this.nodeInterfaceTrait = typeHandler.getNodeInterfaceTrait();
	}

	@Override
	public void onNodeCreation(final SecurityContext securityContext) throws FrameworkException {
		nodeInterfaceTrait.onNodeCreation(this, securityContext);
	}

	@Override
	public void onNodeInstantiation(final boolean isCreation) {
		this.cachedUuid = getProperty(typeHandler.key("id"));
		nodeInterfaceTrait.onNodeInstantiation(this, isCreation);
	}

	@Override
	public void onNodeDeletion(SecurityContext securityContext) throws FrameworkException {
		nodeInterfaceTrait.onNodeDeletion(this, securityContext);
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

		String _name = getProperty(AbstractNode.name);
		if (_name == null) {

			_name = getUuid();
		}

		return _name;
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
		return nodeInterfaceTrait.hasRelationshipTo(this, type, targetNode);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> R getRelationshipTo(final RelationshipType type, final NodeInterface targetNode) {
		return nodeInterfaceTrait.getRelationshipTo(this, type, targetNode);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getRelationships() {
		return nodeInterfaceTrait.getRelationships(this);
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> Iterable<RelationshipInterface<A, B>> getRelationships(final Class<R> type) {
		return nodeInterfaceTrait.getRelationships(this, type);
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> RelationshipInterface<A, B> getIncomingRelationship(final Class<R> type) {
		return nodeInterfaceTrait.getIncomingRelationship(this, type);
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, OneStartpoint<A>, T>> RelationshipInterface<A, B> getIncomingRelationshipAsSuperUser(final Class<R> type) {
		return nodeInterfaceTrait.getIncomingRelationshipAsSuperUser(this, type);
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<RelationshipInterface<A, B>> getIncomingRelationships(final Class<R> type) {
		return nodeInterfaceTrait.getIncomingRelationships(this, type);
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> RelationshipInterface<A, B> getOutgoingRelationship(final Class<R> type) {
		return nodeInterfaceTrait.getOutgoingRelationship(this, type);
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, ManyEndpoint<B>>> Iterable<RelationshipInterface<A, B>> getOutgoingRelationships(final Class<R> type) {
		return nodeInterfaceTrait.getOutgoingRelationships(this, type);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getIncomingRelationships() {
		return nodeInterfaceTrait.getIncomingRelationships(this);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getOutgoingRelationships() {
		return nodeInterfaceTrait.getOutgoingRelationships(this);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, R extends RelationshipInterface<A, B>> Iterable<R> getRelationshipsAsSuperUser() {
		return nodeInterfaceTrait.getRelationshipsAsSuperUser(this);
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, T extends Target, R extends Relation<A, B, ManyStartpoint<A>, T>> Iterable<RelationshipInterface<A, B>> getIncomingRelationshipsAsSuperUser(final Class<R> type, final Predicate<NodeInterface> predicate) {
		return nodeInterfaceTrait.getIncomingRelationshipsAsSuperUser(this, type, predicate);
	}

	@Override
	public <A extends NodeInterface, B extends NodeInterface, S extends Source, R extends Relation<A, B, S, OneEndpoint<B>>> RelationshipInterface<A, B> getOutgoingRelationshipAsSuperUser(final Class<R> type) {
		return nodeInterfaceTrait.getOutgoingRelationshipAsSuperUser(this, type);
	}

	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target> boolean hasRelationship(final Class<? extends Relation<A, B, S, T>> type) {
		return nodeInterfaceTrait.hasRelationship(this, type);
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasIncomingRelationships(final Class<R> type) {
		return nodeInterfaceTrait.hasIncomingRelationships(this, type);
	}

	@Override
	public final <A extends NodeInterface, B extends NodeInterface, S extends Source, T extends Target, R extends Relation<A, B, S, T>> boolean hasOutgoingRelationships(final Class<R> type) {
		return nodeInterfaceTrait.hasOutgoingRelationships(this, type);
	}

	@Override
	public final Principal getOwnerNode() {
		return accessControllableTrait.getOwnerNode(this);
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
	public final RelationshipInterface<Principal, NodeInterface> getSecurityRelationship(final Principal p) {
		return accessControllableTrait.getSecurityRelationship(this, p);
	}

	@Override
	public List<RelationshipInterface<Principal, NodeInterface>> getSecurityRelationships() {
		return accessControllableTrait.getSecurityRelationships(this);
	}

	/*
	@Override
	public void onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		clearCaches();
	}

	@Override
	public void onDeletion(SecurityContext securityContext, ErrorBuffer errorBuffer, PropertyMap properties) throws FrameworkException {
		clearCaches();
	}

	@Override
	public void ownerModified(SecurityContext securityContext) {
		clearCaches();
	}

	@Override
	public void securityModified(SecurityContext securityContext) {
		clearCaches();
	}

	@Override
	public void locationModified(SecurityContext securityContext) {
		clearCaches();
	}

	@Override
	public void propagatedModification(SecurityContext securityContext) {
		clearCaches();
	}
	*/

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
		return getProperty(createdDate);
	}

	@Override
	public final Date getLastModifiedDate() {
		return getProperty(lastModifiedDate);
	}

	public static void clearRelationshipTemplateInstanceCache() {
		relationshipTemplateInstanceCache.clear();
	}

	public static void clearCaches() {
		globalPermissionResolutionCache.clear();
		isGrantedResultCache.clear();
	}

	public static <A extends NodeInterface, B extends NodeInterface, R extends Relation<A, B, ?, ?>> R getRelationshipForType(final Class<R> type) {

		R instance = (R) relationshipTemplateInstanceCache.get(type.getName());
		if (instance == null) {

			try {

				instance = type.getDeclaredConstructor().newInstance();
				relationshipTemplateInstanceCache.put(type.getName(), instance);

			} catch (Throwable t) {

				// TODO: throw meaningful exception here,
				// should be a RuntimeException that indicates
				// wrong use of Relationships etc.
				logger.warn("", t);
			}
		}

		return instance;
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
		accessControllableTrait.grant(this, permissions, principal, ctx);
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

		if (!isGranted(Permission.accessControl, ctx)) {
			throw new FrameworkException(403, getAccessControlNotPermittedExceptionString("revoke", permissions, principal, ctx));
		}

		clearCaches();

		RelationshipInterface<Principal, NodeInterface> secRel = getSecurityRelationship(principal);
		if (secRel != null) {

			secRel.removePermissions(permissions);
		}
	}


	@Override
	public final void setAllowed(Set<Permission> permissions, Principal principal) throws FrameworkException {
		setAllowed(permissions, principal, securityContext);
	}

	@Override
	public final void setAllowed(Set<Permission> permissions, Principal principal, SecurityContext ctx) throws FrameworkException {
		accessControllableTrait.setAllowed(this, permissions, principal, ctx);
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
