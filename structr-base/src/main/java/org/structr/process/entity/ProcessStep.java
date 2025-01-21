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
package org.structr.process.entity;

import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.*;
import org.structr.core.script.Scripting;
import org.structr.core.script.Snippet;
import org.structr.process.entity.relationship.*;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.event.ActionMapping;

import java.util.Map;

public class ProcessStep extends AbstractNode {

    public static final Property<ProcessDecision>              decision = new EndNode<>("decision", ProcessStepLEADS_TOProcessDecision.class).partOfBuiltInSchema();
    public static final Property<Iterable<ProcessParameter>> parameters = new EndNodes<>("parameters", ProcessStepSTEP_PARAMETERProcessParameter.class).partOfBuiltInSchema();
    public static final Property<Iterable<ProcessState>> previousStates = new StartNodes<>("previousStates", ProcessStateNEXT_STEPProcessStep.class).partOfBuiltInSchema();
    public static final Property<Process>                       process = new StartNode<>("process", ProcessHAS_STEPProcessStep.class).partOfBuiltInSchema();
    public static final Property<Iterable<ActionMapping>> actionMappings = new StartNodes<>("actionMappings", ActionMappingTRIGGERSProcessStep.class).partOfBuiltInSchema();

    public static final View defaultView = new View(DOMElement.class, PropertyView.Public,
            id, name, process, decision, parameters, previousStates, actionMappings
    );

    @Export
    public boolean trigger(final SecurityContext securityContext, final Map<String, Object> methodParameters) throws FrameworkException {

        final App app = StructrApp.getInstance(securityContext);

        final ProcessInstance processInstance = app.get(ProcessInstance.class, (String) methodParameters.get("processInstanceId"));

        // Create (or) get and populate process instance parameters with values from this step
        for (final ProcessParameter processParameter : this.getProperty(parameters)) {

            final String parameterName = processParameter.getName();
            if (methodParameters.containsKey(parameterName)) {

                ProcessParameterValue valueObj = (ProcessParameterValue) app.nodeQuery(ProcessParameterValue.class).disableSorting().pageSize(1).andName(parameterName).getFirst();

                if (valueObj == null) {


                    final PropertyMap properties = new PropertyMap();

                    properties.put(ProcessParameterValue.name, parameterName);
                    properties.put(ProcessParameterValue.processInstance, processInstance);
                    properties.put(ProcessParameterValue.parameter, processParameter);
                    properties.put(ProcessParameterValue.value, (String) methodParameters.get(parameterName));

                    valueObj = app.create(ProcessParameterValue.class, properties);

                } else {

                    valueObj.setProperty(ProcessParameterValue.value, (String) methodParameters.get(parameterName));
                }
            }
        }

        final ProcessDecision processDecision = this.getProperty(decision);

        final String condition = processDecision.getProperty(ProcessDecision.condition);

        final boolean decisionResult = "true".equals(Scripting.evaluateScript(new ActionContext(securityContext), GraphObjectMap.fromMap(methodParameters),"js", new Snippet("", condition)));

        processInstance.setProperty(ProcessInstance.state,
                decisionResult ?
                        Iterables.first(processDecision.getProperty(ProcessDecision.possibleStates)) :     // 1st possible state is success state
                        Iterables.nth(processDecision.getProperty(ProcessDecision.possibleStates),1) // 2nd possible state is failure state
        );

        return decisionResult;
    }
}
