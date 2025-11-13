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
package org.structr.feed.traits.wrappers;

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
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.feed.entity.DataFeed;
import org.structr.feed.traits.definitions.DataFeedTraitDefinition;
import org.structr.feed.traits.definitions.FeedItemContentTraitDefinition;
import org.structr.feed.traits.definitions.FeedItemEnclosureTraitDefinition;
import org.structr.feed.traits.definitions.FeedItemTraitDefinition;
import org.structr.rest.common.HttpHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class DataFeedTraitWrapper extends AbstractNodeTraitWrapper implements DataFeed {

	public DataFeedTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	public String getUrl() {
		return wrappedObject.getProperty(traits.key(DataFeedTraitDefinition.URL_PROPERTY));
	}

	public String getFeedType() {
		return wrappedObject.getProperty(traits.key(DataFeedTraitDefinition.FEED_TYPE_PROPERTY));
	}

	public String getDescription() {
		return wrappedObject.getProperty(traits.key(DataFeedTraitDefinition.DESCRIPTION_PROPERTY));
	}

	public Long getUpdateInterval() {
		return wrappedObject.getProperty(traits.key(DataFeedTraitDefinition.UPDATE_INTERVAL_PROPERTY));
	}

	public Date getLastUpdated() {
		return wrappedObject.getProperty(traits.key(DataFeedTraitDefinition.LAST_UPDATED_PROPERTY));
	}

	public Long getMaxAge() {
		return wrappedObject.getProperty(traits.key(DataFeedTraitDefinition.MAX_AGE_PROPERTY));
	}

	public Integer getMaxItems() {
		return wrappedObject.getProperty(traits.key(DataFeedTraitDefinition.MAX_ITEMS_PROPERTY));
	}

	public Iterable<NodeInterface> getItems() {
		return wrappedObject.getProperty(traits.key(DataFeedTraitDefinition.ITEMS_PROPERTY));
	}

	@Override
	public void cleanUp(final SecurityContext ctx) {

		final Integer maxItemsToRetain = this.getMaxItems();
		final Long    maxItemAge       = this.getMaxAge();

		int i = 0;

		// Don't do anything if maxItems and maxAge are not set
		if (maxItemsToRetain != null || maxItemAge != null) {

			final List<NodeInterface> feedItems = Iterables.toList(this.getItems());
			final Traits traits                 = Traits.of(StructrTraits.FEED_ITEM);
			final PropertyKey<Date> dateKey     = traits.key(FeedItemTraitDefinition.PUB_DATE_PROPERTY);

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

	@Override
	public void updateIfDue(final SecurityContext ctx) {

		final Date lastUpdate = this.getLastUpdated();
		final Long interval   = this.getUpdateInterval();

		if (lastUpdate == null || (interval != null && new Date().after(new Date(lastUpdate.getTime() + interval)))) {

			// Update feed and clean-up afterward
			updateFeed(ctx, true);
		}

	}

	@Override
	public void updateFeed(final SecurityContext ctx) {
		updateFeed(ctx, true);
	}

	@Override
	public void updateFeed(final SecurityContext ctx, final boolean cleanUp) {

		final String remoteUrl = this.getUrl();
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
					throw new FrameworkException(422, "Could not get input stream for feed " + this.getUuid());
				}

				try (final Reader reader = new XmlReader(inputStream)) {

					final SyndFeed syndFeed = input.build(reader);
					final List<SyndEntry> entries  = syndFeed.getEntries();
					final Traits enclosureTraits   = Traits.of(StructrTraits.FEED_ITEM_ENCLOSURE);
					final Traits contentTraits     = Traits.of(StructrTraits.FEED_ITEM_CONTENT);
					final Traits itemTraits        = Traits.of(StructrTraits.FEED_ITEM);

					final PropertyKey feedItemUrlKey             = itemTraits.key(FeedItemTraitDefinition.URL_PROPERTY);
					final PropertyKey feedItemPubDateKey         = itemTraits.key(FeedItemTraitDefinition.PUB_DATE_PROPERTY);
					final PropertyKey feedItemUpdatedDateKey     = itemTraits.key(FeedItemTraitDefinition.UPDATED_DATE_PROPERTY);
					final PropertyKey feedItemNameKey            = itemTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);
					final PropertyKey feedItemAuthorKey          = itemTraits.key(FeedItemTraitDefinition.AUTHOR_PROPERTY);
					final PropertyKey feedItemCommentsKey        = itemTraits.key(FeedItemTraitDefinition.COMMENTS_PROPERTY);
					final PropertyKey feedItemDescriptionKey     = itemTraits.key(FeedItemTraitDefinition.DESCRIPTION_PROPERTY);
					final PropertyKey feedItemContentsKey        = itemTraits.key(FeedItemTraitDefinition.CONTENTS_PROPERTY);
					final PropertyKey feedItemEnclosuresKey      = itemTraits.key(FeedItemTraitDefinition.ENCLOSURES_PROPERTY);
					final PropertyKey feedItemContentModeKey     = contentTraits.key(FeedItemContentTraitDefinition.MODE_PROPERTY);
					final PropertyKey feedItemContentItemTypeKey = contentTraits.key(FeedItemContentTraitDefinition.ITEM_TYPE_PROPERTY);
					final PropertyKey feedItemContentValueKey    = contentTraits.key(FeedItemContentTraitDefinition.VALUE_PROPERTY);
					final PropertyKey feedItemEnclosureUrlKey    = enclosureTraits.key(FeedItemEnclosureTraitDefinition.URL_PROPERTY);
					final PropertyKey feedItemEnclosureLengthKey = enclosureTraits.key(FeedItemEnclosureTraitDefinition.ENCLOSURE_LENGTH_PROPERTY);
					final PropertyKey feedItemEnclosureTypeKey   = enclosureTraits.key(FeedItemEnclosureTraitDefinition.ENCLOSURE_TYPE_PROPERTY);

					final List<NodeInterface> newItems = Iterables.toList(this.getItems());

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

								final NodeInterface itemContent = app.create(StructrTraits.FEED_ITEM_CONTENT,
									new NodeAttribute(feedItemContentValueKey,    content.getValue()),
									new NodeAttribute(feedItemContentModeKey,     content.getMode()),
									new NodeAttribute(feedItemContentItemTypeKey, content.getType())
								);

								itemContents.add(itemContent);
							}

							// Get and add all enclosures
							final List<SyndEnclosure> enclosures = entry.getEnclosures();
							for (final SyndEnclosure enclosure : enclosures) {

								final NodeInterface itemEnclosure = app.create(StructrTraits.FEED_ITEM_ENCLOSURE,
									new NodeAttribute(feedItemEnclosureUrlKey,    enclosure.getUrl()),
									new NodeAttribute(feedItemEnclosureTypeKey,   enclosure.getType()),
									new NodeAttribute(feedItemEnclosureLengthKey, enclosure.getLength())
								);

								itemEnclosures.add(itemEnclosure);
							}

							props.put(feedItemContentsKey,   itemContents);
							props.put(feedItemEnclosuresKey, itemEnclosures);

							final NodeInterface item = app.create(StructrTraits.FEED_ITEM, props);

							newItems.add(item);

							final Logger logger = LoggerFactory.getLogger(DataFeedTraitDefinition.class);
							logger.debug("Created new item: {} ({}) ", entry.getTitle(), entry.getPublishedDate());
						}
					}

					final PropertyMap feedProps = new PropertyMap();
					final Traits feedTraits     = Traits.of(StructrTraits.DATA_FEED);
					final PropertyKey<String> nameKey = feedTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY);

					if (StringUtils.isEmpty(this.getProperty(nameKey))) {
						feedProps.put(nameKey, syndFeed.getTitle());
					}

					feedProps.put(feedTraits.key(DataFeedTraitDefinition.FEED_TYPE_PROPERTY),    syndFeed.getFeedType());
					feedProps.put(feedTraits.key(DataFeedTraitDefinition.DESCRIPTION_PROPERTY),  syndFeed.getDescription());
					feedProps.put(feedTraits.key(DataFeedTraitDefinition.ITEMS_PROPERTY),        newItems);
					feedProps.put(feedTraits.key(DataFeedTraitDefinition.LAST_UPDATED_PROPERTY), new Date());

					this.setProperties(ctx, feedProps);
				}

			} catch (IOException | FeedException | IllegalArgumentException ex) {

				final Logger logger = LoggerFactory.getLogger(DataFeedTraitDefinition.class);
				logger.error("Error while trying to read feed '{}' ({}). {}: {}", remoteUrl, this.getUuid(), ex.getClass().getSimpleName(), ex.getMessage());

			} catch (FrameworkException ex) {

				final Logger logger = LoggerFactory.getLogger(DataFeedTraitDefinition.class);
				logger.error("Error while trying to read feed at '{}' ({})", remoteUrl, this.getUuid(), ex);
			}
		}

		if (cleanUp) {
			cleanUp(ctx);
		}
	}
}
