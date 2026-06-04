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
 * BpmnTaskListener -[CALLS]-> SchemaMethod.
 *
 * <p>A task listener points directly at the SchemaMethod it invokes, instead
 * of carrying a free-text method name resolved at dispatch time. The method is
 * also attached to the listener's BpmnElement (via HAS_METHOD) so it shows up
 * in the Code module under that element; this rel is what the engine follows
 * to dispatch.</p>
 *
 * <p>No cascading delete: removing a handler is done explicitly in the editor
 * (delete the method, then the listener), and a re-import rebuilds the binding
 * from the BPMN XML, so we don't want element rebuilds to collaterally delete
 * the method.</p>
 */
public class BpmnTaskListenerCallsMethod extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public BpmnTaskListenerCallsMethod() { super(ProcessTraits.BPMN_TASK_LISTENER_CALLS_METHOD); }

	@Override public String getSourceType() { return ProcessTraits.BPMN_TASK_LISTENER; }
	@Override public String getTargetType() { return StructrTraits.SCHEMA_METHOD; }
	@Override public String getRelationshipType() { return "CALLS"; }
	@Override public Relation.Multiplicity getSourceMultiplicity() { return Relation.Multiplicity.Many; }
	@Override public Relation.Multiplicity getTargetMultiplicity() { return Relation.Multiplicity.One; }
	@Override public int getCascadingDeleteFlag() { return Relation.NONE; }
	@Override public int getAutocreationFlag() { return Relation.NONE; }
	@Override public boolean isInternal() { return false; }
}
