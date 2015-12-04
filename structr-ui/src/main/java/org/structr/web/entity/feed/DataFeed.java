/**
 * Copyright (C) 2010-2015 Structr GmbH
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
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.relation.FeedItems;

/**
 * Represents a data feed as a collection of feed items.
 * 
 */


public class DataFeed extends AbstractNode {

	public static final Property<List<FeedItem>> items  = new EndNodes<>("items", FeedItems.class);
	public static final Property<String>         url    = new StringProperty("url");
	
	public static final View defaultView = new View(DataFeed.class, PropertyView.Public, id, type, url, items);

	public static final View uiView = new View(DataFeed.class, PropertyView.Ui,
		id, name, owner, type, createdBy, deleted, hidden, createdDate, lastModifiedDate, visibleToPublicUsers, visibleToAuthenticatedUsers, visibilityStartDate, visibilityEndDate,
                url, items
	);
        
	
	@Override
	public void afterCreation(final SecurityContext securityContext) {

		try {

			updateFeedItems(securityContext);
			
			
		} catch (Throwable t) {

		}

	}
	
	private void updateFeedItems(final SecurityContext securityContext) {
		
		final String remoteUrl = getProperty(url);
		if (StringUtils.isNotBlank(remoteUrl)) {
			
			final App app = StructrApp.getInstance(securityContext);
			
			try {
				
				final FeedFetcher feedFetcher = new HttpURLFeedFetcher();
				final SyndFeed feed = feedFetcher.retrieveFeed(new URL(remoteUrl));
				final List<SyndEntry> entries = feed.getEntries();
				
				final List<FeedItem> newItems = new LinkedList<>();
				
				for (final SyndEntry entry : entries) {
					
					final String title = entry.getTitle();
					final String link  = entry.getLink();
					
					final FeedItem item = app.create(FeedItem.class, title);
					item.setProperty(FeedItem.url, link);
										
					newItems.add(item);
                                        
                                        setProperty(items, newItems);
										
				}
				
				
			} catch (IllegalArgumentException | IOException | FetcherException | FeedException | FrameworkException ex) {
				Logger.getLogger(DataFeed.class.getName()).log(Level.SEVERE, null, ex);
			}
			
			
			
		}		
	}
	
}
