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
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.AfterCreation;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.feed.entity.FeedItem;
import org.structr.feed.traits.wrappers.FeedItemTraitWrapper;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Represents a single item of a data feed.
 *
 */
public class FeedItemTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String CONTENTS_PROPERTY          = "contents";
	public static final String ENCLOSURES_PROPERTY        = "enclosures";
	public static final String FEED_PROPERTY              = "feed";
	public static final String URL_PROPERTY               = "url";
	public static final String AUTHOR_PROPERTY            = "author";
	public static final String COMMENTS_PROPERTY          = "comments";
	public static final String DESCRIPTION_PROPERTY       = "description";
	public static final String PUB_DATE_PROPERTY          = "pubDate";
	public static final String UPDATED_DATE_PROPERTY      = "updatedDate";
	public static final String CHECKSUM_PROPERTY          = "checksum";
	public static final String CACHE_FOR_SECONDS_PROPERTY = "cacheForSeconds";
	public static final String VERSION_PROPERTY           = "version";

	public FeedItemTraitDefinition() {
		super(StructrTraits.FEED_ITEM);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					final PropertyKey<String> urlProperty = obj.getTraits().key(URL_PROPERTY);
					boolean valid = true;

					valid &= ValidationHelper.isValidPropertyNotNull(obj, urlProperty, errorBuffer);
					valid &= ValidationHelper.isValidUniqueProperty(obj, urlProperty, errorBuffer);

					return valid;
				}
			},

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
					graphObject.as(FeedItem.class).updateIndex(securityContext);
				}
			},

			AfterCreation.class,
			new AfterCreation() {

				@Override
				public void afterCreation(final GraphObject graphObject, final SecurityContext securityContext) throws FrameworkException {
					graphObject.as(FeedItem.class).updateIndex(securityContext);
				}
			}
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> contentsProperty   = new EndNodes(CONTENTS_PROPERTY, StructrTraits.FEED_ITEM_FEED_ITEM_CONTENTS_FEED_ITEM_CONTENT);
		final Property<Iterable<NodeInterface>> enclosuresProperty = new EndNodes(ENCLOSURES_PROPERTY, StructrTraits.FEED_ITEM_FEED_ITEM_ENCLOSURES_FEED_ITEM_ENCLOSURE);
		final Property<NodeInterface> feedProperty                 = new StartNode(FEED_PROPERTY, StructrTraits.DATA_FEED_HAS_FEED_ITEMS_FEED_ITEM);

		final Property<String> urlProperty              = new StringProperty(URL_PROPERTY).indexed().notNull();
		final Property<String> authorProperty           = new StringProperty(AUTHOR_PROPERTY);
		final Property<String> commentsProperty         = new StringProperty(COMMENTS_PROPERTY);
		final Property<String> descriptionProperty      = new StringProperty(DESCRIPTION_PROPERTY);
		final Property<Date> pubDateProperty            = new DateProperty(PUB_DATE_PROPERTY);
		final Property<Date> updatedDateProperty        = new DateProperty(UPDATED_DATE_PROPERTY);
		final Property<Long> checksumProperty           = new LongProperty(CHECKSUM_PROPERTY).readOnly();
		final Property<Integer> cacheForSecondsProperty = new IntProperty(CACHE_FOR_SECONDS_PROPERTY);
		final Property<Integer> versionProperty         = new IntProperty(VERSION_PROPERTY).readOnly();

		return newSet(
			contentsProperty,
			enclosuresProperty,
			feedProperty,
			urlProperty,
			authorProperty,
			commentsProperty,
			descriptionProperty,
			pubDateProperty,
			updatedDateProperty,
			checksumProperty,
			cacheForSecondsProperty,
			versionProperty
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			FeedItem.class, (traits, node) -> new FeedItemTraitWrapper(traits, node)
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
					NodeInterfaceTraitDefinition.OWNER_PROPERTY, NodeInterfaceTraitDefinition.NAME_PROPERTY,
					URL_PROPERTY, AUTHOR_PROPERTY, COMMENTS_PROPERTY, DESCRIPTION_PROPERTY, PUB_DATE_PROPERTY,
					UPDATED_DATE_PROPERTY, CONTENTS_PROPERTY, ENCLOSURES_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					URL_PROPERTY, AUTHOR_PROPERTY, COMMENTS_PROPERTY, DESCRIPTION_PROPERTY, PUB_DATE_PROPERTY,
					UPDATED_DATE_PROPERTY, CHECKSUM_PROPERTY, CACHE_FOR_SECONDS_PROPERTY, VERSION_PROPERTY,
					CONTENTS_PROPERTY, ENCLOSURES_PROPERTY, FEED_PROPERTY
			)
		);
	}
}
