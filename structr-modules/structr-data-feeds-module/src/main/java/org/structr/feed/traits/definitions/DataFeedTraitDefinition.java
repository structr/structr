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

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.JavaMethod;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.IsValid;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.feed.entity.AbstractFeedItem;
import org.structr.feed.entity.DataFeed;
import org.structr.feed.traits.wrappers.DataFeedTraitWrapper;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.EvaluationHints;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.*;


public class DataFeedTraitDefinition extends AbstractNodeTraitDefinition {

	public DataFeedTraitDefinition() {
		super("DataFeed");
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(
			IsValid.class,
			new IsValid() {

				@Override
				public Boolean isValid(final GraphObject obj, final ErrorBuffer errorBuffer) {

					final PropertyKey<String> urlProperty = obj.getTraits().key("url");

					return ValidationHelper.isValidPropertyNotNull(obj, urlProperty, errorBuffer);
				}
			},

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
					graphObject.as(AbstractFeedItem.class).updateIndex(securityContext);
				}
			}
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {

		return newSet(

			new JavaMethod("cleanUp", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {

					cleanUp(securityContext, entity.as(DataFeed.class));
					return null;
				}
			},

			new JavaMethod("updateIfDue", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
					updateIfDue(securityContext, entity.as(DataFeed.class));
					return null;
				}
			},

			new JavaMethod("updateFeed", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
					updateFeed(securityContext, entity.as(DataFeed.class));
					return null;
				}
			},

			new JavaMethod("updateFeed", false, false) {

				@Override
				public Object execute(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments, final EvaluationHints hints) throws FrameworkException {
					updateFeed(securityContext, entity.as(DataFeed.class));
					return null;
				}
			}
		);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			DataFeed.class, (traits, node) -> new DataFeedTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<Iterable<NodeInterface>> itemsProperty = new EndNodes("items", "DataFeedHAS_FEED_ITEMSFeedItem");
		final Property<String> urlProperty                    = new StringProperty("url").indexed().notNull();
		final Property<String> feedTypeProperty               = new StringProperty("feedType");
		final Property<String> descriptionProperty            = new StringProperty("description");
		final Property<Long> updateIntervalProperty           = new LongProperty("updateInterval");
		final Property<Date> lastUpdatedProperty              = new DateProperty("lastUpdated");
		final Property<Long> maxAgeProperty                   = new LongProperty("maxAge");
		final Property<Integer> maxItemsProperty              = new IntProperty("maxItems");

		return newSet(
			itemsProperty,
			urlProperty,
			feedTypeProperty,
			descriptionProperty,
			updateIntervalProperty,
			lastUpdatedProperty,
			maxAgeProperty,
			maxItemsProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
				"url", "feedType", "description", "items"
			),
			PropertyView.Ui,
			newSet(
				"description", "feedType", "maxAge", "url", "updateInterval",
				"lastUpdated", "maxItems", "maxItems", "items"
			)
		);
	}

	public void cleanUp(final SecurityContext ctx, final DataFeed feed) {

		final Integer maxItemsToRetain = feed.getMaxItems();
		final Long    maxItemAge       = feed.getMaxAge();

		int i = 0;

		// Don't do anything if maxItems and maxAge are not set
		if (maxItemsToRetain != null || maxItemAge != null) {

			final List<NodeInterface> feedItems = Iterables.toList(feed.getItems());
			final Traits traits                 = Traits.of("FeedItem");
			final PropertyKey<Date> dateKey     = traits.key("pubDate");

			// Sort by publication date, youngest items first
			feedItems.sort(dateKey.sorted(true));

			for (final NodeInterface item : feedItems) {

				i++;

				final Date itemDate = item.getProperty(dateKey);

				if ((maxItemsToRetain != null && i > maxItemsToRetain) || (maxItemAge != null && itemDate.before(new Date(new Date().getTime() - maxItemAge)))) {

					try {

						StructrApp.getInstance(ctx).delete(item);

					} catch (FrameworkException ex) {
						final Logger logger = LoggerFactory.getLogger(DataFeedTraitDefinition.class);
						logger.error("Error while deleting old/surplus feed item " + item, ex);
					}
				}
			}
		}
	}

	public void updateIfDue(final SecurityContext ctx, final DataFeed feed) {

		final Date lastUpdate = feed.getLastUpdated();
		final Long interval   = feed.getUpdateInterval();

		if (lastUpdate == null || (interval != null && new Date().after(new Date(lastUpdate.getTime() + interval)))) {

			// Update feed and clean-up afterwards
			updateFeed(ctx, feed, true);
		}

	}

	public void updateFeed(final SecurityContext ctx, final DataFeed feed) {
		updateFeed(ctx, feed, true);
	}

	public void updateFeed(final SecurityContext ctx, final DataFeed feed, final boolean cleanUp) {

		final String remoteUrl = feed.getUrl();
		if (StringUtils.isNotBlank(remoteUrl)) {

			final App app = StructrApp.getInstance(ctx);

			try {

				final SyndFeedInput input = new SyndFeedInput();

				InputStream inputStream = null;
				final Map<String, Object> responseData =  HttpHelper.getAsStream(remoteUrl);
				if (responseData != null && responseData.containsKey(HttpHelper.FIELD_BODY) && responseData.get(HttpHelper.FIELD_BODY) instanceof InputStream) {

					inputStream =  (InputStream) responseData.get(HttpHelper.FIELD_BODY);
				}

				if (inputStream == null) {
					throw new FrameworkException(422, "Could not get input stream for feed " + feed.getUuid());
				}

				try (final Reader reader = new XmlReader(inputStream)) {

					final SyndFeed        syndFeed = input.build(reader);
					final List<SyndEntry> entries  = syndFeed.getEntries();
					final Traits enclosureTraits   = Traits.of("FeedItemEnclosure");
					final Traits contentTraits     = Traits.of("FeedItemContent");
					final Traits itemTraits        = Traits.of("FeedItem");

					final PropertyKey feedItemUrlKey             = itemTraits.key("urlProperty");
					final PropertyKey feedItemPubDateKey         = itemTraits.key("pubDateProperty");
					final PropertyKey feedItemUpdatedDateKey     = itemTraits.key("updatedDateProperty");
					final PropertyKey feedItemNameKey            = itemTraits.key("name");
					final PropertyKey feedItemAuthorKey          = itemTraits.key("authorProperty");
					final PropertyKey feedItemCommentsKey        = itemTraits.key("commentsProperty");
					final PropertyKey feedItemDescriptionKey     = itemTraits.key("descriptionProperty");
					final PropertyKey feedItemContentsKey        = itemTraits.key("contentsProperty");
					final PropertyKey feedItemEnclosuresKey      = itemTraits.key("enclosuresProperty");
					final PropertyKey feedItemContentModeKey     = contentTraits.key("modeProperty");
					final PropertyKey feedItemContentItemTypeKey = contentTraits.key("itemTypeProperty");
					final PropertyKey feedItemContentValueKey    = contentTraits.key("valueProperty");
					final PropertyKey feedItemEnclosureUrlKey    = enclosureTraits.key("urlProperty");
					final PropertyKey feedItemEnclosureLengthKey = enclosureTraits.key("enclosureLengthProperty");
					final PropertyKey feedItemEnclosureTypeKey   = enclosureTraits.key("enclosureTypeProperty");

					final List<NodeInterface> newItems = Iterables.toList(feed.getItems());

					for (final SyndEntry entry : entries) {

						final String link = entry.getLink();

						// Check if item with this link is already attached to this feed
						if (!newItems.stream().anyMatch(existingFeedItem -> existingFeedItem.getProperty(feedItemUrlKey).equals(link))) {

							final PropertyMap props = new PropertyMap();

							props.put(feedItemUrlKey,         entry.getLink());
							props.put(feedItemNameKey,        entry.getTitle());
							props.put(feedItemAuthorKey,      entry.getAuthor());
							props.put(feedItemCommentsKey,    entry.getComments());
							props.put(feedItemPubDateKey,     entry.getPublishedDate());
							props.put(feedItemUpdatedDateKey, entry.getUpdatedDate());

							if (entry.getDescription() != null) {
								props.put(feedItemDescriptionKey, entry.getDescription().getValue());
							}

							final List<NodeInterface> itemContents   = new LinkedList<>();
							final List<NodeInterface> itemEnclosures = new LinkedList<>();

							// Get and add all contents
							final List<SyndContent> contents = entry.getContents();
							for (final SyndContent content : contents) {

								final NodeInterface itemContent = app.create("FeedItemContent",
									new NodeAttribute(feedItemContentValueKey,    content.getValue()),
									new NodeAttribute(feedItemContentModeKey,     content.getMode()),
									new NodeAttribute(feedItemContentItemTypeKey, content.getType())
								);

								itemContents.add(itemContent);
							}

							// Get and add all enclosures
							final List<SyndEnclosure> enclosures = entry.getEnclosures();
							for (final SyndEnclosure enclosure : enclosures) {

								final NodeInterface itemEnclosure = app.create("FeedItemEnclosure",
									new NodeAttribute(feedItemEnclosureUrlKey,    enclosure.getUrl()),
									new NodeAttribute(feedItemEnclosureTypeKey,   enclosure.getType()),
									new NodeAttribute(feedItemEnclosureLengthKey, enclosure.getLength())
								);

								itemEnclosures.add(itemEnclosure);
							}

							props.put(feedItemContentsKey,   itemContents);
							props.put(feedItemEnclosuresKey, itemEnclosures);

							final NodeInterface item = app.create("FeedItem", props);

							newItems.add(item);

							final Logger logger = LoggerFactory.getLogger(DataFeedTraitDefinition.class);
							logger.debug("Created new item: {} ({}) ", entry.getTitle(), entry.getPublishedDate());
						}
					}

					final PropertyMap feedProps = new PropertyMap();
					final Traits feedTraits     = Traits.of("DataFeed");
					final PropertyKey<String> nameKey = feedTraits.key("name");

					if (StringUtils.isEmpty(feed.getProperty(nameKey))) {
						feedProps.put(nameKey, syndFeed.getTitle());
					}

					feedProps.put(feedTraits.key("feedType"),    feed.getFeedType());
					feedProps.put(feedTraits.key("description"), feed.getDescription());
					feedProps.put(feedTraits.key("items"),       newItems);
					feedProps.put(feedTraits.key("lastUpdated"), new Date());

					feed.setProperties(ctx, feedProps);
				}

			} catch (IOException | FeedException | IllegalArgumentException ex) {

				final Logger logger = LoggerFactory.getLogger(DataFeedTraitDefinition.class);
				logger.error("Error while trying to read feed '{}' ({}). {}: {}", remoteUrl, feed.getUuid(), ex.getClass().getSimpleName(), ex.getMessage());

			} catch (FrameworkException ex) {

				final Logger logger = LoggerFactory.getLogger(DataFeedTraitDefinition.class);
				logger.error("Error while trying to read feed at '{}' ({})", remoteUrl, feed.getUuid(), ex);
			}
		}

		if (cleanUp) {
			cleanUp(ctx, feed);
		}
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
