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
import org.structr.feed.entity.FeedItem;
import org.structr.feed.entity.FeedItemContent;

public class FeedItemFEED_ITEM_CONTENTSFeedItemContent extends OneToMany<FeedItem, FeedItemContent> {

	@Override
	public Class<FeedItem> getSourceType() {
		return FeedItem.class;
	}

	@Override
	public Class<FeedItemContent> getTargetType() {
		return FeedItemContent.class;
	}

	@Override
	public String name() {
		return "FEED_ITEM_CONTENTS";
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.SOURCE_TO_TARGET;
	}
}
