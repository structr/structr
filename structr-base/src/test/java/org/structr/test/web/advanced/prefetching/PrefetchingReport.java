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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Writes the results of a prefetching measurement as a Markdown report and a CSV file to target/prefetching-report,
 * and the summary to the log. The files carry the time of the run in their name, so the reports of several runs
 * (before and after a change, on different machines) can be compared.
 */
final class PrefetchingReport {

	private static final Logger logger = LoggerFactory.getLogger(PrefetchingReport.class);

	private final List<PrefetchingSuiteBase.ScenarioResult> results       = new ArrayList<>();
	private final List<PrefetchingSuiteBase.ThroughputResult> throughputs = new ArrayList<>();
	private final List<String> setup                                      = new ArrayList<>();
	private final String title;

	PrefetchingReport(final String title) {

		this.title = title;
	}

	void setup(final String line) {

		setup.add(line);
	}

	void add(final PrefetchingSuiteBase.ScenarioResult result) {

		results.add(result);

		logger.info("{} at +{} ms RTT: {}, {} ms with / {} ms without prefetching, {} lookups with / {} without", result.scenario().name(), format(result.roundTripMillis(), 1),
			result.verdict(), format(result.medianMillisWith(), 1), format(result.medianMillisWithout(), 1), format(result.lookupsWith(), 0), format(result.lookupsWithout(), 0));
	}

	void add(final PrefetchingSuiteBase.ThroughputResult result) {

		throughputs.add(result);
	}

	/**
	 * Writes the report and returns the path of the Markdown file.
	 */
	Path write() throws IOException {

		final String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
		final Path directory   = Path.of("target", "prefetching-report");
		final Path markdown    = directory.resolve("prefetching-" + timestamp + ".md");
		final Path csv         = directory.resolve("prefetching-" + timestamp + ".csv");

		Files.createDirectories(directory);
		Files.writeString(markdown, markdown());
		Files.writeString(csv, csv());

		for (final String line : summary()) {

			logger.info(line);
		}

		logger.info("Prefetching report written to {}", markdown.toAbsolutePath());

		return markdown;
	}

	String markdown() {

		final StringBuilder buf = new StringBuilder();

		buf.append("# ").append(title).append("\n\n");

		for (final String line : setup) {

			buf.append("- ").append(line).append("\n");
		}

		buf.append("- Settings: ");
		buf.append(Settings.PrefetchingThreshold.getKey()).append(" = ").append(Settings.PrefetchingThreshold.getValue()).append(", ");
		buf.append(Settings.PrefetchingCostRatio.getKey()).append(" = ").append(Settings.PrefetchingCostRatio.getValue()).append(", ");
		buf.append(Settings.PrefetchingMaxCount.getKey()).append(" = ").append(Settings.PrefetchingMaxCount.getValue()).append(", ");
		buf.append(Settings.PrefetchingMaxDuration.getKey()).append(" = ").append(Settings.PrefetchingMaxDuration.getValue()).append(", ");
		buf.append(Settings.PrefetchingMaxFailures.getKey()).append(" = ").append(Settings.PrefetchingMaxFailures.getValue()).append("\n");
		buf.append("- Java ").append(Runtime.version()).append(", ").append(Runtime.getRuntime().availableProcessors()).append(" processors, max heap ").append(Runtime.getRuntime().maxMemory() / 1_048_576).append(" MB\n\n");

		buf.append("## Summary\n\n");

		for (final String line : summary()) {

			buf.append(line).append("\n\n");
		}

		buf.append("""
			How to read the tables: *with* and *without* are the same requests with database.prefetching.enabled on and off,
			measured in alternating pairs after the patterns were learned. Time is the median as the client sees it.
			Queries and lookups are averages per request; a lookup is a query for the relationships of one node, the kind
			of query prefetching replaces. Coverage is the share of lookups that prefetching answered. Allocated is the
			memory allocated by all threads during a request, which includes what a prefetch loads. Lookup query is the
			average time of one lookup. In the pattern tables, saved ms estimates what the saved lookups would have taken
			(saved lookups times the average lookup time), to compare with the time the prefetch takes; failed is the
			number of evaluations in which the pattern loaded more than database.prefetching.costratio allows or took
			longer than database.prefetching.maxduration.

			""");

		for (final Double rtt : roundTrips()) {

			buf.append("## Added round-trip time ").append(format(rtt, 1)).append(" ms\n\n");
			buf.append("| Scenario | User | Result | Time with / without (ms) | Factor | Lookups with / without | Coverage | Queries with / without | Allocated with / without (MB) | Lookup query (ms) |\n");
			buf.append("|---|---|---|---|---|---|---|---|---|---|\n");

			for (final PrefetchingSuiteBase.ScenarioResult r : results) {

				if (r.roundTripMillis() != rtt) {

					continue;
				}

				buf.append("| ").append(r.scenario().name());
				buf.append(" | ").append(r.scenario().user());
				buf.append(" | ").append(r.verdict());
				buf.append(" | ").append(format(r.medianMillisWith(), 1)).append(" / ").append(format(r.medianMillisWithout(), 1));
				buf.append(" | ").append(format(r.speedup(), 2));
				buf.append(" | ").append(format(r.lookupsWith(), 0)).append(" / ").append(format(r.lookupsWithout(), 0));
				buf.append(" | ").append(format(100.0 * r.lookupCoverage(), 0)).append(" %");
				buf.append(" | ").append(format(r.queriesWith(), 0)).append(" / ").append(format(r.queriesWithout(), 0));
				buf.append(" | ").append(format(r.allocatedMegabytesWith(), 1)).append(" / ").append(format(r.allocatedMegabytesWithout(), 1));
				buf.append(" | ").append(format(r.lookupMillis(), 2));
				buf.append(" |\n");
			}

			buf.append("\n");
		}

		if (!throughputs.isEmpty()) {

			buf.append("## Throughput with concurrent clients\n\n");
			buf.append("| Scenario | Added RTT (ms) | Clients | Requests | Requests/s with | Requests/s without | Factor |\n");
			buf.append("|---|---|---|---|---|---|---|\n");

			for (final PrefetchingSuiteBase.ThroughputResult t : throughputs) {

				buf.append("| ").append(t.scenario().name());
				buf.append(" | ").append(format(t.roundTripMillis(), 1));
				buf.append(" | ").append(t.threads());
				buf.append(" | ").append(t.requests());
				buf.append(" | ").append(format(t.requestsPerSecondWith(), 1));
				buf.append(" | ").append(format(t.requestsPerSecondWithout(), 1));
				buf.append(" | ").append(format(t.requestsPerSecondWith() / t.requestsPerSecondWithout(), 2));
				buf.append(" |\n");
			}

			buf.append("\n");
		}

		buf.append("## Scenarios and their patterns\n\n");

		final Set<String> described = new HashSet<>();

		for (final PrefetchingSuiteBase.ScenarioResult r : results) {

			if (described.add(r.scenario().name())) {

				buf.append("### ").append(r.scenario().name()).append("\n\n");
				buf.append(r.scenario().description()).append("\n\n");
			}

			buf.append("Added round-trip time ").append(format(r.roundTripMillis(), 1)).append(" ms:");

			if (r.patterns().isEmpty()) {

				buf.append(" no pattern learned.\n\n");

			} else {

				buf.append("\n\n| Hint | Pattern | Status | Executions | Entities | Saved lookups | Entities per saved lookup | Prefetch ms | Saved ms | Failed |\n");
				buf.append("|---|---|---|---|---|---|---|---|---|---|\n");

				for (final PrefetchingSuiteBase.PatternResult p : r.patterns()) {

					buf.append("| ").append(p.hint());
					buf.append(" | `").append(p.pattern()).append("`");
					buf.append(" | ").append(p.status() != null ? p.status() : "");
					buf.append(" | ").append(p.executions());
					buf.append(" | ").append(format(p.averageEntities(), 0));
					buf.append(" | ").append(format(p.averageSavedLookups(), 0));
					buf.append(" | ").append(p.averageSavedLookups() > 0 ? format(p.averageEntities() / p.averageSavedLookups(), 1) : "");
					buf.append(" | ").append(format(p.averageMillis(), 1));
					buf.append(" | ").append(format(p.averageSavedMillis(), 1));
					buf.append(" | ").append(p.failedEvaluations());
					buf.append(" |\n");
				}

				buf.append("\n");
			}

			if (r.mismatches() > 0) {

				buf.append("**").append(r.mismatches()).append(" of ").append(r.samples()).append(" responses differed with prefetching.** First one:\n\n```\n").append(r.mismatchExample()).append("\n```\n\n");
			}

			if (!r.errors().isEmpty()) {

				buf.append("**Errors:**\n\n```\n").append(String.join("\n", r.errors())).append("\n```\n\n");
			}
		}

		return buf.toString();
	}

	/**
	 * The success rate: how many scenarios got faster, slower or stayed the same with prefetching, per round-trip
	 * time, and in how many prefetching never ran.
	 */
	List<String> summary() {

		final List<String> lines = new ArrayList<>();

		for (final Double rtt : roundTrips()) {

			final Map<String, Integer> verdicts = new TreeMap<>();
			int total                           = 0;
			int mismatches                      = 0;

			for (final PrefetchingSuiteBase.ScenarioResult r : results) {

				if (r.roundTripMillis() == rtt) {

					verdicts.merge(r.verdict(), 1, Integer::sum);
					total++;
					mismatches += r.mismatches();
				}
			}

			final int faster = verdicts.getOrDefault("faster", 0);
			final int ran    = faster + verdicts.getOrDefault("slower", 0) + verdicts.getOrDefault("no difference", 0);

			lines.add("Added RTT " + format(rtt, 1) + " ms: " + faster + " of " + total + " scenarios faster with prefetching (" + faster + " of the " + ran + " in which a prefetch ran), " + verdicts
				+ (mismatches > 0 ? ", " + mismatches + " DIFFERENT RESPONSES" : ""));
		}

		return lines;
	}

	String csv() {

		final StringBuilder buf = new StringBuilder();

		buf.append("scenario;user;rttMillis;verdict;medianMillisWith;medianMillisWithout;factor;lookupsWith;lookupsWithout;coverage;lookupMillis;queriesWith;queriesWithout;allocatedMbWith;allocatedMbWithout;patterns;activePatterns;mismatches;errors\n");

		for (final PrefetchingSuiteBase.ScenarioResult r : results) {

			buf.append(r.scenario().name()).append(';');
			buf.append(r.scenario().user()).append(';');
			buf.append(format(r.roundTripMillis(), 2)).append(';');
			buf.append(r.verdict()).append(';');
			buf.append(format(r.medianMillisWith(), 2)).append(';');
			buf.append(format(r.medianMillisWithout(), 2)).append(';');
			buf.append(format(r.speedup(), 3)).append(';');
			buf.append(format(r.lookupsWith(), 1)).append(';');
			buf.append(format(r.lookupsWithout(), 1)).append(';');
			buf.append(format(r.lookupCoverage(), 3)).append(';');
			buf.append(format(r.lookupMillis(), 3)).append(';');
			buf.append(format(r.queriesWith(), 1)).append(';');
			buf.append(format(r.queriesWithout(), 1)).append(';');
			buf.append(format(r.allocatedMegabytesWith(), 2)).append(';');
			buf.append(format(r.allocatedMegabytesWithout(), 2)).append(';');
			buf.append(r.patterns().size()).append(';');
			buf.append(r.patterns().stream().filter(p -> p.executions() > 0).count()).append(';');
			buf.append(r.mismatches()).append(';');
			buf.append(r.errors().size()).append('\n');
		}

		return buf.toString();
	}

	// ----- private methods -----
	private SortedSet<Double> roundTrips() {

		final SortedSet<Double> roundTrips = new TreeSet<>();

		for (final PrefetchingSuiteBase.ScenarioResult r : results) {

			roundTrips.add(r.roundTripMillis());
		}

		return roundTrips;
	}

	private static String format(final double value, final int decimals) {

		return String.format(Locale.ROOT, "%." + decimals + "f", value);
	}
}
