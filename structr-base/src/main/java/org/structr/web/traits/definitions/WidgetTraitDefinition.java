/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.structr.core.entity.Relation;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.definitions.AbstractTraitDefinition;
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
public class WidgetTraitDefinition extends AbstractTraitDefinition {

	/*
	public static final View defaultView = new View(Widget.class, PropertyView.Public,
		name, sourceProperty, descriptionProperty, configurationProperty, svgIconPathProperty, thumbnailPathProperty, treePathProperty, isWidgetProperty, selectorsProperty, isPageTemplateProperty
	);

	public static final View uiView = new View(Widget.class, PropertyView.Ui,
		sourceProperty, descriptionProperty, configurationProperty, svgIconPathProperty, thumbnailPathProperty, treePathProperty, isWidgetProperty, selectorsProperty, isPageTemplateProperty
	);

	public static final View editWidgetView = new View(Widget.class, "editWidget",
		selectorsProperty, isPageTemplateProperty
	);
	*/

	public WidgetTraitDefinition() {
		super("Widget");
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

		final Property<String> sourceProperty          = new StringProperty("source").partOfBuiltInSchema();
		final Property<String> descriptionProperty     = new StringProperty("description").partOfBuiltInSchema();
		final Property<String> configurationProperty   = new StringProperty("configuration").partOfBuiltInSchema();
		final Property<String> svgIconPathProperty     = new StringProperty("svgIconPath").partOfBuiltInSchema();
		final Property<String> thumbnailPathProperty   = new StringProperty("thumbnailPath").partOfBuiltInSchema();
		final Property<String> treePathProperty        = new StringProperty("treePath").partOfBuiltInSchema();
		final Property<Boolean> isWidgetProperty       = new ConstantBooleanProperty("isWidget", true).partOfBuiltInSchema();
		final Property<String[]> selectorsProperty     = new ArrayProperty("selectors", String[].class).partOfBuiltInSchema();
		final Property<Boolean> isPageTemplateProperty = new BooleanProperty("isPageTemplate").partOfBuiltInSchema();

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
	public Relation getRelation() {
		return null;
	}
}
