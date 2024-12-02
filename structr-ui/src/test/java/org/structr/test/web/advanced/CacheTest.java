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
package org.structr.test.web.advanced;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.structr.api.schema.JsonProperty;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.structr.test.web.entity.TestFive;
import org.structr.test.web.entity.TestTwo;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.Page;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 */
public class CacheTest extends StructrUiTest {

	@Test
	public void testCaching() {

		try { Thread.sleep(2000); } catch (Throwable t) {}

		final ExecutorService service = Executors.newCachedThreadPool();
		final Queue<TestTwo> queue    = new ConcurrentLinkedQueue<>();
		final AtomicBoolean doRun     = new AtomicBoolean(true);

		service.submit(() -> {

			// This thread simply accesses the relationship collection of
			// every object that gets added to its queue, which causes the
			// wrong number of relationships to be set in the internal
			// caching layer.
			while (doRun.get()) {

				final TestTwo obj = queue.poll();
				if (obj != null) {

					try (final Tx tx = app.tx()) {

						obj.getProperty(TestTwo.testFives);
						tx.success();

					} catch (Throwable t) {
						t.printStackTrace();
					}

				}
			}

		});

		// create an object in a transaction and hand it over
		// to a different thread right afterwards, and add
		// relationships in the original thread

		final int num   = 10;
		final int count = 10;

		for (int i=0; i<num; i++) {

			TestTwo obj = null;

			try (final Tx tx = app.tx()) {

				// create test object
				obj = app.create(TestTwo.class, "testTwo"+ i);

				// add related nodes
				for (int j=0; j<count; j++) {

					final TestFive tmp = app.create(TestFive.class, new NodeAttribute<>(TestFive.testTwo, obj));
				}

				queue.add(obj);

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}

			try (final Tx tx = app.tx()) {

				final List<TestFive> testFives = Iterables.toList(obj.getProperty(TestTwo.testFives));
				final int size                 = testFives.size();

				if (size != count) {

					System.out.println("Leaking cache detected, collection has wrong number of elements: " + count + " vs. " + size);
				}

				assertEquals("Leaking cache detected, collection has wrong number of elements", count, size);

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
			}

		}

		// stop worker thread
		doRun.set(false);

		service.shutdown();
	}

	@Test
	public void testRollback() {

		try {

			try (final Tx tx = app.tx()) {

				app.create(SchemaProperty.class,
					new NodeAttribute<>(SchemaProperty.schemaNode, app.nodeQuery(SchemaNode.class).andName("Person").getFirst()),
					new NodeAttribute<>(SchemaProperty.propertyType, "String"),
					new NodeAttribute<>(SchemaProperty.name, "name")
				);

				tx.success();

			} catch (Throwable t) {
				t.printStackTrace();
			}

			try (final Tx tx = app.tx()) {

				final JsonSchema schema = StructrSchema.createEmptySchema();
				StructrSchema.replaceDatabaseSchema(app, schema);

				tx.success();

			} catch (Throwable t) {
				t.printStackTrace();
			}

			try (final Tx tx = app.tx()) {

				final JsonSchema schema = StructrSchema.createFromDatabase(app);
				final JsonType type     = schema.getType("Person");

				final Iterator<JsonProperty> iterator = type.getProperties().iterator();
				while (iterator.hasNext()) {

					if ("rollbackTest".equals(iterator.next().getName())) {
						iterator.remove();
					}
				}

				tx.success();

			} catch (Throwable t) {
				t.printStackTrace();
			}

			try (final Tx tx = app.tx()) {

				final JsonSchema schema = StructrSchema.createEmptySchema();
				StructrSchema.replaceDatabaseSchema(app, schema);

				tx.success();

			} catch (Throwable t) {
				t.printStackTrace();
			}

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Test
	public void testDeletedNodesAndCache() {

		String uuid = null;

		// setup
		try (final Tx tx = app.tx()) {

			Page.createSimplePage(securityContext, "test");

			final User user1 = app.create(User.class, "user1");

			app.create(User.class,
				new NodeAttribute<>(User.name,                              "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "password"), "admin"),
				new NodeAttribute<>(StructrApp.key(User.class, "isAdmin"),     true)
			);

			uuid = user1.getUuid();

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		// test success
		RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.filter(RequestLoggingFilter.logRequestTo(System.out))
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.expect()
			.statusCode(200)
			.when()
			.get("/test/" + uuid);


		// setup
		try (final Tx tx = app.tx()) {

			// delete user node
			app.delete(app.getNodeById(uuid));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		RestAssured.basePath = "/";

		// test success
		RestAssured
			.given()
			.header("X-User",     "admin")
			.header("X-Password", "admin")
			.filter(RequestLoggingFilter.logRequestTo(System.out))
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.expect()
			.statusCode(200)
			.when()
			.get("/test/" + uuid);


	}
}
