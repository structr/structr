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
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.feed.entity.relationship.FeedItemFEED_ITEM_CONTENTSFeedItemContent;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Represents a content element of a feed item
 */
public class FeedItemContent extends AbstractFeedItem {

	public static final Property<FeedItem> itemProperty   = new StartNode<>("item", FeedItemFEED_ITEM_CONTENTSFeedItemContent.class);
	public static final Property<String> modeProperty     = new StringProperty("mode");
	public static final Property<String> itemTypeProperty = new StringProperty("itemType");
	public static final Property<String> valueProperty    = new StringProperty("value");

	public static final View defaultView = new View(FeedItemContent.class, PropertyView.Public,
		owner, modeProperty, itemTypeProperty, valueProperty
	);

	public static final View uiView      = new View(FeedItemContent.class, PropertyView.Ui,
		modeProperty, itemTypeProperty, valueProperty, itemProperty
	);

	@Override
	public void afterCreation(SecurityContext securityContext) throws FrameworkException {

		super.afterCreation(securityContext);
		updateIndex(securityContext);
	}

	@Override
	public InputStream getInputStream() {
		return IOUtils.toInputStream(getValue(), Charset.forName("utf-8"));
	}

	public String getValue() {
		return getProperty(valueProperty);
	}

	public void setValue(final String value) throws FrameworkException {
		setProperty(valueProperty, value);
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
