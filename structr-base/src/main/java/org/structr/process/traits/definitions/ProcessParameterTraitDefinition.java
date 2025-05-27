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

public class ProcessParameterTraitDefinition extends AbstractNodeTraitDefinition {

    public static final String DISPLAY_NAME_PROPERTY   = "displayName";
    public static final String PARAMETER_TYPE_PROPERTY = "parameterType";
    public static final String REQUIRED_PROPERTY       = "required";
    public static final String STATE_PROPERTY          = "state";
    public static final String STEP_PROPERTY           = "step";

    public ProcessParameterTraitDefinition() { super(StructrTraits.PROCESS_PARAMETER); }
/*
    public static final Property<String>   displayName = new StringProperty("displayName").hint("Display name or label for this parameter").partOfBuiltInSchema();
    public static final Property<String> parameterType = new StringProperty("parameterType").hint("Name of the data type of this parameter, e.g. string, date, boolean etc.").partOfBuiltInSchema();
    public static final Property<Boolean>     required = new BooleanProperty("required").hint("If true, this parameter is mandatory").partOfBuiltInSchema();

    public static final Property<ProcessState>   state = new StartNode<>("state", ProcessStateSTATE_PARAMETERProcessParameter.class).partOfBuiltInSchema();
    public static final Property<ProcessStep>     step = new StartNode<>("step",  ProcessStepSTEP_PARAMETERProcessParameter.class).partOfBuiltInSchema();

 */

    @Override
    public Set<PropertyKey> getPropertyKeys() {

        final Property<String>        displayNameProperty   = new StringProperty(DISPLAY_NAME_PROPERTY).indexed();
        final Property<String>        parameterTypeProperty = new StringProperty(PARAMETER_TYPE_PROPERTY).indexed();
        final Property<Boolean>       requiredProperty      = new BooleanProperty(REQUIRED_PROPERTY).indexed();
        final Property<NodeInterface> stateProperty         = new StartNode(STATE_PROPERTY, StructrTraits.PROCESS_STATE_STATE_PARAMETER_PROCESS_PARAMETER);
        final Property<NodeInterface> stepProperty          = new StartNode(STEP_PROPERTY, StructrTraits.PROCESS_STEP_STEP_PARAMETER_PROCESS_PARAMETER);

        return newSet(
                displayNameProperty,
                parameterTypeProperty,
                requiredProperty,
                stateProperty,
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
                        DISPLAY_NAME_PROPERTY,
                        PARAMETER_TYPE_PROPERTY,
                        REQUIRED_PROPERTY,
                        STATE_PROPERTY,
                        STEP_PROPERTY
                )
        );
    }

    @Override
    public Relation getRelation() {
        return null;
    }

}
