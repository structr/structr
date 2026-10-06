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
package org.structr.rest.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.rest.common.Stats;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps the HTTP access statistics across a restart, in a file next to the server log.
 *
 * <p>The statistics describe the load on this server, not the application, so they live on the host and
 * not in the graph: moving an instance to another server carries them only if the logs directory is
 * copied with it. Written with Gson's tree API, never by reflection, so it works on the module path.
 */
public final class HttpStatsSnapshot {

	private static final Logger logger = LoggerFactory.getLogger(HttpStatsSnapshot.class);
	private static final long DAY      = 24L * 60 * 60 * 1000;

	private HttpStatsSnapshot() {}

	public static Path file() {

		return Path.of(Settings.getBasePath(), "logs", "http-access-statistics.json");
	}

	public static void load(final Map<String, Map<String, Stats>> stats) {

		final Path file = file();
		if (!Files.isRegularFile(file)) {

			return;
		}

		try {

			final JsonObject json     = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
			final long savedInterval  = json.get("interval").getAsLong();
			final long interval       = Settings.HttpStatsAggregationInterval.getValue(60_000);

			// buckets of another size cannot be merged with the current ones
			if (savedInterval != interval) {

				logger.info("Not restoring HTTP access statistics from {}, they were recorded at an aggregation interval of {} ms and the current one is {} ms", file, savedInterval, interval);
				return;
			}

			final long cutoff = cutoff();
			int restored      = 0;

			for (final Map.Entry<String, JsonElement> key : json.getAsJsonObject("stats").entrySet()) {

				final Map<String, Stats> sources = stats.computeIfAbsent(key.getKey(), k -> new ConcurrentHashMap<>());

				for (final Map.Entry<String, JsonElement> source : key.getValue().getAsJsonObject().entrySet()) {

					final Map<Long, Long> buckets = new LinkedHashMap<>();

					for (final Map.Entry<String, JsonElement> bucket : source.getValue().getAsJsonObject().entrySet()) {

						final long start = Long.parseLong(bucket.getKey());
						if (start >= cutoff) {

							buckets.put(start, bucket.getValue().getAsLong());
						}
					}

					sources.computeIfAbsent(source.getKey(), s -> new Stats()).restore(buckets);
					restored += buckets.size();
				}
			}

			logger.info("Restored {} HTTP access statistics buckets from {}", restored, file);

		} catch (Exception ex) {

			logger.warn("Unable to restore HTTP access statistics from {}, starting empty: {}", file, ex.toString());
		}
	}

	public static void save(final Map<String, Map<String, Stats>> stats) {

		final Path file    = file();
		final long cutoff  = cutoff();
		final JsonObject all = new JsonObject();

		for (final Map.Entry<String, Map<String, Stats>> key : stats.entrySet()) {

			final JsonObject sources = new JsonObject();

			for (final Map.Entry<String, Stats> source : key.getValue().entrySet()) {

				// pruned here, so the statistics in memory are kept within the retention as well
				source.getValue().prune(cutoff);

				final Map<Long, Long> buckets = source.getValue().buckets();
				if (!buckets.isEmpty()) {

					final JsonObject json = new JsonObject();

					buckets.forEach((start, count) -> json.addProperty(String.valueOf(start), count));
					sources.add(source.getKey(), json);
				}
			}

			if (!sources.isEmpty()) {

				all.add(key.getKey(), sources);
			}
		}

		final JsonObject json = new JsonObject();

		json.addProperty("interval", Settings.HttpStatsAggregationInterval.getValue(60_000));
		json.add("stats", all);

		try {

			Files.createDirectories(file.getParent());

			// written beside and moved into place, so a crash while writing cannot leave half a file
			final Path temp = file.resolveSibling(file.getFileName() + ".tmp");

			Files.writeString(temp, json.toString(), StandardCharsets.UTF_8);
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

		} catch (Exception ex) {

			logger.warn("Unable to save HTTP access statistics to {}: {}", file, ex.toString());
		}
	}

	private static long cutoff() {

		return System.currentTimeMillis() - Math.max(1, Settings.HttpStatsRetentionDays.getValue(30)) * DAY;
	}
}
