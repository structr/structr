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
import org.structr.flow.impl.FlowContainerPackage;
import org.structr.flow.traits.operations.GetExportData;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class FlowContainerPackageTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String PARENT_PROPERTY                 = "parent";
	public static final String PACKAGES_PROPERTY               = "packages";
	public static final String FLOWS_PROPERTY                  = "flows";
	public static final String NAME_PROPERTY                   = "name";
	public static final String EFFECTIVE_NAME_PROPERTY         = "effectiveName";
	public static final String SCHEDULED_FOR_INDEXING_PROPERTY = "scheduledForIndexing";

	public FlowContainerPackageTraitDefinition() {
		super(StructrTraits.FLOW_CONTAINER_PACKAGE);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowContainerPackage.class, (traits, node) -> new FlowContainerPackage(traits, node)
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

					final Set<String> props = modificationQueue.getModifiedProperties().stream().map(p -> p.jsonName()).collect(Collectors.toSet());
					final Traits traits = graphObject.getTraits();

					if (props.contains(SCHEDULED_FOR_INDEXING_PROPERTY) || props.contains(NAME_PROPERTY) || props.contains(PACKAGES_PROPERTY) || props.contains(FLOWS_PROPERTY)) {
						graphObject.as(FlowContainerPackage.class).scheduleIndexingForRelatedEntities();
					}

					graphObject.setProperty(traits.key(SCHEDULED_FOR_INDEXING_PROPERTY), false);
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

		final Property<NodeInterface> parent             = new StartNode(traitsInstance, PARENT_PROPERTY, StructrTraits.FLOW_CONTAINER_PACKAGE_PACKAGE);
		final Property<Iterable<NodeInterface>> packages = new EndNodes(traitsInstance, PACKAGES_PROPERTY, StructrTraits.FLOW_CONTAINER_PACKAGE_PACKAGE);
		final Property<Iterable<NodeInterface>> flows    = new EndNodes(traitsInstance, FLOWS_PROPERTY, StructrTraits.FLOW_CONTAINER_PACKAGE_FLOW);
		final Property<String> name                      = new StringProperty(NAME_PROPERTY).notNull().indexed();
		final Property<Object> effectiveName             = new FunctionProperty<>(EFFECTIVE_NAME_PROPERTY).indexed().unique().notNull().readFunction("if(empty(this.parent), this.name, concat(this.parent.effectiveName, \".\", this.name))").writeFunction("""
				{
					let self = $.get('this');
					let path = $.get('value');

					const getOrCreatePackage = (name, path) => {

						let effectiveName = $.empty(path) ? name : $.concat(path,".",name);

						let flowContainerPackage = $.first($.find("FlowContainerPackage", "effectiveName", effectiveName));

						if ($.empty(path)) {

							if ($.empty(flowContainerPackage)) {
								flowContainerPackage = $.create("FlowContainerPackage", "name", name);
							}

						} else {

							let parent = $.first($.find("FlowContainerPackage", "effectiveName", path));

							if ($.empty(flowContainerPackage)) {
								flowContainerPackage = $.create("FlowContainerPackage", "name", name, "parent", parent);
							}
						}

						return flowContainerPackage;
					};

					if (!$.empty(path)) {

						if (path.length > 0) {

							let flowName = null;

							if (path.indexOf(".") !== -1) {

								let elements = path.split(".");

								if (elements.length > 1) {

									flowName = elements.pop();
									let currentPath = "";
									let parentPackage = null;

									for (let el of elements) {

										let flowContainerPackage = getOrCreatePackage(el, currentPath);
										parentPackage = flowContainerPackage;
										currentPath = flowContainerPackage.effectiveName;
									}

									self.parent = parentPackage;

								} else {

									flowName = elements[0];
								}

								self.name = flowName;

							} else {

								self.name = path;
							}
						}
					}
				}
				""").typeHint("String");
		final Property<Boolean> scheduledForIndexing     = new BooleanProperty(SCHEDULED_FOR_INDEXING_PROPERTY).defaultValue(false);

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
				NAME_PROPERTY, EFFECTIVE_NAME_PROPERTY, PACKAGES_PROPERTY, FLOWS_PROPERTY, SCHEDULED_FOR_INDEXING_PROPERTY
			),
			PropertyView.Ui,
			newSet(
				NAME_PROPERTY, EFFECTIVE_NAME_PROPERTY, PACKAGES_PROPERTY, FLOWS_PROPERTY, PARENT_PROPERTY, SCHEDULED_FOR_INDEXING_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
