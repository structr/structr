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
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.notion.Notion;
import org.structr.core.notion.RelationshipNotion;
import org.structr.core.property.Property;
import org.structr.core.traits.Traits;

/**
 *
 *
 */
public abstract class ManyToOne extends AbstractRelation implements Relation<ManyStartpoint, OneEndpoint> {

	public ManyToOne(final Property<String> sourceId, final Property<String> targetId) {
		super(sourceId, targetId);
	}

	@Override
	public Multiplicity getSourceMultiplicity() {
		return Multiplicity.Many;
	}

	@Override
	public Multiplicity getTargetMultiplicity() {
		return Multiplicity.One;
	}

	@Override
	public ManyStartpoint getSource() {
		return new ManyStartpoint(this);
	}

	@Override
	public OneEndpoint getTarget() {
		return new OneEndpoint(this);
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.NONE;
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.NONE;
	}

	@Override
	public void ensureCardinality(final SecurityContext securityContext, final NodeInterface sourceNode, final NodeInterface targetNode) throws FrameworkException {

		final App app           = StructrApp.getInstance();
		final String type       = getType();
		final String targetType = getTargetType();

		if (sourceNode != null) {

			// check existing relationships
			final RelationshipInterface outgoingRel = sourceNode.getOutgoingRelationshipAsSuperUser(type);
			if (outgoingRel != null) {

				final Relation relation   = outgoingRel.getRelation();
				final Traits targetTraits = Traits.of(relation.getTargetType());

				if (targetTraits.contains(targetType)) {

					app.delete(outgoingRel);
				}
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