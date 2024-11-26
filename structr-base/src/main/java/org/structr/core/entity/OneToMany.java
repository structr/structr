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

import org.structr.core.traits.NodeTrait;

/**
 *
 *
 */
public interface OneToMany<S extends NodeTrait, T extends NodeTrait> extends Relation<S, T, OneStartpoint<S>, ManyEndpoint<T>> {

	/*
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
		final Class<S> sourceType              = getSourceType();

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
	public Direction getDirectionForType(final Trait<? extends RelationshipTrait> type) {
		return super.getDirectionForType(getSourceType(), getTargetType(), type);
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

	@Override
	public boolean isInternal() {
		return false;
	}
	*/
}