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
import io.restassured.response.Response;
import org.structr.api.RetryException;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.TraitsManager;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.nodeinterface.OnNodeCreation;
import org.structr.schema.export.StructrSchema;
import org.structr.test.common.LogCapture;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * A failure of user code in a flow that enters Structr through HTTP is reported once, with the
 * request at the root of its trace, and every frame of the flow is below that request.
 */
public class CallTraceRestTest extends StructrRestTestBase {

	// how many of the next ScriptRetryItem creations fail as if the database terminated the transaction
	private static final AtomicInteger creationFailures = new AtomicInteger();

	@Override
	protected void createSchema() {

		super.createSchema();

		StructrTraits.registerTrait(new ScriptRetryItemTraitDefinition());
		StructrTraits.registerNodeType("ScriptRetryItem", "ScriptRetryItem");

		TraitsManager.replaceCurrentInstance(TraitsManager.createCopyOfRootInstance());
	}

	@Test
	public void testFailingLifecycleMethodOfARestCreateIsReportedOnceWithTheRequest() {

		extendSchema(type -> type.addMethod("onCreate", "{\n\tthrow new Error('rest-lifecycle-marker');\n}"), "TraceRestItem");

		final List<String> roots = new ArrayList<>();
		final List<String> lines = LogCapture.mentions("rest-lifecycle-marker", () -> roots.addAll(LogCapture.flowsWithoutEntryFrame(() ->

			RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.body("{ \"name\": \"item\" }")
			.when()
				.post("/TraceRestItem")
				.statusCode()
		)));

		assertEquals(lines.toString(), 1, lines.size());
		assertTrue(lines.get(0), lines.get(0).matches("(?s)TraceRestItem\\[[0-9a-f]{32}\\]:onCreate:2:\\d+: .*rest-lifecycle-marker.*called from HTTP POST /structr/rest/TraceRestItem.*"));
		assertEquals("Every frame of the flow is below the request", List.of(), roots);
	}

	@Test
	public void testRetryExceptionThroughAScriptIsNotReportedAndTheRequestIsRepeated() {

		extendSchema(type -> type.addMethod("makeItem", "{\n\t$.create('ScriptRetryItem', { name: 'made' });\n\treturn 'ok';\n}").setIsStatic(true), "TraceRetryHolder");

		creationFailures.set(1);

		final List<String> roots = new ArrayList<>();
		final List<String> lines = LogCapture.mentions("Simulated termination", () -> roots.addAll(LogCapture.flowsWithoutEntryFrame(() -> {

			final Response response = RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.body("{}")
			.when()
				.post("/TraceRetryHolder/makeItem");

			assertEquals(response.asString(), 200, response.statusCode());

			return null;
		})));

		assertEquals("The creation did not fail", 0, creationFailures.get());
		assertEquals("A retried deadlock is not a scripting error", List.of(), lines);
		assertEquals("The repeated request created the item once", 1, countItems());
		assertEquals("Every frame of the flow is below the request", List.of(), roots);
	}

	@Test
	public void testWorkHandedToOtherThreadsKeepsTheRequestAsItsRoot() {

		extendSchema(type -> type.addMethod("hop", "{\n\t$.doInNewTransaction(() => {\n\t\tthrow new Error('hop-transaction-marker');\n\t});\n\t$.schedule(() => {\n\t\tthrow new Error('hop-job-marker');\n\t}, 'hop job');\n\treturn 'ok';\n}").setIsStatic(true), "TraceHopHolder");

		final List<String> transactionLines = new ArrayList<>();
		final List<String> jobLines         = new ArrayList<>();
		final List<String> roots            = LogCapture.flowsWithoutEntryFrame(() -> jobLines.addAll(LogCapture.mentions("hop-job-marker", 10_000, () -> transactionLines.addAll(LogCapture.mentions("hop-transaction-marker", () -> {

			final Response response = RestAssured.given()
				.contentType("application/json; charset=UTF-8")
				.body("{}")
			.when()
				.post("/TraceHopHolder/hop");

			assertEquals(response.asString(), 200, response.statusCode());

			return null;
		})))));

		assertEquals(transactionLines.toString(), 1, transactionLines.size());
		assertTrue(transactionLines.get(0), transactionLines.get(0).matches("(?s)TraceHopHolder\\[static\\]:hop:3:\\d+: .*hop-transaction-marker.*via doInNewTransaction\\(\\).*called from HTTP POST /structr/rest/TraceHopHolder/hop"));

		assertEquals(jobLines.toString(), 1, jobLines.size());
		assertTrue(jobLines.get(0), jobLines.get(0).matches("(?s)TraceHopHolder\\[static\\]:hop:6:\\d+: .*hop-job-marker.*via schedule\\(\\).*called from HTTP POST /structr/rest/TraceHopHolder/hop"));

		assertEquals("Every frame of the flow is below the request", List.of(), roots);
	}

	// ----- private methods -----
	private void extendSchema(final java.util.function.Consumer<JsonType> definition, final String typeName) {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			definition.accept(schema.addType(typeName));

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception " + ex.getMessage());
		}
	}

	private int countItems() {

		try (final Tx tx = app.tx()) {

			final int count = app.nodeQuery("ScriptRetryItem").getAsList().size();

			tx.success();

			return count;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception " + fex.getMessage());
		}

		return -1;
	}

	// ----- nested classes -----
	private static class ScriptRetryItemTraitDefinition extends AbstractNodeTraitDefinition {

		ScriptRetryItemTraitDefinition() {

			super("ScriptRetryItem");
		}

		@Override
		public Map<Class, LifecycleMethod> createLifecycleMethods(final TraitsInstance traitsInstance) {

			return Map.of(

				// runs inside create(), so the exception leaves the script that created the item
				OnNodeCreation.class, new OnNodeCreation() {

					@Override
					public void onNodeCreation(final NodeInterface nodeInterface, final SecurityContext securityContext) throws FrameworkException {

						if (creationFailures.getAndUpdate(value -> value > 0 ? value - 1 : 0) > 0) {

							throw new RetryException("Simulated termination of the transaction");
						}
					}
				}
			);
		}

		@Override
		public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

			return Set.of();
		}

		@Override
		public Map<String, Set<String>> getViews() {

			return Map.of(PropertyView.Public, newSet("name"));
		}

		@Override
		public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

			return Map.of();
		}

		@Override
		public Relation getRelation() {

			return null;
		}
	}
}
