/*
 * Copyright (C) 2010-2025 Structr GmbH
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

public class ProcessStateTraitDefinition extends AbstractNodeTraitDefinition {

    public static final String PARAMETERS_PROPERTY                    = "parameters";
    public static final String PROCESS_INSTANCES_PROPERTY             = "processInstances";
    public static final String NEXT_STEP_PROPERTY                     = "nextStep";
    public static final String PROCESS_THIS_STATE_IS_INITIAL_PROPERTY = "processThisStateIsInitial";
    public static final String DECISION_PROPERTY                      = "decision";
    public static final String STATUS_PROPERTY                        = "status";

    public ProcessStateTraitDefinition() { super(StructrTraits.PROCESS_STATE); }
/*
    public static final Property<Iterable<ProcessParameter>>      parameters = new EndNodes<>("parameters", ProcessStateSTATE_PARAMETERProcessParameter.class).partOfBuiltInSchema();
    public static final Property<Iterable<ProcessInstance>> processInstances = new StartNodes<>("processInstances", ProcessInstanceCURRENT_STATEProcessState.class).partOfBuiltInSchema();
    public static final Property<ProcessStep>                       nextStep = new EndNode<>("nextStep", ProcessStateNEXT_STEPProcessStep.class).partOfBuiltInSchema();
    public static final Property<Process>          processThisStateIsInitial = new StartNode<>("processThisStateIsInitial", ProcessINITIAL_STATEProcessState.class).partOfBuiltInSchema();
    public static final Property<ProcessDecision>                   decision = new StartNode<>("decision", ProcessDecisionPOSSIBLE_STATEProcessState.class).partOfBuiltInSchema();
    public static final Property<Integer>                             status = new IntProperty("status").partOfBuiltInSchema();

    public static final View defaultView = new View(DOMElement.class, PropertyView.Public,
            id, name, parameters, nextStep, processThisStateIsInitial, decision, status
    );
 */

    @Override
    public Set<PropertyKey> getPropertyKeys() {

        final Property<Iterable<NodeInterface>> parametersProperty                = new EndNodes(PARAMETERS_PROPERTY, StructrTraits.PROCESS_STATE_STATE_PARAMETER_PROCESS_PARAMETER);
        final Property<Iterable<NodeInterface>> processInstancesProperty          = new StartNodes(PROCESS_INSTANCES_PROPERTY, StructrTraits.PROCESS_INSTANCE_CURRENT_STATE_PROCESS_STATE);
        final Property<NodeInterface>           nextStepProperty                  = new EndNode(NEXT_STEP_PROPERTY, StructrTraits.PROCESS_STATE_NEXT_STEP_PROCESS_STEP);
        final Property<NodeInterface>           processThisStateIsInitialProperty = new StartNode(PROCESS_THIS_STATE_IS_INITIAL_PROPERTY, StructrTraits.PROCESS_INITIAL_STATE_PROCESS_STATE);
        final Property<NodeInterface>           decisionProperty                  = new StartNode(DECISION_PROPERTY, StructrTraits.PROCESS_DECISION_POSSIBLE_STATE_PROCESS_STATE);
        final Property<Integer>                 statusProperty                    = new IntProperty(STATUS_PROPERTY);

        return newSet(
                parametersProperty,
                processInstancesProperty,
                nextStepProperty,
                processThisStateIsInitialProperty,
                decisionProperty,
                statusProperty
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
                        PARAMETERS_PROPERTY,
                        PROCESS_INSTANCES_PROPERTY,
                        NEXT_STEP_PROPERTY,
                        PROCESS_THIS_STATE_IS_INITIAL_PROPERTY,
                        DECISION_PROPERTY,
                        STATUS_PROPERTY
                )

        );
    }

    @Override
    public Relation getRelation() {
        return null;
    }

}
