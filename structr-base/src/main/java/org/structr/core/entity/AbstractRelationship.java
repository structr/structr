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

import org.structr.api.graph.PropertyContainer;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.IdNotFoundToken;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.schema.action.Function;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Base class for all relationship entities in Structr.
 */
public final class AbstractRelationship extends AbstractGraphObject<Relationship> implements Comparable<AbstractRelationship>, RelationshipInterface {

	private String cachedEndNodeId             = null;
	private String cachedStartNodeId           = null;
	private PropertyKey sourceProperty         = null;
	private PropertyKey targetProperty         = null;

	public AbstractRelationship(final SecurityContext securityContext, final Relationship dbRel, final String entityType, final long transactionId) {
		super(securityContext, dbRel, entityType, transactionId);
	}

	@Override
	public PropertyContainer getPropertyContainer() {
		return TransactionCommand.getCurrentTransaction().getRelationship(id);
	}

	@Override
	public boolean isVisibleToPublicUsers() {
		return true;
	}

	@Override
	public boolean isVisibleToAuthenticatedUsers() {
		return true;
	}

	@Override
	public boolean isHidden() {
		return false;
	}

	@Override
	public boolean equals(final Object o) {
		return (o != null && Integer.valueOf(this.hashCode()).equals(o.hashCode()));
	}

	@Override
	public int hashCode() {
		final String uuid = getUuid();
		if (uuid != null) {
			return uuid.hashCode();
		} else {
			return getRelationship().getId().hashCode();
		}
	}

	@Override
	public int compareTo(final AbstractRelationship rel) {

		// TODO: implement finer compare methods, e.g. taking title and position into account
		if (rel == null) {

			return -1;
		}

		return getUuid().compareTo(rel.getUuid());
	}

	@Override
	public PropertyMap getProperties() throws FrameworkException {

		Map<String, Object> properties = new LinkedHashMap<>();

		for (String key : getRelationship().getPropertyKeys()) {

			properties.put(key, getRelationship().getProperty(key));
		}

		// convert the database properties back to their java types
		return PropertyMap.databaseTypeToJavaType(securityContext, this, properties);

	}

	/**
	 * Return database relationship
	 *
	 * @return database relationship
	 */
	@Override
	public Relationship getRelationship() {
		return TransactionCommand.getCurrentTransaction().getRelationship(id);
	}

	@Override
	public boolean isDeleted() {
		return TransactionCommand.getCurrentTransaction().isRelationshipDeleted(id.getId());
	}

	@Override
	public NodeInterface getTargetNode() {
		NodeFactory nodeFactory = new NodeFactory(securityContext);
		return nodeFactory.instantiate(getRelationship().getEndNode());
	}

	@Override
	public NodeInterface getTargetNodeAsSuperUser() {
		NodeFactory nodeFactory = new NodeFactory(SecurityContext.getSuperUserInstance());
		return nodeFactory.instantiate(getRelationship().getEndNode());
	}

	@Override
	public NodeInterface getSourceNode() {
		NodeFactory nodeFactory = new NodeFactory(securityContext);
		return nodeFactory.instantiate(getRelationship().getStartNode());
	}

	@Override
	public NodeInterface getSourceNodeAsSuperUser() {
		NodeFactory nodeFactory = new NodeFactory(SecurityContext.getSuperUserInstance());
		return nodeFactory.instantiate(getRelationship().getStartNode());
	}

	@Override
	public NodeInterface getOtherNode(final NodeInterface node) {
		NodeFactory nodeFactory = new NodeFactory(securityContext);
		return nodeFactory.instantiate(getRelationship().getOtherNode(node.getNode()));
	}

	public NodeInterface getOtherNodeAsSuperUser(final NodeInterface node) {
		NodeFactory nodeFactory = new NodeFactory(SecurityContext.getSuperUserInstance());
		return nodeFactory.instantiate(getRelationship().getOtherNode(node.getNode()));
	}

	@Override
	public RelationshipType getRelType() {

		final Relationship dbRelationship = getRelationship();
		if (dbRelationship != null) {

			return dbRelationship.getType();
		}

		return null;
	}

	@Override
	public Relation getRelation() {
		return typeHandler.getRelation();
	}

	@Override
	public String getType() {
		return getProperty(typeHandler.key("type"));
	}

	@Override
	public String getSourceNodeId() {

		if (cachedStartNodeId == null) {

			final NodeInterface source = getSourceNode();
			if (source != null) {
				cachedStartNodeId = source.getUuid();
			}
		}

		return cachedStartNodeId;
	}

	@Override
	public String getTargetNodeId() {

		if (cachedEndNodeId == null) {

			final NodeInterface target = getTargetNode();
			if (target != null) {
				cachedEndNodeId = target.getUuid();
			}
		}

		return cachedEndNodeId;
	}

	public String getOtherNodeId(final AbstractNode node) {
		return getOtherNode(node).getUuid();
	}

	@Override
	public void setSourceNodeId(final String sourceNodeId) throws FrameworkException {

		// Do nothing if new id equals old
		if (getSourceNodeId().equals(sourceNodeId)) {
			return;
		}

		final App app = StructrApp.getInstance(securityContext);

		final NodeInterface newStartNode = app.getNodeById(sourceNodeId);
		final NodeInterface endNode      = getTargetNode();
		final String type                = typeHandler.getName();
		final PropertyMap _props         = getProperties();

		if (newStartNode == null) {
			throw new FrameworkException(404, "Node with ID " + sourceNodeId + " not found", new IdNotFoundToken(type, sourceNodeId));
		}

		// delete this as the new rel will be the container afterwards
		app.delete(this);

		// create new relationship
		app.create(newStartNode, endNode, type, _props);
	}

	@Override
	public void setTargetNodeId(final String targetNodeId) throws FrameworkException {

		// Do nothing if new id equals old
		if (getTargetNodeId().equals(targetNodeId)) {
			return;
		}

		final App app = StructrApp.getInstance(securityContext);

		final NodeInterface newTargetNode = app.getNodeById(targetNodeId);
		final NodeInterface startNode     = getSourceNode();
		final String type                 = typeHandler.getName();
		final PropertyMap _props          = getProperties();

		if (newTargetNode == null) {
			throw new FrameworkException(404, "Node with ID " + targetNodeId + " not found", new IdNotFoundToken(type, targetNodeId));
		}

		// delete this as the new rel will be the container afterwards
		app.delete(this);

		// create new relationship and store here
		app.create(startNode, newTargetNode, type, _props);
	}

	@Override
	public Object evaluate(final ActionContext actionContext, final String key, final String defaultValue, EvaluationHints hints, final int row, final int column) throws FrameworkException {

		switch (key) {

			case "_source":
				return getSourceNode();

			case "_target":
				return getTargetNode();

			default:

				// evaluate object value or return default
				final Object value = getProperty(typeHandler.key(key), actionContext.getPredicate());
				if (value == null) {

					return Function.numberOrString(defaultValue);
				}
				return value;
		}
	}

	public void setSourceProperty(final PropertyKey source) {
		this.sourceProperty = source;
	}

	public void setTargetProperty(final PropertyKey target) {
		this.targetProperty = target;
	}

	public PropertyKey getSourceProperty() {
		return sourceProperty;
	}

	public PropertyKey getTargetProperty() {
		return targetProperty;
	}

	@Override
	public boolean changelogEnabled() {
		return true;
	}

	// ----- Cloud synchronization and replication -----
	@Override
	public List<GraphObject> getSyncData() {
		return new ArrayList<>(); // provide a basis for super.getSyncData() calls
	}

	@Override
	public NodeInterface getSyncNode() {
		throw new ClassCastException(this.getClass() + " cannot be cast to org.structr.core.graph.NodeInterface");
	}

	@Override
	public RelationshipInterface getSyncRelationship() {
		return this;
	}
}
