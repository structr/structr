/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.process.traits.relationship;

import org.structr.core.entity.Relation;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;

public class DOMNodePROCESS_HIDE_ELEMENT_ON_FAILUREActionMapping extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

    public DOMNodePROCESS_HIDE_ELEMENT_ON_FAILUREActionMapping() { super(StructrTraits.DOM_NODE_PROCESS_HIDE_ELEMENT_ON_FAILURE_ACTION_MAPPING); }

    @Override
    public String getSourceType() {
        return StructrTraits.DOM_NODE;
    }

    @Override
    public String getTargetType() {
        return StructrTraits.ACTION_MAPPING;
    }

    @Override
    public String getRelationshipType() {
        return "PROCESS_HIDE_ELEMENT_ON_FAILURE";
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
        return Relation.NONE;
    }

    @Override
    public boolean isInternal() {
        return false;
    }
}

