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
package org.structr.web.traits.relationships;

import org.structr.api.graph.PropagationDirection;
import org.structr.api.graph.PropagationMode;
import org.structr.core.entity.Relation;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;

/**
 * DOMNode -[HAS]-> VisibilityMapping.
 *
 * <p>Connects a DOMNode (typically a partial container like a div, template, or
 * shared component) to one or more VisibilityMappings that gate whether the
 * node is rendered. Multiple mappings on the same node are OR-combined by the
 * renderer; the node is emitted when any mapping evaluates to true.</p>
 *
 * <p>The VisibilityMapping node type itself is registered here as a stub (so this
 * relationship can resolve at structr-base load time). Feature modules
 * (e.g. the process engine) supply the concrete trait that implements
 * {@code evaluate()} and contributes mapping-specific properties.</p>
 *
 * <p>Cascading delete: removing the DOMNode also removes its mappings (mappings
 * have no meaning without the host node). Read access propagates so mappings
 * are visible to anyone who can read the containing DOMNode.</p>
 */
public class DOMNodeHASVisibilityMapping extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

	public DOMNodeHASVisibilityMapping() {

		super(StructrTraits.DOM_NODE_HAS_VISIBILITY_MAPPING);
	}

	@Override public String getSourceType() { return StructrTraits.DOM_NODE; }
	@Override public String getTargetType() { return StructrTraits.VISIBILITY_MAPPING; }
	@Override public String getRelationshipType() { return "HAS"; }
	@Override public Relation.Multiplicity getSourceMultiplicity() { return Relation.Multiplicity.One; }
	@Override public Relation.Multiplicity getTargetMultiplicity() { return Relation.Multiplicity.Many; }
	@Override public int getCascadingDeleteFlag() { return Relation.SOURCE_TO_TARGET; }
	@Override public int getAutocreationFlag() { return Relation.ALWAYS; }
	@Override public boolean isInternal() { return false; }
	@Override public PropagationDirection getPropagationDirection() { return PropagationDirection.Both; }
	@Override public PropagationMode getReadPropagation() { return PropagationMode.Add; }
	@Override public PropagationMode getWritePropagation() { return PropagationMode.Keep; }
	@Override public PropagationMode getDeletePropagation() { return PropagationMode.Keep; }
	@Override public PropagationMode getAccessControlPropagation() { return PropagationMode.Keep; }
	@Override public String getDeltaProperties() { return null; }
}
