/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.test.rest.test;

import io.restassured.RestAssured;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.*;
import static org.testng.AssertJUnit.fail;

/**
 * Verifies the documented contract for the non-empty search predicate `?prop=[]` (ticket #414):
 * on a dynamic (data-model) property it works only when the property is indexed-when-empty, which for
 * dynamic properties is enabled automatically when the property is indexed AND has a non-blank default
 * value. Without a default value the predicate is rejected with HTTP 400.
 */
public class NonEmptyQueryTest extends StructrRestTestBase {

	@Test
	public void nonEmptyQueryRequiresDefaultValueOnDynamicProperty() {

		// one type, two indexed string properties: one with a default value, one without
		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Item");

			type.addStringProperty("withDefault").setIndexed(true).setDefaultValue("x");
			type.addStringProperty("noDefault").setIndexed(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception during schema setup.");
		}

		createEntity("/Item", "{ name: item1, withDefault: a, noDefault: b }");
		createEntity("/Item", "{ name: item2, withDefault: c, noDefault: d }");

		// non-empty predicate WORKS on the indexed property that has a default value
		// (the default value makes the dynamic property indexed-when-empty)
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result",       hasSize(2))
				.body("result_count", equalTo(2))
			.when()
				.get("/Item?withDefault=[]");

		// ...and is REJECTED with 400 on the indexed property WITHOUT a default value,
		// because it is not indexed-when-empty
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(400)
				.body("code",    equalTo(400))
				.body("message", containsString("indexedWhenEmpty"))
			.when()
				.get("/Item?noDefault=[]");
	}
}
