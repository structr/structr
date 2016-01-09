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
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.files.text.FulltextIndexingTask;
import org.structr.web.common.DownloadHelper;
import org.structr.web.entity.relation.FeedItemContents;

/**
 * Represents a content element of a feed item
 *
 */
public class FeedItemContent extends AbstractNode implements Indexable {

	private static final Logger logger = Logger.getLogger(FeedItemContent.class.getName());

	public static final Property<String> mode                    = new StringProperty("mode");
	public static final Property<String> itemType                = new StringProperty("itemType");
	public static final Property<String> value                   = new StringProperty("value");
	public static final Property<FeedItem> item                  = new StartNode<>("item", FeedItemContents.class);
	
	public static final View publicView = new View(FeedItemContent.class, PropertyView.Public, type, contentType, owner, 
		mode, itemType, value, item);
	public static final View uiView     = new View(FeedItemContent.class, PropertyView.Ui, type, contentType, owner, extractedContent, indexedWords,
		mode, itemType, value, item);

	@Override
	public void afterCreation(SecurityContext securityContext) {

		try {

			StructrApp.getInstance(securityContext).processTasks(new FulltextIndexingTask(this));
			
		} catch (Throwable t) {

		}

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

	@Override
	public InputStream getInputStream() {
		
		return IOUtils.toInputStream(getProperty(value));
	}

	// ----- private methods -----

}
