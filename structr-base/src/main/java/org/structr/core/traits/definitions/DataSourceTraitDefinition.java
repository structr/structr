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

import org.structr.core.entity.DataSource;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.wrappers.DataSourceTraitWrapper;
import org.structr.web.entity.dom.DOMNode;

import java.util.Map;
import java.util.Set;

public class DataSourceTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DOM_NODES_PROPERTY  = "domNodes";
	public static final String PROVIDER_PROPERTY   = "provider";
	public static final String MAPPING_PROPERTY    = "mapping";
	public static final String FIELD_SETS_PROPERTY = "fieldSets";
	public static final String DATA_KEY_PROPERTY   = "dataKey";
	public static final String CHANNEL_PROPERTY    = "channel";

	public DataSourceTraitDefinition() {
		super(StructrTraits.DATA_SOURCE);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			DataSource.class, (traits, node) -> new DataSourceTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<Iterable<NodeInterface>> domNodesProperty = new StartNodes(traitsInstance, DOM_NODES_PROPERTY, StructrTraits.COMPONENT_CONFIGURATION_HAS_DATA_SOURCE).category(DOMNode.WIDGETS_CATEGORY);
		final Property<NodeInterface> providerProperty           = new EndNode(traitsInstance, PROVIDER_PROPERTY, StructrTraits.DATA_SOURCE_HAS_PROVIDER_DATA_PROVIDER).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> mappingProperty                   = new StringProperty(MAPPING_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> fieldSetsProperty                 = new StringProperty(FIELD_SETS_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> dataKeyProperty                   = new StringProperty(DATA_KEY_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> channelProperty                   = new StringProperty(CHANNEL_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);

		return newSet(
			domNodesProperty,
			providerProperty,
			mappingProperty,
			fieldSetsProperty,
			dataKeyProperty,
			channelProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			"dataSource", newSet(
				GraphObjectTraitDefinition.ID_PROPERTY,
				NodeInterfaceTraitDefinition.NAME_PROPERTY,
				MAPPING_PROPERTY,
				PROVIDER_PROPERTY,
				FIELD_SETS_PROPERTY,
				DATA_KEY_PROPERTY,
				CHANNEL_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
