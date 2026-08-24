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
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * Whether a write that carries the value a node already holds runs the modification chain.
 *
 * A modification moves lastModifiedDate, appends to the changelog and runs the node's
 * onModification, so in a process-driven app a redundant write can cascade into real work. Both
 * write routes in PropertyContainerTraitDefinition compare the old value before writing - SetProperty
 * with equals, SetProperties with Objects.deepEquals - so the claim under test is that a redundant
 * value never reaches the write path at all.
 *
 * The tests go through HTTP rather than the in-process API on purpose. Writing to a nested node is
 * gated on the "setNestedProperties" attribute of the SecurityContext, which only the REST layer and
 * the XML import job set, so an in-process test silently exercises a different code path than a
 * client does.
 *
 * onModification is the observable, counted by the Audit nodes it creates, because that is the effect
 * that actually matters. Every "did not run" assertion is paired with a control that makes a real
 * change, so a chain that never runs at all cannot make the test pass for the wrong reason.
 */
public class NoOpModificationTest extends StructrRestTestBase {

	@Test
	public void testPutWithIdenticalValueDoesNotRunTheModificationChain() {

		createTestSchema();

		final String uuid = createEntity("/Department", "{ \"code\": \"ENG\", \"description\": \"initial\" }");
		final int baseline = auditCount();

		// the same value the node already holds
		put(uuid, "{ \"description\": \"initial\" }");

		assertEquals("a PUT with an unchanged value ran the modification chain", baseline, auditCount());

		// control: a real change must run it, proving the counter moves at all
		put(uuid, "{ \"description\": \"changed\" }");

		assertDescription("changed");
		assertEquals("a PUT with a new value did not run the modification chain", baseline + 1, auditCount());
	}

	@Test
	public void testPutWithIdenticalArrayValueDoesNotRunTheModificationChain() {

		createTestSchema();

		final String uuid  = createEntity("/Department", "{ \"code\": \"ENG\", \"tags\": [\"a\", \"b\"] }");
		final int baseline = auditCount();

		// the same array content the node already holds, in the same order
		put(uuid, "{ \"tags\": [\"a\", \"b\"] }");

		assertEquals("a PUT with unchanged array content ran the modification chain", baseline, auditCount());

		// control: changing one element must run it, so the counter is known to move here
		put(uuid, "{ \"tags\": [\"a\", \"c\"] }");

		assertEquals("a PUT with changed array content did not run the modification chain", baseline + 1, auditCount());
	}

	@Test
	public void testPutOfAnIdenticalNestedReferenceDoesNotRunTheModificationChain() {

		createTestSchema();

		final String departmentId = createEntity("/Department", "{ \"code\": \"ENG\" }");
		final String employeeId   = createEntity("/Employee",   "{ \"name\": \"Alice\", \"department\": { \"code\": \"ENG\" } }");

		final int baseline = auditCount();

		// re-assert the relationship that already exists, by reference
		put("/Employee/" + employeeId, "{ \"department\": { \"code\": \"ENG\" } }");

		assertEquals("re-asserting an existing nested reference ran the modification chain of the referenced node", baseline, auditCount());
	}

	@Test
	public void testNestedReferenceAppliesItsExtraProperties() {

		createTestSchema();

		createEntity("/Department", "{ \"code\": \"ENG\", \"description\": \"initial\" }");

		// a nested set that both resolves the node and carries a new value for it
		createEntity("/Employee", "{ \"name\": \"Bob\", \"department\": { \"code\": \"ENG\", \"description\": \"viaNested\" } }");

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result[0].description", equalTo("viaNested"))
		.when()
			.get("/Department");
	}

	// ----- private methods -----
	private void createTestSchema() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			// what onModification leaves behind, one node per run
			schema.addType("Audit");

			final JsonObjectType department = schema.addType("Department");
			final JsonObjectType employee   = schema.addType("Employee");

			// the key a nested reference resolves on. It has to be unique: IdDeserializationStrategy
			// only queries for keys that are unique or compound, or identifying, and identifying
			// covers name and eMail on Principal types only.
			department.addStringProperty("code", PropertyView.Public).setUnique(true);
			department.addStringProperty("description", PropertyView.Public);

			// the interesting case: SetProperty compares with equals, which on an array is identity,
			// while SetProperties compares with Objects.deepEquals. A REST request goes through the
			// second one, so an unchanged array should be filtered out - but the two routes disagree,
			// so it is worth pinning down which one a PUT actually takes.
			department.addStringArrayProperty("tags", PropertyView.Public);

			// onSave is the SchemaMethod name bound to OnModification. A SchemaMethod named
			// onModification is listed in SchemaMethodTraitWrapper.DeprecatedLifecycleMethods and is
			// never dispatched, so a test using that name observes nothing and passes every "did not
			// run" assertion. Only the SchemaMethod name is dead: the Java trait callback of the same
			// name is live, dispatched from GraphObjectModificationState:314 and implemented by
			// DOMNodeTraitDefinition and others.
			department.addMethod("onSave", "create('Audit', 'name', 'touched')");

			employee.relate(department, "WORKS_IN", Cardinality.ManyToOne, "employees", "department");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception while creating the schema.");
		}
	}

	private void put(final String resource, final String body) {

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body(body)
		.expect()
			.statusCode(200)
		.when()
			.put(resource.startsWith("/") ? resource : "/Department/" + resource);
	}

	private void assertDescription(final String expected) {

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
		.expect()
			.statusCode(200)
			.body("result[0].description", equalTo(expected))
		.when()
			.get("/Department");
	}

	private int auditCount() {

		try (final Tx tx = app.tx()) {

			final int count = app.nodeQuery("Audit").getAsList().size();

			tx.success();

			return count;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while counting Audit nodes.");
		}

		return -1;
	}
}
