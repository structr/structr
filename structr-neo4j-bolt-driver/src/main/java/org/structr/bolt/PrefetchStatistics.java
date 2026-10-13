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
package org.structr.bolt;

import org.structr.api.config.Settings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Counts what the learned (automatic) prefetching costs and what it saves, per prefetch hint, so that its effect can be judged in a running
 * application instead of guessed.
 *
 * <p>A transaction is counted with the state of database.prefetching.enabled it started with, in separate figures for
 * both states. Turning prefetching off for a while therefore shows, for every hint, the queries and the time of the
 * same kind of request with and without it.
 *
 * <p>A saved lookup is a relationship lookup of a node that a prefetch answered, which would otherwise have been a
 * query. Each node and relationship key counts once per transaction, because a repeated lookup in the same transaction
 * is answered by the relationship cache of the node anyway.
 *
 * <p>The average time of a relationship lookup query is measured as well, over all transactions, so that the time a
 * prefetch takes can be compared with the time of the lookups it saves.
 */
final class PrefetchStatistics {

	// A hint names a kind of request, but a request path with an identifier that is not a UUID (a slug, a search term)
	// makes every request its own hint, so the number of hints is limited. Further hints are only counted.
	static final int MAX_HINTS = 1000;

	static final String NO_HINT = "(no hint)";

	private static final Map<String, HintStatistics> hints = new ConcurrentHashMap<>();
	private static final LongAdder untrackedTransactions   = new LongAdder();
	private static final LongAdder lookups                 = new LongAdder();
	private static final LongAdder lookupNanos             = new LongAdder();
	private static volatile long since                     = System.currentTimeMillis();

	private PrefetchStatistics() {
	}

	static void transactionClosed(final String hint, final boolean prefetchingEnabled, final long nanos, final long queries, final long lookupQueries) {

		final HintStatistics stats = get(hint);
		if (stats == null) {

			untrackedTransactions.increment();

			return;
		}

		final TransactionStatistics tx = prefetchingEnabled ? stats.withPrefetching : stats.withoutPrefetching;

		tx.count.increment();
		tx.nanos.add(nanos);
		tx.queries.add(queries);
		tx.lookupQueries.add(lookupQueries);
	}

	static void patternLearned(final String hint, final String pattern) {

		final PatternStatistics stats = pattern(hint, pattern);
		if (stats != null) {

			stats.status = "active";
		}
	}

	static void patternDeactivated(final String hint, final String pattern, final String reason) {

		final PatternStatistics stats = pattern(hint, pattern);
		if (stats != null) {

			stats.status = "deactivated: " + reason;
		}
	}

	static void prefetchExecuted(final String hint, final String pattern, final long entities, final long nanos) {

		final PatternStatistics stats = pattern(hint, pattern);
		if (stats != null) {

			stats.executions.increment();
			stats.entities.add(entities);
			stats.nanos.add(nanos);
		}
	}

	static void lookupsSaved(final String hint, final String pattern, final long lookups) {

		final PatternStatistics stats = pattern(hint, pattern);
		if (stats != null) {

			stats.savedLookups.add(lookups);
		}
	}

	static void evaluationFailed(final String hint, final String pattern) {

		final PatternStatistics stats = pattern(hint, pattern);
		if (stats != null) {

			stats.failedEvaluations.increment();
		}
	}

	/**
	 * Counts a relationship lookup query, with the time it took to run and read its result.
	 */
	static void lookupExecuted(final long nanos) {

		lookups.increment();
		lookupNanos.add(nanos);
	}

	static void reset() {

		hints.clear();
		untrackedTransactions.reset();
		lookups.reset();
		lookupNanos.reset();

		since = System.currentTimeMillis();
	}

	/**
	 * The hints, those whose transactions took the most time in total first.
	 */
	static Map<String, Object> snapshot() {

		final Map<String, Object> result = new LinkedHashMap<>();
		final List<Map.Entry<String, HintStatistics>> entries = new ArrayList<>(hints.entrySet());
		final List<Map<String, Object>> list = new ArrayList<>();
		final long lookupCount               = lookups.sum();
		final double averageLookupMillis     = lookupCount > 0 ? millis(lookupNanos.sum() / lookupCount) : 0.0;

		entries.sort(Comparator.comparingLong((Map.Entry<String, HintStatistics> e) -> e.getValue().totalNanos()).reversed());

		for (final Map.Entry<String, HintStatistics> entry : entries) {

			list.add(entry.getValue().toMap(entry.getKey(), averageLookupMillis));
		}

		result.put("since",                 since);
		result.put("enabled",               Settings.PrefetchingEnabled.getValue());
		result.put("untrackedTransactions", untrackedTransactions.sum());
		result.put("lookupQueries",         lookupCount);
		result.put("averageLookupMillis",   averageLookupMillis);
		result.put("hints",                 list);

		return result;
	}

	// ----- private methods -----
	private static HintStatistics get(final String rawHint) {

		final String hint = rawHint != null ? rawHint : NO_HINT;

		HintStatistics stats = hints.get(hint);
		if (stats == null && hints.size() < MAX_HINTS) {

			stats = hints.computeIfAbsent(hint, k -> new HintStatistics());
		}

		return stats;
	}

	private static PatternStatistics pattern(final String hint, final String pattern) {

		final HintStatistics stats = get(hint);
		if (stats == null) {

			return null;
		}

		return stats.patterns.computeIfAbsent(pattern, k -> new PatternStatistics());
	}

	private static double millis(final long nanos) {

		return Math.round(nanos / 10_000.0) / 100.0;
	}

	private static double average(final long sum, final long count) {

		if (count == 0) {

			return 0.0;
		}

		return Math.round(100.0 * sum / count) / 100.0;
	}

	// ----- nested classes -----
	private static class HintStatistics {

		private final TransactionStatistics withPrefetching    = new TransactionStatistics();
		private final TransactionStatistics withoutPrefetching = new TransactionStatistics();
		private final Map<String, PatternStatistics> patterns  = new ConcurrentHashMap<>();

		long totalNanos() {

			return withPrefetching.nanos.sum() + withoutPrefetching.nanos.sum();
		}

		Map<String, Object> toMap(final String hint, final double averageLookupMillis) {

			final Map<String, Object> map = new LinkedHashMap<>();
			final List<Map<String, Object>> list = new ArrayList<>();

			for (final Map.Entry<String, PatternStatistics> entry : patterns.entrySet()) {

				list.add(entry.getValue().toMap(entry.getKey(), averageLookupMillis));
			}

			map.put("hint",               hint);
			map.put("withPrefetching",    withPrefetching.toMap());
			map.put("withoutPrefetching", withoutPrefetching.toMap());
			map.put("patterns",           list);

			return map;
		}
	}

	private static class TransactionStatistics {

		private final LongAdder count         = new LongAdder();
		private final LongAdder nanos         = new LongAdder();
		private final LongAdder queries       = new LongAdder();
		private final LongAdder lookupQueries = new LongAdder();

		Map<String, Object> toMap() {

			final Map<String, Object> map = new LinkedHashMap<>();
			final long n                  = count.sum();

			map.put("transactions",         n);
			map.put("averageMillis",        n > 0 ? millis(nanos.sum() / n) : 0.0);
			map.put("averageQueries",       average(queries.sum(), n));
			map.put("averageLookupQueries", average(lookupQueries.sum(), n));

			return map;
		}
	}

	private static class PatternStatistics {

		private final LongAdder executions        = new LongAdder();
		private final LongAdder entities          = new LongAdder();
		private final LongAdder nanos             = new LongAdder();
		private final LongAdder savedLookups      = new LongAdder();
		private final LongAdder failedEvaluations = new LongAdder();
		private volatile String status            = null;

		/**
		 * @param averageLookupMillis the average time of a lookup query, which estimates the time the saved lookups would have taken
		 */
		Map<String, Object> toMap(final String pattern, final double averageLookupMillis) {

			final Map<String, Object> map    = new LinkedHashMap<>();
			final long n                     = executions.sum();
			final double averageSavedLookups = average(savedLookups.sum(), n);

			map.put("pattern",             pattern);
			map.put("status",              status);
			map.put("executions",          n);
			map.put("averageEntities",     average(entities.sum(), n));
			map.put("averageMillis",       n > 0 ? millis(nanos.sum() / n) : 0.0);
			map.put("averageSavedLookups", averageSavedLookups);
			map.put("averageSavedMillis",  Math.round(100.0 * averageSavedLookups * averageLookupMillis) / 100.0);
			map.put("failedEvaluations",   failedEvaluations.sum());

			return map;
		}
	}
}
