/*
 * Copyright (C) 2010-2025 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.web.basic;

import io.restassured.RestAssured;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.UserTraitDefinition;
import org.structr.test.web.StructrUiTest;

public abstract class FrontendTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(FrontendTest.class.getName());

	public static final String ADMIN_USERNAME = "admin";
	public static final String ADMIN_PASSWORD = "admin";

	protected void clearLocalStorage() {

		final NodeInterface user;

		try (final Tx tx = app.tx()) {

			user = app.nodeQuery(StructrTraits.USER).name("admin").getFirst();
			user.setProperty(Traits.of(StructrTraits.USER).key(UserTraitDefinition.LOCAL_STORAGE_PROPERTY), null);
			tx.success();

		} catch (Throwable t) {
			logger.warn("", t);
		}
	}

	protected NodeInterface createAdminUser() {

		final PropertyMap properties = new PropertyMap();

		properties.put(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), ADMIN_USERNAME);
		properties.put(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), ADMIN_PASSWORD);
		properties.put(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY), true);

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
				.headers(X_USER_HEADER, ADMIN_USERNAME , X_PASSWORD_HEADER, ADMIN_PASSWORD)

			.body(buf.toString())
				.expect().statusCode(201)
			.when().post(resource).getHeader("Location"));
	}
}
