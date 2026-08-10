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
 * BpmnElement -[HAS_BPMN_METHOD]-> SchemaMethod.
 *
 * <p>Per-element method namespace -- the primary home for code bound to a
 * specific (process, step, event) triple. A BpmnTaskListener carries
 * {@code (element-link, event, method-name)}; the engine looks up the named
 * method on the element FIRST (matches the triple directly), then falls back
 * to the BpmnDefinitions process-scope tier (cross-step utilities), then to
 * TaskInstance schema methods, then to the convention fallback.</p>
 *
 * <p>HAS_METHOD means ownership, so cascading delete is ON: deleting an element
 * (or the process above it) deletes the handlers attached to it, exactly like
 * deleting a type deletes its methods (SchemaNodeMethodDefinition). The earlier
 * rationale for leaving it off -- surviving an element rebuild -- no longer
 * applies: a re-import builds a NEW version and clones the previous version's
 * bodies into it (BpmnImporter#cloneElementMethods) rather than rebuilding the
 * same element, so nothing is collateral damage. With the flag off, every
 * deleted process left its listener handlers behind as free-floating
 * user-defined functions. A method that should outlive its element must be
 * referenced, not owned -- that is what the listener CALLS relationship
 * (TARGET_TO_SOURCE) is for.</p>
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

	/**
	 * Deliberately NOT "HAS_METHOD" -- see the same method on {@code BpmnProcessHasMethod} for the
	 * full reasoning: sharing that raw type with AbstractSchemaNode -> SchemaMethod ownership made
	 * BPMN handlers invisible to {@code schemaNode == null} queries on Neo4j, which silently cost
	 * deployment exports, uniqueness validation and user-function resolution.
	 */
	@Override
	public String getRelationshipType() {

		return "HAS_BPMN_METHOD";
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
