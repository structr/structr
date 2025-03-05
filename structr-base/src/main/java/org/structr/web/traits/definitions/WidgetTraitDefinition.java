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

import org.structr.common.PropertyView;
import org.structr.core.entity.Relation;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
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

		final Property<String> sourceProperty          = new StringProperty("source");
		final Property<String> descriptionProperty     = new StringProperty("description");
		final Property<String> configurationProperty   = new StringProperty("configuration");
		final Property<String> svgIconPathProperty     = new StringProperty("svgIconPath");
		final Property<String> thumbnailPathProperty   = new StringProperty("thumbnailPath");
		final Property<String> treePathProperty        = new StringProperty("treePath");
		final Property<Boolean> isWidgetProperty       = new ConstantBooleanProperty("isWidget", true);
		final Property<String[]> selectorsProperty     = new ArrayProperty("selectors", String[].class);
		final Property<Boolean> isPageTemplateProperty = new BooleanProperty("isPageTemplate");

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
				"name", "source", "description", "configuration", "svgIconPath", "thumbnailPath",
				"treePath", "isWidget", "selectors", "isPageTemplate"
			),
			PropertyView.Ui,
			newSet(
				"source", "description", "configuration", "svgIconPath", "thumbnailPath",
				"treePath", "isWidget", "selectors", "isPageTemplate"
			),
			"editWidget",
			newSet(
				"selectors", "isPageTemplate"
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
