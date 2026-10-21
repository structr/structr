/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import io.restassured.response.Response;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.testng.AssertJUnit.assertTrue;

/**
 * Creates the packages of a nested flow with the requests that the flows UI sent before it created a flow in one
 * request: one request per package, then one request per package that sets its parent, all of them at the same
 * time, each with the whole chain of parents in its payload. The requests set the same relationships concurrently,
 * so the database terminates some of their transactions to resolve deadlocks, and every request must succeed when
 * its transaction is repeated. A deadlock does not occur in every round, so the test runs twenty of them.
 */
public class FlowPackageCreationTest extends StructrUiTest {

	@Test
	public void testConcurrentRequestsLinkingNestedPackages() {

		createAdminUser();

		RestAssured.basePath = restUrl;

		final List<String> errors = new ArrayList<>();

		for (int round = 0; round < 20; round++) {

			final List<Map<String, Object>> packages = new ArrayList<>();

			// the packages are created one after the other, the innermost first
			for (int i = 9; i >= 0; i--) {

				final String name = "round" + round + "test" + i;
				final String id   = post("FlowContainerPackage", Map.of("name", name)).jsonPath().getString("result[0]");
				final Map<String, Object> object = new LinkedHashMap<>();

				object.put("id", id);
				object.put("type", "FlowContainerPackage");
				object.put("name", name);

				packages.add(0, object);
			}

			// the parents are set at the same time, and the value of a parent contains its own parent
			final ExecutorService executor = Executors.newFixedThreadPool(10);
			final CountDownLatch start     = new CountDownLatch(1);
			final List<Future<Response>> responses = new ArrayList<>();

			for (int i = 1; i < packages.size(); i++) {

				final Map<String, Object> child  = packages.get(i);
				final Map<String, Object> parent = packages.get(i - 1);

				child.put("parent", parent);

				final Map<String, Object> payload = Map.of("parent", parent);
				final String id                   = (String) child.get("id");

				responses.add(executor.submit(() -> {

					start.await();

					return put("FlowContainerPackage/" + id, payload);
				}));
			}

			final String innermost = (String) packages.get(packages.size() - 1).get("id");

			responses.add(executor.submit(() -> {

				start.await();

				return post("FlowContainer", Map.of("name", "final_flow1", "flowPackage", innermost));
			}));

			start.countDown();

			for (final Future<Response> future : responses) {

				try {

					final Response response = future.get();
					if (response.statusCode() >= 300) {

						errors.add("round " + round + ": " + response.statusCode() + " " + response.body().asString());
					}

				} catch (Throwable t) {

					errors.add("round " + round + ": " + t.getMessage());
				}
			}

			executor.shutdown();

			// every package has exactly one parent, so the effective names are the whole chain
			for (int i = 0; i < packages.size(); i++) {

				final StringBuilder expected = new StringBuilder();

				for (int j = 0; j <= i; j++) {

					if (j > 0) {

						expected.append(".");
					}

					expected.append(packages.get(j).get("name"));
				}

				final String id     = (String) packages.get(i).get("id");
				final String actual = get("FlowContainerPackage/" + id).jsonPath().getString("result.effectiveName");

				if (!expected.toString().equals(actual)) {

					errors.add("round " + round + ": effective name of " + packages.get(i).get("name") + " is " + actual);
				}

				final long parents = countParentRelationships(id);
				if (parents != (i == 0 ? 0 : 1)) {

					errors.add("round " + round + ": " + packages.get(i).get("name") + " has " + parents + " parent relationships");
				}
			}
		}

		assertTrue("Concurrent requests failed: " + errors, errors.isEmpty());
	}

	// ----- private methods -----
	private long countParentRelationships(final String id) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(StructrTraits.FLOW_CONTAINER_PACKAGE, id);
			final long count         = Iterables.count(node.getIncomingRelationships(StructrTraits.FLOW_CONTAINER_PACKAGE_PACKAGE));

			tx.success();

			return count;

		} catch (FrameworkException fex) {

			fex.printStackTrace();

			return -1;
		}
	}

	private Response post(final String path, final Object body) {

		return RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers("X-User", "admin", "X-Password", "admin")
				.body(body)
			.when()
				.post("/" + path);
	}

	private Response get(final String path) {

		return RestAssured
			.given()
				.headers("X-User", "admin", "X-Password", "admin")
			.when()
				.get("/" + path);
	}

	private Response put(final String path, final Object body) {

		return RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers("X-User", "admin", "X-Password", "admin")
				.body(body)
			.when()
				.put("/" + path);
	}
}
