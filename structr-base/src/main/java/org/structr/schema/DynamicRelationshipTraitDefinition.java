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
package org.structr.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.core.entity.Relation;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.core.traits.TraitDefinition;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;

public class DynamicRelationshipTraitDefinition extends AbstractDynamicTraitDefinition<SchemaRelationshipNode> implements RelationshipBaseTraitDefinition {

	private static final Logger logger = LoggerFactory.getLogger(DynamicRelationshipTraitDefinition.class);

	private Relation.Multiplicity sourceMultiplicity;
	private Relation.Multiplicity targetMultiplicity;
	private PropagationDirection propagationDirection;
	private PropagationMode readPropagation;
	private PropagationMode writePropagation;
	private PropagationMode deletePropagation;
	private PropagationMode accessControlPropagation;
	private String deltaProperties;
	private String relationshipType;
	private String sourceType;
	private String targetType;
	private int cascadingDeleteFlag = 0;
	private int autocreationFlag    = 0;

	public DynamicRelationshipTraitDefinition(final TraitsInstance traitsInstance, final SchemaRelationshipNode schemaNode) {

		super(traitsInstance, schemaNode);

		this.relationshipType    = schemaNode.getRelationshipType();
		this.sourceType          = schemaNode.getSchemaNodeSourceType();
		this.targetType          = schemaNode.getSchemaNodeTargetType();

		// The schema still builds without them, but the relationship properties of this definition then cannot
		// resolve a related node, and that only shows when one is set. Report it where the cause is visible.
		if (sourceType == null || targetType == null) {

			final String missing = sourceType == null ? (targetType == null ? "source and target type" : "source type") : "target type";

			logger.warn("Schema relationship {} ({}, relationship type {}) has no {}: neither a linked schema node nor sourceType/targetType names it. Its relationship properties cannot resolve related nodes.",
				schemaNode.getClassName(), schemaNode.getUuid(), relationshipType, missing
			);
		}

		initializeMultiplicity(schemaNode);
		initializePermissionPropagation(schemaNode);
		initializeFlags(schemaNode);
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

		return sourceMultiplicity;
	}

	@Override
	public Relation.Multiplicity getTargetMultiplicity() {

		return targetMultiplicity;
	}

	@Override
	public int getCascadingDeleteFlag() {

		return cascadingDeleteFlag;
	}

	@Override
	public int getAutocreationFlag() {

		return autocreationFlag;
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

		return propagationDirection;
	}

	public PropagationMode getReadPropagation() {

		return readPropagation;
	}

	public PropagationMode getWritePropagation() {

		return writePropagation;
	}

	public PropagationMode getDeletePropagation() {

		return deletePropagation;
	}

	public PropagationMode getAccessControlPropagation() {

		return accessControlPropagation;
	}

	public String getDeltaProperties() {

		return deltaProperties;
	}

	// ----- protected methods -----
	protected void initializeMultiplicity(final SchemaRelationshipNode schemaNode) {

		final String sourceMultiplicity = schemaNode.getSourceMultiplicity();
		if (sourceMultiplicity != null) {

			switch (sourceMultiplicity) {

				case "1" -> {
					this.sourceMultiplicity = Relation.Multiplicity.One;
				}
				case "*" -> {
					this.sourceMultiplicity = Relation.Multiplicity.Many;
				}
			}
		}

		final String targetMultiplicity = schemaNode.getTargetMultiplicity();
		if (targetMultiplicity != null) {

			switch (targetMultiplicity) {

				case "1" -> {
					this.targetMultiplicity = Relation.Multiplicity.One;
				}
				case "*" -> {
					this.targetMultiplicity = Relation.Multiplicity.Many;
				}
			}
		}
	}

	protected void initializePermissionPropagation(final SchemaRelationshipNode schemaNode) {

		this.propagationDirection     = schemaNode.getPermissionPropagation();
		this.readPropagation          = schemaNode.getReadPropagation();
		this.writePropagation         = schemaNode.getWritePropagation();
		this.deletePropagation        = schemaNode.getDeletePropagation();
		this.accessControlPropagation = schemaNode.getAccessControlPropagation();
		this.deltaProperties          =  schemaNode.getPropertyMask();
	}

	protected void initializeFlags(final SchemaRelationshipNode schemaNode) {

		final Long cascadingDeleteFlag = schemaNode.getCascadingDeleteFlag();
		if (cascadingDeleteFlag != null) {

			this.cascadingDeleteFlag = cascadingDeleteFlag.intValue();
		}

		final Long autocreationFlag = schemaNode.getAutocreationFlag();
		if (autocreationFlag != null) {

			this.autocreationFlag = autocreationFlag.intValue();
		}
	}
}
