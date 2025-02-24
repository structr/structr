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
package org.structr.test.web.basic;

import io.restassured.RestAssured;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.test.web.StructrUiTest;

public abstract class FrontendTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(FrontendTest.class.getName());

	public static final String ADMIN_USERNAME = "admin";
	public static final String ADMIN_PASSWORD = "admin";

	protected void clearLocalStorage() {

		final NodeInterface user;

		try (final Tx tx = app.tx()) {

			user = app.nodeQuery(StructrTraits.USER).andName("admin").getFirst();
			user.setProperty(Traits.of(StructrTraits.USER).key("localStorage"), null);
			tx.success();

		} catch (Throwable t) {
			logger.warn("", t);
		}
	}

	protected NodeInterface createAdminUser() {

		final PropertyMap properties = new PropertyMap();

		properties.put(Traits.of(StructrTraits.USER).key("name"), ADMIN_USERNAME);
		properties.put(Traits.of(StructrTraits.USER).key("password"), ADMIN_PASSWORD);
		properties.put(Traits.of(StructrTraits.USER).key("isAdmin"), true);

		NodeInterface user = null;

		try (final Tx tx = app.tx()) {

			user = app.create(StructrTraits.USER, properties);
			tx.success();

		} catch (Throwable t) {
			logger.warn("", t);
		}

		return user;
	}

	protected String createEntityAsAdmin(String resource, String... body) {

		StringBuilder buf = new StringBuilder();

		for (String part : body) {
			buf.append(part);
		}

		return getUuidFromLocation(
			RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(200))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(201))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(400))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(401))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(403))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(404))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(422))
				.filter(ResponseLoggingFilter.logResponseIfStatusCodeIs(500))
				.headers("X-User", ADMIN_USERNAME , "X-Password", ADMIN_PASSWORD)

			.body(buf.toString())
				.expect().statusCode(201)
			.when().post(resource).getHeader("Location"));
	}
}
