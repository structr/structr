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
package org.structr.schema;

import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.traits.TraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;

public class DynamicRelationshipTraitDefinition extends AbstractDynamicTraitDefinition<SchemaRelationshipNode> implements RelationshipBaseTraitDefinition {

	final String relationshipType;
	final String sourceType;
	final String targetType;

	public DynamicRelationshipTraitDefinition(final SchemaRelationshipNode schemaNode) {

		super(schemaNode);

		this.relationshipType = schemaNode.getRelationshipType();
		this.sourceType       = schemaNode.getSchemaNodeSourceType();
		this.targetType       = schemaNode.getSchemaNodeTargetType();
	}

	@Override
	public String getSourceType() {
		return sourceType;
	}

	@Override
	public String getTargetType() {
		return targetType;
	}

	@Override
	public String getRelationshipType() {
		return relationshipType;
	}

	@Override
	public Relation.Multiplicity getSourceMultiplicity() {

		final String multiplicity = schemaNode.getSourceMultiplicity();
		if (multiplicity != null) {

			switch (multiplicity) {

				case "1" -> {
					return Relation.Multiplicity.One;
				}
				case "*" -> {
					return Relation.Multiplicity.Many;
				}
			}
		}

		return null;
	}

	@Override
	public Relation.Multiplicity getTargetMultiplicity() {

		final String multiplicity = schemaNode.getTargetMultiplicity();
		if (multiplicity != null) {

			switch (multiplicity) {

				case "1" -> {
					return Relation.Multiplicity.One;
				}
				case "*" -> {
					return Relation.Multiplicity.Many;
				}
			}
		}

		return null;
	}

	@Override
	public int getCascadingDeleteFlag() {

		final Long flag = schemaNode.getCascadingDeleteFlag();
		if (flag != null) {

			return flag.intValue();
		}

		return 0;
	}

	@Override
	public int getAutocreationFlag() {

		final Long flag = schemaNode.getAutocreationFlag();
		if (flag != null) {

			return flag.intValue();
		}

		return 0;
	}

	@Override
	public boolean isInternal() {
		return false;
	}

	@Override
	public boolean isRelationship() {
		return true;
	}

	@Override
	public int compareTo(final TraitDefinition o) {
		return getName().compareTo(o.getName());
	}

	public PropagationDirection getPropagationDirection() {
		return schemaNode.getPermissionPropagation();
	}

	public PropagationMode getReadPropagation() {
		return schemaNode.getReadPropagation();
	}

	public PropagationMode getWritePropagation() {
		return schemaNode.getWritePropagation();
	}

	public PropagationMode getDeletePropagation() {
		return schemaNode.getDeletePropagation();
	}

	public PropagationMode getAccessControlPropagation() {
		return schemaNode.getAccessControlPropagation();
	}

	public String getDeltaProperties() {
		return schemaNode.getPropertyMask();
	}
}
