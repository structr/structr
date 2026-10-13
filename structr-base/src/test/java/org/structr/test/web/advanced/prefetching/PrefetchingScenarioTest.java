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

import org.structr.api.config.Settings;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Runs every scenario of the sample application on a small data set, through the real entry points (REST, page
 * rendering, schema methods) and with a real login, once with and once without the automatic prefetching.
 *
 * <p>The threshold is lowered so that the small data set is enough to learn patterns. The test requires that
 * prefetching never changes a response, and that it learns and uses patterns for the requests it is made for,
 * lists that use every task of the database, also when some requests of the same kind ask for an empty page.
 * How much it saves is the subject of PrefetchingBenchmarkTest, but the report of this run is written as well,
 * see PrefetchingReport.
 */
public class PrefetchingScenarioTest extends PrefetchingSuiteBase {

	private static final Set<String> PREFETCHED = Set.of("rest-list-nested", "rest-task-page", "rest-task-page-sometimes-empty");

	@AfterMethod
	public void restorePrefetchSettings() {

		Settings.PrefetchingThreshold.setValue(Settings.PrefetchingThreshold.getDefaultValue());
		Settings.PrefetchingCompareDuration.setValue(Settings.PrefetchingCompareDuration.getDefaultValue());
	}

	@Test
	public void testPrefetchingChangesNoResponseOfTheSampleApplication() throws IOException {

		requireBolt();

		setUpSampleApplication(PrefetchingSampleData.SMALL);

		Settings.PrefetchingThreshold.setValue(5);

		// the test asserts that prefetching saves lookups, which a prefetch deactivated because it takes longer than
		// the lookups it saves on this machine would not; the benchmark measures that comparison
		Settings.PrefetchingCompareDuration.setValue(false);

		final PrefetchingReport report = new PrefetchingReport("Prefetching scenarios, small data set");
		final Plan plan                = new Plan(3, 0, 2);

		report.setup("Data: " + PrefetchingSampleData.SMALL);
		report.setup("Plan: " + plan);

		for (final PrefetchingScenario scenario : PrefetchingScenario.all(data)) {

			final ScenarioResult result = measure(scenario, plan, 0.0);

			report.add(result);

			assertTrue("Requests of " + scenario.name() + " failed: " + result.errors(), result.errors().isEmpty());
			assertEquals("Prefetching changed responses of " + scenario.name() + ": " + result.mismatchExample(), 0, result.mismatches());

			if (PREFETCHED.contains(scenario.name())) {

				assertTrue("No pattern was learned for " + scenario.name(), result.learned());
				assertTrue("No learned pattern ran in " + scenario.name(), result.prefetched());
				assertTrue("Prefetching answered no lookup in " + scenario.name(), result.lookupsWith() < result.lookupsWithout());
			}
		}

		report.write();
	}
}
