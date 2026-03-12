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
package org.structr.web.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.traits.wrappers.ComponentConfigurationTraitWrapper;

import java.util.Map;
import java.util.Set;

public class ComponentConfigurationTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DOM_NODE_PROPERTY              = "domNode";
	public static final String DATA_ADAPTER_PROPERTY          = "dataAdapter";
	public static final String DISPLAY_MODE_PROPERTY          = "displayMode";
	public static final String SAVE_MODE_PROPERTY             = "saveMode";
	public static final String SHOW_LABELS_PROPERTY           = "labels";
	public static final String FIELD_SET_PROPERTY             = "fieldSet";
	public static final String ROLE_PROPERTY                  = "role";
	public static final String RELOAD_BEHAVIOUR_PROPERTY      = "reload";
	public static final String COLUMNS_PROPERTY               = "columns";
	public static final String DATA_SOURCE_PROPERTY           = "dataSource";
	public static final String SELECTION_CHANNEL_PROPERTY     = "selectionChannel";
	public static final String TRANSFORM_PROPERTY             = "transform";

	public ComponentConfigurationTraitDefinition() {
		super(StructrTraits.COMPONENT_CONFIGURATION);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			ComponentConfiguration.class, (traits, node) -> new ComponentConfigurationTraitWrapper(traits, node)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface> domNodeProperty      = new StartNode(traitsInstance, DOM_NODE_PROPERTY, StructrTraits.DOM_NODE_HAS_COMPONENT_CONFIGURATION).category(DOMNode.PAGE_CATEGORY);
		final Property<NodeInterface> dataAdapterProperty  = new EndNode(traitsInstance, DATA_ADAPTER_PROPERTY, StructrTraits.COMPONENT_CONFIGURATION_HAS_DATA_ADAPTER).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> displayModeProperty         = new StringProperty(DISPLAY_MODE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> saveModeProperty            = new StringProperty(SAVE_MODE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> fieldSetProperty            = new StringProperty(FIELD_SET_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> roleProperty                = new StringProperty(ROLE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> reloadBehaviourProperty     = new StringProperty(RELOAD_BEHAVIOUR_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<Boolean> showLabelsProperty         = new BooleanProperty(SHOW_LABELS_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<Integer> columnsProperty            = new IntProperty(COLUMNS_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> dataSourceProperty          = new StringProperty(DATA_SOURCE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> selectionChannelProperty    = new StringProperty(SELECTION_CHANNEL_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> transformProperty           = new StringProperty(TRANSFORM_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);

		return newSet(
			domNodeProperty,
			dataAdapterProperty,
			displayModeProperty,
			saveModeProperty,
			fieldSetProperty,
			roleProperty,
			reloadBehaviourProperty,
			showLabelsProperty,
			columnsProperty,
			dataSourceProperty,
			selectionChannelProperty,
			transformProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Ui,
			newSet(
				DOM_NODE_PROPERTY, DATA_ADAPTER_PROPERTY, DISPLAY_MODE_PROPERTY, SAVE_MODE_PROPERTY,
				FIELD_SET_PROPERTY, SHOW_LABELS_PROPERTY, ROLE_PROPERTY, RELOAD_BEHAVIOUR_PROPERTY,
				COLUMNS_PROPERTY, DATA_SOURCE_PROPERTY, SELECTION_CHANNEL_PROPERTY,
				TRANSFORM_PROPERTY
			)
		);
	}
}
