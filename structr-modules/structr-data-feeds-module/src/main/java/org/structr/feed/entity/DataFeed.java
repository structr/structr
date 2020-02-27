/**
 * Copyright (C) 2010-2020 Structr GmbH
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
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.schema.SchemaService;



public interface DataFeed extends NodeInterface {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("DataFeed");
		final JsonObjectType item = (JsonObjectType)schema.addType("FeedItem");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/DataFeed"));

		type.addStringProperty("url",          PropertyView.Public, PropertyView.Ui).setIndexed(true);
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

		type.relate(item, "HAS_FEED_ITEMS", Cardinality.OneToMany, "feed", "items");

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

				final PropertyKey<Date> dateKey  = StructrApp.key(FeedItem.class, "pubDate");
				final PropertyKey<String> urlKey = StructrApp.key(FeedItem.class, "url");
                                final URL remote                 = new URL(remoteUrl);
                                final SyndFeedInput input        = new SyndFeedInput();

				try (final Reader reader = new XmlReader(remote)) {

					final SyndFeed      feed      = input.build(reader);
					final List<SyndEntry> entries = feed.getEntries();

					thisFeed.setProperty(StructrApp.key(DataFeed.class, "feedType"),    feed.getFeedType());
					thisFeed.setProperty(StructrApp.key(DataFeed.class, "description"), feed.getDescription());

					final List<FeedItem> newItems = Iterables.toList(thisFeed.getItems());

					for (final SyndEntry entry : entries) {

						final PropertyMap props = new PropertyMap();

						final String link = entry.getLink();

						// Check if item with this link already exists
						if (app.nodeQuery(FeedItem.class).and(urlKey, link).getFirst() == null) {

							props.put(urlKey,                                        entry.getLink());
							props.put(StructrApp.key(FeedItem.class, "name"),        entry.getTitle());
							props.put(StructrApp.key(FeedItem.class, "author"),      entry.getAuthor());
							props.put(StructrApp.key(FeedItem.class, "comments"),    entry.getComments());

							if(entry.getDescription() != null) {
								props.put(StructrApp.key(FeedItem.class, "description"), entry.getDescription().getValue());
							}

							final FeedItem item = app.create(FeedItem.class, props);
							item.setProperty(dateKey, entry.getPublishedDate());

							final List<FeedItemContent> itemContents = new LinkedList<>();
							final List<FeedItemEnclosure> itemEnclosures = new LinkedList<>();

							//Get and add all contents
							final List<SyndContent> contents = entry.getContents();
							for (final SyndContent content : contents) {

								final FeedItemContent itemContent = app.create(FeedItemContent.class);
								itemContent.setValue(content.getValue());

								itemContents.add(itemContent);
							}

							//Get and add all enclosures
							final List<SyndEnclosure> enclosures = entry.getEnclosures();
							for (final SyndEnclosure enclosure : enclosures){

								final FeedItemEnclosure itemEnclosure = app.create(FeedItemEnclosure.class);

								itemEnclosure.setProperty(StructrApp.key(FeedItemEnclosure.class, "url"),             enclosure.getUrl());
								itemEnclosure.setProperty(StructrApp.key(FeedItemEnclosure.class, "enclosureLength"), enclosure.getLength());
								itemEnclosure.setProperty(StructrApp.key(FeedItemEnclosure.class, "enclosureType"),   enclosure.getType());

								itemEnclosures.add(itemEnclosure);
							}

							item.setProperty(StructrApp.key(FeedItem.class, "contents"),   itemContents);
							item.setProperty(StructrApp.key(FeedItem.class, "enclosures"), itemEnclosures);

							newItems.add(item);

							logger.debug("Created new item: {} ({}) ", item.getProperty(FeedItem.name), item.getProperty(dateKey));
						}
					}

					thisFeed.setProperty(StructrApp.key(DataFeed.class, "items"),       newItems);
					thisFeed.setProperty(StructrApp.key(DataFeed.class, "lastUpdated"), new Date());
				}

			} catch (IllegalArgumentException | IOException | FeedException | FrameworkException ex) {
				logger.error("Error while updating feed", ex);
			}
		}

		if (cleanUp) {
			thisFeed.cleanUp(ctx);
		}
	}
}
