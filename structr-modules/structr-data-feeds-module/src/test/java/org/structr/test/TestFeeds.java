/*
 * Copyright (C) 2010-2023 Structr GmbH
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
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.feed.entity.DataFeed;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.User;
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

		final Class type = StructrApp.getConfiguration().getNodeEntityClass("DataFeed");

		assertNotNull("Type DataFeed should exist", type);

		try (final Tx tx = app.tx()) {

			app.create(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"),  true)
			);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			app.create(type,
				new NodeAttribute<>(StructrApp.key(DataFeed.class, "url"),            "https://structr.com/blog/rss"),
				new NodeAttribute<>(StructrApp.key(DataFeed.class, "updateInterval"), 86400000),
				new NodeAttribute<>(StructrApp.key(DataFeed.class, "maxItems"),       3)
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
				.headers("X-User", "admin" , "X-Password", "admin")
			.expect()
				.statusCode(200)
				.body("result[0].type",           equalTo("DataFeed"))
				.body("result[0].description",    equalTo("This is the official Structr blog"))
				.body("result[0].feedType",       equalTo("rss_2.0"))
				.body("result[0].maxAge",         equalTo(null))
				.body("result[0].maxItems",       equalTo(3))
				.body("result[0].updateInterval", equalTo(86400000))
				.body("result[0].url",            equalTo("https://structr.com/blog/rss"))
				.body("result[0].items",          hasSize(3))
				.body("result[0].items[0].type",  equalTo("FeedItem"))
				.body("result[0].items[1].type",  equalTo("FeedItem"))
				.body("result[0].items[2].type",  equalTo("FeedItem"))
			.when()
				.get("/DataFeed/ui");

	}

	@Test
	public void testRemoteDocument() {

		final Class type = StructrApp.getConfiguration().getNodeEntityClass("RemoteDocument");

		assertNotNull("Type RemoteDocument should exist", type);

		try (final Tx tx = app.tx()) {

			app.create(User.class,
				new NodeAttribute<>(StructrApp.key(User.class, "name"),     "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"),  true)
			);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			app.create(type,
				new NodeAttribute<>(StructrApp.key(type, "url"), "https://structr.com/blog")
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
				.headers("X-User", "admin" , "X-Password", "admin")
			.expect()
				.statusCode(200)
				.body("result[0].type",           equalTo("RemoteDocument"))
			.when()
				.get("/RemoteDocument");

	}
}
