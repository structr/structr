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

import org.structr.api.graph.Direction;
import org.structr.api.graph.PropertyContainer;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.ManyEndpoint;
import org.structr.core.entity.OneStartpoint;
import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;
import org.structr.core.graph.search.SearchCommand;
import org.structr.core.notion.Notion;
import org.structr.core.notion.RelationshipNotion;

public abstract class OneToManyTrait<S extends NodeTrait, T extends NodeTrait> extends RelationshipTraitImpl<S, T> implements OneToMany<S, T> {

	public OneToManyTrait(final PropertyContainer propertyContainer) {
		super(propertyContainer);
	}

	@Override
	public Multiplicity getSourceMultiplicity() {
		return Multiplicity.One;
	}

	@Override
	public Multiplicity getTargetMultiplicity() {
		return Multiplicity.Many;
	}

	@Override
	public OneStartpoint<S> getSource() {
		return new OneStartpoint<>(this);
	}

	@Override
	public ManyEndpoint<T> getTarget() {
		return new ManyEndpoint<>(this);
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
	public void ensureCardinality(final SecurityContext securityContext, final NodeTrait sourceNode, final NodeTrait targetNode) throws FrameworkException {

		final App app                          = StructrApp.getInstance();
		final Trait<? extends OneToMany> clazz = this.trait;
		final Trait<S> sourceType              = getSourceType();

		if (targetNode != null) {

			// check existing relationships
			final Relation<?, T, ?, ?> incomingRel = targetNode.getIncomingRelationshipAsSuperUser(clazz);
			if (incomingRel != null && SearchCommand.isTypeAssignableFromOtherType(sourceType, incomingRel.getSourceType())) {

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
	public Direction getDirectionForType(final Trait<?> type) {
		return null;
	}

	@Override
	public Trait<?> getOtherType(final Trait<?> type) {

		switch (getDirectionForType(type)) {

			case INCOMING: return getSourceType();
			case OUTGOING: return getTargetType();
			case BOTH:     return getSourceType();	// don't know...
		}

		return null;
	}

	@Override
	public boolean isHidden() {
		return false;
	}
}
