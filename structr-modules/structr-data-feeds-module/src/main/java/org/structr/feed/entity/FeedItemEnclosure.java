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

import org.apache.commons.io.IOUtils;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.FrameworkException;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.feed.entity.relationship.FeedItemFEED_ITEM_ENCLOSURESFeedItemEnclosure;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Represents feed enclosures
 */
public class FeedItemEnclosure extends AbstractFeedItem {

	public static final Property<FeedItem> itemProperty        = new StartNode<>("item", FeedItemFEED_ITEM_ENCLOSURESFeedItemEnclosure.class);
	public static final Property<String> urlProperty           = new StringProperty("url");
	public static final Property<Long> enclosureLengthProperty = new LongProperty("enclosureLength");
	public static final Property<String> enclosureTypeProperty = new StringProperty("enclosureType");

	public static final View defaultView = new View(FeedItemEnclosure.class, PropertyView.Public,
		urlProperty, enclosureLengthProperty, enclosureTypeProperty, itemProperty, owner
	);

	public static final View uiView      = new View(FeedItemEnclosure.class, PropertyView.Ui,
		urlProperty, enclosureLengthProperty, enclosureTypeProperty, itemProperty
	);

	@Override
	public void afterCreation(final SecurityContext securityContext) throws FrameworkException {

		super.afterCreation(securityContext);
		updateIndex(securityContext);
	}

	public String getUrl() {
		return getProperty(urlProperty);
	}

	@Override
	public InputStream getInputStream() {
		return IOUtils.toInputStream(getUrl(), Charset.forName("utf-8"));
	}

	@Override
	public String getExtractedContent() {
		return getProperty(extractedContentProperty);
	}

	@Override
	public String getContentType() {
		return getProperty(contentTypeProperty);
	}
}
