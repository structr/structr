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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.structr.api.DatabaseService;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.testng.SkipException;

import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.testng.AssertJUnit.fail;

/**
 * Runs the scenarios of the sample application over HTTP and measures them with and without the automatic
 * prefetching (database.prefetching.enabled), see measure().
 */
abstract class PrefetchingSuiteBase extends StructrUiTest {

	// the hints of the session store transactions belong to no scenario, they run beside every request
	private static final String SESSION_STORE_HINT = "StructrSessionDataStore";

	private final HttpClient http              = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
	private final Map<String, String> sessions = new HashMap<>();

	protected PrefetchingSampleData.Data data = null;

	/**
	 * How often a scenario runs in each phase of a measurement.
	 *
	 * @param learn   requests with prefetching before the measurement, in which the patterns are learned and evaluated
	 * @param warmup  pairs of requests (with and without prefetching) that are not measured
	 * @param samples pairs of requests that are measured
	 */
	record Plan(int learn, int warmup, int samples) {
	}

	/**
	 * A pattern as the prefetch statistics show it, with the status it had after the learning phase unless it
	 * changed during the measurement. The saved time is an estimate: the saved lookups times the average time of
	 * a lookup query.
	 */
	record PatternResult(String hint, String pattern, String status, long executions, double averageEntities, double averageMillis, double averageSavedLookups, double averageSavedMillis, long failedEvaluations) {
	}

	/**
	 * The figures per request are averages over the measured requests with and without prefetching.
	 */
	record ScenarioResult(PrefetchingScenario scenario, double roundTripMillis, int samples, double medianMillisWith, double medianMillisWithout,
		double allocatedMegabytesWith, double allocatedMegabytesWithout, double queriesWith, double queriesWithout, double lookupsWith, double lookupsWithout,
		double lookupMillis, List<PatternResult> patterns, int mismatches, String mismatchExample, List<String> errors) {

		double speedup() {

			return medianMillisWith > 0 ? medianMillisWithout / medianMillisWith : 0.0;
		}

		/**
		 * The share of the relationship lookups that prefetching answered instead of the database.
		 */
		double lookupCoverage() {

			return lookupsWithout > 0 ? 1.0 - lookupsWith / lookupsWithout : 0.0;
		}

		boolean learned() {

			return !patterns.isEmpty();
		}

		boolean prefetched() {

			return patterns.stream().anyMatch(p -> p.executions() > 0);
		}

		/**
		 * The outcome in one word: whether prefetching was involved at all, and if so, whether the requests were
		 * faster or slower with it. Differences below 5 percent count as none.
		 */
		String verdict() {

			if (!learned()) {

				return "nothing learned";
			}

			if (!prefetched()) {

				return "deactivated";
			}

			if (speedup() >= 1.05) {

				return "faster";
			}

			if (speedup() <= 0.95) {

				return "slower";
			}

			return "no difference";
		}
	}

	record ThroughputResult(PrefetchingScenario scenario, double roundTripMillis, int threads, int requests, double requestsPerSecondWith, double requestsPerSecondWithout) {
	}

	// ----- protected methods -----
	protected void requireBolt() {

		if (!Settings.DatabaseDriver.getValue().contains("bolt")) {

			throw new SkipException("Prefetching is a feature of the bolt driver, skipping test with " + Settings.DatabaseDriver.getValue());
		}
	}

	/**
	 * Creates the sample application and signs in its two users.
	 */
	protected void setUpSampleApplication(final PrefetchingSampleData.Scale scale) {

		try {

			createAdminUser();

			PrefetchingSampleData.createSchema(app);

			data = PrefetchingSampleData.createData(app, scale);

			PrefetchingSampleData.createPage(app, securityContext);
			PrefetchingSampleData.createMember(app, securityContext);

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unable to create the sample application: " + fex.getMessage());
		}

		grant("_login",          UiAuthenticator.NON_AUTH_USER_POST, true);
		grant("Project/_Report", UiAuthenticator.AUTH_USER_GET,      false);

		// sessions instead of X-User headers: a header login hashes the password on every request, which
		// takes longer than most of the requests and would bury what prefetching changes
		sessions.put(PrefetchingScenario.ADMIN,  login(ADMIN_USERNAME, ADMIN_PASSWORD));
		sessions.put(PrefetchingScenario.MEMBER, login(PrefetchingSampleData.MEMBER_NAME, PrefetchingSampleData.MEMBER_PASSWORD));
	}

	/**
	 * Measures a scenario: first it runs with prefetching, so that the patterns are learned and evaluated as in a
	 * running application, then the same requests are sent in pairs with and without prefetching. Which of the two
	 * runs first alternates, so that being first is worth the same to each of them.
	 *
	 * <p>The time is measured by the client and includes everything a user waits for. Queries and lookups come
	 * from the prefetch statistics, per request. The responses of each pair are compared: prefetching must not
	 * change what a request returns.
	 */
	protected ScenarioResult measure(final PrefetchingScenario scenario, final Plan plan, final double roundTripMillis) {

		final DatabaseService db  = Services.getInstance().getDatabaseService();
		final List<String> errors = new ArrayList<>();
		int n                     = 0;

		db.flushCaches();
		db.resetPrefetchStatistics();

		for (int i = 0; i < plan.learn(); i++) {

			check(scenario, send(scenario, n++, true), errors);
		}

		final Map<String, PatternResult> learned = patterns(db.getPrefetchStatistics(), Map.of());

		for (int i = 0; i < plan.warmup(); i++) {

			check(scenario, send(scenario, n, true), errors);
			check(scenario, send(scenario, n++, false), errors);
		}

		db.resetPrefetchStatistics();

		final double[] millisWith    = new double[plan.samples()];
		final double[] millisWithout = new double[plan.samples()];
		long allocatedWith           = 0L;
		long allocatedWithout        = 0L;
		int mismatches               = 0;
		String mismatchExample       = null;

		for (int i = 0; i < plan.samples(); i++, n++) {

			final Response with;
			final Response without;

			if (i % 2 == 0) {

				with    = send(scenario, n, true);
				without = send(scenario, n, false);

			} else {

				without = send(scenario, n, false);
				with    = send(scenario, n, true);
			}

			check(scenario, with, errors);
			check(scenario, without, errors);

			millisWith[i]     = with.millis();
			millisWithout[i]  = without.millis();
			allocatedWith    += with.allocatedBytes();
			allocatedWithout += without.allocatedBytes();

			if (scenario.readOnly() && !fingerprint(with).equals(fingerprint(without))) {

				mismatches++;

				if (mismatchExample == null) {

					mismatchExample = scenario.request().apply(n) + "\nwith prefetching:    " + abbreviate(fingerprint(with)) + "\nwithout prefetching: " + abbreviate(fingerprint(without));
				}
			}
		}

		final Map<String, Object> statistics = db.getPrefetchStatistics();
		final double[] with                  = totals(statistics, true);
		final double[] without               = totals(statistics, false);
		final int samples                    = plan.samples();

		return new ScenarioResult(scenario, roundTripMillis, samples, median(millisWith), median(millisWithout),
			allocatedWith / 1_048_576.0 / samples, allocatedWithout / 1_048_576.0 / samples,
			with[0] / samples, without[0] / samples, with[1] / samples, without[1] / samples,
			(Double) statistics.getOrDefault("averageLookupMillis", 0.0), new ArrayList<>(patterns(statistics, learned).values()), mismatches, mismatchExample, errors
		);
	}

	/**
	 * Measures the requests per second of a scenario with several clients at the same time, after the patterns
	 * have been learned by a measure() of the same scenario.
	 */
	protected ThroughputResult measureThroughput(final PrefetchingScenario scenario, final int threads, final int requestsPerThread, final double roundTripMillis) {

		final double with    = throughput(scenario, threads, requestsPerThread, true);
		final double without = throughput(scenario, threads, requestsPerThread, false);

		return new ThroughputResult(scenario, roundTripMillis, threads, threads * requestsPerThread, with, without);
	}

	protected String fingerprint(final Response response) {

		final String body = response.body();

		// JSON responses contain timing information, only the result is compared
		if (body.startsWith("{")) {

			final JsonObject json = JsonParser.parseString(body).getAsJsonObject();
			final JsonElement result = json.get("result");

			return result != null ? result.toString() : body;
		}

		return body;
	}

	record Response(int status, String body, double millis, long allocatedBytes) {
	}

	/**
	 * Sends the n-th request of a scenario with or without prefetching.
	 */
	protected Response send(final PrefetchingScenario scenario, final int n, final boolean prefetching) {

		Settings.PrefetchingEnabled.setValue(prefetching);

		try {

			final long allocated0 = allocatedBytes();
			final long t0         = System.nanoTime();

			final HttpResponse<String> response = http.send(request(scenario, n), HttpResponse.BodyHandlers.ofString());

			final double millis   = (System.nanoTime() - t0) / 1_000_000.0;
			final long allocated  = allocatedBytes() - allocated0;

			return new Response(response.statusCode(), response.body(), millis, allocated);

		} catch (Exception ex) {

			return new Response(-1, ex.toString(), 0.0, 0L);

		} finally {

			Settings.PrefetchingEnabled.setValue(true);
		}
	}

	// ----- private methods -----
	private String login(final String name, final String password) {

		try {

			final HttpRequest request = HttpRequest.newBuilder(URI.create("http://" + host + ":" + httpPort + "/structr/rest/login"))
				.header("Content-Type", "application/json; charset=UTF-8")
				.POST(HttpRequest.BodyPublishers.ofString("{ \"name\": \"" + name + "\", \"password\": \"" + password + "\" }"))
				.build();

			final HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

			for (final String cookie : response.headers().allValues("Set-Cookie")) {

				if (cookie.startsWith("JSESSIONID=")) {

					return cookie.substring("JSESSIONID=".length(), cookie.indexOf(';') > 0 ? cookie.indexOf(';') : cookie.length());
				}
			}

			fail("Login of " + name + " returned no session: " + response.statusCode() + " " + response.body());

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Login of " + name + " failed: " + ex.getMessage());
		}

		return null;
	}

	private double throughput(final PrefetchingScenario scenario, final int threads, final int requestsPerThread, final boolean prefetching) {

		final ExecutorService executor = Executors.newFixedThreadPool(threads);
		final List<Future<?>> futures   = new ArrayList<>();

		// the setting is switched once for the whole run, not per request as in send()
		Settings.PrefetchingEnabled.setValue(prefetching);

		try {

			final long t0 = System.nanoTime();

			for (int t = 0; t < threads; t++) {

				final int thread = t;

				futures.add(executor.submit(() -> {

					for (int i = 0; i < requestsPerThread; i++) {

						sendWithoutSwitching(scenario, thread * requestsPerThread + i);
					}
				}));
			}

			for (final Future<?> future : futures) {

				future.get();
			}

			return threads * requestsPerThread / ((System.nanoTime() - t0) / 1_000_000_000.0);

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Throughput measurement failed: " + ex.getMessage());

			return 0.0;

		} finally {

			executor.shutdown();

			Settings.PrefetchingEnabled.setValue(true);
		}
	}

	private void sendWithoutSwitching(final PrefetchingScenario scenario, final int n) {

		try {

			http.send(request(scenario, n), HttpResponse.BodyHandlers.discarding());

		} catch (Exception ex) {

			throw new RuntimeException(ex);
		}
	}

	private HttpRequest request(final PrefetchingScenario scenario, final int n) {

		final PrefetchingScenario.Request request = scenario.request().apply(n);
		final HttpRequest.Builder builder         = HttpRequest.newBuilder(URI.create("http://" + host + ":" + httpPort + request.path()))
			.header("Cookie", "JSESSIONID=" + sessions.get(scenario.user()));

		if ("POST".equals(request.method())) {

			builder.header("Content-Type", "application/json; charset=UTF-8").POST(HttpRequest.BodyPublishers.ofString("{}"));

		} else {

			builder.GET();
		}

		return builder.build();
	}

	private void check(final PrefetchingScenario scenario, final Response response, final List<String> errors) {

		if (response.status() != 200 && errors.size() < 5) {

			errors.add(scenario.request().apply(0).path() + ": " + response.status() + " " + abbreviate(response.body()));
		}
	}

	/**
	 * Sums transactions, queries and lookup queries over all hints except the session store.
	 *
	 * @return queries and lookup queries in total
	 */
	private double[] totals(final Map<String, Object> statistics, final boolean withPrefetching) {

		double queries = 0.0;
		double lookups = 0.0;

		for (final Map<String, Object> hint : hints(statistics)) {

			if (((String) hint.get("hint")).startsWith(SESSION_STORE_HINT)) {

				continue;
			}

			final Map<String, Object> tx = (Map<String, Object>) hint.get(withPrefetching ? "withPrefetching" : "withoutPrefetching");
			final long count             = (Long) tx.get("transactions");

			queries += count * (Double) tx.get("averageQueries");
			lookups += count * (Double) tx.get("averageLookupQueries");
		}

		return new double[] { queries, lookups };
	}

	private Map<String, PatternResult> patterns(final Map<String, Object> statistics, final Map<String, PatternResult> learned) {

		final Map<String, PatternResult> patterns = new LinkedHashMap<>();

		// patterns that were learned but did not run during the measurement are part of the result as well
		patterns.putAll(learned);

		for (final Map<String, Object> hint : hints(statistics)) {

			final String hintName = (String) hint.get("hint");

			for (final Map<String, Object> pattern : (List<Map<String, Object>>) hint.get("patterns")) {

				final String name           = (String) pattern.get("pattern");
				final String key            = hintName + " | " + name;
				final PatternResult before  = learned.get(key);
				final String status         = pattern.get("status") != null ? (String) pattern.get("status") : before != null ? before.status() : null;

				patterns.put(key, new PatternResult(hintName, name, status, (Long) pattern.get("executions"), (Double) pattern.get("averageEntities"), (Double) pattern.get("averageMillis"),
					(Double) pattern.get("averageSavedLookups"), (Double) pattern.get("averageSavedMillis"), (Long) pattern.get("failedEvaluations")));
			}
		}

		return patterns;
	}

	private List<Map<String, Object>> hints(final Map<String, Object> statistics) {

		return (List<Map<String, Object>>) statistics.getOrDefault("hints", List.of());
	}

	/**
	 * The bytes allocated by all threads of the JVM so far, including the server threads that handle the request.
	 */
	private static long allocatedBytes() {

		if (ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean bean) {

			return bean.getTotalThreadAllocatedBytes();
		}

		return 0L;
	}

	private static double median(final double[] values) {

		if (values.length == 0) {

			return 0.0;
		}

		final double[] sorted = values.clone();

		Arrays.sort(sorted);

		return sorted[sorted.length / 2];
	}

	private static String abbreviate(final String text) {

		return text.length() > 300 ? text.substring(0, 300) + "..." : text;
	}
}
