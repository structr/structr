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

import org.structr.core.entity.Relation;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;
import org.structr.process.ProcessTraits;

/**
 * ActionMapping -[CONTROLS]-> BpmnProcess.
 *
 * <p>Set when an ActionMapping's {@code action} is "control-process". Identifies the
 * BpmnProcess the action operates on. Required for "start" operations (the engine
 * needs to know which process to instantiate); for task-level (claim, complete,
 * completeWithSubject, ...) and instance-level (terminate, suspend, resume)
 * operations the relationship is a static binding for editor introspection but
 * the runtime resolves the target instance/task via {@code idExpression} and
 * the {@code targetsElement} BpmnElement.</p>
 *
 * <p>Target type is BpmnProcess (not BpmnDefinitions) because, post the multi-process
 * refactor, a BpmnDefinitions is a file-level container that can hold N processes
 * (e.g. collaborations with one process per participant pool). The meaningful
 * binding for any action is one specific process.</p>
 */
public class ActionMappingCONTROLSBpmnProcess extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public ActionMappingCONTROLSBpmnProcess() {

		super(StructrTraits.ACTION_MAPPING_CONTROLS_BPMN_PROCESS);
	}

	@Override
	public String getSourceType() {

		return StructrTraits.ACTION_MAPPING;
	}

	@Override
	public String getTargetType() {

		return ProcessTraits.BPMN_PROCESS;
	}

	@Override
	public String getRelationshipType() {

		return "CONTROLS";
	}

	@Override
	public Relation.Multiplicity getSourceMultiplicity() {

		return Relation.Multiplicity.Many;
	}

	@Override
	public Relation.Multiplicity getTargetMultiplicity() {

		return Relation.Multiplicity.One;
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
	public boolean isInternal() {

		return false;
	}
}
