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
import org.structr.rest.common.UsageStatistics;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * The usage statistics of grants and pages outlive a restart through a file in logs/, and keep only the configured number of days.
 * Needs no database: they only touch the counts in memory and one file.
 */
public class UsageStatisticsTest {

	private static final long DAY = 24L * 60 * 60 * 1000;

	private String previousBasePath = null;
	private Path basePath           = null;

	@BeforeMethod
	public void useTemporaryBasePath() throws Exception {

		previousBasePath = Settings.BasePath.getValue();
		basePath         = Files.createTempDirectory("usage-stats");

		Settings.BasePath.setValue(basePath.toString());
		UsageStatistics.clear();
	}

	@AfterMethod(alwaysRun = true)
	public void restoreBasePath() {

		UsageStatistics.clear();
		Settings.BasePath.setValue(previousBasePath);
		Settings.HttpStatsRetentionDays.setValue(Settings.HttpStatsRetentionDays.getDefaultValue());
	}

	@Test
	public void testUsageSurvivesARestart() {

		record(UsageStatistics.GRANTS, "User AUTH_USER_GET allowed", 3);
		record(UsageStatistics.PAGES, "a1b2c3", 2);

		final Map<String, Map<String, Object>> grantsBefore = UsageStatistics.get(UsageStatistics.GRANTS);

		UsageStatistics.save();

		assertTrue("the snapshot belongs in the logs directory", Files.isRegularFile(basePath.resolve("logs").resolve("usage-statistics.json")));

		// a new process: empty statistics, restored from the file
		UsageStatistics.clear();
		UsageStatistics.load();

		assertEquals("the counts, the last use and the days of the grant must come back", grantsBefore, UsageStatistics.get(UsageStatistics.GRANTS));
		assertEquals(2L, UsageStatistics.get(UsageStatistics.PAGES).get("a1b2c3").get("total"));
	}

	@Test
	public void testLoadingAgainDoesNotCountTwice() {

		record(UsageStatistics.PAGES, "a1b2c3", 2);

		UsageStatistics.save();

		// the HTTP service restarts within the same process
		UsageStatistics.load();

		assertEquals(2L, UsageStatistics.get(UsageStatistics.PAGES).get("a1b2c3").get("total"));
	}

	@Test
	public void testDaysOlderThanTheRetentionAreDropped() throws Exception {

		Settings.HttpStatsRetentionDays.setValue(7);

		final long now   = System.currentTimeMillis();
		final long today = now - (now % DAY);
		final Path file  = basePath.resolve("logs").resolve("usage-statistics.json");

		Files.createDirectories(file.getParent());
		Files.writeString(file, "{\"pages\":{\"a1b2c3\":{\"lastUsed\":" + now + ",\"days\":{\"" + (today - 3 * DAY) + "\":4,\"" + (today - 10 * DAY) + "\":5}},\"d4e5f6\":{\"lastUsed\":" + (today - 10 * DAY) + ",\"days\":{\"" + (today - 10 * DAY) + "\":1}}}}");

		UsageStatistics.load();

		final Map<String, Map<String, Object>> pages = UsageStatistics.get(UsageStatistics.PAGES);

		assertEquals("only the day within seven days counts", 4L, pages.get("a1b2c3").get("total"));
		assertEquals("a page without a view within seven days is not listed", 1, pages.size());
	}

	@Test
	public void testAnUnreadableFileStartsEmpty() throws Exception {

		final Path file = basePath.resolve("logs").resolve("usage-statistics.json");

		Files.createDirectories(file.getParent());
		Files.writeString(file, "{ not json");

		UsageStatistics.load();

		assertTrue(UsageStatistics.get(UsageStatistics.PAGES).isEmpty());
	}

	private static void record(final String category, final String source, final int count) {

		for (int i = 0; i < count; i++) {

			UsageStatistics.record(category, source);
		}
	}
}
