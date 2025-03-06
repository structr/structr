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
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.flow.impl.FlowContainerConfiguration;

import java.util.Map;
import java.util.Set;

public class FlowContainerConfigurationTraitDefinition extends AbstractNodeTraitDefinition {

	public FlowContainerConfigurationTraitDefinition() {
		super("FlowContainerConfiguration");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
					graphObject.setVisibility(true, true);
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowContainerConfiguration.class, (traits, node) -> new FlowContainerConfiguration(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> flow          = new EndNode("flow", "FlowContainerConfigurationFlow");
		final Property<NodeInterface> activeForFlow = new EndNode("activeForFlow", "FlowActiveContainerConfiguration");
		final Property<String> validForEditor       = new StringProperty("validForEditor").indexed();
		final Property<String> configJson           = new StringProperty("configJson");

		return newSet(
			flow,
			activeForFlow,
			validForEditor,
			configJson
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				"validForEditor", "configJson"
			),
			PropertyView.Ui,
			newSet(
				"flow", "activeForFlow", "validForEditor", "configJson"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
