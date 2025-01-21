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

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.*;
import org.structr.process.entity.relationship.*;
import org.structr.web.entity.dom.DOMElement;

public class ProcessState extends AbstractNode {

    public static final Property<Iterable<ProcessParameter>>      parameters = new EndNodes<>("parameters", ProcessStateSTATE_PARAMETERProcessParameter.class).partOfBuiltInSchema();
    public static final Property<Iterable<ProcessInstance>> processInstances = new StartNodes<>("processInstances", ProcessInstanceCURRENT_STATEProcessState.class).partOfBuiltInSchema();
    public static final Property<ProcessStep>                       nextStep = new EndNode<>("nextStep", ProcessStateNEXT_STEPProcessStep.class).partOfBuiltInSchema();
    public static final Property<Process>          processThisStateIsInitial = new StartNode<>("processThisStateIsInitial", ProcessINITIAL_STATEProcessState.class).partOfBuiltInSchema();
    public static final Property<ProcessDecision>                   decision = new StartNode<>("decision", ProcessDecisionPOSSIBLE_STATEProcessState.class).partOfBuiltInSchema();
    public static final Property<Integer>                             status = new IntProperty("status").partOfBuiltInSchema();

    public static final View defaultView = new View(DOMElement.class, PropertyView.Public,
            id, name, parameters, nextStep, processThisStateIsInitial, decision, status
    );
}


