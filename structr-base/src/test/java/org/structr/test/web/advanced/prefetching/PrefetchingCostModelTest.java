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
package org.structr.test.web.advanced.prefetching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.schema.export.StructrSchema;
import org.structr.test.web.StructrUiTest;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.fail;

/**
 * Measures when a learned prefetch pays off. A learned pattern like (n:Item)-[r:HAS]->(m) loads the relationships of
 * all nodes of a type, so its cost grows with the database, while what it saves grows with the number of lookups
 * of the request. The test measures both sides for a range of database sizes and of request sizes (the share of
 * the items whose parts a request looks up) and logs a table of the time per request with and without prefetching.
 *
 * <p>From the same runs it derives the two figures of the cost model: the time of one relationship lookup query,
 * and the time to load one entity in a prefetch. Their ratio is the number of entities a prefetch can load per saved
 * lookup before it costs more than it saves, which is what database.prefetching.costratio is meant to be. The
 * lookup time includes a network round trip to the database, so the ratio measured against a local database is a
 * lower bound for one on another host.
 *
 * <p>Like SitePerformanceTest, the test has no time limits, because the times depend on the machine. It only checks
 * that the prefetch ran in every measured request, so that the figures compare what they claim to compare. It is
 * part of the prefetching benchmark and runs with -Dprefetching.benchmark=true only, see PrefetchingBenchmarkTest.
 */
public class PrefetchingCostModelTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(PrefetchingCostModelTest.class);

	private static final int[] DATABASE_SIZES    = { 1_000, 4_000, 16_000 };
	private static final double[] REQUEST_SHARES = { 0.01, 0.05, 0.25, 1.0 };
	private static final int SAMPLES             = 5;
	private static final int WARMUP              = 1;

	@Test
	public void testCostOfPrefetchingComparedToLookups() {

		if (!Boolean.getBoolean(PrefetchingBenchmarkTest.ENABLED)) {

			throw new SkipException("The prefetching benchmark only runs with -D" + PrefetchingBenchmarkTest.ENABLED + "=true");
		}

		if (!Settings.DatabaseDriver.getValue().contains("bolt")) {

			throw new SkipException("Prefetching is a feature of the bolt driver, skipping test with " + Settings.DatabaseDriver.getValue());
		}

		createItemSchema();

		final List<String> table      = new ArrayList<>();
		final List<double[]> perSize  = new ArrayList<>();
		int items                     = 0;

		try {

			// the smallest request must exceed the threshold to learn the pattern; nothing may be deactivated,
			// because the measurement must compare the same requests with and without the prefetch
			Settings.PrefetchingThreshold.setValue(5);
			Settings.PrefetchingCostRatio.setValue(Integer.MAX_VALUE);
			Settings.PrefetchingMaxCount.setValue(Integer.MAX_VALUE);
			Settings.PrefetchingMaxDuration.setValue(Integer.MAX_VALUE);
			Settings.PrefetchingCompareDuration.setValue(false);

			table.add(String.format("%8s %8s %10s %12s %12s %12s %8s", "items", "lookups", "entities", "without ms", "with ms", "prefetch ms", "with %"));

			for (final int size : DATABASE_SIZES) {

				createItems(items, size);
				items = size;

				double lookupMillis = 0.0;
				double entityMillis = 0.0;

				for (final double share : REQUEST_SHARES) {

					final int lookups = (int) Math.round(size * share);
					final String hint = "Prefetching cost model: " + size + " items, " + lookups + " lookups";

					final double[] with    = new double[SAMPLES];
					final double[] without = new double[SAMPLES];

					// the first request learns the pattern
					timeRequest(hint, lookups, true);

					for (int i = 0; i < WARMUP; i++) {

						timeRequest(hint, lookups, true);
						timeRequest(hint, lookups, false);
					}

					Services.getInstance().getDatabaseService().resetPrefetchStatistics();

					// the two are swapped every other pair, so being measured first is worth the same to each of them
					for (int i = 0; i < SAMPLES; i++) {

						if (i % 2 == 0) {

							with[i]    = timeRequest(hint, lookups, true);
							without[i] = timeRequest(hint, lookups, false);

						} else {

							without[i] = timeRequest(hint, lookups, false);
							with[i]    = timeRequest(hint, lookups, true);
						}
					}

					final Map<String, Object> pattern = pattern(hint);
					final double prefetchMillis       = (Double) pattern.get("averageMillis");
					final double entities             = (Double) pattern.get("averageEntities");
					final double medianWith           = median(with);
					final double medianWithout        = median(without);

					// the statistics were reset after learning, so a status is only there if something changed since
					assertEquals("The prefetch did not run in every measured request", (long) SAMPLES, pattern.get("executions"));
					assertNull("The prefetch was deactivated during the measurement", pattern.get("status"));

					table.add(String.format("%8d %8d %10.0f %12.1f %12.1f %12.1f %8.0f", size, lookups, entities, medianWithout, medianWith, prefetchMillis, 100.0 * medianWith / medianWithout));

					// the largest request has the most lookups and the most reliable time per lookup
					lookupMillis = medianWithout / lookups;
					entityMillis = prefetchMillis / entities;
				}

				perSize.add(new double[] { size, lookupMillis, entityMillis });
			}

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");

		} finally {

			Settings.PrefetchingThreshold.setValue(Settings.PrefetchingThreshold.getDefaultValue());
			Settings.PrefetchingCostRatio.setValue(Settings.PrefetchingCostRatio.getDefaultValue());
			Settings.PrefetchingMaxCount.setValue(Settings.PrefetchingMaxCount.getDefaultValue());
			Settings.PrefetchingMaxDuration.setValue(Settings.PrefetchingMaxDuration.getDefaultValue());
			Settings.PrefetchingCompareDuration.setValue(Settings.PrefetchingCompareDuration.getDefaultValue());
		}

		logger.info("Prefetching cost model: median time per request of {} pairs, alternating with and without prefetching", SAMPLES);

		for (final String line : table) {

			logger.info("  {}", line);
		}

		for (final double[] figures : perSize) {

			logger.info("  {} items: {} ms per lookup query, {} ms per prefetched entity, break-even at {} entities per saved lookup ({} is {})",
				(int) figures[0], format(figures[1], 3), format(figures[2], 5), format(figures[1] / figures[2], 0),
				Settings.PrefetchingCostRatio.getKey(), Settings.PrefetchingCostRatio.getValue()
			);
		}
	}

	// ----- private methods -----
	private void createItemSchema() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema     = StructrSchema.createFromDatabase(app);
			final JsonObjectType item   = schema.addType("Item");
			final JsonObjectType part   = schema.addType("Part");

			item.addIntegerProperty("index").setIndexed(true);
			item.relate(part, "HAS", Cardinality.OneToMany, "item", "parts");

			StructrSchema.replaceDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {

			t.printStackTrace();
			fail("Unexpected exception");
		}
	}

	/**
	 * Adds items with one part each until there are the given number of items.
	 */
	private void createItems(final int from, final int to) throws FrameworkException {

		final PropertyKey index = Traits.of("Item").key("index");
		final PropertyKey item  = Traits.of("Part").key("item");
		final int batchSize     = 1000;

		for (int start = from; start < to; start += batchSize) {

			try (final Tx tx = app.tx()) {

				for (int i = start; i < Math.min(start + batchSize, to); i++) {

					final NodeInterface node = app.create("Item", "item" + i);

					node.setProperty(index, i);

					app.create("Part", "part" + i).setProperty(item, node);
				}

				tx.success();
			}
		}
	}

	/**
	 * Simulates a request that looks up the parts of the given number of items, one lookup per item, with or
	 * without prefetching.
	 *
	 * @return the time of the request in milliseconds
	 */
	private double timeRequest(final String hint, final int lookups, final boolean prefetching) throws FrameworkException {

		final PropertyKey<Integer> index = Traits.of("Item").key("index");
		final PropertyKey parts          = Traits.of("Item").key("parts");

		Settings.PrefetchingEnabled.setValue(prefetching);

		try {

			final long start = System.nanoTime();

			try (final Tx tx = app.tx()) {

				tx.prefetchHint(hint);

				for (final NodeInterface item : app.nodeQuery("Item").range(index, 0, lookups - 1).getAsList()) {

					for (final Object part : (Iterable) item.getProperty(parts)) {

						((NodeInterface) part).getUuid();
					}
				}

				tx.success();
			}

			return (System.nanoTime() - start) / 1_000_000.0;

		} finally {

			Settings.PrefetchingEnabled.setValue(true);
		}
	}

	private Map<String, Object> pattern(final String hint) {

		final Map<String, Object> statistics = Services.getInstance().getDatabaseService().getPrefetchStatistics();

		for (final Map<String, Object> entry : (List<Map<String, Object>>) statistics.get("hints")) {

			if (hint.equals(entry.get("hint"))) {

				final List<Map<String, Object>> patterns = (List<Map<String, Object>>) entry.get("patterns");

				if (!patterns.isEmpty()) {

					return patterns.get(0);
				}
			}
		}

		fail("No learned pattern for " + hint);

		return null;
	}

	private double median(final double[] values) {

		final double[] sorted = values.clone();

		Arrays.sort(sorted);

		return sorted[sorted.length / 2];
	}

	private String format(final double value, final int decimals) {

		return String.format("%." + decimals + "f", value);
	}
}
