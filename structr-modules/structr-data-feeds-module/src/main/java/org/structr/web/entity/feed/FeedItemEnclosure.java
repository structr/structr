/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.FulltextIndexer;
import org.structr.common.fulltext.Indexable;
import static org.structr.common.fulltext.Indexable.contentType;
import static org.structr.common.fulltext.Indexable.extractedContent;
import static org.structr.common.fulltext.Indexable.indexedWords;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import static org.structr.core.GraphObject.type;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import static org.structr.core.graph.NodeInterface.owner;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.schema.SchemaService;
import org.structr.web.entity.relation.FeedItemContents;

/**
 * Represents feed enclosures
 */
public class FeedItemEnclosure extends AbstractNode implements Indexable {

    private static final Logger logger = LoggerFactory.getLogger(FeedItemContent.class.getName());

    public static final Property<String> url                     = new StringProperty("url");
    public static final Property<Long> enclosureLength                  = new LongProperty("enclosureLength");
    public static final Property<String> enclosureType             = new StringProperty("enclosureType");
    public static final Property<FeedItem> item                 = new StartNode<>("item", FeedItemContents.class);

    public static final View publicView = new View(FeedItemContent.class, PropertyView.Public, type, contentType, owner,
            url, enclosureLength, enclosureType, item);
    public static final View uiView     = new View(FeedItemContent.class, PropertyView.Ui, type, contentType, owner, extractedContent, indexedWords,
            url, enclosureLength, enclosureType, item);

	@Override
	public void afterCreation(SecurityContext securityContext) {

		try {
			final FulltextIndexer indexer = StructrApp.getInstance(securityContext).getFulltextIndexer();
			indexer.addToFulltextIndex(this);

		} catch (FrameworkException fex) {

			logger.warn("Unable to index " + this, fex);
		}
	}

	@Export
	@Override
	public GraphObject getSearchContext(final String searchTerm, final int contextLength) {

		final String text = getProperty(url);
		if (text != null) {

			final FulltextIndexer indexer = StructrApp.getInstance(securityContext).getFulltextIndexer();
			return indexer.getContextObject(searchTerm, text, contextLength);
		}

		return null;
	}

	@Override
	public InputStream getInputStream() {

		return IOUtils.toInputStream(getProperty(url));
	}


        static {
            SchemaService.registerBuiltinTypeOverride("FeedItemEnclosure", FeedItemEnclosure.class.getName());
        }
}
