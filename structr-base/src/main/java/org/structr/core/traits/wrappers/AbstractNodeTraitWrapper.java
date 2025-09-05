/*
 * Copyright (C) 2010-2025 Structr GmbH
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
package org.structr.core.traits.wrappers;

import org.structr.api.Predicate;
import org.structr.api.graph.Identity;
import org.structr.api.graph.Node;
import org.structr.api.graph.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.traits.Traits;

import java.util.Map;

public class AbstractNodeTraitWrapper extends GraphObjectTraitWrapper<NodeInterface> implements NodeInterface {

	public AbstractNodeTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public void onNodeCreation(final SecurityContext securityContext) throws FrameworkException {
		wrappedObject.onNodeCreation(securityContext);
	}

	@Override
	public void onNodeInstantiation(final boolean isCreation) {
		wrappedObject.onNodeInstantiation(isCreation);
	}

	@Override
	public void onNodeDeletion(final SecurityContext securityContext) throws FrameworkException {
		wrappedObject.onNodeDeletion(securityContext);
	}

	@Override
	public Node getNode() {
		return wrappedObject.getNode();
	}

	@Override
	public boolean isDeleted() {
		return wrappedObject.isDeleted();
	}

	@Override
	public Object getPath(final SecurityContext securityContext) {
		return wrappedObject.getPath(securityContext);
	}

	@Override
	public boolean hasRelationshipTo(final RelationshipType type, final NodeInterface targetNode) {
		return wrappedObject.hasRelationshipTo(type, targetNode);
	}

	@Override
	public RelationshipInterface getRelationshipTo(final RelationshipType type, final NodeInterface targetNode) {
		return wrappedObject.getRelationshipTo(type, targetNode);
	}

	@Override
	public Iterable<RelationshipInterface> getRelationships() {
		return wrappedObject.getRelationships();
	}

	@Override
	public Iterable<RelationshipInterface> getRelationshipsAsSuperUser() {
		return wrappedObject.getRelationshipsAsSuperUser();
	}

	@Override
	public Iterable<RelationshipInterface> getRelationshipsAsSuperUser(final String type) {
		return wrappedObject.getRelationshipsAsSuperUser(type);
	}

	@Override
	public Iterable<RelationshipInterface> getIncomingRelationships() {
		return wrappedObject.getIncomingRelationships();
	}

	@Override
	public Iterable<RelationshipInterface> getOutgoingRelationships() {
		return wrappedObject.getOutgoingRelationships();
	}

	@Override
	public boolean hasRelationship(final String type) {
		return wrappedObject.hasRelationship(type);
	}

	@Override
	public boolean hasIncomingRelationships(final String type) {
		return wrappedObject.hasIncomingRelationships(type);
	}

	@Override
	public boolean hasOutgoingRelationships(final String type) {
		return wrappedObject.hasOutgoingRelationships(type);
	}

	@Override
	public Iterable<RelationshipInterface> getRelationships(final String type) {
		return wrappedObject.getRelationships(type);
	}

	@Override
	public RelationshipInterface getIncomingRelationship(final String type) {
		return wrappedObject.getIncomingRelationship(type);
	}

	@Override
	public RelationshipInterface getIncomingRelationshipAsSuperUser(final String type) {
		return wrappedObject.getIncomingRelationshipAsSuperUser(type);
	}

	@Override
	public Iterable<RelationshipInterface> getIncomingRelationships(final String type) {
		return wrappedObject.getIncomingRelationships(type);
	}

	@Override
	public Iterable<RelationshipInterface> getIncomingRelationshipsAsSuperUser(final String type, final Predicate<GraphObject> predicate) {
		return wrappedObject.getIncomingRelationshipsAsSuperUser(type, predicate);
	}

	@Override
	public RelationshipInterface getOutgoingRelationship(final String type) {
		return wrappedObject.getOutgoingRelationship(type);
	}

	@Override
	public RelationshipInterface getOutgoingRelationshipAsSuperUser(final String type) {
		return wrappedObject.getOutgoingRelationshipAsSuperUser(type);
	}

	@Override
	public Iterable<RelationshipInterface> getOutgoingRelationships(final String type) {
		return wrappedObject.getOutgoingRelationships(type);
	}

	@Override
	public void setRawPathSegmentId(final Identity pathSegmentId) {
		wrappedObject.setRawPathSegmentId(pathSegmentId);
	}

	@Override
	public Map<String, Object> getTemporaryStorage() {
		return wrappedObject.getTemporaryStorage();
	}

	@Override
	public void visitForUsage(final Map<String, Object> data) {
		wrappedObject.visitForUsage(data);
	}

	@Override
	public int compareTo(final NodeInterface o) {
		return wrappedObject.compareTo(o);
	}
}
