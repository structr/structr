/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.AfterCreation;
import org.structr.feed.entity.AbstractFeedItem;
import org.structr.feed.entity.FeedItemContent;
import org.structr.feed.traits.wrappers.FeedItemContentTraitWrapper;

import java.util.Map;
import java.util.Set;

/**
 * Represents a content element of a feed item
 */
public class FeedItemContentTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String ITEM_PROPERTY      = "item";
	public static final String MODE_PROPERTY      = "mode";
	public static final String ITEM_TYPE_PROPERTY = "itemType";
	public static final String VALUE_PROPERTY     = "value";

	public FeedItemContentTraitDefinition() {
		super(StructrTraits.FEED_ITEM_CONTENT);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(
			AfterCreation.class,
			new AfterCreation() {
				@Override
				public void afterCreation(final GraphObject graphObject, final SecurityContext securityContext) throws FrameworkException {
					graphObject.as(AbstractFeedItem.class).updateIndex(securityContext);
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<NodeInterface> itemProperty = new StartNode(ITEM_PROPERTY, StructrTraits.FEED_ITEM_FEED_ITEM_CONTENTS_FEED_ITEM_CONTENT);
		final Property<String> modeProperty        = new StringProperty(MODE_PROPERTY);
		final Property<String> itemTypeProperty    = new StringProperty(ITEM_TYPE_PROPERTY);
		final Property<String> valueProperty       = new StringProperty(VALUE_PROPERTY);

		return newSet(
			itemProperty,
			modeProperty,
			itemTypeProperty,
			valueProperty
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FeedItemContent.class, (traits, node) -> new FeedItemContentTraitWrapper(traits, node)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
				PropertyView.Public,
				newSet(
						MODE_PROPERTY, ITEM_TYPE_PROPERTY, VALUE_PROPERTY,
						NodeInterfaceTraitDefinition.OWNER_PROPERTY
				),

				PropertyView.Ui,
				newSet(
						MODE_PROPERTY, ITEM_TYPE_PROPERTY, VALUE_PROPERTY,
						ITEM_PROPERTY
				)
		);
	}
}
