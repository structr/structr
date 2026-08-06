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
 * BpmnProcessListener -[CALLS]-> SchemaMethod.
 *
 * <p>Process-level sibling of {@link BpmnTaskListenerCallsMethod}: a process
 * listener points directly at the SchemaMethod it invokes, instead of a
 * free-text method name resolved at dispatch time. The method is also attached
 * to the listener's BpmnProcess (via HAS_METHOD) so it shows up in the Code
 * module under that process; this rel is what the engine follows to dispatch.</p>
 *
 * <p>Cascading delete is TARGET_TO_SOURCE only, same rule as the task-listener
 * sibling: deleting the method also deletes the listener (a listener without a
 * method is dead weight), while removing a listener deletes its method only via
 * the editor's conditional path (empty body). Re-imports never delete methods.</p>
 */
public class BpmnProcessListenerCallsMethod extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public BpmnProcessListenerCallsMethod() {

		super(ProcessTraits.BPMN_PROCESS_LISTENER_CALLS_METHOD);
	}

	@Override
	public String getSourceType() {

		return ProcessTraits.BPMN_PROCESS_LISTENER;
	}

	@Override
	public String getTargetType() {

		return StructrTraits.SCHEMA_METHOD;
	}

	@Override
	public String getRelationshipType() {

		return "CALLS";
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

		return Relation.TARGET_TO_SOURCE;
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
