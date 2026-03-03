/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.core.traits.definitions;

import org.structr.core.entity.DataProvider;
import org.structr.core.entity.DataSource;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNode;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.wrappers.DataProviderTraitWrapper;
import org.structr.web.entity.dom.DOMNode;

import java.util.Map;
import java.util.Set;

public class DataProviderTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String SOURCE_PROPERTY = "source";

	public DataProviderTraitDefinition() {
		super(StructrTraits.DATA_PROVIDER);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			DataProvider.class, (traits, node) -> new DataProviderTraitWrapper(traits, node)
		);
	}

	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface> sourceProperty = new StartNode(traitsInstance, SOURCE_PROPERTY, StructrTraits.DATA_SOURCE_HAS_PROVIDER_DATA_PROVIDER).category(DOMNode.WIDGETS_CATEGORY);

		return newSet(
			sourceProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			"dataSource", newSet(GraphObjectTraitDefinition.ID_PROPERTY, NodeInterfaceTraitDefinition.NAME_PROPERTY, SOURCE_PROPERTY)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
