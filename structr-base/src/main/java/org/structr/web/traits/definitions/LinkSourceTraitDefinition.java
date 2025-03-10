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
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.EndNode;
import org.structr.core.property.EntityIdProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.web.entity.LinkSource;
import org.structr.web.traits.wrappers.LinkSourceTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 * This class represents elements which can have an outgoing link to a resource.
 */
public class LinkSourceTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String LINKABLE_PROPERTY    = "linkable";
	public static final String LINKABLE_ID_PROPERTY = "linkableId";

	public LinkSourceTraitDefinition() {
		super(StructrTraits.LINK_SOURCE);
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
			LinkSource.class, (traits, node) -> new LinkSourceTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> linkableProperty = new EndNode(LINKABLE_PROPERTY, StructrTraits.LINK_SOURCE_LINK_LINKABLE);
		final Property<String> linkableIdProperty      = new EntityIdProperty(LINKABLE_ID_PROPERTY, StructrTraits.LINK_SOURCE, LINKABLE_PROPERTY, StructrTraits.LINKABLE);

		return Set.of(
			linkableProperty,
			linkableIdProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Ui,
			newSet(
				LINKABLE_ID_PROPERTY, LINKABLE_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}