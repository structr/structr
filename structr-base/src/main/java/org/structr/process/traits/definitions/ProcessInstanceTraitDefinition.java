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

public class ProcessInstanceTraitDefinition extends AbstractNodeTraitDefinition {

    public static final String PROCESS_PROPERTY          = "process";
    public static final String STATE_PROPERTY            = "state";
    public static final String PARAMETER_VALUES_PROPERTY = "parameterValues";

    public ProcessInstanceTraitDefinition() { super(StructrTraits.PROCESS_INSTANCE); }
/*
    public static final Property<Process>                                 process = new EndNode<>("process",          ProcessInstanceINSTANCE_OFProcess.class).partOfBuiltInSchema();
    public static final Property<ProcessState>                              state = new EndNode<>("state",            ProcessInstanceCURRENT_STATEProcessState.class).partOfBuiltInSchema();
    public static final Property<Iterable<ProcessParameterValue>> parameterValues = new EndNodes<>("parameterValues", ProcessInstanceHAS_PARAMETERProcessParameterValue.class).partOfBuiltInSchema();

    public static final View defaultView = new View(DOMElement.class, PropertyView.Public,
            id, name, process, state, parameterValues
    );
 */

    @Override
    public Set<PropertyKey> getPropertyKeys() {

        final Property<NodeInterface> processProperty                   = new EndNode(PROCESS_PROPERTY, StructrTraits.PROCESS_INSTANCE_INSTANCE_OF_PROCESS);
        final Property<NodeInterface> processStateProperty              = new EndNode(STATE_PROPERTY, StructrTraits.PROCESS_INSTANCE_CURRENT_STATE_PROCESS_STATE);
        final Property<Iterable<NodeInterface>> parameterValuesProperty = new EndNodes(PARAMETER_VALUES_PROPERTY, StructrTraits.PROCESS_INSTANCE_HAS_PARAMETER_PROCESS_PARAMETER_VALUE);

        return newSet(
                processProperty,
                processStateProperty,
                parameterValuesProperty
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
                        PROCESS_PROPERTY,
                        STATE_PROPERTY,
                        PARAMETER_VALUES_PROPERTY
                )
        );
    }

    @Override
    public Relation getRelation() {
        return null;
    }

}
