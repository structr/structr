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
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
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

	public FeedItemTraitDefinition() {
		super("FeedItem");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					final PropertyKey<String> urlProperty = obj.getTraits().key("url");
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

		final Property<Iterable<NodeInterface>> contentsProperty     = new EndNodes("contents", "FeedItemFEED_ITEM_CONTENTSFeedItemContent");
		final Property<Iterable<NodeInterface>> enclosuresProperty = new EndNodes("enclosures", "FeedItemFEED_ITEM_ENCLOSURESFeedItemEnclosure");
		final Property<NodeInterface> feedProperty                          = new StartNode("feed", "DataFeedHAS_FEED_ITEMSFeedItem");

		final Property<String> urlProperty              = new StringProperty("url").indexed().notNull();
		final Property<String> authorProperty           = new StringProperty("author");
		final Property<String> commentsProperty         = new StringProperty("comments");
		final Property<String> descriptionProperty      = new StringProperty("description");
		final Property<Date> pubDateProperty            = new DateProperty("pubDate");
		final Property<Date> updatedDateProperty        = new DateProperty("updatedDate");
		final Property<Long> checksumProperty           = new LongProperty("checksum").readOnly();
		final Property<Integer> cacheForSecondsProperty = new IntProperty("cacheForSeconds");
		final Property<Integer> versionProperty         = new IntProperty("version").readOnly();

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
				"owner", "name", "url", "author", "comments", "description", "pubDate",
				"updatedDate", "contents", "enclosures"
			),
			PropertyView.Ui,
			newSet(
				"url", "author", "comments", "description", "pubDate",
				"updatedDate", "checksum", "cacheForSeconds", "version",
				"contents", "enclosures", "feed"
			)
		);
	}
}
