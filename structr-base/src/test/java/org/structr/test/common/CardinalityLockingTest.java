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
package org.structr.test.common;

import org.structr.api.RetryException;
import org.structr.api.graph.Cardinality;
import org.structr.api.graph.Direction;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.schema.export.StructrSchema;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Sets a relationship with cardinality 1 from several transactions at the same time, each to a different node. Every
 * transaction reads the current value first and waits until all others have read it too, so all of them find the same
 * relationship before one of them replaces it. Exactly one relationship must remain, otherwise ensureCardinality did
 * not see the relationships of the transactions that committed before it.
 */
public class CardinalityLockingTest extends StructrTest {

	private static final int ROUNDS  = 20;
	private static final int THREADS = 10;

	@Test
	public void testConcurrentlySetOneToMany() {

		createTypes();

		final PropertyKey<NodeInterface> containerKey = Traits.of("Item").key("container");
		final List<String> errors                     = new ArrayList<>();

		for (int round = 0; round < ROUNDS; round++) {

			final String item             = create("Item", 1).get(0);
			final List<String> containers = create("Container", THREADS);

			errors.addAll(runConcurrently(round, (index, awaitOthers) -> {

				final NodeInterface node = app.getNodeById("Item", item);

				node.getProperty(containerKey);
				awaitOthers.run();
				node.setProperty(containerKey, app.getNodeById("Container", containers.get(index)));
			}));

			errors.addAll(checkSingleRelationship(round, "Item", item, "CONTAINS", Direction.INCOMING));
		}

		assertTrue("Concurrent writes failed: " + errors, errors.isEmpty());
	}

	@Test
	public void testConcurrentlySetManyToOne() {

		createTypes();

		final PropertyKey<NodeInterface> categoryKey = Traits.of("Item").key("category");
		final List<String> errors                 = new ArrayList<>();

		for (int round = 0; round < ROUNDS; round++) {

			final String item         = create("Item", 1).get(0);
			final List<String> categories = create("Category", THREADS);

			errors.addAll(runConcurrently(round, (index, awaitOthers) -> {

				final NodeInterface node = app.getNodeById("Item", item);

				node.getProperty(categoryKey);
				awaitOthers.run();
				node.setProperty(categoryKey, app.getNodeById("Category", categories.get(index)));
			}));

			errors.addAll(checkSingleRelationship(round, "Item", item, "BELONGS_TO", Direction.OUTGOING));
		}

		assertTrue("Concurrent writes failed: " + errors, errors.isEmpty());
	}

	@Test
	public void testConcurrentlySetOneToOne() {

		createTypes();

		final PropertyKey<NodeInterface> partnerKey = Traits.of("Item").key("partner");
		final List<String> errors                   = new ArrayList<>();

		for (int round = 0; round < ROUNDS; round++) {

			// different items take the same partner, so the lock of the target node is what counts here
			final String partner     = create("Partner", 1).get(0);
			final List<String> items = create("Item", THREADS);

			errors.addAll(runConcurrently(round, (index, awaitOthers) -> {

				final NodeInterface node = app.getNodeById("Partner", partner);

				node.getProperty(Traits.of("Partner").key("item"));
				awaitOthers.run();
				app.getNodeById("Item", items.get(index)).setProperty(partnerKey, node);
			}));

			errors.addAll(checkSingleRelationship(round, "Partner", partner, "PAIRED_WITH", Direction.INCOMING));
		}

		assertTrue("Concurrent writes failed: " + errors, errors.isEmpty());
	}

	// ----- private methods -----
	private void createTypes() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema        = StructrSchema.createFromDatabase(app);
			final JsonObjectType container = schema.addType("Container");
			final JsonObjectType item      = schema.addType("Item");
			final JsonObjectType category  = schema.addType("Category");
			final JsonObjectType partner   = schema.addType("Partner");

			container.relate(item, "CONTAINS", Cardinality.OneToMany, "container", "items");
			item.relate(category, "BELONGS_TO", Cardinality.ManyToOne, "items", "category");
			item.relate(partner, "PAIRED_WITH", Cardinality.OneToOne, "item", "partner");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}
	}

	private List<String> create(final String type, final int count) {

		final List<String> ids = new ArrayList<>();

		try (final Tx tx = app.tx()) {

			for (int i = 0; i < count; i++) {

				ids.add(app.create(type, type + i).getUuid());
			}

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		return ids;
	}

	/**
	 * Runs the write once per thread, each in its own transaction, and repeats a transaction that the database
	 * terminated, like the REST servlets do.
	 */
	private List<String> runConcurrently(final int round, final Write write) {

		final ExecutorService executor  = Executors.newFixedThreadPool(THREADS);
		final CyclicBarrier barrier     = new CyclicBarrier(THREADS);
		final List<Future<?>> futures   = new ArrayList<>();
		final List<String> errors       = new ArrayList<>();

		for (int i = 0; i < THREADS; i++) {

			final int index = i;

			futures.add(executor.submit(() -> {

				final boolean[] waited = { false };
				boolean retry          = true;
				int attempts           = 0;

				while (retry) {

					try (final Tx tx = app.tx()) {

						write.run(index, () -> {

							// only the first attempt waits, a repetition runs after the others have written
							if (!waited[0]) {

								waited[0] = true;

								try {

									barrier.await(30, TimeUnit.SECONDS);

								} catch (Exception ex) {

									throw new RuntimeException(ex);
								}
							}
						});

						tx.success();

						retry = false;

					} catch (RetryException rex) {

						retry = RetryException.retry(rex, ++attempts);
					}
				}

				return null;
			}));
		}

		for (final Future<?> future : futures) {

			try {

				future.get();

			} catch (Throwable t) {

				errors.add("round " + round + ": " + t.getMessage());
			}
		}

		executor.shutdown();

		return errors;
	}

	/**
	 * Counts the relationships in the database, because reading the property through Structr returns only the first.
	 */
	private List<String> checkSingleRelationship(final int round, final String type, final String id, final String relationshipType, final Direction direction) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(type, id);
			final long count         = Iterables.count(Iterables.filter(r -> relationshipType.equals(r.getType().name()), node.getNode().getRelationships(direction)));

			tx.success();

			if (count != 1) {

				return List.of("round " + round + ": " + count + " " + relationshipType + " relationships");
			}

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		return List.of();
	}

	// ----- nested interfaces -----
	@FunctionalInterface
	private interface Write {

		void run(final int index, final Runnable awaitOthers) throws FrameworkException;
	}
}
