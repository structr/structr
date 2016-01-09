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

import org.structr.web.entity.*;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import static org.structr.core.graph.NodeInterface.name;
import org.structr.core.property.EndNodes;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.core.validator.SimpleNonEmptyValueValidator;
import org.structr.core.validator.TypeUniquenessValidator;
import org.structr.files.text.FulltextIndexingTask;
import org.structr.web.common.DownloadHelper;
import org.structr.web.entity.relation.FeedItemContents;
import org.structr.web.entity.relation.FeedItems;

/**
 * Represents a single item of a data feed.
 *
 */
public class FeedItem extends AbstractNode implements Indexable {

	private static final Logger logger = Logger.getLogger(FeedItem.class.getName());

	public static final Property<String> url                     = new StringProperty("url").unique().indexed();
	public static final Property<String> author                  = new StringProperty("author");
	public static final Property<String> comments                = new StringProperty("comments");
	public static final Property<List<FeedItemContent>> contents = new EndNodes<>("contents", FeedItemContents.class);
	public static final Property<Date> pubDate                   = new ISO8601DateProperty("pubDate").indexed().unvalidated();	
	
	public static final Property<Long> checksum                  = new LongProperty("checksum").indexed().unvalidated().readOnly();
	public static final Property<Integer> cacheForSeconds        = new IntProperty("cacheForSeconds").cmis();
	public static final Property<Integer> version                = new IntProperty("version").indexed().readOnly();

	public static final Property<DataFeed> feed                  = new StartNode<>("feed", FeedItems.class);
	
	public static final View publicView = new View(FeedItem.class, PropertyView.Public, type, name, contentType, owner, 
		url, feed, author, comments, contents, pubDate);
	public static final View uiView     = new View(FeedItem.class, PropertyView.Ui, type, contentType, checksum, version, cacheForSeconds, owner, extractedContent, indexedWords,
		url, feed, author, comments, contents, pubDate);

	
	static {

		FeedItem.url.addValidator(new SimpleNonEmptyValueValidator(FeedItem.class));
		FeedItem.url.addValidator(new TypeUniquenessValidator(FeedItem.class));
	}

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {
		StructrApp.getInstance(securityContext).processTasks(new FulltextIndexingTask(this));
		return super.onCreation(securityContext, errorBuffer);
	}
	
	
		
	
	@Override
	public void afterCreation(SecurityContext securityContext) {
		StructrApp.getInstance(securityContext).processTasks(new FulltextIndexingTask(this));
	}

	@Export
	@Override
	public GraphObject getSearchContext(final String searchTerm, final int contextLength) {

		final String text = getProperty(extractedContent);
		if (text != null) {

			return DownloadHelper.getContextObject(searchTerm, text, contextLength);
		}

		return null;
	}

	public void increaseVersion() throws FrameworkException {

		final Integer _version = getProperty(FeedItem.version);

		unlockReadOnlyPropertiesOnce();
		if (_version == null) {

			setProperty(FeedItem.version, 1);

		} else {

			setProperty(FeedItem.version, _version + 1);
		}
	}

	@Override
	public InputStream getInputStream() {
		
		final String remoteUrl = getProperty(url);
		if (StringUtils.isNotBlank(remoteUrl)) {
			
			return DownloadHelper.getInputStream(remoteUrl);
		}
		
		return null;
	}

	// ----- private methods -----

}
