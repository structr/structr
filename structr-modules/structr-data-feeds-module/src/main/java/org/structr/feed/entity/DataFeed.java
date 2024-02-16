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
package org.structr.feed.entity;


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
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonSchema.Cascade;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.SchemaService;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;



public interface DataFeed extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("DataFeed");
		final JsonObjectType item = (JsonObjectType)schema.addType("FeedItem");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/DataFeed"));

		type.addStringProperty("url",          PropertyView.Public, PropertyView.Ui).setIndexed(true).setRequired(true);
		type.addStringProperty("feedType",     PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("description",  PropertyView.Public, PropertyView.Ui).setIndexed(true);

		type.addLongProperty("updateInterval", PropertyView.Ui);  // update interval in milliseconds
		type.addDateProperty("lastUpdated",    PropertyView.Ui);  // last updated
		type.addLongProperty("maxAge",         PropertyView.Ui);  // maximum age of the oldest feed entry in milliseconds
		type.addIntegerProperty("maxItems",    PropertyView.Ui);  // maximum number of feed entries to retain

		type.addPropertyGetter("items",            Iterable.class);
		type.addPropertyGetter("url",              String.class);
		type.addPropertyGetter("feedType",         String.class);
		type.addPropertyGetter("description",      String.class);
		type.addPropertyGetter("updateInterval",   Long.class);
		type.addPropertyGetter("lastUpdated",      Date.class);
		type.addPropertyGetter("maxAge",           Long.class);
		type.addPropertyGetter("maxItems",         Integer.class);

		type.overrideMethod("onCreation", true,  "updateFeed(arg0, true);");

		type.addMethod("updateFeed")
			.addParameter("ctx", SecurityContext.class.getName())
			.setSource("updateFeed(ctx, true);")
			.setDoExport(true);

		type.addMethod("updateFeed")
			.addParameter("ctx", SecurityContext.class.getName())
			.addParameter("cleanUp", "boolean")
			.setSource(DataFeed.class.getName() + ".updateFeed(this, cleanUp, ctx);")
			.setDoExport(true);

		type.addMethod("cleanUp")
			.addParameter("ctx", SecurityContext.class.getName())
			.setSource(DataFeed.class.getName() + ".cleanUp(this, ctx);")
			.setDoExport(true);

		type.addMethod("updateIfDue")
			.addParameter("ctx", SecurityContext.class.getName())
			.setSource(DataFeed.class.getName() + ".updateIfDue(this, ctx);")
			.setDoExport(true);

		type.relate(item, "HAS_FEED_ITEMS", Cardinality.OneToMany, "feed", "items").setCascadingDelete(Cascade.sourceToTarget);

		// view configuration
		type.addViewProperty(PropertyView.Public, "items");
		type.addViewProperty(PropertyView.Ui, "items");
	}}

	void cleanUp(final SecurityContext ctx);
	void updateIfDue(final SecurityContext ctx);
	void updateFeed(final SecurityContext ctx);
	void updateFeed(final SecurityContext ctx, final boolean cleanItems);

	String getUrl();
	String getFeedType();
	String getDescription();
	Long getUpdateInterval();
	Date getLastUpdated();
	Long getMaxAge();
	Integer getMaxItems();

	Iterable<FeedItem> getItems();

	static void cleanUp(final DataFeed thisFeed, final SecurityContext ctx) {

		final Integer maxItemsToRetain = thisFeed.getMaxItems();
		final Long    maxItemAge       = thisFeed.getMaxAge();

		int i = 0;

		// Don't do anything if maxItems and maxAge are not set
		if (maxItemsToRetain != null || maxItemAge != null) {

			final List<FeedItem> feedItems  = Iterables.toList(thisFeed.getItems());
			final PropertyKey<Date> dateKey = StructrApp.key(FeedItem.class, "pubDate");

			// Sort by publication date, youngest items first
			feedItems.sort(dateKey.sorted(true));

			for (final FeedItem item : feedItems) {

				i++;

				final Date itemDate = item.getProperty(dateKey);

				if ((maxItemsToRetain != null && i > maxItemsToRetain) || (maxItemAge != null && itemDate.before(new Date(new Date().getTime() - maxItemAge)))) {

					try {

						StructrApp.getInstance(ctx).delete(item);

					} catch (FrameworkException ex) {
						final Logger logger = LoggerFactory.getLogger(DataFeed.class);
						logger.error("Error while deleting old/surplus feed item " + item, ex);
					}
				}
			}
		}
	}

	static void updateIfDue(final DataFeed thisFeed, final SecurityContext ctx) {

		final Date lastUpdate = thisFeed.getLastUpdated();
		final Long interval   = thisFeed.getUpdateInterval();

		if (lastUpdate == null || (interval != null && new Date().after(new Date(lastUpdate.getTime() + interval)))) {

			// Update feed and clean-up afterwards
			thisFeed.updateFeed(ctx, true);
		}

	}

	static void updateFeed(final DataFeed thisFeed, final boolean cleanUp, final SecurityContext ctx) {

		final String remoteUrl = thisFeed.getUrl();
		if (StringUtils.isNotBlank(remoteUrl)) {

			final App app = StructrApp.getInstance(ctx);

			try {

				final SyndFeedInput input              = new SyndFeedInput();

				try (final Reader reader = new XmlReader(HttpHelper.getAsStream(remoteUrl))) {

					final SyndFeed        feed    = input.build(reader);
					final List<SyndEntry> entries = feed.getEntries();

					final PropertyKey feedItemUrlKey             = StructrApp.key(FeedItem.class, "url");
					final PropertyKey feedItemPubDateKey         = StructrApp.key(FeedItem.class, "pubDate");
					final PropertyKey feedItemUpdatedDateKey     = StructrApp.key(FeedItem.class, "updatedDate");
					final PropertyKey feedItemNameKey            = StructrApp.key(FeedItem.class, "name");
					final PropertyKey feedItemAuthorKey          = StructrApp.key(FeedItem.class, "author");
					final PropertyKey feedItemCommentsKey        = StructrApp.key(FeedItem.class, "comments");
					final PropertyKey feedItemDescriptionKey     = StructrApp.key(FeedItem.class, "description");
					final PropertyKey feedItemContentsKey        = StructrApp.key(FeedItem.class, "contents");
					final PropertyKey feedItemEnclosuresKey      = StructrApp.key(FeedItem.class, "enclosures");
					final PropertyKey feedItemContentModeKey     = StructrApp.key(FeedItemContent.class, "mode");
					final PropertyKey feedItemContentItemTypeKey = StructrApp.key(FeedItemContent.class, "itemType");
					final PropertyKey feedItemContentValueKey    = StructrApp.key(FeedItemContent.class, "value");
					final PropertyKey feedItemEnclosureUrlKey    = StructrApp.key(FeedItemEnclosure.class, "url");
					final PropertyKey feedItemEnclosureLengthKey = StructrApp.key(FeedItemEnclosure.class, "enclosureLength");
					final PropertyKey feedItemEnclosureTypeKey   = StructrApp.key(FeedItemEnclosure.class, "enclosureType");

					final List<FeedItem> newItems = Iterables.toList(thisFeed.getItems());

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

							final List<FeedItemContent> itemContents     = new ArrayList<>();
							final List<FeedItemEnclosure> itemEnclosures = new ArrayList<>();

							// Get and add all contents
							final List<SyndContent> contents = entry.getContents();
							for (final SyndContent content : contents) {

								final FeedItemContent itemContent = app.create(FeedItemContent.class,
									new NodeAttribute(feedItemContentValueKey,    content.getValue()),
									new NodeAttribute(feedItemContentModeKey,     content.getMode()),
									new NodeAttribute(feedItemContentItemTypeKey, content.getType())
								);

								itemContents.add(itemContent);
							}

							// Get and add all enclosures
							final List<SyndEnclosure> enclosures = entry.getEnclosures();
							for (final SyndEnclosure enclosure : enclosures) {

								final FeedItemEnclosure itemEnclosure = app.create(FeedItemEnclosure.class,
									new NodeAttribute(feedItemEnclosureUrlKey,    enclosure.getUrl()),
									new NodeAttribute(feedItemEnclosureTypeKey,   enclosure.getType()),
									new NodeAttribute(feedItemEnclosureLengthKey, enclosure.getLength())
								);

								itemEnclosures.add(itemEnclosure);
							}

							props.put(feedItemContentsKey,   itemContents);
							props.put(feedItemEnclosuresKey, itemEnclosures);

							final FeedItem item = app.create(FeedItem.class, props);

							newItems.add(item);

							final Logger logger = LoggerFactory.getLogger(DataFeed.class);
							logger.debug("Created new item: {} ({}) ", entry.getTitle(), entry.getPublishedDate());
						}
					}

					final PropertyMap feedProps = new PropertyMap();

					if (StringUtils.isEmpty(thisFeed.getProperty(DataFeed.name))) {
						feedProps.put(DataFeed.name, feed.getTitle());
					}

					feedProps.put(StructrApp.key(DataFeed.class, "feedType"),    feed.getFeedType());
					feedProps.put(StructrApp.key(DataFeed.class, "description"), feed.getDescription());
					feedProps.put(StructrApp.key(DataFeed.class, "items"),       newItems);
					feedProps.put(StructrApp.key(DataFeed.class, "lastUpdated"), new Date());

					thisFeed.setProperties(ctx, feedProps);
				}

			} catch (IOException | FeedException | IllegalArgumentException ex) {

				final Logger logger = LoggerFactory.getLogger(DataFeed.class);
				logger.error("Error while trying to read feed '{}' ({}). {}: {}", remoteUrl, thisFeed.getUuid(), ex.getClass().getSimpleName(), ex.getMessage());

			} catch (FrameworkException ex) {

				final Logger logger = LoggerFactory.getLogger(DataFeed.class);
				logger.error("Error while trying to read feed at '{}' ({})", remoteUrl, thisFeed.getUuid(), ex);
			}
		}

		if (cleanUp) {
			thisFeed.cleanUp(ctx);
		}
	}
}
