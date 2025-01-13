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
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.definitions.AbstractTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.web.entity.ApplicationConfigurationDataNode;
import org.structr.web.traits.wrappers.ApplicationConfigurationDataNodeTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 * Storage object for configuration data.
 */
public class ApplicationConfigurationDataNodeTraitDefinition extends AbstractTraitDefinition {

	/*
	public static final View uiView = new View(ApplicationConfigurationDataNode.class, PropertyView.Ui,
		configTypeProperty, contentProperty
	);
	*/

	public ApplicationConfigurationDataNodeTraitDefinition() {
		super("ApplicationConfigurationDataNode");
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
			ApplicationConfigurationDataNode.class, (traits, node) -> new ApplicationConfigurationDataNodeTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<String> configTypeProperty = new StringProperty("configType").indexed();
		final Property<String> contentProperty    = new StringProperty("content");

		return Set.of(
			configTypeProperty,
			contentProperty
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}