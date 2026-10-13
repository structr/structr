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
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Measures what the automatic prefetching does for the requests of the sample application: for every scenario and
 * every added round-trip time, how many lookups it answers, how many queries and how much time it saves or costs,
 * and how much memory it uses. With several clients at once, it also measures the throughput. The results are
 * written as a report, see PrefetchingReport.
 *
 * <p>Structr talks to the database through DelayProxy, which adds the round-trip time of a database on another
 * host. Prefetching replaces many queries with one, so how much it saves depends on that time more than on
 * anything else, and a measurement against a local database alone would understate it.
 *
 * <p>The prefetching settings are the defaults, because the measurement is about the behaviour that ships. Only the
 * threshold can be set, to measure another default.
 *
 * <p>The measurement takes from about ten minutes (medium) to hours (large), so it only runs when asked for:
 * <pre>
 * mvn -pl structr-base verify -Dit.test=PrefetchingBenchmarkTest -Dprefetching.benchmark=true ...
 * </pre>
 * Optional system properties: prefetching.benchmark.scale (small, medium, large; default medium),
 * prefetching.benchmark.rtt (added round-trip times in ms; default 0,1,3), prefetching.benchmark.samples (pairs
 * per scenario; default 5), prefetching.benchmark.threads (concurrent clients; default 8),
 * prefetching.benchmark.threshold (database.prefetching.threshold; default its default value).
 */
public class PrefetchingBenchmarkTest extends PrefetchingSuiteBase {

	private static final Logger logger = LoggerFactory.getLogger(PrefetchingBenchmarkTest.class);

	static final String ENABLED = "prefetching.benchmark";

	// the scenarios whose throughput is measured: one that uses the whole database, one per object, one page
	private static final Set<String> THROUGHPUT_SCENARIOS = Set.of("rest-list-nested", "rest-detail-mixed", "page-nested-repeaters");

	private DelayProxy proxy = null;

	@Override
	protected void setupDatabaseConnection(final String testDatabaseConnection) {

		super.setupDatabaseConnection(testDatabaseConnection);

		if (Boolean.getBoolean(ENABLED)) {

			try {

				final URI database = URI.create(Settings.ConnectionUrl.getValue());

				proxy = new DelayProxy(database.getHost(), database.getPort());

				Settings.ConnectionUrl.setValue(database.getScheme() + "://localhost:" + proxy.getPort());

			} catch (IOException ioex) {

				throw new RuntimeException("Unable to start the delay proxy", ioex);
			}
		}
	}

	@AfterMethod
	public void restorePrefetchSettings() {

		Settings.PrefetchingThreshold.setValue(Settings.PrefetchingThreshold.getDefaultValue());
	}

	@Test
	public void testWhatPrefetchingSavesInTheSampleApplication() throws IOException {

		if (!Boolean.getBoolean(ENABLED)) {

			throw new SkipException("The prefetching benchmark only runs with -D" + ENABLED + "=true");
		}

		requireBolt();

		final PrefetchingSampleData.Scale scale = PrefetchingSampleData.scale(System.getProperty(ENABLED + ".scale", "medium"));
		final List<Double> roundTrips           = roundTrips(System.getProperty(ENABLED + ".rtt", "0,1,3"));
		final int samples                       = Integer.getInteger(ENABLED + ".samples", 5);
		final int threads                       = Integer.getInteger(ENABLED + ".threads", 8);
		final int threshold                     = Integer.getInteger(ENABLED + ".threshold", Settings.PrefetchingThreshold.getDefaultValue());
		final Plan plan                         = new Plan(3, 2, samples);
		final PrefetchingReport report          = new PrefetchingReport("Prefetching benchmark, " + scale.name() + " data set");
		final long t0                           = System.currentTimeMillis();

		Settings.PrefetchingThreshold.setValue(threshold);

		setUpSampleApplication(scale);

		report.setup("Data: " + scale + ", created in " + (System.currentTimeMillis() - t0) / 1000 + " s");
		report.setup("Plan: " + plan + ", throughput with " + threads + " clients");
		report.setup("Added round-trip times (ms): " + roundTrips);

		final List<ScenarioResult> results = new ArrayList<>();

		for (final double rtt : roundTrips) {

			proxy.setRoundTripMillis(rtt);

			for (final PrefetchingScenario scenario : PrefetchingScenario.all(data)) {

				final ScenarioResult result = measure(scenario, plan, rtt);

				report.add(result);
				results.add(result);

				if (THROUGHPUT_SCENARIOS.contains(scenario.name())) {

					report.add(measureThroughput(scenario, threads, 3, rtt));
				}
			}
		}

		report.setup("Duration: " + (System.currentTimeMillis() - t0) / 60_000 + " min");

		logger.info("Prefetching benchmark report: {}", report.write().toAbsolutePath());

		// the measurement has no time limits, but a different response is a bug whatever the timing
		for (final ScenarioResult result : results) {

			assertTrue("Prefetching changed responses of " + result.scenario().name() + ": " + result.mismatchExample(), result.mismatches() == 0);
		}
	}

	private List<Double> roundTrips(final String value) {

		final List<Double> roundTrips = new ArrayList<>();

		for (final String part : value.split(",")) {

			try {

				roundTrips.add(Double.parseDouble(part.trim()));

			} catch (NumberFormatException nfex) {

				fail("Invalid round-trip time in " + ENABLED + ".rtt: " + part);
			}
		}

		return roundTrips;
	}
}
