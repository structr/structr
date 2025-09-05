/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.web.entity.Widget;
import org.structr.web.traits.wrappers.WidgetTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public class WidgetTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String DESCRIPTION_PROPERTY      = "description";
	public static final String SOURCE_PROPERTY           = "source";
	public static final String CONFIGURATION_PROPERTY    = "configuration";
	public static final String SVG_ICON_PATH_PROPERTY    = "svgIconPath";
	public static final String THUMBNAIL_PATH_PROPERTY   = "thumbnailPath";
	public static final String TREE_PATH_PROPERTY        = "treePath";
	public static final String IS_WIDGET_PROPERTY        = "isWidget";
	public static final String SELECTORS_PROPERTY        = "selectors";
	public static final String IS_PAGE_TEMPLATE_PROPERTY = "isPageTemplate";

	public WidgetTraitDefinition() {
		super(StructrTraits.WIDGET);
	}


	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {
		return Map.of();
	}

	@Override
	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			Widget.class, (traits, node) -> new WidgetTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<String> sourceProperty          = new StringProperty(SOURCE_PROPERTY);
		final Property<String> descriptionProperty     = new StringProperty(DESCRIPTION_PROPERTY);
		final Property<String> configurationProperty   = new StringProperty(CONFIGURATION_PROPERTY);
		final Property<String> svgIconPathProperty     = new StringProperty(SVG_ICON_PATH_PROPERTY);
		final Property<String> thumbnailPathProperty   = new StringProperty(THUMBNAIL_PATH_PROPERTY);
		final Property<String> treePathProperty        = new StringProperty(TREE_PATH_PROPERTY);
		final Property<Boolean> isWidgetProperty       = new ConstantBooleanProperty(IS_WIDGET_PROPERTY, true);
		final Property<String[]> selectorsProperty     = new ArrayProperty(SELECTORS_PROPERTY, String[].class);
		final Property<Boolean> isPageTemplateProperty = new BooleanProperty(IS_PAGE_TEMPLATE_PROPERTY);

		return Set.of(
			sourceProperty,
			descriptionProperty,
			configurationProperty,
			svgIconPathProperty,
			thumbnailPathProperty,
			treePathProperty,
			isWidgetProperty,
			selectorsProperty,
			isPageTemplateProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			newSet(
					SOURCE_PROPERTY, DESCRIPTION_PROPERTY, CONFIGURATION_PROPERTY, SVG_ICON_PATH_PROPERTY, THUMBNAIL_PATH_PROPERTY,
					TREE_PATH_PROPERTY, IS_WIDGET_PROPERTY, SELECTORS_PROPERTY, IS_PAGE_TEMPLATE_PROPERTY
			),

			PropertyView.Ui,
			newSet(
					SOURCE_PROPERTY, DESCRIPTION_PROPERTY, CONFIGURATION_PROPERTY, SVG_ICON_PATH_PROPERTY, THUMBNAIL_PATH_PROPERTY,
					TREE_PATH_PROPERTY, IS_WIDGET_PROPERTY, SELECTORS_PROPERTY, IS_PAGE_TEMPLATE_PROPERTY
			),

			"editWidget",
			newSet(
					SELECTORS_PROPERTY, IS_PAGE_TEMPLATE_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
