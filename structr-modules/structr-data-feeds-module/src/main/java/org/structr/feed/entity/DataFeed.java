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
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.Export;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.*;
import org.structr.feed.entity.relationship.DataFeedHAS_FEED_ITEMSFeedItem;
import org.structr.rest.common.HttpHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class DataFeed extends AbstractNode {

	public static final Property<Iterable<FeedItem>> itemsProperty = new EndNodes<>("items", DataFeedHAS_FEED_ITEMSFeedItem.class);

	public static final Property<String> urlProperty         = new StringProperty("url").indexed().notNull();
	public static final Property<String> feedTypeProperty    = new StringProperty("feedType");
	public static final Property<String> descriptionProperty = new StringProperty("description");

	public static final Property<Long> updateIntervalProperty = new LongProperty("updateInterval");
	public static final Property<Date> lastUpdatedProperty    = new DateProperty("lastUpdated");
	public static final Property<Long> maxAgeProperty         = new LongProperty("maxAge");
	public static final Property<Integer> maxItemsProperty    = new IntProperty("maxItems");

	public static final View defaultView = new View(DataFeed.class, PropertyView.Public,
		urlProperty, feedTypeProperty, descriptionProperty, itemsProperty
	);

	public static final View uiView = new View(DataFeed.class, PropertyView.Ui,
		descriptionProperty, feedTypeProperty, maxAgeProperty, urlProperty, updateIntervalProperty,
		lastUpdatedProperty, maxItemsProperty, maxItemsProperty, itemsProperty
	);

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidPropertyNotNull(this, DataFeed.urlProperty, errorBuffer);

		return valid;
	}

	@Override
	public void onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);
		updateFeed(securityContext, true);
	}

	public String getUrl() {
		return getProperty(urlProperty);
	}

	public String getFeedType() {
		return getProperty(feedTypeProperty);
	}

	public String getDescription() {
		return getProperty(descriptionProperty);
	}

	public Long getUpdateInterval() {
		return getProperty(updateIntervalProperty);
	}

	public Date getLastUpdated() {
		return getProperty(lastUpdatedProperty);
	}

	public Long getMaxAge() {
		return getProperty(maxAgeProperty);
	}

	public Integer getMaxItems() {
		return getProperty(maxItemsProperty);
	}

	public Iterable<FeedItem> getItems() {
		return getProperty(itemsProperty);
	}

	@Export
	public void cleanUp(final SecurityContext ctx) {

		final Integer maxItemsToRetain = this.getMaxItems();
		final Long    maxItemAge       = this.getMaxAge();

		int i = 0;

		// Don't do anything if maxItems and maxAge are not set
		if (maxItemsToRetain != null || maxItemAge != null) {

			final List<FeedItem> feedItems  = Iterables.toList(this.getItems());
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

	@Export
	public void updateIfDue(final SecurityContext ctx) {

		final Date lastUpdate = this.getLastUpdated();
		final Long interval   = this.getUpdateInterval();

		if (lastUpdate == null || (interval != null && new Date().after(new Date(lastUpdate.getTime() + interval)))) {

			// Update feed and clean-up afterwards
			this.updateFeed(ctx, true);
		}

	}

	@Export
	public void updateFeed(final SecurityContext ctx) {
		updateFeed(ctx, true);
	}

	@Export
	public void updateFeed(final SecurityContext ctx, final boolean cleanUp) {

		final String remoteUrl = this.getUrl();
		if (StringUtils.isNotBlank(remoteUrl)) {

			final App app = StructrApp.getInstance(ctx);

			try {

				final SyndFeedInput input              = new SyndFeedInput();

				InputStream inputStream = null;
				final Map<String, Object> responseData =  HttpHelper.getAsStream(remoteUrl);
				if (responseData != null && responseData.containsKey(HttpHelper.FIELD_BODY) && responseData.get(HttpHelper.FIELD_BODY) instanceof InputStream) {

					inputStream =  (InputStream) responseData.get(HttpHelper.FIELD_BODY);
				}

				if (inputStream == null) {
					throw new FrameworkException(422, "Could not get input stream for feed " + getUuid());
				}

				try (final Reader reader = new XmlReader(inputStream)) {

					final SyndFeed        feed    = input.build(reader);
					final List<SyndEntry> entries = feed.getEntries();

					final PropertyKey feedItemUrlKey             = FeedItem.urlProperty;
					final PropertyKey feedItemPubDateKey         = FeedItem.pubDateProperty;
					final PropertyKey feedItemUpdatedDateKey     = FeedItem.updatedDateProperty;
					final PropertyKey feedItemNameKey            = FeedItem.name;
					final PropertyKey feedItemAuthorKey          = FeedItem.authorProperty;
					final PropertyKey feedItemCommentsKey        = FeedItem.commentsProperty;
					final PropertyKey feedItemDescriptionKey     = FeedItem.descriptionProperty;
					final PropertyKey feedItemContentsKey        = FeedItem.contentsProperty;
					final PropertyKey feedItemEnclosuresKey      = FeedItem.enclosuresProperty;
					final PropertyKey feedItemContentModeKey     = FeedItemContent.modeProperty;
					final PropertyKey feedItemContentItemTypeKey = FeedItemContent.itemTypeProperty;
					final PropertyKey feedItemContentValueKey    = FeedItemContent.valueProperty;
					final PropertyKey feedItemEnclosureUrlKey    = FeedItemEnclosure.urlProperty;
					final PropertyKey feedItemEnclosureLengthKey = FeedItemEnclosure.enclosureLengthProperty;
					final PropertyKey feedItemEnclosureTypeKey   = FeedItemEnclosure.enclosureTypeProperty;

					final List<FeedItem> newItems = Iterables.toList(this.getItems());

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

							final List<FeedItemContent> itemContents     = new LinkedList<>();
							final List<FeedItemEnclosure> itemEnclosures = new LinkedList<>();

							// Get and add all contents
							final List<SyndContent> contents = entry.getContents();
							for (final SyndContent content : contents) {

								final FeedItemContent itemContent = app.create("FeedItemContent",
									new NodeAttribute(feedItemContentValueKey,    content.getValue()),
									new NodeAttribute(feedItemContentModeKey,     content.getMode()),
									new NodeAttribute(feedItemContentItemTypeKey, content.getType())
								);

								itemContents.add(itemContent);
							}

							// Get and add all enclosures
							final List<SyndEnclosure> enclosures = entry.getEnclosures();
							for (final SyndEnclosure enclosure : enclosures) {

								final FeedItemEnclosure itemEnclosure = app.create("FeedItemEnclosure",
									new NodeAttribute(feedItemEnclosureUrlKey,    enclosure.getUrl()),
									new NodeAttribute(feedItemEnclosureTypeKey,   enclosure.getType()),
									new NodeAttribute(feedItemEnclosureLengthKey, enclosure.getLength())
								);

								itemEnclosures.add(itemEnclosure);
							}

							props.put(feedItemContentsKey,   itemContents);
							props.put(feedItemEnclosuresKey, itemEnclosures);

							final FeedItem item = app.create("FeedItem", props);

							newItems.add(item);

							final Logger logger = LoggerFactory.getLogger(DataFeed.class);
							logger.debug("Created new item: {} ({}) ", entry.getTitle(), entry.getPublishedDate());
						}
					}

					final PropertyMap feedProps = new PropertyMap();

					if (StringUtils.isEmpty(this.getProperty(DataFeed.name))) {
						feedProps.put(DataFeed.name, feed.getTitle());
					}

					feedProps.put(DataFeed.feedTypeProperty,    feed.getFeedType());
					feedProps.put(DataFeed.descriptionProperty, feed.getDescription());
					feedProps.put(DataFeed.itemsProperty,       newItems);
					feedProps.put(DataFeed.lastUpdatedProperty, new Date());

					this.setProperties(ctx, feedProps);
				}

			} catch (IOException | FeedException | IllegalArgumentException ex) {

				final Logger logger = LoggerFactory.getLogger(DataFeed.class);
				logger.error("Error while trying to read feed '{}' ({}). {}: {}", remoteUrl, this.getUuid(), ex.getClass().getSimpleName(), ex.getMessage());

			} catch (FrameworkException ex) {

				final Logger logger = LoggerFactory.getLogger(DataFeed.class);
				logger.error("Error while trying to read feed at '{}' ({})", remoteUrl, this.getUuid(), ex);
			}
		}

		if (cleanUp) {
			this.cleanUp(ctx);
		}
	}
}
