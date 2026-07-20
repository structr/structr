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
 * BpmnElement -[HAS_METHOD]-> SchemaMethod.
 *
 * <p>Per-element method namespace -- the primary home for code bound to a
 * specific (process, step, event) triple. A BpmnTaskListener carries
 * {@code (element-link, event, method-name)}; the engine looks up the named
 * method on the element FIRST (matches the triple directly), then falls back
 * to the BpmnDefinitions process-scope tier (cross-step utilities), then to
 * TaskInstance schema methods, then to the convention fallback.</p>
 *
 * <p>Cascading delete is intentionally NOT enabled: methods can be re-attached
 * across re-imports without being collateral-damage of element rebuild.</p>
 */
public class BpmnElementHasMethod extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public BpmnElementHasMethod() {
		super(ProcessTraits.BPMN_ELEMENT_HAS_METHOD);
	}

	@Override
	public String getSourceType() {
		return ProcessTraits.BPMN_ELEMENT;
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
