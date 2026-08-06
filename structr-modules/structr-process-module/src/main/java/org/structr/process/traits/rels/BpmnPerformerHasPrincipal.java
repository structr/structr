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
 * BpmnPerformer -[HAS_PRINCIPAL]-> Principal.
 *
 * <p>Typed alternative to the BpmnPerformer's free-form {@code expression}
 * string (e.g. {@code user(alice), group(managers)}). When the
 * relationship set is non-empty, the engine resolves task assignment
 * straight from the linked {@code Principal} nodes (User / Group) and
 * skips expression evaluation. Both representations can coexist; the
 * relationship takes priority.</p>
 *
 * <p>Many-to-Many: a single performer can pin a list of principals (a
 * {@code potentialOwner}'s candidate set), and a principal may appear on
 * many performers across the diagram. Cascading delete is OFF: dropping
 * a User must not silently remove it from every performer it's listed on
 * (the editor / re-import flow re-resolves; we don't want graph drift to
 * happen behind the author's back).</p>
 */
public class BpmnPerformerHasPrincipal extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public BpmnPerformerHasPrincipal() {

		super(ProcessTraits.BPMN_PERFORMER_HAS_PRINCIPAL);
	}

	@Override
	public String getSourceType() {

		return ProcessTraits.BPMN_PERFORMER;
	}

	@Override
	public String getTargetType() {

		return StructrTraits.PRINCIPAL;
	}

	@Override
	public String getRelationshipType() {

		return "HAS_PRINCIPAL";
	}

	@Override
	public Relation.Multiplicity getSourceMultiplicity() {

		return Relation.Multiplicity.Many;
	}

	@Override
	public Relation.Multiplicity getTargetMultiplicity() {

		return Relation.Multiplicity.Many;
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
