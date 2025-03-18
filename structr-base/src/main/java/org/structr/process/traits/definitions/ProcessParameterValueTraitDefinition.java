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

import org.structr.api.graph.Node;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;

import java.util.Set;

public class ProcessParameterValueTraitDefinition extends AbstractNodeTraitDefinition {

    public static final String VALUE_PROPERTY            = "value";
    public static final String PARAMETER_PROPERTY        = "parameter";
    public static final String PROCESS_INSTANCE_PROPERTY = "processInstance";

    public ProcessParameterValueTraitDefinition() { super(StructrTraits.PROCESS_PARAMETER_VALUE); }
/*

    public static final Property<String>                    value = new StringProperty("value").hint("String representation of this parameter value").partOfBuiltInSchema();
    public static final Property<ProcessParameter>      parameter = new EndNode<>("parameter", ProcessParameterValueVALUE_OFProcessParameter.class).partOfBuiltInSchema();
    public static final Property<ProcessInstance> processInstance = new StartNode<>("processInstance", ProcessInstanceHAS_PARAMETERProcessParameterValue.class).partOfBuiltInSchema();

 */

    @Override
    public Set<PropertyKey> getPropertyKeys() {

        final Property<String>        valueProperty           = new StringProperty(VALUE_PROPERTY);
        final Property<NodeInterface> parameterProperty       = new EndNode(PARAMETER_PROPERTY, StructrTraits.PROCESS_PARAMETER_VALUE_VALUE_OF_PROCESS_PARAMETER);
        final Property<NodeInterface> processInstanceProperty = new StartNode(PROCESS_INSTANCE_PROPERTY, StructrTraits.PROCESS_INSTANCE_HAS_PARAMETER_PROCESS_PARAMETER_VALUE);

        return newSet(
                valueProperty,
                parameterProperty,
                processInstanceProperty
        );
    }

    @Override
    public Relation getRelation() {
        return null;
    }
    
}
