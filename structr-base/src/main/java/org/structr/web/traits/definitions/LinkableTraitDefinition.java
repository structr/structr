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

import org.structr.core.api.AbstractMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.definitions.AbstractTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.web.entity.Linkable;
import org.structr.web.traits.wrappers.LinkableTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public class LinkableTraitDefinition extends AbstractTraitDefinition {

	public LinkableTraitDefinition() {
		super("Linkable");
	}

	/*
	View uiView = new View(LinkableTraitDefinition.class, PropertyView.Ui,
		linkingElementsProperty, linkinkElementsIdsProperty, enableBasicAuthProperty, basicAuthRealmProperty
	);
	*/

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

		final Property<Iterable<NodeInterface>> linkingElementsProperty = new StartNodes("linkingElements", "LinkSourceLINKLinkable");
		final Property<Iterable<String>> linkinkElementsIdsProperty     = new CollectionIdProperty<>("linkingElementsIds", linkingElementsProperty);
		final Property<Boolean> enableBasicAuthProperty                 = new BooleanProperty("enableBasicAuth").defaultValue(false).indexed();
		final Property<String> basicAuthRealmProperty                   = new StringProperty("basicAuthRealm");

		return Set.of(
			linkingElementsProperty,
			linkinkElementsIdsProperty,
			enableBasicAuthProperty,
			basicAuthRealmProperty
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
