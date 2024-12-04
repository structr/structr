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

import org.apache.commons.lang3.StringUtils;
import org.structr.api.config.Settings;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.ValidationHelper;
import org.structr.core.app.StructrApp;
import org.structr.core.property.*;
import org.structr.feed.entity.relationship.DataFeedHAS_FEED_ITEMSFeedItem;
import org.structr.feed.entity.relationship.FeedItemFEED_ITEM_CONTENTSFeedItemContent;
import org.structr.feed.entity.relationship.FeedItemFEED_ITEM_ENCLOSURESFeedItemEnclosure;
import org.structr.rest.common.HttpHelper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Date;
/**
 * Represents a single item of a data feed.
 *
 */
public class FeedItem extends AbstractFeedItem {

	public static final Property<Iterable<FeedItemContent>> contentsProperty     = new EndNodes<>("contents", FeedItemFEED_ITEM_CONTENTSFeedItemContent.class);
	public static final Property<Iterable<FeedItemEnclosure>> enclosuresProperty = new EndNodes<>("enclosures", FeedItemFEED_ITEM_ENCLOSURESFeedItemEnclosure.class);
	public static final Property<DataFeed> feedProperty                          = new StartNode<>("feed", DataFeedHAS_FEED_ITEMSFeedItem.class);

	public static final Property<String> urlProperty              = new StringProperty("url").indexed().notNull();
	public static final Property<String> authorProperty           = new StringProperty("author");
	public static final Property<String> commentsProperty         = new StringProperty("comments");
	public static final Property<String> descriptionProperty      = new StringProperty("description");
	public static final Property<Date> pubDateProperty            = new DateProperty("pubDate");
	public static final Property<Date> updatedDateProperty        = new DateProperty("updatedDate");
	public static final Property<Long> checksumProperty           = new LongProperty("checksum").readOnly();
	public static final Property<Integer> cacheForSecondsProperty = new IntProperty("cacheForSeconds");
	public static final Property<Integer> versionProperty         = new IntProperty("version").readOnly();

	public static final View defaultView = new View(FeedItem.class, PropertyView.Public,
		owner, name, urlProperty, authorProperty, commentsProperty, descriptionProperty, pubDateProperty,
		updatedDateProperty, contentsProperty, enclosuresProperty
	);

	public static final View uiView = new View(FeedItem.class, PropertyView.Ui,
		urlProperty, authorProperty, commentsProperty, descriptionProperty, pubDateProperty,
		updatedDateProperty, checksumProperty, cacheForSecondsProperty, versionProperty,
		contentsProperty, enclosuresProperty, feedProperty
	);

	@Override
	public boolean isValid(final ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= ValidationHelper.isValidPropertyNotNull(this, FeedItem.urlProperty, errorBuffer);
		valid &= ValidationHelper.isValidUniqueProperty(this, FeedItem.urlProperty, errorBuffer);

		return valid;
	}

	@Override
	public void onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);
		updateIndex(securityContext);
	}

	@Override
	public void afterCreation(SecurityContext securityContext) throws FrameworkException {

		super.afterCreation(securityContext);
		updateIndex(securityContext);
	}

	public String getUrl() {
		return getProperty(urlProperty);
	}

	@Override
	public InputStream getInputStream() {

		final boolean indexRemoteDocument = Settings.FeedItemIndexRemoteDocument.getValue();

		if (indexRemoteDocument) {

			final String remoteUrl = getUrl();
			if (StringUtils.isNotBlank(remoteUrl)) {

				final Map<String, Object> responseData =  HttpHelper.getAsStream(remoteUrl);
				if (responseData != null && responseData.containsKey(HttpHelper.FIELD_BODY) && responseData.get(HttpHelper.FIELD_BODY) instanceof InputStream) {

					return (InputStream) responseData.get(HttpHelper.FIELD_BODY);
				}
			}
		}

		final String description = getProperty(StructrApp.key(FeedItem.class, "description"));
		return new ByteArrayInputStream(description.getBytes());
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
