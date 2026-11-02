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
import org.structr.api.RetryException;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.TraitsManager;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.AfterCreation;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.test.rest.common.StructrRestTestBase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Repeats REST requests whose transaction the database terminated. The type RetryItem throws a RetryException from
 * its lifecycle methods as often as a test asks for, which is what a driver does when the database terminates the
 * transaction at commit, so each test knows exactly which attempt fails. The callbacks after the commit modify the
 * object, so their own transaction fails at its commit as well.
 */
public class TransactionRetryTest extends StructrRestTestBase {

	// how many of the next commits fail, separately for the commit of the request and the commit of the callbacks after it
	private static final AtomicInteger commitFailures   = new AtomicInteger();
	private static final AtomicInteger callbackFailures = new AtomicInteger();
	private static final AtomicInteger callbackRuns     = new AtomicInteger();

	@BeforeMethod
	public void resetFailures() {

		commitFailures.set(0);
		callbackFailures.set(0);
		callbackRuns.set(0);
	}

	@Override
	protected void createSchema() {

		super.createSchema();

		StructrTraits.registerTrait(new RetryItemTraitDefinition());
		StructrTraits.registerNodeType("RetryItem", "RetryItem");

		TraitsManager.replaceCurrentInstance(TraitsManager.createCopyOfRootInstance());
	}

	@Test
	public void testRepeatedPatchUpdatesTheExistingObject() {

		final String id = createItem("before");

		commitFailures.set(1);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("[{ \"id\": \"" + id + "\", \"name\": \"after\" }]")
		.expect()
			.statusCode(200)
		.when()
			.patch("/RetryItem");

		assertEquals("The terminated attempt was not repeated", 0, commitFailures.get());

		final Map<String, String> items = getItems();

		assertEquals("A repeated PATCH created a new object instead of updating the existing one", 1, items.size());
		assertEquals("A repeated PATCH did not update the existing object", "after", items.get(id));
	}

	@Test
	public void testRepeatedPostReturnsOnlyTheObjectsThatExist() {

		commitFailures.set(1);

		final List<String> ids = RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("[{ \"name\": \"one\" }, { \"name\": \"two\" }]")
		.expect()
			.statusCode(201)
		.when()
			.post("/RetryItem")
			.jsonPath().getList("result", String.class);

		assertEquals("The terminated attempt was not repeated", 0, commitFailures.get());

		final Map<String, String> items = getItems();

		assertEquals("A repeated POST created the wrong number of objects", 2, items.size());
		assertEquals("A repeated POST returned the objects of the attempt that was rolled back", 2, ids.size());
		assertEquals("A repeated POST returned the objects of the attempt that was rolled back", items.keySet(), Set.copyOf(ids));
	}

	@Test
	public void testRepeatedCallbacksRunAgain() {

		callbackFailures.set(1);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("{ \"name\": \"item\" }")
		.expect()
			.statusCode(201)
		.when()
			.post("/RetryItem");

		assertEquals("The terminated callbacks were not repeated", 0, callbackFailures.get());
		assertEquals("The repeated callbacks did not run", 2, callbackRuns.get());
		assertEquals("The callbacks repeated the request", 1, getItems().size());
	}

	@Test
	public void testFailingCallbacksDoNotRepeatTheCommittedRequest() {

		// more failures than attempts, so the callbacks after the commit fail for good
		callbackFailures.set(RetryException.MAX_ATTEMPTS * 2);

		RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("{ \"name\": \"item\" }")
		.when()
			.post("/RetryItem");

		assertEquals("The callbacks were not repeated until the attempts ran out", RetryException.MAX_ATTEMPTS, callbackRuns.get());
		assertEquals("The request was repeated although its data was already committed", 1, getItems().size());
	}

	@Test
	public void testMaintenanceCommandIsNotRepeated() throws Exception {

		// a data deployment commits transactions of its own, so a repetition would run the whole import again
		final Path source = Files.createTempDirectory("structr-retry-test");
		final Path nodes  = Files.createDirectories(source.resolve("nodes"));

		Files.writeString(nodes.resolve("RetryItem.json"), "[{ \"id\": \"" + NodeServiceCommand.getNextUuid() + "\", \"type\": \"RetryItem\", \"name\": \"item\" }]");

		commitFailures.set(2);

		final int status = RestAssured.given()
			.contentType("application/json; charset=UTF-8")
			.body("{ \"mode\": \"import\", \"doInnerCallbacks\": true, \"source\": \"" + source.toAbsolutePath() + "\" }")
		.when()
			.post("/maintenance/deployData")
			.statusCode();

		assertEquals("The maintenance command was repeated after a transient error", 1, commitFailures.get());
		assertTrue("The maintenance command did not fail after a transient error", status >= 400);
		assertEquals("The failed import created objects", 0, getItems().size());
	}

	// ----- private methods -----
	private String createItem(final String name) {

		try (final Tx tx = app.tx()) {

			final String id = app.create("RetryItem", name).getUuid();

			tx.success();

			return id;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		return null;
	}

	/**
	 * Returns the names of all items by their id.
	 */
	private Map<String, String> getItems() {

		final Map<String, String> items = new LinkedHashMap<>();

		try (final Tx tx = app.tx()) {

			for (final NodeInterface item : app.nodeQuery("RetryItem").getAsList()) {

				items.put(item.getUuid(), item.getName());
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		return items;
	}

	/**
	 * Consumes one of the given failures and returns true if there was one left.
	 */
	private static boolean consume(final AtomicInteger failures) {

		return failures.getAndUpdate(value -> value > 0 ? value - 1 : 0) > 0;
	}

	// ----- nested classes -----
	private static class RetryItemTraitDefinition extends AbstractNodeTraitDefinition {

		RetryItemTraitDefinition() {

			super("RetryItem");
		}

		@Override
		public Map<Class, LifecycleMethod> createLifecycleMethods(final TraitsInstance traitsInstance) {

			return Map.of(

				OnCreation.class, new OnCreation() {

					@Override
					public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

						if (consume(commitFailures)) {

							throw new RetryException("Simulated termination of the transaction");
						}
					}
				},

				OnModification.class, new OnModification() {

					@Override
					public void onModification(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

						if (consume(commitFailures)) {

							throw new RetryException("Simulated termination of the transaction");
						}

						// the modification made by afterCreation, so the callback transaction fails at its commit
						if (consume(callbackFailures)) {

							throw new RetryException("Simulated termination of the callback transaction");
						}
					}
				},

				AfterCreation.class, new AfterCreation() {

					@Override
					public void afterCreation(final GraphObject graphObject, final SecurityContext securityContext) throws FrameworkException {

						callbackRuns.incrementAndGet();

						// modifies the object, so that onModification runs when the callback transaction is committed
						graphObject.setProperty(graphObject.getTraits().key("name"), "created");
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
