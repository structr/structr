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

import org.apache.jena.atlas.iterator.Iter;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.entity.Relation;
import org.structr.core.property.*;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.propertycontainer.SetProperties;
import org.structr.process.entity.Process;
import org.structr.process.entity.ProcessInstance;
import org.structr.process.entity.ProcessState;
import org.structr.process.traits.operations.CreateInstance;
import org.structr.web.entity.dom.Page;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class ProcessTraitDefinition extends AbstractNodeTraitDefinition {

    public static final String INITIAL_STATE_PROPERTY     = "initialState";
    public static final String PROCESS_INSTANCES_PROPERTY = "processInstances";
    public static final String STEPS_PROPERTY             = "steps";

    public ProcessTraitDefinition() { super(StructrTraits.PROCESS); }

/*
    public static final Property<ProcessState>                  initialState = new EndNode<>("initialState", ProcessINITIAL_STATEProcessState.class).partOfBuiltInSchema();
    public static final Property<Iterable<ProcessInstance>> processInstances = new StartNodes<>("processInstances", ProcessInstanceINSTANCE_OFProcess.class).partOfBuiltInSchema();
    public static final Property<Iterable<ProcessStep>>                steps = new EndNodes<>("steps", ProcessHAS_STEPProcessStep.class).partOfBuiltInSchema();

    public static final View uiView = new View(Process.class, PropertyView.Ui,
            id, name, initialState, steps
    );

 */

    @Override
    public Set<PropertyKey> getPropertyKeys() {

        final Property<NodeInterface> initialStateProperty               = new EndNode(INITIAL_STATE_PROPERTY, StructrTraits.PROCESS_INITIAL_STATE_PROCESS_STATE);
        final Property<Iterable<NodeInterface>> processInstancesProperty = new StartNodes(PROCESS_INSTANCES_PROPERTY, StructrTraits.PROCESS_INSTANCE_INSTANCE_OF_PROCESS);
        final Property<Iterable<NodeInterface>> stepsProperty            = new EndNodes(STEPS_PROPERTY, StructrTraits.PROCESS_HAS_STEP_PROCESS_STEP);

        return newSet(
                initialStateProperty,
                processInstancesProperty
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
                        INITIAL_STATE_PROPERTY,
                        PROCESS_INSTANCES_PROPERTY,
                        STEPS_PROPERTY
                )
        );
    }

    @Override
    public Map<Class, FrameworkMethod> getFrameworkMethods() {

        return Map.of(

            CreateInstance.class,
            new CreateInstance() {

                @Override
                public NodeInterface createInstance(final NodeInterface node, final SecurityContext securityContext) throws FrameworkException {

                    final Traits traits = Traits.of(StructrTraits.PROCESS_INSTANCE);
                    final PropertyKey<String> nameKey                = traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
                    final PropertyKey<String> typeKey                = traits.key(GraphObjectTraitDefinition.TYPE_PROPERTY);
                    final PropertyKey<NodeInterface> initialStateKey = traits.key(ProcessTraitDefinition.INITIAL_STATE_PROPERTY);
                    final PropertyKey<NodeInterface> processKey      = traits.key(ProcessInstanceTraitDefinition.PROCESS_PROPERTY);

                    // Processes are created by the current user which is also the initial owner
                    final App app = StructrApp.getInstance(securityContext);

                    // The initial name can and should be changed
                    final String initialProcessInstanceName = node.getName() + "-" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date());

                    final PropertyMap properties = new PropertyMap();
                    properties.put(nameKey, initialProcessInstanceName);
                    properties.put(processKey, node);
                    properties.put(initialStateKey, node.getProperty(traits.key(ProcessTraitDefinition.INITIAL_STATE_PROPERTY)));

                    return app.create(StructrTraits.PROCESS_INSTANCE, properties);
                }
            }

        );
    }

    @Override
    public Relation getRelation() {
        return null;
    }
}
