/*
 * Copyright (C) 2010-2024 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.feed.traits.definitions;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.AfterCreation;
import org.structr.feed.entity.FeedItemEnclosure;
import org.structr.feed.traits.wrappers.FeedItemEnclosureTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 * Represents feed enclosures
 */
public class FeedItemEnclosureTraitDefinition extends AbstractNodeTraitDefinition {

	public FeedItemEnclosureTraitDefinition() {
		super("FeedItemEnclosure");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(
			AfterCreation.class,
			new AfterCreation() {

				@Override
				public void afterCreation(final GraphObject graphObject, final SecurityContext securityContext) throws FrameworkException {

					graphObject.as(FeedItemEnclosure.class).updateIndex(securityContext);
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> itemProperty   = new StartNode("item", "FeedItemFEED_ITEM_ENCLOSURESFeedItemEnclosure");
		final Property<String> urlProperty           = new StringProperty("url");
		final Property<Long> enclosureLengthProperty = new LongProperty("enclosureLength");
		final Property<String> enclosureTypeProperty = new StringProperty("enclosureType");

		return newSet(
			itemProperty,
			urlProperty,
			enclosureLengthProperty,
			enclosureTypeProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				"url", "enclosureLength", "enclosureType", "item", "owner"
			),
			PropertyView.Ui,
			newSet(
				"url", "enclosureLength", "enclosureType", "item"
			)
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FeedItemEnclosure.class, (traits, node) -> new FeedItemEnclosureTraitWrapper(traits, node)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
