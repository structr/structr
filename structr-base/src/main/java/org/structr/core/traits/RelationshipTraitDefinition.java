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

import org.structr.core.entity.*;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;

import static org.structr.core.traits.GraphObjectTraitDefinition.SYSTEM_CATEGORY;

public abstract class RelationshipTraitDefinition extends AbstractTraitDefinition {

	protected static final Property<String>        internalTimestamp  = new StringProperty("internalTimestamp").systemInternal().unvalidated().writeOnce().partOfBuiltInSchema().category(SYSTEM_CATEGORY);
	protected static final Property<String>        relType            = new RelationshipTypeProperty();
	protected static final SourceId                sourceId           = new SourceId("sourceId");
	protected static final TargetId                targetId           = new TargetId("targetId");
	protected static final Property<NodeInterface> sourceNode         = new SourceNodeProperty("sourceNode");
	protected static final Property<NodeInterface> targetNode         = new TargetNodeProperty("targetNode");

	protected abstract String getSourceType();
	protected abstract String getTargetType();
	protected abstract String getRelationshipType();
	protected abstract Relation.Multiplicity getSourceMultiplicity();
	protected abstract Relation.Multiplicity getTargetMultiplicity();

	public RelationshipTraitDefinition(final String name) {
		super(name);
	}

	@Override
	public Relation getRelation() {

		int which = 0;

		// 0 = OneToOne, 1 = OneToMany, 2=ManyToOne, 3=ManyToMany
		//      0  1
		//   0 OO OM
		//   1 MO MM

		if (Relation.Multiplicity.Many.equals(getSourceMultiplicity())) {
			which += 1;
		}

		if (Relation.Multiplicity.Many.equals(getTargetMultiplicity())) {
			which += 2;
		}

		switch (which) {

			case 0 -> {

				return new OneToOne(sourceId, targetId) {

					@Override
					public String name() {
						return RelationshipTraitDefinition.this.getRelationshipType();
					}

					@Override
					public String getSourceType() {
						return RelationshipTraitDefinition.this.getSourceType();
					}

					@Override
					public String getTargetType() {
						return RelationshipTraitDefinition.this.getTargetType();
					}
				};
			}

			case 1 -> {

				return new ManyToOne(sourceId, targetId) {

					@Override
					public String name() {
						return RelationshipTraitDefinition.this.getRelationshipType();
					}

					@Override
					public String getSourceType() {
						return RelationshipTraitDefinition.this.getSourceType();
					}

					@Override
					public String getTargetType() {
						return RelationshipTraitDefinition.this.getTargetType();
					}
				};
			}

			case 2 -> {

				return new OneToMany(sourceId, targetId) {

					@Override
					public String name() {
						return RelationshipTraitDefinition.this.getRelationshipType();
					}

					@Override
					public String getSourceType() {
						return RelationshipTraitDefinition.this.getSourceType();
					}

					@Override
					public String getTargetType() {
						return RelationshipTraitDefinition.this.getTargetType();
					}
				};
			}

			case 3 -> {

				return new ManyToMany(sourceId, targetId) {

					@Override
					public String name() {
						return RelationshipTraitDefinition.this.getRelationshipType();
					}

					@Override
					public String getSourceType() {
						return RelationshipTraitDefinition.this.getSourceType();
					}

					@Override
					public String getTargetType() {
						return RelationshipTraitDefinition.this.getTargetType();
					}
				};
			}
		}

		return null;
	}

}
