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
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.flow.impl.FlowGetProperty;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class FlowGetPropertyTraitDefinition extends AbstractNodeTraitDefinition {

	public FlowGetPropertyTraitDefinition() {
		super("FlowGetProperty");
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FlowGetProperty.class, (traits, node) -> new FlowGetProperty(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> nodeSource         = new StartNode("nodeSource",         "FlowNodeDataSource");
		final Property<NodeInterface> propertyNameSource = new StartNode("propertyNameSource", "FlowNameDataSource");
		final Property<String> propertyName              = new StringProperty("propertyName");

		return newSet(
			nodeSource,
			propertyNameSource,
			propertyName
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				"nodeSource", "propertyNameSource", "propertyName"
			),
			PropertyView.Ui,
			newSet(
				"nodeSource", "propertyNameSource", "propertyName"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

}
