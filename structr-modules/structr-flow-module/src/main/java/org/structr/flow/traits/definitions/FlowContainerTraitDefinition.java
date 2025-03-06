/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.core.traits.operations.nodeinterface.OnNodeDeletion;
import org.structr.flow.impl.FlowContainer;
import org.structr.schema.action.EvaluationHints;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class FlowContainerTraitDefinition extends AbstractNodeTraitDefinition {

	public FlowContainerTraitDefinition() {
		super("FlowContainer");
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return newSet(

			new JavaMethod("evaluate", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
					return entity.as(FlowContainer.class).evaluate(arguments.toMap());
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
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					final Traits traits = obj.getTraits();
					boolean valid = true;

					valid &= ValidationHelper.isValidStringNotBlank(obj, traits.key("name"), errorBuffer);
					valid &= ValidationHelper.isValidPropertyNotNull(obj, traits.key("effectiveName"), errorBuffer);
					valid &= ValidationHelper.isValidUniqueProperty(obj, traits.key("effectiveName"), errorBuffer);

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
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowContainer.class, (traits, node) -> new FlowContainer(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> flowPackage                  = new StartNode("flowPackage", "FlowContainerPackageFlow");
		final Property<Iterable<NodeInterface>> flowNodes          = new EndNodes("flowNodes", "FlowContainerBaseNode");
		final Property<Iterable<NodeInterface>> flowConfigurations = new StartNodes("flowConfigurations", "FlowContainerConfigurationFlow");
		final Property<NodeInterface> activeFlowConfiguration      = new StartNode("activeConfiguration", "FlowActiveContainerConfiguration");
		final Property<NodeInterface> startNode                    = new EndNode("startNode", "FlowContainerFlowNode").indexed();
		final Property<String> name                                = new StringProperty("name").notNull().indexed();
		final Property<Object> effectiveName                       = new FunctionProperty<>("effectiveName").indexed().unique().notNull().readFunction("if(empty(this.flowPackage), this.name, concat(this.flowPackage.effectiveName, \".\", this.name))").writeFunction("{\r\n\tlet self = Structr.get(\'this\');\r\n\tlet path = Structr.get(\'value\');\r\n\r\n\tfunction getOrCreatePackage(name, path) {\r\n\t\tlet effectiveName = Structr.empty(path) ? name : Structr.concat(path,\".\",name);\r\n\r\n\t\tlet package = Structr.first(Structr.find(\"FlowContainerPackage\", \"effectiveName\", effectiveName));\r\n\r\n\t\tif (Structr.empty(path)) {\r\n\t\t\t\r\n\t\t\tif (Structr.empty(package)) {\r\n\t\t\t\tpackage = Structr.create(\"FlowContainerPackage\", \"name\", name);\r\n\t\t\t}\r\n\t\t} else {\r\n\t\t\tlet parent = Structr.first(Structr.find(\"FlowContainerPackage\", \"effectiveName\", path));\r\n\r\n\t\t\tif (Structr.empty(package)) {\r\n\t\t\t\tpackage = Structr.create(\"FlowContainerPackage\", \"name\", name, \"parent\", parent);\r\n\t\t\t}\r\n\t\t}\r\n\r\n\t\treturn package;\r\n\t}\r\n\r\n\tif (!Structr.empty(path)) {\r\n\r\n\t\tif (path.length > 0) {\r\n\r\n\t\t\tlet flowName = null;\r\n\r\n\t\t\tif (path.indexOf(\".\") !== -1) {\r\n\r\n\t\t\t\tlet elements = path.split(\".\");\r\n\r\n\t\t\t\tif (elements.length > 1) {\r\n\r\n\t\t\t\t\tflowName = elements.pop();\r\n\t\t\t\t\tlet currentPath = \"\";\r\n\t\t\t\t\tlet parentPackage = null;\r\n\r\n\t\t\t\t\tfor (let el of elements) {\r\n\t\t\t\t\t\tlet package = getOrCreatePackage(el, currentPath);\r\n\t\t\t\t\t\tparentPackage = package;\r\n\t\t\t\t\t\tcurrentPath = package.effectiveName;\r\n\t\t\t\t\t}\r\n\r\n\t\t\t\t\tself.flowPackage = parentPackage;\r\n\t\t\t\t} else {\r\n\r\n\t\t\t\t\tflowName = elements[0];\r\n\t\t\t\t}\r\n\r\n\t\t\t\tself.name = flowName;\r\n\t\t\t} else {\r\n\r\n\t\t\t\tself.name = path;\r\n\t\t\t}\r\n\r\n\t\t}\r\n\r\n\t}\r\n\r\n}").typeHint("String");
		final Property<Boolean> scheduledForIndexing               = new BooleanProperty("scheduledForIndexing").defaultValue(false);
		final Property<Iterable<NodeInterface>> repeaterNodes      = new StartNodes("repeaterNodes", "DOMNodeFLOWFlowContainer");
		final Property<String> apiSpecification                    = new StringProperty("apiSpecification");

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
				"name", "flowNodes", "startNode", "effectiveName", "scheduledForIndexing", "repeaterNodes", "activeFlowConfiguration"
			),
			PropertyView.Ui,
			newSet(
				"name", "flowNodes", "startNode", "flowPackage", "effectiveName", "scheduledForIndexing", "repeaterNodes", "activeFlowConfiguration", "apiSpecification"
			),
			"effectiveNameView",
			newSet(
				"type", "id", "effectiveName"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
