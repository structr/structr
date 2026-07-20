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
 * BpmnProcess -[HAS_INSTANCE_PAGE]-> Page.
 *
 * <p>Explicit binding from a process to the page that renders one of its
 * instances. Replaces the previous slug-by-name convention
 * ({@code /<cleanString(processName)>/<instance-uuid>}): with a typed
 * relationship the link survives renames on either side, the engine looks
 * the page up directly without slug resolution, and a process can clearly
 * declare "no page bound" by leaving the relationship null.</p>
 *
 * <p>Multiplicity: Many BpmnProcess to One Page. One process binds to a
 * single instance page; multiple processes may share a generic instance
 * page (e.g. a pan-process "instance details" partial that introspects
 * its bound ProcessInstance at render time).</p>
 *
 * <p>Cascading delete is OFF on purpose: deleting the page must not take
 * the process with it, and vice versa. Both ends are authored
 * independently and the relationship is recoverable from the editor.</p>
 */
public class BpmnProcessHasInstancePage extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public BpmnProcessHasInstancePage() {
		super(ProcessTraits.BPMN_PROCESS_HAS_INSTANCE_PAGE);
	}

	@Override
	public String getSourceType() {
		return ProcessTraits.BPMN_PROCESS;
	}

	@Override
	public String getTargetType() {
		return StructrTraits.PAGE;
	}

	@Override
	public String getRelationshipType() {
		return "HAS_INSTANCE_PAGE";
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
