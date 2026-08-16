/*
 * Copyright (C) 2010-2026 Structr GmbH
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

import io.restassured.RestAssured;
import org.structr.api.config.Settings;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.feed.traits.definitions.DataFeedTraitDefinition;
import org.structr.common.error.FrameworkException;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.feed.entity.FeedItem;
import org.structr.feed.traits.definitions.FeedItemTraitDefinition;
import org.structr.web.traits.definitions.FileTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.testng.annotations.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

public class FeedsTest extends StructrUiTest {

	@Test
	public void testFeeds() {

		final String type       = StructrTraits.DATA_FEED;
		final Traits feedTraits = Traits.of(type);

		assertNotNull("Type DataFeed should exist", feedTraits);

		// Disable SSRF protection for this test because the feed URL points to localhost
		Settings.SsrfProtection.setValue(false);

		try (final Tx tx = app.tx()) {

			createAdminUser();

			try (final InputStream is = FeedsTest.class.getResourceAsStream("/rss.xml")) {

				NodeInterface file = FileHelper.createFile(securityContext, is, "text/xml", StructrTraits.FILE, "rss.xml");
				file.setVisibility(true, true);
			}

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			app.create(type,
				new NodeAttribute<>(feedTraits.key(DataFeedTraitDefinition.URL_PROPERTY),             RestAssured.baseURI + "/rss.xml"),
				new NodeAttribute<>(feedTraits.key(DataFeedTraitDefinition.UPDATE_INTERVAL_PROPERTY), 86400000),
				new NodeAttribute<>(feedTraits.key(DataFeedTraitDefinition.MAX_ITEMS_PROPERTY),       3)
			);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
				.body("result[0].type",           equalTo(type))
				.body("result[0].description",    equalTo("This is the official Structr blog"))
				.body("result[0].feedType",       equalTo("rss_2.0"))
				.body("result[0].maxAge",         equalTo(null))
				.body("result[0].maxItems",       equalTo(3))
				.body("result[0].updateInterval", equalTo(86400000))
				.body("result[0].url",            equalTo(RestAssured.baseURI + "/rss.xml"))
				.body("result[0].items",          hasSize(3))
				.body("result[0].items[0].type",  equalTo(StructrTraits.FEED_ITEM))
				.body("result[0].items[1].type",  equalTo(StructrTraits.FEED_ITEM))
				.body("result[0].items[2].type",  equalTo(StructrTraits.FEED_ITEM))
			.when()
				.get("/DataFeed/ui");

	}

	@Test
	public void testRemoteDocument() {

		final String type                 = StructrTraits.REMOTE_DOCUMENT;
		final Traits remoteDocumentTraits = Traits.of(type);

		assertNotNull("Type RemoteDocument should exist", remoteDocumentTraits);

		try (final Tx tx = app.tx()) {

			createAdminUser();

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			app.create(type, new NodeAttribute<>(remoteDocumentTraits.key("url"), "https://download.structr.com/blog"));

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
				.body("result[0].type",           equalTo(type))
			.when()
				.get("/RemoteDocument");

	}

	@Test
	public void testExtractedContentOfFeedItems() {

		// The fulltext indexer stores the extracted text under the File key (FulltextIndexingAgent), for
		// any node it is given - including feed items. getExtractedContent() has to read the same property.

		final String[] uuid = new String[1];

		try (final Tx tx = app.tx()) {

			final NodeInterface item = app.create(StructrTraits.FEED_ITEM,
				new NodeAttribute<>(Traits.of(StructrTraits.FEED_ITEM).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "item1"),
				new NodeAttribute<>(Traits.of(StructrTraits.FEED_ITEM).key(FeedItemTraitDefinition.URL_PROPERTY), "http://localhost/item1")
			);

			uuid[0] = item.getUuid();

			// this is what the indexing agent does
			item.setProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.EXTRACTED_CONTENT_PROPERTY), "the extracted text");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final FeedItem item = app.getNodeById(StructrTraits.FEED_ITEM, uuid[0]).as(FeedItem.class);

			assertEquals("Extracted content must be readable through the accessor", "the extracted text", item.getExtractedContent());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}
}
