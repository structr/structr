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

import org.apache.commons.io.IOUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.feed.entity.FeedItemContent;
import org.structr.feed.traits.definitions.FeedItemContentTraitDefinition;
import org.structr.feed.traits.relationship.AbstractFeedItemTraitDefinition;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Represents a content element of a feed item
 */
public class FeedItemContentTraitWrapper extends AbstractFeedItemTraitWrapper implements FeedItemContent {

	public FeedItemContentTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public InputStream getInputStream() {
		return IOUtils.toInputStream(getValue(), Charset.forName("utf-8"));
	}

	public String getValue() {
		return wrappedObject.getProperty(traits.key(FeedItemContentTraitDefinition.VALUE_PROPERTY));
	}

	public void setValue(final String value) throws FrameworkException {
		wrappedObject.setProperty(traits.key(FeedItemContentTraitDefinition.VALUE_PROPERTY), value);
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
