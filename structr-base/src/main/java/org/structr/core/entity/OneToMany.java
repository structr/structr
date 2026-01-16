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
package org.structr.core.entity;

import org.structr.api.graph.Direction;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.traits.Traits;

/**
 *
 *
 */
public abstract class OneToMany extends AbstractRelation implements Relation<OneStartpoint, ManyEndpoint> {

	@Override
	public Multiplicity getSourceMultiplicity() {
		return Multiplicity.One;
	}

	@Override
	public Multiplicity getTargetMultiplicity() {
		return Multiplicity.Many;
	}

	@Override
	public OneStartpoint getSource() {

		if (getSourceProperty() == null) {

			throw new RuntimeException("Invalid schema setup: missing StartNode(s) property for " + getType() + " in " + getTargetType());
		}

		return new OneStartpoint(this, getSourceProperty().jsonName());
	}

	@Override
	public ManyEndpoint getTarget() {

		if (getTargetProperty() == null) {

			throw new RuntimeException("Invalid schema setup: missing EndNode(s) property for " + getType() + " in " + getSourceType());
		}

		return new ManyEndpoint(this, getTargetProperty().jsonName());
	}

	@Override
	public void ensureCardinality(final SecurityContext securityContext, final NodeInterface sourceNode, final NodeInterface targetNode) throws FrameworkException {

		final App app           = StructrApp.getInstance();
		final String type       = this.getType();
		final String sourceType = getSourceType();

		if (targetNode != null) {

			// check existing relationships
			final RelationshipInterface incomingRel = targetNode.getIncomingRelationshipAsSuperUser(type);
			if (incomingRel != null) {

				incomingRel.setSecurityContext(securityContext);

				final Relation relation = incomingRel.getRelation();
				final Traits sourceTraits = Traits.of(relation.getSourceType());

				if (sourceTraits.contains(sourceType)) {

					app.delete(incomingRel);
				}
			}
		}
	}

	@Override
	public Direction getDirectionForType(final String type) {
		return getDirectionForType(getSourceType(), getTargetType(), type);
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
}