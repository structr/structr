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
import org.structr.core.api.AbstractMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.web.entity.Linkable;
import org.structr.web.traits.wrappers.LinkableTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class LinkableTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String LINKING_ELEMENTS_PROPERTY     = "linkingElements";
	public static final String LINKING_ELEMENTS_IDS_PROPERTY = "linkingElementsIds";
	public static final String ENABLE_BASIC_AUTH_PROPERTY    = "enableBasicAuth";
	public static final String BASIC_AUTH_REALM_PROPERTY     = "basicAuthRealm";

	public LinkableTraitDefinition() {
		super(StructrTraits.LINKABLE);
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
			Linkable.class, (traits, node) -> new LinkableTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {
		return Set.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> linkingElementsProperty = new StartNodes(LINKING_ELEMENTS_PROPERTY, StructrTraits.LINK_SOURCE_LINK_LINKABLE);
		final Property<Iterable<String>> linkingElementsIdsProperty     = new CollectionIdProperty<>(LINKING_ELEMENTS_IDS_PROPERTY, StructrTraits.LINKABLE,LINKING_ELEMENTS_PROPERTY, StructrTraits.LINK_SOURCE);
		final Property<Boolean> enableBasicAuthProperty                 = new BooleanProperty(ENABLE_BASIC_AUTH_PROPERTY).defaultValue(false).indexed();
		final Property<String> basicAuthRealmProperty                   = new StringProperty(BASIC_AUTH_REALM_PROPERTY);

		return Set.of(
			linkingElementsProperty,
			linkingElementsIdsProperty,
			enableBasicAuthProperty,
			basicAuthRealmProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Ui,
			newSet(
				LINKING_ELEMENTS_PROPERTY, LINKING_ELEMENTS_IDS_PROPERTY, ENABLE_BASIC_AUTH_PROPERTY, BASIC_AUTH_REALM_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
