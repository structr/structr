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
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.feed.entity.FeedItemEnclosure;
import org.structr.feed.traits.definitions.FeedItemEnclosureTraitDefinition;
import org.structr.feed.traits.relationship.AbstractFeedItemTraitDefinition;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * Represents feed enclosures
 */
public class FeedItemEnclosureTraitWrapper extends AbstractFeedItemTraitWrapper implements FeedItemEnclosure {

	public FeedItemEnclosureTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public String getUrl() {
		return wrappedObject.getProperty(traits.key(FeedItemEnclosureTraitDefinition.URL_PROPERTY));
	}

	@Override
	public InputStream getInputStream() {
		return IOUtils.toInputStream(getUrl(), Charset.forName("utf-8"));
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
