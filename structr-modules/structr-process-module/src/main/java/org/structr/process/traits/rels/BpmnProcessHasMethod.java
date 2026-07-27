/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.process.traits.rels;

import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.core.entity.Relation;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;
import org.structr.process.ProcessTraits;

/**
 * BpmnProcess -[HAS_METHOD]-> SchemaMethod. Per-process method namespace,
 * replacing the old per-definitions namespace once the model split lands.
 *
 * <p>HAS_METHOD means ownership, so cascading delete is ON: deleting a process
 * deletes the methods attached to it, exactly like deleting a type deletes its
 * methods (SchemaNodeMethodDefinition). Without it, every deleted process left
 * its listener handlers behind as free-floating user-defined functions, and a
 * later import could silently adopt them. Re-import doesn't lose code: a new
 * version CLONES the previous version's methods (BpmnImporter#cloneProcessMethods)
 * and the old version keeps its own until it is deleted. A method that should
 * outlive the process must be referenced, not owned -- that is what the
 * listener CALLS relationship (TARGET_TO_SOURCE) is for.</p>
 */
public class BpmnProcessHasMethod extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public BpmnProcessHasMethod() {
		super(ProcessTraits.BPMN_PROCESS_HAS_METHOD);
	}

	@Override
	public String getSourceType() {
		return ProcessTraits.BPMN_PROCESS;
	}

	@Override
	public String getTargetType() {
		return StructrTraits.SCHEMA_METHOD;
	}

	@Override
	public String getRelationshipType() {
		return "HAS_METHOD";
	}

	@Override
	public Relation.Multiplicity getSourceMultiplicity() {
		return Relation.Multiplicity.One;
	}

	@Override
	public Relation.Multiplicity getTargetMultiplicity() {
		return Relation.Multiplicity.Many;
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.SOURCE_TO_TARGET;
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.ALWAYS;
	}

	@Override
	public boolean isInternal() {
		return false;
	}

	@Override
	public PropagationDirection getPropagationDirection() {
		return PropagationDirection.Both;
	}

	@Override
	public PropagationMode getReadPropagation() {
		return PropagationMode.Add;
	}

	@Override
	public PropagationMode getWritePropagation() {
		return PropagationMode.Keep;
	}

	@Override
	public PropagationMode getDeletePropagation() {
		return PropagationMode.Keep;
	}

	@Override
	public PropagationMode getAccessControlPropagation() {
		return PropagationMode.Keep;
	}

	@Override
	public String getDeltaProperties() {
		return null;
	}
}
