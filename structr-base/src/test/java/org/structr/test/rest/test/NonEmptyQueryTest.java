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
import org.structr.core.graph.BulkRebuildIndexCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.SchemaPropertyTraitDefinition;
import org.structr.schema.export.StructrSchema;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.testng.AssertJUnit.fail;

/**
 * The non-empty search predicate `?prop=[]` selects the objects that have a stored value for the property
 * (tickets 414 and 1271).
 *
 * <p>Searches operate on stored values. A default value is not stored when the property is read. It is stored
 * when an object is created, and for an indexed property also when the object is modified or rebuildIndex runs.
 * The predicate used to require indexedWhenEmpty(), which a dynamic property only gets together with a default
 * value, although the "is not null" condition it becomes does not depend on it; it now works on every property
 * that stores its value on the object.
 */
public class NonEmptyQueryTest extends StructrRestTestBase {

	@Test
	public void nonEmptyQuerySelectsObjectsWithAStoredValue() {

		createItemType();

		createEntity("/Item", "{ name: item1, withDefault: a, noDefault: b }");
		createEntity("/Item", "{ name: item2, withDefault: c }");
		createEntity("/Item", "{ name: item3 }");

		// item3 has the default value stored, because it was created with the default in place
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result",       hasSize(3))
				.body("result_count", equalTo(3))
			.when()
				.get("/Item?withDefault=[]");

		// no default value: only the object that was given one has a value
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result",         hasSize(1))
				.body("result[0].name", equalTo("item1"))
			.when()
				.get("/Item?noDefault=[]&_sort=name");
	}

	/**
	 * Objects that exist before a default value is added to an indexed property show the default but have no
	 * stored value, so no search finds them by it until it is stored, which rebuildIndex does for all of them.
	 */
	@Test
	public void defaultValueAddedLaterIsSearchableAfterRebuildIndex() {

		createItemType();

		createEntity("/Item", "{ name: before }");

		try (final Tx tx = app.tx()) {

			final NodeInterface property = app.nodeQuery(StructrTraits.SCHEMA_PROPERTY).name("noDefault").getFirst();

			property.setProperty(Traits.of(StructrTraits.SCHEMA_PROPERTY).key(SchemaPropertyTraitDefinition.DEFAULT_VALUE_PROPERTY), "y");

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while adding the default value.");
		}

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result[0].noDefault", equalTo("y"))
			.when()
				.get("/Item");

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result", hasSize(0))
			.when()
				.get("/Item?noDefault=[]");

		app.command(BulkRebuildIndexCommand.class).execute(Map.of("type", "Item"));

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result", hasSize(1))
			.when()
				.get("/Item?noDefault=[]");

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
			.expect()
				.statusCode(200)
				.body("result", hasSize(1))
			.when()
				.get("/Item?noDefault=y");
	}

	/**
	 * One type, two indexed string properties: one with a default value, one without.
	 */
	private void createItemType() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("Item");

			type.addStringProperty("withDefault", "public").setIndexed(true).setDefaultValue("x");
			type.addStringProperty("noDefault", "public").setIndexed(true);

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception during schema setup.");
		}
	}
}
