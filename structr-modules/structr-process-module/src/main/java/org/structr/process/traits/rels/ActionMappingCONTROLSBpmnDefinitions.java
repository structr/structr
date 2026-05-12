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
package org.structr.process.traits.rels;

import org.structr.core.entity.Relation;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;
import org.structr.process.ProcessTraits;

/**
 * ActionMapping -[CONTROLS]-> BpmnDefinitions.
 *
 * <p>Set when an ActionMapping's {@code action} is "control-process". Identifies the
 * process definition the action operates on. Required for "start" operations
 * (the engine needs to know which definition to instantiate); for instance-level
 * operations (claim, complete, signal, etc.) the relationship is a static binding
 * for the App Graph and editor introspection but the runtime resolves the target
 * instance/task via {@code idExpression}.</p>
 */
public class ActionMappingCONTROLSBpmnDefinitions extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public ActionMappingCONTROLSBpmnDefinitions() {
		super(StructrTraits.ACTION_MAPPING_CONTROLS_BPMN_DEFINITIONS);
	}

	@Override
	public String getSourceType() {
		return StructrTraits.ACTION_MAPPING;
	}

	@Override
	public String getTargetType() {
		return ProcessTraits.BPMN_DEFINITIONS;
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
