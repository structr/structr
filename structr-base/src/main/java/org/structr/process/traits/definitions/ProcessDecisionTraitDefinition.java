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

package org.structr.process.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;

import java.util.Map;
import java.util.Set;

public class ProcessDecisionTraitDefinition extends AbstractNodeTraitDefinition {

    public static final String CONDITION_PROPERTY       = "condition";
    public static final String POSSIBLE_STATES_PROPERTY = "possibleStates";
    public static final String STEP_PROPERTY            = "step";

    public ProcessDecisionTraitDefinition() { super(StructrTraits.PROCESS_DECISION); }
/*
    public static final Property<String>                      condition = new StringProperty("condition").hint("Condition that has to evaluate to true/false to determine the next process state").partOfBuiltInSchema();
    public static final Property<Iterable<ProcessState>> possibleStates = new EndNodes<>("possibleStates", ProcessDecisionPOSSIBLE_STATEProcessState.class).partOfBuiltInSchema();
    public static final Property<ProcessStep>                      step = new StartNode<>("step", ProcessStepLEADS_TOProcessDecision.class).partOfBuiltInSchema();

 */

    @Override
    public Set<PropertyKey> getPropertyKeys() {

        final Property<String>                  conditionProperty      = new StringProperty(CONDITION_PROPERTY).indexed();
        final Property<Iterable<NodeInterface>> possibleStatesProperty = new EndNodes(POSSIBLE_STATES_PROPERTY, StructrTraits.PROCESS_DECISION_POSSIBLE_STATE_PROCESS_STATE);
        final Property<NodeInterface>           stepProperty           = new StartNode(STEP_PROPERTY, StructrTraits.PROCESS_STEP_LEADS_TO_PROCESS_DECISION);

        return newSet(
                conditionProperty,
                possibleStatesProperty,
                stepProperty
        );
    }

    @Override
    public Map<String, Set<String>> getViews() {

        return Map.of(
                PropertyView.Public,
                newSet(
                ),
                PropertyView.Ui,
                newSet(
                        CONDITION_PROPERTY,
                        POSSIBLE_STATES_PROPERTY,
                        STEP_PROPERTY
                )
        );
    }

    @Override
    public Relation getRelation() {
        return null;
    }

}
