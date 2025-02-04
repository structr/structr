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
import org.structr.api.graph.*;
import org.structr.common.AccessControllable;
import org.structr.common.ContextStore;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.Methods;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipFactory;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.operations.nodeinterface.*;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.schema.action.Function;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base class for all node entities in Structr.
 */
public final class AbstractNode extends AbstractGraphObject<Node> implements NodeInterface {

	/*
	public static final View defaultView = new View(NodeInterface.class, PropertyView.Public, id, type, name);

	public static final View uiView = new View(NodeInterface.class, PropertyView.Ui,
		id, name, owner, type, createdBy, hidden, createdDate, lastModifiedDate, visibleToPublicUsers, visibleToAuthenticatedUsers
	);

	 */

	private Identity rawPathSegmentId = null;

	public AbstractNode(final SecurityContext securityContext, final PropertyContainer propertyContainer, final long sourceTransactionId) {
		super(securityContext, propertyContainer, sourceTransactionId);
	}

	@Override
	public String getType() {
		return getProperty(typeHandler.key("type"));
	}

	@Override
	public PropertyContainer getPropertyContainer() {
		return TransactionCommand.getCurrentTransaction().getNode(id);
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
	public int compareTo(final NodeInterface other) {

		final NodeInterface node = other;
		final String _name       = getName();

		if (_name == null) {
			return -1;
		}

		final String nodeName = node.getName();
		if (nodeName == null) {

			return -1;
		}

		return _name.compareTo(nodeName);
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
	public String getName() {

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
	public Node getNode() {
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
	public Iterable<RelationshipInterface> getRelationships(final String type) {
		return typeHandler.getMethod(GetRelationships.class).getRelationships(this, type);
	}

	@Override
	public RelationshipInterface getIncomingRelationship(final String type) {
		return typeHandler.getMethod(GetRelationships.class).getIncomingRelationship(this, type);
	}

	@Override
	public RelationshipInterface getIncomingRelationshipAsSuperUser(final String type) {
		return typeHandler.getMethod(GetRelationships.class).getIncomingRelationshipAsSuperUser(this, type);
	}

	@Override
	public Iterable<RelationshipInterface> getIncomingRelationships(final String type) {
		return typeHandler.getMethod(GetRelationships.class).getIncomingRelationships(this, type);
	}

	@Override
	public RelationshipInterface getOutgoingRelationship(final String type) {
		return typeHandler.getMethod(GetRelationships.class).getOutgoingRelationship(this, type);
	}

	@Override
	public Iterable<RelationshipInterface> getOutgoingRelationships(final String type) {
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
	public Iterable<RelationshipInterface> getIncomingRelationshipsAsSuperUser(final String type, final Predicate<GraphObject> predicate) {
		return typeHandler.getMethod(GetRelationships.class).getIncomingRelationshipsAsSuperUser(this, type, predicate);
	}

	@Override
	public RelationshipInterface getOutgoingRelationshipAsSuperUser(final String type) {
		return typeHandler.getMethod(GetRelationships.class).getOutgoingRelationshipAsSuperUser(this, type);
	}

	public boolean hasRelationship(final String type) {
		return typeHandler.getMethod(GetRelationships.class).hasRelationship(this, type);
	}

	@Override
	public boolean hasIncomingRelationships(final String type) {
		return typeHandler.getMethod(GetRelationships.class).hasIncomingRelationships(this, type);
	}

	@Override
	public boolean hasOutgoingRelationships(final String type) {
		return typeHandler.getMethod(GetRelationships.class).hasOutgoingRelationships(this, type);
	}

	public Object getPath(final SecurityContext currentSecurityContext) {

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

				final Principal owner = as(AccessControllable.class).getOwnerNode();
				if (owner != null) {

					return owner.getWrappedNode();
				}

				return null;

			case "_path":
				hints.reportExistingKey(key);
				return getPath(actionContext.getSecurityContext());

			default:

				// evaluate object value or return default
				if (typeHandler.hasKey(key)) {

					final PropertyKey propertyKey = typeHandler.key(key);

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
	public void setRawPathSegmentId(final Identity rawPathSegmentId) {
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
	public NodeInterface getSyncNode() {
		return this;
	}

	@Override
	public RelationshipInterface getSyncRelationship() {
		throw new ClassCastException(this.getClass() + " cannot be cast to org.structr.core.graph.RelationshipInterface");
	}

	@Override
	public synchronized Map<String, Object> getTemporaryStorage() {

		if (tmpStorageContainer == null) {
			tmpStorageContainer = new LinkedHashMap<>();
		}

		return tmpStorageContainer;
	}

	@Override
	public void visitForUsage(final Map<String, Object> data) {
		typeHandler.getMethod(VisitForUsage.class).visitForUsage(this, data);
	}

	protected boolean isGenericNode() {
		return false;
	}
}
