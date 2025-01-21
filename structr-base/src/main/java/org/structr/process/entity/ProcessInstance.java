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
import org.structr.core.property.EndNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.process.entity.relationship.ProcessInstanceCURRENT_STATEProcessState;
import org.structr.process.entity.relationship.ProcessInstanceHAS_PARAMETERProcessParameterValue;
import org.structr.process.entity.relationship.ProcessInstanceINSTANCE_OFProcess;
import org.structr.web.entity.dom.DOMElement;

public class ProcessInstance extends AbstractNode {

    public static final Property<Process>                                 process = new EndNode<>("process",          ProcessInstanceINSTANCE_OFProcess.class).partOfBuiltInSchema();
    public static final Property<ProcessState>                              state = new EndNode<>("state",            ProcessInstanceCURRENT_STATEProcessState.class).partOfBuiltInSchema();
    public static final Property<Iterable<ProcessParameterValue>> parameterValues = new EndNodes<>("parameterValues", ProcessInstanceHAS_PARAMETERProcessParameterValue.class).partOfBuiltInSchema();

    public static final View defaultView = new View(DOMElement.class, PropertyView.Public,
            id, name, process, state, parameterValues
    );


}
