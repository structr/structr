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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.DataAdapterTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.web.entity.ComponentConfiguration;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.traits.wrappers.ComponentConfigurationTraitWrapper;

import java.util.Map;
import java.util.Set;

public class ComponentConfigurationTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DOM_NODE_PROPERTY               = "domNode";
	public static final String DATA_ADAPTER_PROPERTY           = "dataAdapter";
	public static final String DISPLAY_MODE_PROPERTY           = "displayMode";
	public static final String SAVE_MODE_PROPERTY              = "saveMode";
	public static final String SHOW_LABELS_PROPERTY            = "labels";
	public static final String FIELD_SET_PROPERTY              = "fieldSet";
	public static final String ROLE_PROPERTY                   = "role";
	public static final String RELOAD_BEHAVIOUR_PROPERTY       = "reload";
	public static final String COLUMNS_PROPERTY                = "columns";
	public static final String DATA_SOURCE_PROPERTY            = "dataSource";
	public static final String SELECTION_CHANNEL_PROPERTY      = "selectionChannel";
	public static final String TRANSFORM_PROPERTY              = "transform";
	public static final String PAGE_SIZE_PROPERTY              = "pageSize";
	public static final String PAGINATION_WINDOW_SIZE_PROPERTY = "paginationWindowSize";
	public static final String EXPECTED_DATA_TYPE_PROPERTY     = "expectedDataType";

	// Binding mode: how this component relates to a process-side declaration
	// of its data type. Default `standalone`: the dataSource and field config
	// are owned by the UI designer. `processBound`: the dataSource is derived
	// at render time from the BPMN UserTask the component's ActionMapping
	// targets, and the process designer's choice of subject type leads.
	// See project_process_ui_contract_pillar.md for the design rationale.
	public static final String BINDING_MODE_PROPERTY           = "bindingMode";
	public static final String BINDING_MODE_STANDALONE         = "standalone";
	public static final String BINDING_MODE_PROCESS_BOUND      = "processBound";

	// Optional EndNode -> BpmnElement (a UserTask). Set when bindingMode is
	// `processBound`. The property itself is declared by ProcessModule.onLoad
	// (it crosses module boundaries: source type lives in structr-base,
	// target type lives in structr-process-module). The constant lives here
	// so structr-base code can read the property by name without importing
	// process-module types.
	public static final String BOUND_USER_TASK_PROPERTY        = "boundUserTask";

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
	public Map<Class, LifecycleMethod> createLifecycleMethods(TraitsInstance traitsInstance) {

		return Map.of(

			OnCreation.class,
			new OnCreation() {
				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final ComponentConfiguration componentConfiguration = graphObject.as(ComponentConfiguration.class);
					final Traits adapterTraits                          = Traits.of(StructrTraits.DATA_ADAPTER);
					final Traits componentTraits                        = componentConfiguration.getTraits();
					final String dataKey                                = "item";

					// create new data adapter (one per component)
					if (componentConfiguration.getDataAdapter() == null) {

						StructrApp.getInstance(securityContext).create(StructrTraits.DATA_ADAPTER,
							new NodeAttribute<>(adapterTraits.key(DataAdapterTraitDefinition.CONFIGURATION_PROPERTY), componentConfiguration),
							new NodeAttribute<>(adapterTraits.key(DataAdapterTraitDefinition.DATA_KEY_PROPERTY), dataKey)
						);
					}

					// default field is "name"
					if (StringUtils.isEmpty(componentConfiguration.getFieldSet())) {

						componentConfiguration.setProperty(componentTraits.key(FIELD_SET_PROPERTY), "name");
					}

					// default field is "name"
					if (StringUtils.isEmpty(componentConfiguration.getDisplayMode())) {

						componentConfiguration.setProperty(componentTraits.key(DISPLAY_MODE_PROPERTY), "output");
					}

					componentConfiguration.checkCompatibility();
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					final ComponentConfiguration componentConfiguration = graphObject.as(ComponentConfiguration.class);

					componentConfiguration.updateFieldSetForChildren();
					componentConfiguration.checkCompatibility();
				}
			}
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface> domNodeProperty        = new StartNode(traitsInstance, DOM_NODE_PROPERTY, StructrTraits.DOM_NODE_HAS_COMPONENT_CONFIGURATION).category(DOMNode.PAGE_CATEGORY);
		final Property<NodeInterface> dataAdapterProperty    = new EndNode(traitsInstance, DATA_ADAPTER_PROPERTY, StructrTraits.COMPONENT_CONFIGURATION_HAS_DATA_ADAPTER).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> displayModeProperty           = new StringProperty(DISPLAY_MODE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> saveModeProperty              = new StringProperty(SAVE_MODE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> fieldSetProperty              = new StringProperty(FIELD_SET_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> roleProperty                  = new StringProperty(ROLE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> reloadBehaviourProperty       = new StringProperty(RELOAD_BEHAVIOUR_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<Boolean> showLabelsProperty           = new BooleanProperty(SHOW_LABELS_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<Integer> columnsProperty              = new IntProperty(COLUMNS_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> dataSourceProperty            = new StringProperty(DATA_SOURCE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> selectionChannelProperty      = new StringProperty(SELECTION_CHANNEL_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> transformProperty             = new StringProperty(TRANSFORM_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<Integer> pageSizeProperty             = new IntProperty(PAGE_SIZE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<Integer> paginationWindowSizeProperty = new IntProperty(PAGINATION_WINDOW_SIZE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String>  bindingModeProperty          = new EnumProperty(BINDING_MODE_PROPERTY, Set.of(BINDING_MODE_STANDALONE, BINDING_MODE_PROCESS_BOUND))
			.defaultValue(BINDING_MODE_STANDALONE).indexed().category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> expectedDataTypeProperty      = new StringProperty(EXPECTED_DATA_TYPE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);

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
			transformProperty,
			pageSizeProperty,
			paginationWindowSizeProperty,
			bindingModeProperty,
			expectedDataTypeProperty
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
				TRANSFORM_PROPERTY, PAGE_SIZE_PROPERTY, PAGINATION_WINDOW_SIZE_PROPERTY,
				BINDING_MODE_PROPERTY,
				// `boundUserTask` is registered cross-module by ProcessModule.onLoad
				// (target type lives in structr-process-module). Listed here so
				// the General-tab dialog can read it when the trait is composed
				// with the process module loaded; ignored otherwise.
				BOUND_USER_TASK_PROPERTY,
				EXPECTED_DATA_TYPE_PROPERTY
			)
		);
	}
}
