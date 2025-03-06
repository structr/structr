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
import org.structr.flow.impl.FlowContainerPackage;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 */
public class FlowContainerPackageTraitDefinition extends AbstractNodeTraitDefinition {

	public FlowContainerPackageTraitDefinition() {
		super("FlowContainerPackage");
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

					final Set<String> props = modificationQueue.getModifiedProperties().stream().map(p -> p.jsonName()).collect(Collectors.toSet());
					final Traits traits = graphObject.getTraits();

					if (props.contains("scheduledForIndexing") || props.contains("name") || props.contains("packages") || props.contains("flows")) {
						graphObject.as(FlowContainerPackage.class).scheduleIndexingForRelatedEntities();
					}

					graphObject.setProperty(traits.key("scheduledForIndexing"), false);
				}
			},

			OnNodeDeletion.class,
			new OnNodeDeletion() {

				@Override
				public void onNodeDeletion(final NodeInterface nodeInterface, final SecurityContext securityContext) throws FrameworkException {
					nodeInterface.as(FlowContainerPackage.class).deleteChildren();
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowContainerPackage.class, (traits, node) -> new FlowContainerPackage(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> parent             = new StartNode("parent", "FlowContainerPackagePackage");
		final Property<Iterable<NodeInterface>> packages = new EndNodes("packages", "FlowContainerPackagePackage");
		final Property<Iterable<NodeInterface>> flows    = new EndNodes("flows", "FlowContainerPackageFlow");
		final Property<String> name                      = new StringProperty("name").notNull().indexed();
		final Property<Object> effectiveName             = new FunctionProperty<>("effectiveName").indexed().unique().notNull().readFunction("if(empty(this.parent), this.name, concat(this.parent.effectiveName, \".\", this.name))").writeFunction("{\r\n\tlet self = Structr.get(\'this\');\r\n\tlet path = Structr.get(\'value\');\r\n\r\n\tfunction getOrCreatePackage(name, path) {\r\n\t\tlet effectiveName = Structr.empty(path) ? name : Structr.concat(path,\".\",name);\r\n\r\n\t\tlet package = Structr.first(Structr.find(\"FlowContainerPackage\", \"effectiveName\", effectiveName));\r\n\r\n\t\tif (Structr.empty(path)) {\r\n\t\t\t\r\n\t\t\tif (Structr.empty(package)) {\r\n\t\t\t\tpackage = Structr.create(\"FlowContainerPackage\", \"name\", name);\r\n\t\t\t}\r\n\t\t} else {\r\n\t\t\tlet parent = Structr.first(Structr.find(\"FlowContainerPackage\", \"effectiveName\", path));\r\n\r\n\t\t\tif (Structr.empty(package)) {\r\n\t\t\t\tpackage = Structr.create(\"FlowContainerPackage\", \"name\", name, \"parent\", parent);\r\n\t\t\t}\r\n\t\t}\r\n\r\n\t\treturn package;\r\n\t}\r\n\r\n\tif (!Structr.empty(path)) {\r\n\r\n\t\tif (path.length > 0) {\r\n\r\n\t\t\tlet flowName = null;\r\n\r\n\t\t\tif (path.indexOf(\".\") !== -1) {\r\n\r\n\t\t\t\tlet elements = path.split(\".\");\r\n\r\n\t\t\t\tif (elements.length > 1) {\r\n\r\n\t\t\t\t\tflowName = elements.pop();\r\n\t\t\t\t\tlet currentPath = \"\";\r\n\t\t\t\t\tlet parentPackage = null;\r\n\r\n\t\t\t\t\tfor (let el of elements) {\r\n\t\t\t\t\t\tlet package = getOrCreatePackage(el, currentPath);\r\n\t\t\t\t\t\tparentPackage = package;\r\n\t\t\t\t\t\tcurrentPath = package.effectiveName;\r\n\t\t\t\t\t}\r\n\r\n\t\t\t\t\tself.flowPackage = parentPackage;\r\n\t\t\t\t} else {\r\n\r\n\t\t\t\t\tflowName = elements[0];\r\n\t\t\t\t}\r\n\r\n\t\t\t\tself.name = flowName;\r\n\t\t\t} else {\r\n\r\n\t\t\t\tself.name = path;\r\n\t\t\t}\r\n\r\n\t\t}\r\n\r\n\t}\r\n\r\n}").typeHint("String");
		final Property<Boolean> scheduledForIndexing     = new BooleanProperty("scheduledForIndexing").defaultValue(false);

		return newSet(
			parent,
			packages,
			flows,
			name,
			effectiveName,
			scheduledForIndexing
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				"name", "effectiveName", "packages", "flows", "scheduledForIndexing"
			),
			PropertyView.Ui,
			newSet(
				"name", "effectiveName", "packages", "flows", "parent", "scheduledForIndexing"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
