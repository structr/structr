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
package org.structr.test;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.feed.traits.definitions.DataFeedTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

/**
 */
public class TestFeeds extends StructrUiTest {

	@Test
	public void testFeeds() {

		final String type       = StructrTraits.DATA_FEED;
		final Traits feedTraits = Traits.of(type);

		assertNotNull("Type DataFeed should exist", feedTraits);

		try (final Tx tx = app.tx()) {

			createAdminUser();

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			app.create(type,
				new NodeAttribute<>(feedTraits.key(DataFeedTraitDefinition.URL_PROPERTY),            "https://structr.com/blog/rss"),
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
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
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
				.body("result[0].url",            equalTo("https://structr.com/blog/rss"))
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

			app.create(type,
				new NodeAttribute<>(remoteDocumentTraits.key("url"), "https://structr.com/blog")
			);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		RestAssured
			.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
				.body("result[0].type",           equalTo(type))
			.when()
				.get("/RemoteDocument");

	}
}
