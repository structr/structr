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
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Two transactions change the same two nodes in opposite order, so the database terminates one of them to resolve the
 * deadlock. The code in the terminated transaction catches the RetryException, like many catch blocks in Structr do.
 * The transaction must not commit its first change anyway, but end with a RetryException so that it is repeated.
 * Each transaction sets its own property, so the other one cannot overwrite a partial commit.
 */
public class TerminatedTransactionTest extends StructrTest {

	private static final int ROUNDS = 10;

	@Test
	public void testSwallowedRetryExceptionRepeatsTransaction() {

		createTypes();

		final List<String> errors    = new ArrayList<>();
		final AtomicInteger attempts = new AtomicInteger();

		for (int round = 0; round < ROUNDS; round++) {

			final List<String> items       = create("Item", 2);
			final ExecutorService executor = Executors.newFixedThreadPool(2);
			final CyclicBarrier barrier    = new CyclicBarrier(2);
			final List<Future<?>> futures  = new ArrayList<>();

			for (int i = 0; i < 2; i++) {

				final PropertyKey<Boolean> key = Traits.of("Item").key("writtenBy" + i);
				final List<String> order       = i == 0 ? List.of(items.get(0), items.get(1)) : List.of(items.get(1), items.get(0));

				futures.add(executor.submit(() -> {

					final boolean[] waited = { false };

					RetryException.repeat(() -> {

						attempts.incrementAndGet();

						try (final Tx tx = app.tx()) {

							app.getNodeById("Item", order.get(0)).setProperty(key, true);

							// only the first attempt waits, a repetition runs after the other transaction has committed
							if (!waited[0]) {

								waited[0] = true;

								barrier.await(30, TimeUnit.SECONDS);
							}

							try {

								// written to the database directly, because the modification queue would write the node again at commit
								app.getNodeById("Item", order.get(1)).getNode().setProperty(key.dbName(), true);

							} catch (RuntimeException swallowed) {

								// the RetryException is caught here and the transaction goes on as if nothing happened
							}

							tx.success();
						}
					});

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

			try (final Tx tx = app.tx()) {

				// both transactions changed both nodes, a partial commit leaves a property unset
				for (final String item : items) {

					for (int i = 0; i < 2; i++) {

						final Boolean written = app.getNodeById("Item", item).getProperty(Traits.of("Item").key("writtenBy" + i));
						if (!Boolean.TRUE.equals(written)) {

							errors.add("round " + round + ": " + item + " was not written by transaction " + i);
						}
					}
				}

				tx.success();

			} catch (FrameworkException fex) {

				fex.printStackTrace();
				fail("Unexpected exception");
			}
		}

		assertTrue("Concurrent writes failed: " + errors, errors.isEmpty());

		// two attempts per round mean that the database never terminated a transaction, so the test did not test anything
		assertTrue("No transaction was terminated", attempts.get() > 2 * ROUNDS);
	}

	// ----- private methods -----
	private void createTypes() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);

			final JsonObjectType item = schema.addType("Item");

			item.addBooleanProperty("writtenBy0");
			item.addBooleanProperty("writtenBy1");

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
}
