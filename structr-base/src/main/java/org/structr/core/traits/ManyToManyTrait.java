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
import org.structr.common.error.DuplicateRelationshipToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.ManyEndpoint;
import org.structr.core.entity.ManyStartpoint;
import org.structr.core.entity.ManyToMany;
import org.structr.core.entity.Relation;
import org.structr.core.notion.Notion;
import org.structr.core.notion.RelationshipNotion;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;

public abstract class ManyToManyTrait<S extends NodeTrait, T extends NodeTrait> extends RelationshipTraitImpl<S, T> implements ManyToMany<S, T> {

	public ManyToManyTrait(final PropertyContainer propertyContainer) {
		super(propertyContainer);
	}

	@Override
	public Trait<?> getOtherType(Trait<?> type) {
		return null;
	}

	@Override
	public Direction getDirectionForType(Trait<?> type) {
		return null;
	}

	@Override
	public Multiplicity getSourceMultiplicity() {
		return Multiplicity.Many;
	}

	@Override
	public Multiplicity getTargetMultiplicity() {
		return Multiplicity.Many;
	}

	@Override
	public ManyStartpoint<S> getSource() {
		return new ManyStartpoint<>(this);
	}

	@Override
	public ManyEndpoint<T> getTarget() {
		return new ManyEndpoint<>(this);
	}

	@Override
	public Property<String> getSourceIdProperty() {
		return null;
	}

	@Override
	public Property<String> getTargetIdProperty() {
		return null;
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
	public Notion getEndNodeNotion() {
		return new RelationshipNotion(getTargetIdProperty());

	}

	@Override
	public Notion getStartNodeNotion() {
		return new RelationshipNotion(getSourceIdProperty());
	}

	@Override
	public boolean isHidden() {
		return false;
	}

	@Override
	public void setSourceProperty(PropertyKey source) {

	}

	@Override
	public void setTargetProperty(PropertyKey target) {

	}

	@Override
	public PropertyKey getSourceProperty() {
		return null;
	}

	@Override
	public PropertyKey getTargetProperty() {
		return null;
	}

	@Override
	public String name() {
		return getName();
	}
}
