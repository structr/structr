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

import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractRelationshipTraitDefinition;
import org.structr.core.traits.definitions.RelationshipBaseTraitDefinition;
import org.structr.process.entity.Process;
import org.structr.process.entity.ProcessStep;

public class ProcessHAS_STEPProcessStep extends AbstractRelationshipTraitDefinition implements RelationshipBaseTraitDefinition {

    public ProcessHAS_STEPProcessStep() { super(StructrTraits.PROCESS_HAS_STEP_PROCESS_STEP); }

    @Override
    public String getSourceType() {
        return StructrTraits.PROCESS;
    }

    @Override
    public String getTargetType() {
        return StructrTraits.PROCESS_STEP;
    }

    @Override
    public String getRelationshipType() {
        return "HAS_STEP";
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
        return Relation.NONE;
    }

    @Override
    public boolean isInternal() {
        return false;
    }
}
