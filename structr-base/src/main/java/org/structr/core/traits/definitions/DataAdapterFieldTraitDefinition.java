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

import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.DataAdapterField;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.wrappers.DataAdapterFieldTraitWrapper;
import org.structr.web.entity.dom.DOMNode;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DataAdapterFieldTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DATA_ADAPTER_PROPERTY       = "dataAdapter";
	public static final String RENDER_TEMPLATE_PROPERTY    = "renderTemplate";
	public static final String EDIT_TEMPLATE_PROPERTY      = "editTemplate";
	public static final String LABEL_PROPERTY              = "label";
	public static final String CONFIG_PROPERTY             = "config";
	public static final String VALUE_PROPERTY              = "value";
	public static final String DATA_TYPE_PROPERTY          = "dataType";
	public static final String SORT_KEY_PROPERTY           = "sortKey";
	public static final String IS_SEARCHABLE_PROPERTY      = "isSearchable";
	public static final String ROWS_PROPERTY               = "rows";
	public static final String COLUMNS_PROPERTY            = "columns";
	public static final String COLUMN_DATA_SOURCE_PROPERTY = "columnDataSource";
	public static final String COLUMN_KEY_PROPERTY         = "columnKey";

	public DataAdapterFieldTraitDefinition() {
		super(StructrTraits.DATA_ADAPTER_FIELD);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			DataAdapterField.class, (traits, node) -> new DataAdapterFieldTraitWrapper(traits, node)
		);
	}

	@Override
	public Map<Class, LifecycleMethod> createLifecycleMethods(final TraitsInstance traitsInstance) {

		return Map.of(
			OnCreation.class,
			new OnCreation() {
				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
					// all fields are searchable by default
					graphObject.as(DataAdapterField.class).setIsSearchable(true);
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> createPropertyKeys(TraitsInstance traitsInstance) {

		final Property<NodeInterface> dataAdapterProperty = new StartNode(traitsInstance, DATA_ADAPTER_PROPERTY, StructrTraits.DATA_ADAPTER_HAS_FIELD_DATA_ADAPTER_FIELD).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> renderTemplateProperty     = new StringProperty(RENDER_TEMPLATE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> editTemplateProperty       = new StringProperty(EDIT_TEMPLATE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> labelProperty              = new StringProperty(LABEL_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> configProperty             = new StringProperty(CONFIG_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> valueProperty              = new StringProperty(VALUE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> dataTypeProperty           = new StringProperty(DATA_TYPE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> sortKeyProperty            = new StringProperty(SORT_KEY_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<Boolean> isSearchableProperty      = new BooleanProperty(IS_SEARCHABLE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<Integer> rowsProperty              = new IntProperty(ROWS_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<Integer> columnsProperty           = new IntProperty(COLUMNS_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> columnDataSourceProperty   = new StringProperty(COLUMN_DATA_SOURCE_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);
		final Property<String> columnKeyProperty          = new StringProperty(COLUMN_KEY_PROPERTY).category(DOMNode.WIDGETS_CATEGORY);

		return newSet(
			dataAdapterProperty,
			renderTemplateProperty,
			editTemplateProperty,
			labelProperty,
			valueProperty,
			dataTypeProperty,
			configProperty,
			sortKeyProperty,
			isSearchableProperty,
			rowsProperty,
			columnsProperty,
			columnDataSourceProperty,
			columnKeyProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			"adapter", newSet(
				GraphObjectTraitDefinition.ID_PROPERTY,
				NodeInterfaceTraitDefinition.NAME_PROPERTY,
				DATA_ADAPTER_PROPERTY,
				RENDER_TEMPLATE_PROPERTY,
				EDIT_TEMPLATE_PROPERTY,
				LABEL_PROPERTY,
				VALUE_PROPERTY,
				DATA_TYPE_PROPERTY,
				CONFIG_PROPERTY,
				SORT_KEY_PROPERTY,
				IS_SEARCHABLE_PROPERTY,
				ROWS_PROPERTY,
				COLUMNS_PROPERTY,
				COLUMN_DATA_SOURCE_PROPERTY,
				COLUMN_KEY_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
