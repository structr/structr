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
package org.structr.test.files.sync;

import org.structr.storage.sync.SyncSettings;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.*;

/**
 * Unit tests for reading the per-mount sync settings from a storage configuration.
 */
public class SyncSettingsTest {

	@Test
	public void testAbsentEntriesAreTheDefaults() {

		for (final Map<String, String> configuration : new Map[] { null, Map.of(), withNulls() }) {

			final SyncSettings settings = SyncSettings.fromConfiguration(configuration);

			assertFalse(settings.watchContents());
			assertNull(settings.scanInterval());
			assertNull(settings.fileType());
			assertNull(settings.folderType());
		}
	}

	@Test
	public void testValuesAreReadLeniently() {

		final SyncSettings settings = SyncSettings.fromConfiguration(Map.of(
			SyncSettings.WATCH_CONTENTS_KEY, " TRUE ",
			SyncSettings.SCAN_INTERVAL_KEY,  " 30 ",
			SyncSettings.FILE_TYPE_KEY,      " Image ",
			SyncSettings.FOLDER_TYPE_KEY,    "Folder"
		));

		assertTrue(settings.watchContents());
		assertEquals(Integer.valueOf(30), settings.scanInterval());
		assertEquals("Image",  settings.fileType());
		assertEquals("Folder", settings.folderType());
	}

	@Test
	public void testUnreadableValuesFallBackToTheDefaults() {

		final SyncSettings settings = SyncSettings.fromConfiguration(Map.of(
			SyncSettings.WATCH_CONTENTS_KEY, "yes",
			SyncSettings.SCAN_INTERVAL_KEY,  "soon",
			SyncSettings.FILE_TYPE_KEY,      "  "
		));

		assertFalse("Only true enables watching", settings.watchContents());
		assertNull("An unreadable interval means no rescans", settings.scanInterval());
		assertNull("A blank type means the default", settings.fileType());
	}

	@Test
	public void testZeroOrNegativeIntervalMeansNoRescans() {

		assertNull(SyncSettings.fromConfiguration(Map.of(SyncSettings.SCAN_INTERVAL_KEY, "0")).scanInterval());
		assertNull(SyncSettings.fromConfiguration(Map.of(SyncSettings.SCAN_INTERVAL_KEY, "-5")).scanInterval());
	}

	private static Map<String, String> withNulls() {

		final Map<String, String> configuration = new HashMap<>();

		configuration.put(SyncSettings.WATCH_CONTENTS_KEY, null);
		configuration.put(SyncSettings.SCAN_INTERVAL_KEY,  null);

		return configuration;
	}
}
