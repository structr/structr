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
package org.structr.rest.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Counts how often the elements of an application are used, per day: the resource access grants
 * a request was checked against, and the pages that were delivered.
 *
 * <p>Days instead of the minutes of the HTTP access statistics, because a source here is a single
 * grant or page and there can be thousands of them; at one bucket per day, a source holds at most
 * as many buckets as the retention has days. Kept across a restart in a file next to the server log,
 * like the HTTP access statistics, and pruned to the same retention.
 */
public final class UsageStatistics {

	public static final String GRANTS = "grants";
	public static final String PAGES  = "pages";

	private static final Logger logger                           = LoggerFactory.getLogger(UsageStatistics.class);
	private static final long DAY                                = 24L * 60 * 60 * 1000;
	private static final Map<String, Map<String, Usage>> usages  = new ConcurrentHashMap<>();

	private UsageStatistics() {}

	/**
	 * Counts a use for the given request, once per category: a request that is repeated because the
	 * database terminated its transaction is still one view of a page and one decision of a grant.
	 */
	public static void record(final HttpServletRequest request, final String category, final String source) {

		if (request != null) {

			final String attribute = UsageStatistics.class.getName() + "." + category;

			if (request.getAttribute(attribute) != null) {

				return;
			}

			request.setAttribute(attribute, source);
		}

		record(category, source);
	}

	public static void record(final String category, final String source) {

		// a statistic is not worth a failed request
		if (category == null || source == null) {

			return;
		}

		final long now = System.currentTimeMillis();

		usages.computeIfAbsent(category, c -> new ConcurrentHashMap<>()).computeIfAbsent(source, s -> new Usage()).count(now - (now % DAY), 1L, now);
	}

	/**
	 * The usage of each source of the given category within the retention: the total number of uses,
	 * the time of the last use and the number of uses per day, keyed by the start of the day in UTC.
	 */
	public static Map<String, Map<String, Object>> get(final String category) {

		final Map<String, Map<String, Object>> result = new TreeMap<>();
		final Map<String, Usage> sources              = usages.get(category);
		final long cutoff                             = cutoff();

		if (sources != null) {

			for (final Map.Entry<String, Usage> entry : sources.entrySet()) {

				final Map<Long, Long> days = entry.getValue().days(cutoff);
				if (!days.isEmpty()) {

					final Map<String, Object> usage = new LinkedHashMap<>();

					usage.put("total",    days.values().stream().mapToLong(Long::longValue).sum());
					usage.put("lastUsed", entry.getValue().lastUsed.get());
					usage.put("days",     days);

					result.put(entry.getKey(), usage);
				}
			}
		}

		return result;
	}

	public static Path file() {

		return Path.of(Settings.getBasePath(), "logs", "usage-statistics.json");
	}

	/**
	 * Replaces the counts in memory with the saved ones. Called before the server takes requests, also
	 * when it restarts within the same process, where adding to the counts would count everything twice.
	 */
	public static void load() {

		usages.clear();

		final Path file = file();
		if (!Files.isRegularFile(file)) {

			return;
		}

		try {

			final JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
			final long cutoff     = cutoff();

			for (final Map.Entry<String, JsonElement> category : json.entrySet()) {

				final Map<String, Usage> sources = usages.computeIfAbsent(category.getKey(), c -> new ConcurrentHashMap<>());

				for (final Map.Entry<String, JsonElement> source : category.getValue().getAsJsonObject().entrySet()) {

					final JsonObject saved = source.getValue().getAsJsonObject();
					final long lastUsed    = saved.get("lastUsed").getAsLong();
					final Usage usage      = sources.computeIfAbsent(source.getKey(), s -> new Usage());

					for (final Map.Entry<String, JsonElement> day : saved.getAsJsonObject("days").entrySet()) {

						final long start = Long.parseLong(day.getKey());
						if (start >= cutoff) {

							usage.count(start, day.getValue().getAsLong(), lastUsed);
						}
					}
				}
			}

		} catch (Exception ex) {

			logger.warn("Unable to restore usage statistics from {}, starting empty: {}", file, ex.toString());
		}
	}

	public static void save() {

		final Path file      = file();
		final long cutoff    = cutoff();
		final JsonObject all = new JsonObject();

		for (final Map.Entry<String, Map<String, Usage>> category : usages.entrySet()) {

			final JsonObject sources = new JsonObject();

			for (final Map.Entry<String, Usage> source : category.getValue().entrySet()) {

				final Usage usage = source.getValue();

				// pruned here, so the statistics in memory are kept within the retention as well
				usage.prune(cutoff);

				final Map<Long, Long> days = usage.days(cutoff);
				if (days.isEmpty()) {

					category.getValue().remove(source.getKey(), usage);
					continue;
				}

				final JsonObject daysJson = new JsonObject();
				final JsonObject json     = new JsonObject();

				days.forEach((start, count) -> daysJson.addProperty(String.valueOf(start), count));

				json.addProperty("lastUsed", usage.lastUsed.get());
				json.add("days", daysJson);

				sources.add(source.getKey(), json);
			}

			if (!sources.isEmpty()) {

				all.add(category.getKey(), sources);
			}
		}

		try {

			Files.createDirectories(file.getParent());

			// written beside and moved into place, so a crash while writing cannot leave half a file
			final Path temp = file.resolveSibling(file.getFileName() + ".tmp");

			Files.writeString(temp, all.toString(), StandardCharsets.UTF_8);
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

		} catch (Exception ex) {

			logger.warn("Unable to save usage statistics to {}: {}", file, ex.toString());
		}
	}

	/**
	 * Forgets everything that was counted, for tests.
	 */
	public static void clear() {

		usages.clear();
	}

	/**
	 * How many days the usage is kept, the current one included.
	 */
	public static int retentionDays() {

		return Math.max(1, Settings.HttpStatsRetentionDays.getValue(30));
	}

	private static long cutoff() {

		final long now = System.currentTimeMillis();

		// whole days, so the oldest day is not cut in half
		return (now - (now % DAY)) - (retentionDays() - 1) * DAY;
	}

	// ----- nested classes -----
	private static class Usage {

		private final Map<Long, LongAdder> days = new ConcurrentHashMap<>();
		private final AtomicLong lastUsed       = new AtomicLong();

		void count(final long day, final long count, final long time) {

			days.computeIfAbsent(day, d -> new LongAdder()).add(count);
			lastUsed.accumulateAndGet(time, Math::max);
		}

		void prune(final long cutoff) {

			days.keySet().removeIf(start -> start < cutoff);
		}

		Map<Long, Long> days(final long cutoff) {

			final Map<Long, Long> result = new TreeMap<>();

			days.forEach((start, count) -> {

				if (start >= cutoff) {

					result.put(start, count.sum());
				}
			});

			return result;
		}
	}
}
