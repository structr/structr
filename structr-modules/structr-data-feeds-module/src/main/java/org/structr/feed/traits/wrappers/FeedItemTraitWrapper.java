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
package org.structr.feed.traits.wrappers;

import org.apache.commons.lang3.StringUtils;
import org.structr.api.config.Settings;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.feed.entity.FeedItem;
import org.structr.feed.traits.definitions.FeedItemTraitDefinition;
import org.structr.feed.traits.relationship.AbstractFeedItemTraitDefinition;
import org.structr.rest.common.HttpHelper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;

/**
 * Represents a single item of a data feed.
 *
 */
public class FeedItemTraitWrapper extends AbstractFeedItemTraitWrapper implements FeedItem {

	public FeedItemTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getUrl() {
		return wrappedObject.getProperty(traits.key(FeedItemTraitDefinition.URL_PROPERTY));
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

		final String description = wrappedObject.getProperty(traits.key(FeedItemTraitDefinition.DESCRIPTION_PROPERTY));
		return new ByteArrayInputStream(description.getBytes());
	}

	@Override
	public String getExtractedContent() {
		return wrappedObject.getProperty(traits.key("extractedContent"));			// FIXME: extractedContent... this used to extend "Indexable"
	}

	@Override
	public String getContentType() {
		return wrappedObject.getProperty(traits.key(AbstractFeedItemTraitDefinition.CONTENT_TYPE_PROPERTY));
	}
}
