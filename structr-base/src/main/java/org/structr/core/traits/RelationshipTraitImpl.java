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
package org.structr.core.traits;

import org.structr.api.graph.PropertyContainer;
import org.structr.api.graph.Relationship;
import org.structr.api.graph.RelationshipType;
import org.structr.common.SecurityContext;
import org.structr.core.graph.NodeFactory;

public class RelationshipTraitImpl<S extends NodeTrait, T extends NodeTrait> extends GraphTraitImpl implements RelationshipTrait<S, T> {

	private String cachedStartNodeId;
	private String cachedEndNodeId;

	public RelationshipTraitImpl(final PropertyContainer propertyContainer) {
		super(propertyContainer);
	}

	@Override
	public Relationship getRelationship() {
		//return TransactionCommand.getCurrentTransaction().getRelationship(relationshipId);
		return (Relationship)getPropertyContainer();
	}

	@Override
	public final T getTargetNode() {

		final NodeFactory<T> nodeFactory = new NodeFactory<>(securityContext);
		return nodeFactory.instantiate(getRelationship().getEndNode());
	}

	@Override
	public int getCascadingDeleteFlag() {
		return 0;
	}

	@Override
	public final T getTargetNodeAsSuperUser() {

		final NodeFactory<T> nodeFactory = new NodeFactory<>(SecurityContext.getSuperUserInstance());
		return nodeFactory.instantiate(getRelationship().getEndNode());
	}

	@Override
	public final S getSourceNode() {

		final NodeFactory<S> nodeFactory = new NodeFactory<>(securityContext);
		return nodeFactory.instantiate(getRelationship().getStartNode());
	}

	@Override
	public final String getSourceNodeId() {

		if (cachedStartNodeId == null) {

			final NodeTrait source = getSourceNode();
			if (source != null) {
				cachedStartNodeId = source.getUuid();
			}
		}

		return cachedStartNodeId;
	}

	@Override
	public final String getTargetNodeId() {

		if (cachedEndNodeId == null) {

			final NodeTrait target = getTargetNode();
			if (target != null) {
				cachedEndNodeId = target.getUuid();
			}
		}

		return cachedEndNodeId;
	}

	@Override
	public final S getSourceNodeAsSuperUser() {

		final NodeFactory<S> nodeFactory = new NodeFactory<>(SecurityContext.getSuperUserInstance());
		return nodeFactory.instantiate(getRelationship().getStartNode());
	}

	@Override
	public final NodeTrait getOtherNode(final NodeTrait node) {

		final NodeFactory nodeFactory = new NodeFactory(securityContext);
		return nodeFactory.instantiate(getRelationship().getOtherNode(node.getNode()));
	}

	public final NodeTrait getOtherNodeAsSuperUser(final NodeTrait node) {

		final NodeFactory nodeFactory = new NodeFactory(SecurityContext.getSuperUserInstance());
		return nodeFactory.instantiate(getRelationship().getOtherNode(node.getNode()));
	}

	@Override
	public final RelationshipType getRelType() {

		final Relationship dbRelationship = getRelationship();
		if (dbRelationship != null) {

			return dbRelationship.getType();
		}

		return null;
	}

	@Override
	public String getName() {
		return traits.getName();
	}
}
