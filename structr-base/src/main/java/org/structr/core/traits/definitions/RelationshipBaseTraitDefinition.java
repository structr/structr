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
package org.structr.core.traits.definitions;

import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.core.entity.*;
import org.structr.core.property.Property;

public interface RelationshipBaseTraitDefinition extends TraitDefinition {

	String getSourceType();
	String getTargetType();
	String getRelationshipType();
	Relation.Multiplicity getSourceMultiplicity();
	Relation.Multiplicity getTargetMultiplicity();
	int getCascadingDeleteFlag();
	int getAutocreationFlag();
	boolean isInternal();

	@Override
	default Relation getRelation() {

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

		// FIXME
		final Property<String> sourceId = null;
		final Property<String> targetId = null;

		switch (which) {

			case 0 -> {

				return new OneToOne(sourceId, targetId) {

					@Override
					public String name() {
						return RelationshipBaseTraitDefinition.this.getRelationshipType();
					}

					@Override
					public String getType() {
						return getName();
					}

					@Override
					public String getSourceType() {
						return RelationshipBaseTraitDefinition.this.getSourceType();
					}

					@Override
					public String getTargetType() {
						return RelationshipBaseTraitDefinition.this.getTargetType();
					}

					@Override
					public int getCascadingDeleteFlag() {
						return RelationshipBaseTraitDefinition.this.getCascadingDeleteFlag();
					}

					@Override
					public int getAutocreationFlag() {
						return RelationshipBaseTraitDefinition.this.getAutocreationFlag();
					}

					@Override
					public PropagationDirection getPropagationDirection() {
						return RelationshipBaseTraitDefinition.this.getPropagationDirection();
					}

					@Override
					public PropagationMode getReadPropagation() {
						return RelationshipBaseTraitDefinition.this.getReadPropagation();
					}

					@Override
					public PropagationMode getWritePropagation() {
						return RelationshipBaseTraitDefinition.this.getWritePropagation();
					}

					@Override
					public PropagationMode getDeletePropagation() {
						return RelationshipBaseTraitDefinition.this.getDeletePropagation();
					}

					@Override
					public PropagationMode getAccessControlPropagation() {
						return RelationshipBaseTraitDefinition.this.getAccessControlPropagation();
					}

					@Override
					public String getDeltaProperties() {
						return RelationshipBaseTraitDefinition.this.getDeltaProperties();
					}

					@Override
					public boolean isInternal() {
						return RelationshipBaseTraitDefinition.this.isInternal();
					}
				};
			}

			case 1 -> {

				return new ManyToOne(sourceId, targetId) {

					@Override
					public String name() {
						return RelationshipBaseTraitDefinition.this.getRelationshipType();
					}

					@Override
					public String getType() {
						return getName();
					}

					@Override
					public String getSourceType() {
						return RelationshipBaseTraitDefinition.this.getSourceType();
					}

					@Override
					public String getTargetType() {
						return RelationshipBaseTraitDefinition.this.getTargetType();
					}

					@Override
					public int getCascadingDeleteFlag() {
						return RelationshipBaseTraitDefinition.this.getCascadingDeleteFlag();
					}

					@Override
					public int getAutocreationFlag() {
						return RelationshipBaseTraitDefinition.this.getAutocreationFlag();
					}

					@Override
					public PropagationDirection getPropagationDirection() {
						return RelationshipBaseTraitDefinition.this.getPropagationDirection();
					}

					@Override
					public PropagationMode getReadPropagation() {
						return RelationshipBaseTraitDefinition.this.getReadPropagation();
					}

					@Override
					public PropagationMode getWritePropagation() {
						return RelationshipBaseTraitDefinition.this.getWritePropagation();
					}

					@Override
					public PropagationMode getDeletePropagation() {
						return RelationshipBaseTraitDefinition.this.getDeletePropagation();
					}

					@Override
					public PropagationMode getAccessControlPropagation() {
						return RelationshipBaseTraitDefinition.this.getAccessControlPropagation();
					}

					@Override
					public String getDeltaProperties() {
						return RelationshipBaseTraitDefinition.this.getDeltaProperties();
					}

					@Override
					public boolean isInternal() {
						return RelationshipBaseTraitDefinition.this.isInternal();
					}
				};
			}

			case 2 -> {

				return new OneToMany(sourceId, targetId) {

					@Override
					public String name() {
						return RelationshipBaseTraitDefinition.this.getRelationshipType();
					}

					@Override
					public String getType() {
						return getName();
					}

					@Override
					public String getSourceType() {
						return RelationshipBaseTraitDefinition.this.getSourceType();
					}

					@Override
					public String getTargetType() {
						return RelationshipBaseTraitDefinition.this.getTargetType();
					}

					@Override
					public int getCascadingDeleteFlag() {
						return RelationshipBaseTraitDefinition.this.getCascadingDeleteFlag();
					}

					@Override
					public int getAutocreationFlag() {
						return RelationshipBaseTraitDefinition.this.getAutocreationFlag();
					}

					@Override
					public PropagationDirection getPropagationDirection() {
						return RelationshipBaseTraitDefinition.this.getPropagationDirection();
					}

					@Override
					public PropagationMode getReadPropagation() {
						return RelationshipBaseTraitDefinition.this.getReadPropagation();
					}

					@Override
					public PropagationMode getWritePropagation() {
						return RelationshipBaseTraitDefinition.this.getWritePropagation();
					}

					@Override
					public PropagationMode getDeletePropagation() {
						return RelationshipBaseTraitDefinition.this.getDeletePropagation();
					}

					@Override
					public PropagationMode getAccessControlPropagation() {
						return RelationshipBaseTraitDefinition.this.getAccessControlPropagation();
					}

					@Override
					public String getDeltaProperties() {
						return RelationshipBaseTraitDefinition.this.getDeltaProperties();
					}

					@Override
					public boolean isInternal() {
						return RelationshipBaseTraitDefinition.this.isInternal();
					}
				};
			}

			case 3 -> {

				return new ManyToMany(sourceId, targetId) {

					@Override
					public String name() {
						return RelationshipBaseTraitDefinition.this.getRelationshipType();
					}

					@Override
					public String getType() {
						return getName();
					}

					@Override
					public String getSourceType() {
						return RelationshipBaseTraitDefinition.this.getSourceType();
					}

					@Override
					public String getTargetType() {
						return RelationshipBaseTraitDefinition.this.getTargetType();
					}

					@Override
					public int getCascadingDeleteFlag() {
						return RelationshipBaseTraitDefinition.this.getCascadingDeleteFlag();
					}

					@Override
					public int getAutocreationFlag() {
						return RelationshipBaseTraitDefinition.this.getAutocreationFlag();
					}

					@Override
					public PropagationDirection getPropagationDirection() {
						return RelationshipBaseTraitDefinition.this.getPropagationDirection();
					}

					@Override
					public PropagationMode getReadPropagation() {
						return RelationshipBaseTraitDefinition.this.getReadPropagation();
					}

					@Override
					public PropagationMode getWritePropagation() {
						return RelationshipBaseTraitDefinition.this.getWritePropagation();
					}

					@Override
					public PropagationMode getDeletePropagation() {
						return RelationshipBaseTraitDefinition.this.getDeletePropagation();
					}

					@Override
					public PropagationMode getAccessControlPropagation() {
						return RelationshipBaseTraitDefinition.this.getAccessControlPropagation();
					}

					@Override
					public String getDeltaProperties() {
						return RelationshipBaseTraitDefinition.this.getDeltaProperties();
					}

					@Override
					public boolean isInternal() {
						return RelationshipBaseTraitDefinition.this.isInternal();
					}
				};
			}
		}

		return null;
	}

	default PropagationDirection getPropagationDirection() {
		return PropagationDirection.None;
	}

	default PropagationMode getReadPropagation() {
		return null;
	}

	default PropagationMode getWritePropagation() {
		return null;
	}

	default PropagationMode getDeletePropagation() {
		return null;
	}

	default PropagationMode getAccessControlPropagation() {
		return null;
	}

	default String getDeltaProperties() {
		return null;
	}
}
