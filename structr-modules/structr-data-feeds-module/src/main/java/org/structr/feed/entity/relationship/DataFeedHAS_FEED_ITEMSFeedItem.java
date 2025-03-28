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
package org.structr.feed.entity.relationship;

import org.structr.core.entity.OneToMany;
import org.structr.core.entity.Relation;
import org.structr.feed.entity.DataFeed;
import org.structr.feed.entity.FeedItem;

public class DataFeedHAS_FEED_ITEMSFeedItem extends OneToMany<DataFeed, FeedItem> {

	@Override
	public Class<DataFeed> getSourceType() {
		return DataFeed.class;
	}

	@Override
	public Class<FeedItem> getTargetType() {
		return FeedItem.class;
	}

	@Override
	public String name() {
		return "HAS_FEED_ITEMS";
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.SOURCE_TO_TARGET;
	}
}
