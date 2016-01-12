/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.web.entity.feed;

import com.rometools.fetcher.FeedFetcher;
import com.rometools.fetcher.FetcherException;
import com.rometools.fetcher.impl.HttpURLFeedFetcher;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.GraphObjectComparator;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.relation.FeedItems;

/**
 * Represents a data feed as a collection of feed items.
 * 
 */


public class DataFeed extends AbstractNode {

	private static final Logger logger = Logger.getLogger(DataFeed.class.getName());
	
	public static final Property<List<FeedItem>> items          = new EndNodes<>("items", FeedItems.class);
	public static final Property<String>         url            = new StringProperty("url").indexed();
	public static final Property<String>         feedType       = new StringProperty("feedType").indexed();
	public static final Property<String>         description    = new StringProperty("description").indexed();
	public static final Property<Long>           updateInterval = new LongProperty("updateInterval"); // update interval in milliseconds
	public static final Property<Date>           lastUpdated    = new ISO8601DateProperty("lastUpdated");
	public static final Property<Long>           maxAge         = new LongProperty("maxAge"); // maximum age of the oldest feed entry in milliseconds
	public static final Property<Integer>        maxItems       = new IntProperty("maxItems"); // maximum number of feed entries to retain
	
	public static final View defaultView = new View(DataFeed.class, PropertyView.Public, id, type, url, items, feedType, description);

	public static final View uiView = new View(DataFeed.class, PropertyView.Ui,
		id, name, owner, type, createdBy, deleted, hidden, createdDate, lastModifiedDate, visibleToPublicUsers, visibleToAuthenticatedUsers, visibilityStartDate, visibilityEndDate,
                url, items, feedType, description, lastUpdated, maxAge, maxItems
	);

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		updateFeed(true);
		return super.onCreation(securityContext, errorBuffer);
	}

	/**
	 * Clean-up feed items which are either too old or too many.
	 */
	@Export
	public void cleanUp() {
		
		final Integer maxItemsToRetain = getProperty(maxItems);
		final Long    maxItemAge       = getProperty(maxAge);
		
		int i = 0;

		// Don't do anything if maxItems and maxAge are not set
		if (maxItemsToRetain != null || maxItemAge != null) {
			
			final List<FeedItem> feedItems = getProperty(items);
			
			// Sort by publication date, youngest items first
			feedItems.sort(new GraphObjectComparator(FeedItem.pubDate, GraphObjectComparator.DESCENDING));

			for (final FeedItem item : feedItems) {
				
				i++;

				final Date itemDate = item.getProperty(FeedItem.pubDate);
				
				if ((maxItemsToRetain != null && i > maxItemsToRetain) || (maxItemAge != null && itemDate.before(new Date(new Date().getTime() - maxItemAge)))) {
					
					try {
						StructrApp.getInstance().delete(item);
						
					} catch (FrameworkException ex) {
						logger.log(Level.SEVERE, "Error while deleting old/surplus feed item " + item, ex);
					}
				}
			}
			
		}
		
	}
	
	/**
	 * Update the feed only if it was last updated before the update interval.
	 */
	@Export
	public void updateIfDue() {
		
		final Date lastUpdate = getProperty(lastUpdated);
		final Long interval   = getProperty(updateInterval);
		
		if ((lastUpdate == null || interval != null) && new Date().after(new Date(lastUpdate.getTime() + interval))) {
			
			// Update feed and clean-up afterwards
			updateFeed(true);
			
		}
		
	}
	@Export
	public void updateFeed() {
		updateFeed(true);
	}
	
	/**
	 * Update the feed from the given URL.
	 * 
	 * @param cleanUp	Clean-up old items after update
	 */
	@Export
	public void updateFeed(final boolean cleanUp) {
		
		final String remoteUrl = getProperty(url);
		if (StringUtils.isNotBlank(remoteUrl)) {
			
			final App app = StructrApp.getInstance(securityContext);
			
			try {
				
				final FeedFetcher     feedFetcher = new HttpURLFeedFetcher();
				final SyndFeed        feed        = feedFetcher.retrieveFeed(new URL(remoteUrl));
				final List<SyndEntry> entries     = feed.getEntries();
				
				setProperty(feedType,    feed.getFeedType());
				setProperty(description, feed.getDescription());
				
				final List<FeedItem> newItems = getProperty(items);
				
				for (final SyndEntry entry : entries) {
					 
					final PropertyMap props = new PropertyMap();

					final String link = entry.getLink();
					
					// Check if item with this link already exists
					if (app.nodeQuery(FeedItem.class).and(FeedItem.url, link).getFirst() == null) {
					
						props.put(FeedItem.url, entry.getLink());
						props.put(FeedItem.name, entry.getTitle());
						props.put(FeedItem.author, entry.getAuthor());
						props.put(FeedItem.comments, entry.getComments());

						final FeedItem item = app.create(FeedItem.class, props);
						item.setProperty(FeedItem.pubDate, entry.getPublishedDate());
						
						final List<FeedItemContent> itemContents = new LinkedList<>();

						final List<SyndContent> contents = entry.getContents();
						for (final SyndContent content : contents) {

							final FeedItemContent itemContent = app.create(FeedItemContent.class);
							itemContent.setProperty(FeedItemContent.value, content.getValue());

							itemContents.add(itemContent);
						}

						item.setProperty(FeedItem.contents, itemContents);

						newItems.add(item);

						logger.log(Level.FINE, "Created new item: {0} ({1}) ", new Object[]{item.getProperty(FeedItem.name), item.getProperty(FeedItem.pubDate)});

					}
				}

				setProperty(items, newItems);
				setProperty(lastUpdated, new Date());
				
			} catch (IllegalArgumentException | IOException | FetcherException | FeedException | FrameworkException ex) {
				logger.log(Level.SEVERE, "Error while updating feed", ex);
			}
			
		}
		
		if (cleanUp) {
			cleanUp();
		}
	}
	
}
