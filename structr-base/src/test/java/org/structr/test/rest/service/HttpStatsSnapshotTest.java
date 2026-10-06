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
package org.structr.test.rest.service;

import org.structr.api.config.Settings;
import org.structr.rest.common.Stats;
import org.structr.rest.service.HttpStatsSnapshot;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * The HTTP access statistics outlive a restart through a file in logs/, and keep only the configured number of days.
 * Needs no database: the snapshot only touches the statistics in memory and one file.
 */
public class HttpStatsSnapshotTest {

	private static final long MINUTE = 60_000L;
	private static final long DAY    = 24 * 60 * MINUTE;

	private String previousBasePath = null;
	private Path basePath           = null;

	@BeforeMethod
	public void useTemporaryBasePath() throws Exception {

		previousBasePath = Settings.BasePath.getValue();
		basePath         = Files.createTempDirectory("http-stats");

		Settings.BasePath.setValue(basePath.toString());
	}

	@AfterMethod(alwaysRun = true)
	public void restoreBasePath() {

		Settings.BasePath.setValue(previousBasePath);
		Settings.HttpStatsRetentionDays.setValue(Settings.HttpStatsRetentionDays.getDefaultValue());
	}

	@Test
	public void testStatisticsSurviveARestart() {

		final long now                           = System.currentTimeMillis();
		final Map<String, Map<String, Stats>> before = new ConcurrentHashMap<>();

		record(before, "get",  now - 2 * MINUTE, 3);
		record(before, "get",  now - DAY, 5);
		record(before, "post", now - 10 * MINUTE, 2);

		HttpStatsSnapshot.save(before);

		assertTrue("the snapshot belongs in the logs directory", Files.isRegularFile(basePath.resolve("logs").resolve("http-access-statistics.json")));

		// a new process: empty statistics, restored from the file
		final Map<String, Map<String, Stats>> after = new ConcurrentHashMap<>();

		HttpStatsSnapshot.load(after);

		assertEquals("every bucket of GET must come back", before.get("http").get("get").buckets(),  after.get("http").get("get").buckets());
		assertEquals("every bucket of POST must come back", before.get("http").get("post").buckets(), after.get("http").get("post").buckets());
	}

	@Test
	public void testRestoredBucketsAreCountedOnFromWhereTheyStopped() throws Exception {

		final long now                           = System.currentTimeMillis();
		final Map<String, Map<String, Stats>> before = new ConcurrentHashMap<>();

		record(before, "get", now, 4);
		HttpStatsSnapshot.save(before);

		final Map<String, Map<String, Stats>> after = new ConcurrentHashMap<>();

		HttpStatsSnapshot.load(after);

		// a request in the same minute after the restart adds to the restored bucket
		after.get("http").get("get").value(now, false);

		assertEquals(5L, (long) after.get("http").get("get").aggregate(MINUTE, 10).get(now - (now % MINUTE)));
	}

	@Test
	public void testBucketsOlderThanTheRetentionAreDropped() {

		Settings.HttpStatsRetentionDays.setValue(7);

		final long now                        = System.currentTimeMillis();
		final Map<String, Map<String, Stats>> stats = new ConcurrentHashMap<>();

		record(stats, "get", now - 3 * DAY, 1);
		record(stats, "get", now - 10 * DAY, 1);

		HttpStatsSnapshot.save(stats);

		final Map<Long, Long> kept = stats.get("http").get("get").buckets();

		assertEquals("only the bucket within seven days stays, in memory as well as in the file", 1, kept.size());
		assertTrue(kept.keySet().iterator().next() > now - 7 * DAY);
	}

	@Test
	public void testAFileWithAnotherIntervalIsNotRestored() throws Exception {

		final Path file = basePath.resolve("logs").resolve("http-access-statistics.json");

		Files.createDirectories(file.getParent());
		Files.writeString(file, "{\"interval\":1000,\"stats\":{\"http\":{\"get\":{\"" + System.currentTimeMillis() + "\":7}}}}");

		final Map<String, Map<String, Stats>> stats = new ConcurrentHashMap<>();

		HttpStatsSnapshot.load(stats);

		assertTrue("buckets of another size cannot be merged with the current ones", stats.isEmpty());
	}

	@Test
	public void testAnUnreadableFileStartsEmpty() throws Exception {

		final Path file = basePath.resolve("logs").resolve("http-access-statistics.json");

		Files.createDirectories(file.getParent());
		Files.writeString(file, "{ not json");

		final Map<String, Map<String, Stats>> stats = new ConcurrentHashMap<>();

		HttpStatsSnapshot.load(stats);

		assertTrue(stats.isEmpty());
		assertFalse("a failed save must not leave its temporary file behind", Files.exists(file.resolveSibling("http-access-statistics.json.tmp")));
	}

	private static void record(final Map<String, Map<String, Stats>> stats, final String verb, final long time, final int count) {

		final Stats verbStats = stats.computeIfAbsent("http", k -> new ConcurrentHashMap<>()).computeIfAbsent(verb, k -> new Stats());

		for (int i = 0; i < count; i++) {

			verbStats.value(time, false);
		}
	}
}
