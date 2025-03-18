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

import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.script.Scripting;
import org.structr.core.script.Snippet;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.process.traits.operations.Trigger;
import org.structr.schema.action.ActionContext;

import java.util.Map;
import java.util.Set;

public class ProcessStepTraitDefinition extends AbstractNodeTraitDefinition {

    public static final String DECISION_PROPERTY            = "decision";
    public static final String PARAMETERS_PROPERTY          = "parameters";
    public static final String PREVIOUS_STATES_PROPERTY     = "previousStates";
    public static final String PROCESS_PROPERTY             = "process";
    public static final String ACTION_MAPPINGS_PROPERTY     = "actionMappings";

    public ProcessStepTraitDefinition() { super(StructrTraits.PROCESS_STEP); }
/*
    public static final Property<ProcessDecision>              decision = new EndNode<>("decision", ProcessStepLEADS_TOProcessDecision.class).partOfBuiltInSchema();
    public static final Property<Iterable<ProcessParameter>> parameters = new EndNodes<>("parameters", ProcessStepSTEP_PARAMETERProcessParameter.class).partOfBuiltInSchema();
    public static final Property<Iterable<ProcessState>> previousStates = new StartNodes<>("previousStates", ProcessStateNEXT_STEPProcessStep.class).partOfBuiltInSchema();
    public static final Property<Process>                       process = new StartNode<>("process", ProcessHAS_STEPProcessStep.class).partOfBuiltInSchema();
    public static final Property<Iterable<ActionMapping>> actionMappings = new StartNodes<>("actionMappings", ActionMappingTRIGGERSProcessStep.class).partOfBuiltInSchema();

    public static final View defaultView = new View(DOMElement.class, PropertyView.Public,
            id, name, process, decision, parameters, previousStates, actionMappings
    );

 */

    @Override
    public Set<PropertyKey> getPropertyKeys() {

        final Property<NodeInterface>           decisionProperty       = new EndNode(DECISION_PROPERTY, StructrTraits.PROCESS_STEP_LEADS_TO_PROCESS_DECISION);
        final Property<Iterable<NodeInterface>> parametersProperty     = new EndNodes(PARAMETERS_PROPERTY, StructrTraits.PROCESS_STEP_STEP_PARAMETER_PROCESS_PARAMETER);
        final Property<Iterable<NodeInterface>> previousStatesProperty = new StartNodes(PREVIOUS_STATES_PROPERTY, StructrTraits.PROCESS_STATE_NEXT_STEP_PROCESS_STEP);
        final Property<NodeInterface>           processProperty        = new StartNode(PROCESS_PROPERTY, StructrTraits.PROCESS_HAS_STEP_PROCESS_STEP);
        final Property<Iterable<NodeInterface>> actionMappingsProperty = new StartNodes(ACTION_MAPPINGS_PROPERTY, StructrTraits.ACTION_MAPPING_TRIGGERS_PROCESS_STEP);

        return newSet(
                decisionProperty,
                parametersProperty,
                previousStatesProperty,
                processProperty,
                actionMappingsProperty
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
                        DECISION_PROPERTY,
                        PARAMETERS_PROPERTY,
                        PREVIOUS_STATES_PROPERTY,
                        PROCESS_PROPERTY,
                        ACTION_MAPPINGS_PROPERTY
                )
        );
    }

    @Override
    public Map<Class, FrameworkMethod> getFrameworkMethods() {

        return Map.of(

            Trigger.class,
            new Trigger() {

                @Override
                public boolean trigger(final NodeInterface node, final SecurityContext securityContext, final Map<String, Object> methodParameters) throws FrameworkException {

                    final Traits processInstanceTraits       = Traits.of(StructrTraits.PROCESS_INSTANCE);
                    final Traits processStepTraits           = Traits.of(StructrTraits.PROCESS_STEP);
                    final Traits processParameterValueTraits = Traits.of(StructrTraits.PROCESS_PARAMETER_VALUE);
                    final Traits processDecisionTraits       = Traits.of(StructrTraits.PROCESS_DECISION);

                    final App app = StructrApp.getInstance(securityContext);

                    final NodeInterface processInstance = app.getNodeById(StructrTraits.PROCESS_INSTANCE, (String) methodParameters.get(PROCESS_PROPERTY));

                    // Create (or) get and populate process instance parameters with values from this step
                    Iterable<NodeInterface> processParameters = node.getProperty(processStepTraits.key(ProcessStepTraitDefinition.PARAMETERS_PROPERTY));

                    for (final NodeInterface processParameter : processParameters) {

                        final String parameterName = processParameter.getName();
                        if (methodParameters.containsKey(parameterName)) {

                            NodeInterface valueObj = app.nodeQuery(StructrTraits.PROCESS_PARAMETER_VALUE).disableSorting().pageSize(1).andName(parameterName).getFirst();

                            if (valueObj == null) {


                                final PropertyMap properties = new PropertyMap();

                                properties.put(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),         parameterName);
                                properties.put(processParameterValueTraits.key(ProcessParameterValueTraitDefinition.PROCESS_INSTANCE_PROPERTY), processInstance);
                                properties.put(processParameterValueTraits.key(ProcessParameterValueTraitDefinition.PARAMETER_PROPERTY),        processParameter);
                                properties.put(processParameterValueTraits.key(ProcessParameterValueTraitDefinition.VALUE_PROPERTY),            methodParameters.get(parameterName));

                                valueObj = app.create(StructrTraits.PROCESS_PARAMETER_VALUE, properties);

                            } else {

                                valueObj.setProperty(processParameterValueTraits.key(ProcessParameterValueTraitDefinition.VALUE_PROPERTY), (String) methodParameters.get(parameterName));
                            }
                        }
                    }

                    final NodeInterface processDecision = node.getProperty(processStepTraits.key(ProcessStepTraitDefinition.DECISION_PROPERTY));

                    final String condition = processDecision.getProperty(processDecisionTraits.key(ProcessDecisionTraitDefinition.CONDITION_PROPERTY));

                    final boolean decisionResult = "true".equals(Scripting.evaluateScript(new ActionContext(securityContext), GraphObjectMap.fromMap(methodParameters), "js", new Snippet("", condition)));

                    processInstance.setProperty(processInstanceTraits.key(ProcessInstanceTraitDefinition.STATE_PROPERTY),
                            decisionResult ?
                                    Iterables.first(processDecision.getProperty(processDecisionTraits.key(ProcessDecisionTraitDefinition.POSSIBLE_STATES_PROPERTY))) :     // 1st possible state is success state
                                    Iterables.nth(processDecision.getProperty(processDecisionTraits.key(ProcessDecisionTraitDefinition.POSSIBLE_STATES_PROPERTY)), 1) // 2nd possible state is failure state
                    );

                    return decisionResult;
                }

            }
        );
    }

    @Override
    public Relation getRelation() {
        return null;
    }

}
