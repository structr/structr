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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.fulltext.FulltextIndexer;
import org.structr.common.fulltext.Indexable;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;

/**
 * Represents a content element of a feed item
 *
 */
public abstract class AbstractFeedItem extends AbstractNode implements Indexable {

	public void updateIndex(final SecurityContext securityContext) {

		try {

			if (indexingEnabled()) {

				final FulltextIndexer indexer = StructrApp.getInstance(securityContext).getFulltextIndexer();
				indexer.addToFulltextIndex(this);
			}

		} catch (FrameworkException fex) {

			final Logger logger = LoggerFactory.getLogger(AbstractFeedItem.class);
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
