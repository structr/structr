/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.FulltextIndexer;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractNodeTraitWrapper;
import org.structr.feed.entity.AbstractFeedItem;

/**
 * Represents a content element of a feed item
 *
 */
public class AbstractFeedItemTraitWrapper extends AbstractNodeTraitWrapper implements AbstractFeedItem {

	public AbstractFeedItemTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override
	public void updateIndex(final SecurityContext securityContext) {

		try {

			if (indexingEnabled()) {

				final FulltextIndexer indexer = StructrApp.getInstance(securityContext).getFulltextIndexer();
				indexer.addToFulltextIndex(this);
			}

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(AbstractFeedItemTraitWrapper.class);
			logger.warn("Unable to index {}: {}", this, fex.getMessage());
		}
	}

	@Override
	public boolean indexingEnabled() {
		return Settings.FeedItemContentIndexingEnabled.getValue();
	}

	@Override
	public Integer maximumIndexedWords() {
		return Settings.FeedItemContentIndexingLimit.getValue();
	}

	@Override
	public Integer indexedWordMinLength() {
		return Settings.FeedItemContentIndexingMinLength.getValue();
	}

	@Override
	public Integer indexedWordMaxLength() {
		return Settings.FeedItemContentIndexingMaxLength.getValue();
	}
}
