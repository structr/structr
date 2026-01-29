/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.flow.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.operations.nodeinterface.OnNodeDeletion;
import org.structr.flow.impl.FlowBaseNode;
import org.structr.flow.impl.FlowContainer;
import org.structr.flow.traits.operations.GetExportData;
import org.structr.schema.action.EvaluationHints;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FlowContainerTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String FLOW_PACKAGE_PROPERTY           = "flowPackage";
	public static final String FLOW_NODES_PROPERTY             = "flowNodes";
	public static final String FLOW_CONFIGURATIONS_PROPERTY    = "flowConfigurations";
	public static final String ACTIVE_CONFIGURATION_PROPERTY   = "activeConfiguration";
	public static final String START_NODE_PROPERTY             = "startNode";
	public static final String NAME_PROPERTY                   = "name";
	public static final String EFFECTIVE_NAME_PROPERTY         = "effectiveName";
	public static final String SCHEDULED_FOR_INDEXING_PROPERTY = "scheduledForIndexing";
	public static final String REPEATER_NODES_PROPERTY         = "repeaterNodes";
	public static final String API_SPECIFICATION_PROPERTY      = "apiSpecification";

	public FlowContainerTraitDefinition() {
		super(StructrTraits.FLOW_CONTAINER);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowContainer.class, (traits, node) -> new FlowContainer(traits, node)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return newSet(

			new JavaMethod("evaluate", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
					return entity.as(FlowContainer.class).evaluate(securityContext, arguments.toMap());
				}
			},

			new JavaMethod("getFlowNodes", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
					return entity.as(FlowContainer.class).getFlowNodes(securityContext);
				}
			},

			new JavaMethod("getFlowRelationships", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
					return entity.as(FlowContainer.class).getFlowRelationships(securityContext);
				}
			}
		);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					final Traits traits = obj.getTraits();
					boolean valid = true;

					valid &= ValidationHelper.isValidStringNotBlank(obj, traits.key(NAME_PROPERTY), errorBuffer);
					valid &= ValidationHelper.isValidPropertyNotNull(obj, traits.key(EFFECTIVE_NAME_PROPERTY), errorBuffer);
					valid &= ValidationHelper.isValidUniqueProperty(obj, traits.key(EFFECTIVE_NAME_PROPERTY), errorBuffer);

					return valid;
				}
			},

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
					graphObject.setVisibility(true, true);
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
					graphObject.as(FlowContainer.class).setScheduledForIndexing(false);
				}
			},

			OnNodeDeletion.class,
			new OnNodeDeletion() {

				@Override
				public void onNodeDeletion(final NodeInterface nodeInterface, final SecurityContext securityContext) throws FrameworkException {
					nodeInterface.as(FlowContainer.class).deleteChildren();
				}
			}
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

				GetExportData.class,
				new GetExportData() {

					@Override
					public Map<String, Object> getExportData(final FlowBaseNode flowBaseNode) {

						final Map<String, Object> result = new TreeMap<>();

						result.put(GraphObjectTraitDefinition.ID_PROPERTY,                             flowBaseNode.getUuid());
						result.put(GraphObjectTraitDefinition.TYPE_PROPERTY,                           flowBaseNode.getType());
						result.put(NodeInterfaceTraitDefinition.NAME_PROPERTY,                         flowBaseNode.getName());
						result.put(GraphObjectTraitDefinition.VISIBLE_TO_PUBLIC_USERS_PROPERTY,        flowBaseNode.isVisibleToPublicUsers());
						result.put(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY, flowBaseNode.isVisibleToAuthenticatedUsers());

						return result;
					}
				}
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface> flowPackage                  = new StartNode(traitsInstance, FLOW_PACKAGE_PROPERTY, StructrTraits.FLOW_CONTAINER_PACKAGE_FLOW);
		final Property<Iterable<NodeInterface>> flowNodes          = new EndNodes(traitsInstance, FLOW_NODES_PROPERTY, StructrTraits.FLOW_CONTAINER_BASE_NODE);
		final Property<Iterable<NodeInterface>> flowConfigurations = new StartNodes(traitsInstance, FLOW_CONFIGURATIONS_PROPERTY, StructrTraits.FLOW_CONTAINER_CONFIGURATION_FLOW);
		final Property<NodeInterface> activeFlowConfiguration      = new StartNode(traitsInstance, ACTIVE_CONFIGURATION_PROPERTY, StructrTraits.FLOW_ACTIVE_CONTAINER_CONFIGURATION);
		final Property<NodeInterface> startNode                    = new EndNode(traitsInstance, START_NODE_PROPERTY, StructrTraits.FLOW_CONTAINER_FLOW_NODE).indexed();
		final Property<String> name                                = new StringProperty(NAME_PROPERTY).notNull().indexed();
		final Property<Object> effectiveName                       = new FunctionProperty<>(EFFECTIVE_NAME_PROPERTY).indexed().unique().notNull().readFunction("if(empty(this.flowPackage), this.name, concat(this.flowPackage.effectiveName, \".\", this.name))").writeFunction("{\r\n\tlet self = Structr.get(\'this\');\r\n\tlet path = Structr.get(\'value\');\r\n\r\n\tconst getOrCreatePackage = (name, path) => {\r\n\t\tlet effectiveName = Structr.empty(path) ? name : Structr.concat(path,\".\",name);\r\n\r\n\t\tlet flowPackage = Structr.first(Structr.find(\"FlowContainerPackage\", \"effectiveName\", effectiveName));\r\n\r\n\t\tif (Structr.empty(path)) {\r\n\t\t\t\r\n\t\t\tif (Structr.empty(flowPackage)) {\r\n\t\t\t\tflowPackage = Structr.create(\"FlowContainerPackage\", \"name\", name);\r\n\t\t\t}\r\n\t\t} else {\r\n\t\t\tlet parent = Structr.first(Structr.find(\"FlowContainerPackage\", \"effectiveName\", path));\r\n\r\n\t\t\tif (Structr.empty(flowPackage)) {\r\n\t\t\t\tflowPackage = Structr.create(\"FlowContainerPackage\", \"name\", name, \"parent\", parent);\r\n\t\t\t}\r\n\t\t}\r\n\r\n\t\treturn flowPackage;\r\n\t}\r\n\r\n\tif (!Structr.empty(path)) {\r\n\r\n\t\tif (path.length > 0) {\r\n\r\n\t\t\tlet flowName = null;\r\n\r\n\t\t\tif (path.indexOf(\".\") !== -1) {\r\n\r\n\t\t\t\tlet elements = path.split(\".\");\r\n\r\n\t\t\t\tif (elements.length > 1) {\r\n\r\n\t\t\t\t\tflowName = elements.pop();\r\n\t\t\t\t\tlet currentPath = \"\";\r\n\t\t\t\t\tlet parentPackage = null;\r\n\r\n\t\t\t\t\tfor (let el of elements) {\r\n\t\t\t\t\t\tlet flowPackage = getOrCreatePackage(el, currentPath);\r\n\t\t\t\t\t\tparentPackage = flowPackage;\r\n\t\t\t\t\t\tcurrentPath = flowPackage.effectiveName;\r\n\t\t\t\t\t}\r\n\r\n\t\t\t\t\tself.flowPackage = parentPackage;\r\n\t\t\t\t} else {\r\n\r\n\t\t\t\t\tflowName = elements[0];\r\n\t\t\t\t}\r\n\r\n\t\t\t\tself.name = flowName;\r\n\t\t\t} else {\r\n\r\n\t\t\t\tself.name = path;\r\n\t\t\t}\r\n\r\n\t\t}\r\n\r\n\t}\r\n\r\n}").typeHint("String").cachingEnabled(true);
		final Property<Boolean> scheduledForIndexing               = new BooleanProperty(SCHEDULED_FOR_INDEXING_PROPERTY).defaultValue(false);
		final Property<Iterable<NodeInterface>> repeaterNodes      = new StartNodes(traitsInstance, REPEATER_NODES_PROPERTY, StructrTraits.DOM_NODE_FLOW_FLOW_CONTAINER);
		final Property<String> apiSpecification                    = new StringProperty(API_SPECIFICATION_PROPERTY);

		return newSet(
			flowPackage,
			flowNodes,
			flowConfigurations,
			activeFlowConfiguration,
			startNode,
			name,
			effectiveName,
			scheduledForIndexing,
			repeaterNodes,
			apiSpecification
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					NAME_PROPERTY, FLOW_NODES_PROPERTY, START_NODE_PROPERTY, EFFECTIVE_NAME_PROPERTY, SCHEDULED_FOR_INDEXING_PROPERTY, REPEATER_NODES_PROPERTY, ACTIVE_CONFIGURATION_PROPERTY
			),

			PropertyView.Ui,
			newSet(
					NAME_PROPERTY, FLOW_NODES_PROPERTY, START_NODE_PROPERTY, FLOW_PACKAGE_PROPERTY, EFFECTIVE_NAME_PROPERTY, SCHEDULED_FOR_INDEXING_PROPERTY, REPEATER_NODES_PROPERTY, ACTIVE_CONFIGURATION_PROPERTY, API_SPECIFICATION_PROPERTY
			),

			"effectiveNameView",
			newSet(
					GraphObjectTraitDefinition.TYPE_PROPERTY, GraphObjectTraitDefinition.ID_PROPERTY, EFFECTIVE_NAME_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
