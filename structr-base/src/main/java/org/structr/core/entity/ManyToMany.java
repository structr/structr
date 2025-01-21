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

import org.structr.api.graph.Direction;
import org.structr.common.SecurityContext;
import org.structr.common.error.DuplicateRelationshipToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;

/**
 *
 *
 */
public abstract class ManyToMany extends AbstractRelation implements Relation<ManyStartpoint, ManyEndpoint> {

	@Override
	public Multiplicity getSourceMultiplicity() {
		return Multiplicity.Many;
	}

	@Override
	public Multiplicity getTargetMultiplicity() {
		return Multiplicity.Many;
	}

	@Override
	public ManyStartpoint getSource() {
		return new ManyStartpoint(this);
	}

	@Override
	public ManyEndpoint getTarget() {
		return new ManyEndpoint(this);
	}

	@Override
	public void ensureCardinality(final SecurityContext securityContext, final NodeInterface sourceNode, final NodeInterface targetNode) throws FrameworkException {

		// The following code has nothing to do with cardinality, although it is implemented in the ensureCardinality method.
		// It checks for and prevents duplicate relationships (between exactly the same two nodes, which we don't want).

		if (securityContext.preventDuplicateRelationships() && targetNode != null && sourceNode.hasRelationshipTo(this, targetNode)) {

			final String message = "Relationship already exists from " + sourceNode.getType() + "(" + sourceNode.getUuid() + ") " + sourceNode.getName() + " to " + targetNode.getType() + " (" + targetNode.getUuid() + ") " + targetNode.getName();

			throw new FrameworkException(422, message, new DuplicateRelationshipToken(
				getClass().getSimpleName(),
				message
			));
		}
	}

	@Override
	public String getOtherType(final String type) {

		switch (getDirectionForType(type)) {

			case INCOMING: return getSourceType();
			case OUTGOING: return getTargetType();
			case BOTH:     return getSourceType();	// don't know...
		}

		return null;
	}

	@Override
	public Direction getDirectionForType(String type) {
		return getDirectionForType(getSourceType(), getTargetType(), type);
	}
}
