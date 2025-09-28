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
package org.structr.feed;

import org.structr.api.service.LicenseManager;
import org.structr.core.traits.StructrTraits;
import org.structr.feed.traits.definitions.*;
import org.structr.feed.traits.relationship.AbstractFeedItemTraitDefinition;
import org.structr.feed.traits.relationship.DataFeedHAS_FEED_ITEMSFeedItem;
import org.structr.feed.traits.relationship.FeedItemFEED_ITEM_CONTENTSFeedItemContent;
import org.structr.feed.traits.relationship.FeedItemFEED_ITEM_ENCLOSURESFeedItemEnclosure;
import org.structr.module.StructrModule;

import java.util.Set;

/**
 *
 */
public class DataFeedsModule implements StructrModule {

	@Override
	public void onLoad() {

		StructrTraits.registerTrait(new DataFeedHAS_FEED_ITEMSFeedItem());
		StructrTraits.registerTrait(new FeedItemFEED_ITEM_CONTENTSFeedItemContent());
		StructrTraits.registerTrait(new FeedItemFEED_ITEM_ENCLOSURESFeedItemEnclosure());

		StructrTraits.registerRelationshipType(StructrTraits.DATA_FEED_HAS_FEED_ITEMS_FEED_ITEM,                 StructrTraits.DATA_FEED_HAS_FEED_ITEMS_FEED_ITEM);
		StructrTraits.registerRelationshipType(StructrTraits.FEED_ITEM_FEED_ITEM_CONTENTS_FEED_ITEM_CONTENT,     StructrTraits.FEED_ITEM_FEED_ITEM_CONTENTS_FEED_ITEM_CONTENT);
		StructrTraits.registerRelationshipType(StructrTraits.FEED_ITEM_FEED_ITEM_ENCLOSURES_FEED_ITEM_ENCLOSURE, StructrTraits.FEED_ITEM_FEED_ITEM_ENCLOSURES_FEED_ITEM_ENCLOSURE);

		StructrTraits.registerTrait(new AbstractFeedItemTraitDefinition());
		StructrTraits.registerTrait(new DataFeedTraitDefinition());
		StructrTraits.registerTrait(new FeedItemTraitDefinition());
		StructrTraits.registerTrait(new FeedItemContentTraitDefinition());
		StructrTraits.registerTrait(new FeedItemEnclosureTraitDefinition());
		StructrTraits.registerTrait(new RemoteDocumentTraitDefinition());

		StructrTraits.registerNodeType(StructrTraits.ABSTRACT_FEED_ITEM,  StructrTraits.ABSTRACT_FEED_ITEM);
		StructrTraits.registerNodeType(StructrTraits.DATA_FEED,           StructrTraits.DATA_FEED);
		StructrTraits.registerNodeType(StructrTraits.FEED_ITEM,           StructrTraits.ABSTRACT_FEED_ITEM, StructrTraits.FEED_ITEM);
		StructrTraits.registerNodeType(StructrTraits.FEED_ITEM_CONTENT,   StructrTraits.ABSTRACT_FEED_ITEM, StructrTraits.FEED_ITEM_CONTENT);
		StructrTraits.registerNodeType(StructrTraits.FEED_ITEM_ENCLOSURE, StructrTraits.ABSTRACT_FEED_ITEM, StructrTraits.FEED_ITEM_ENCLOSURE);
		StructrTraits.registerNodeType(StructrTraits.REMOTE_DOCUMENT,     StructrTraits.REMOTE_DOCUMENT);
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {
	}

	@Override
	public String getName() {
		return "data-feeds";
	}

	@Override
	public Set<String> getDependencies() {
		return Set.of("ui");
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}
}
