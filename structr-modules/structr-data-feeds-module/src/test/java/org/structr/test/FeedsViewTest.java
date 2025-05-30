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
package org.structr.test;

import org.structr.core.traits.StructrTraits;
import org.structr.feed.traits.definitions.DataFeedTraitDefinition;
import org.structr.feed.traits.definitions.FeedItemTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.TestHelper;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class FeedsViewTest extends StructrUiTest {

	@Test
	public void testViews() {

		final Map<String, List<String>> requiredAttributes   = new LinkedHashMap<>();

		requiredAttributes.put(StructrTraits.DATA_FEED, Arrays.asList(DataFeedTraitDefinition.URL_PROPERTY));
		requiredAttributes.put(StructrTraits.FEED_ITEM, Arrays.asList(FeedItemTraitDefinition.URL_PROPERTY));

		TestHelper.testViews(app, FeedsViewTest.class.getResourceAsStream("/views.properties"), requiredAttributes);
	}
}
