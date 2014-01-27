/**
 * Copyright (C) 2010-2014 Structr, c/o Morgner UG (haftungsbeschränkt) <structr@structr.org>
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

import org.neo4j.graphdb.Direction;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.notion.Notion;
import org.structr.core.notion.RelationshipNotion;

/**
 *
 * @author Christian Morgner
 */
public abstract class OneToOne<S extends NodeInterface, T extends NodeInterface> extends AbstractRelationship<S, T> implements Relation<S, T, OneStartpoint<S>, OneEndpoint<T>> {

	@Override
	public Multiplicity getSourceMultiplicity() {
		return Multiplicity.One;
	}

	@Override
	public Multiplicity getTargetMultiplicity() {
		return Multiplicity.One;
	}

	@Override
	public OneStartpoint<S> getSource() {
		return new OneStartpoint<>(this);
	}

	@Override
	public OneEndpoint<T> getTarget() {
		return new OneEndpoint<>(this);
	}
	
	@Override
	public int getCascadingDeleteFlag() {
		return Relation.NONE;
	}

	@Override
	public void ensureCardinality(final SecurityContext securityContext, final NodeInterface sourceNode, final NodeInterface targetNode) throws FrameworkException {

		final App app                         = StructrApp.getInstance();
		final Class<? extends OneToOne> clazz = getClass();
		final Class<S> sourceType             = getSourceType();
		final Class<T> targetType             = getTargetType();

		if (sourceNode != null && targetNode != null) {

			// check existing relationships
			final Relation<S, ?, ?, ?> outgoingRel = sourceNode.getOutgoingRelationship(clazz);
			final Relation<?, T, ?, ?> incomingRel = targetNode.getIncomingRelationship(clazz);

			// remove relationship if exists
			if (outgoingRel != null && targetType.isAssignableFrom(outgoingRel.getTargetType())) {
				app.delete(outgoingRel);
			}

			if (incomingRel != null && sourceType.isAssignableFrom(incomingRel.getSourceType())) {
				app.delete(incomingRel);
			}
		}
	}
	
	@Override
	public Notion getEndNodeNotion() {
		return new RelationshipNotion(getTargetIdProperty());

	}

	@Override
	public Notion getStartNodeNotion() {
		return new RelationshipNotion(getSourceIdProperty());
	}
	
	@Override
	public Direction getDirectionForType(final Class<? extends NodeInterface> type) {
		return super.getDirectionForType(getSourceType(), getTargetType(), type);
	}
}