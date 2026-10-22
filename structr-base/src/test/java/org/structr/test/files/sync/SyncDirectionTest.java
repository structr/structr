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

import org.structr.storage.sync.SyncDirection;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.testng.AssertJUnit.*;

/**
 * Unit tests for reading the sync direction from a storage configuration.
 */
public class SyncDirectionTest {

	@Test
	public void testAbsentOrNullConfigurationIsIn() {

		assertEquals(SyncDirection.IN, SyncDirection.fromConfiguration(null));
		assertEquals(SyncDirection.IN, SyncDirection.fromConfiguration(Map.of()));

		final Map<String, String> unset = new HashMap<>();
		unset.put(SyncDirection.DIRECTION_KEY, null);

		assertEquals("an entry that exists without a value is the default", SyncDirection.IN, SyncDirection.fromConfiguration(unset));
	}

	@Test
	public void testValuesAreReadLeniently() {

		assertEquals(SyncDirection.IN,   SyncDirection.fromConfiguration(Map.of(SyncDirection.DIRECTION_KEY, "in")));
		assertEquals(SyncDirection.OUT,  SyncDirection.fromConfiguration(Map.of(SyncDirection.DIRECTION_KEY, " OUT ")));
		assertEquals(SyncDirection.BOTH, SyncDirection.fromConfiguration(Map.of(SyncDirection.DIRECTION_KEY, "Both")));
	}

	@Test
	public void testUnknownValueFallsBackToIn() {

		// the safe default: an unreadable setting must never start writing to the storage
		assertEquals(SyncDirection.IN, SyncDirection.fromConfiguration(Map.of(SyncDirection.DIRECTION_KEY, "outbound")));
		assertEquals(SyncDirection.IN, SyncDirection.fromConfiguration(Map.of(SyncDirection.DIRECTION_KEY, "")));
	}

	@Test
	public void testParsingDoesNotDependOnTheDefaultLocale() {

		final Locale previous = Locale.getDefault();

		try {

			Locale.setDefault(Locale.forLanguageTag("tr-TR"));

			assertEquals(SyncDirection.IN,   SyncDirection.fromConfiguration(Map.of(SyncDirection.DIRECTION_KEY, "IN")));
			assertEquals(SyncDirection.BOTH, SyncDirection.fromConfiguration(Map.of(SyncDirection.DIRECTION_KEY, "BOTH")));

		} finally {

			Locale.setDefault(previous);
		}
	}

	@Test
	public void testDirectionsAndWhatTheyInclude() {

		assertTrue(SyncDirection.IN.isInbound());
		assertFalse(SyncDirection.IN.isOutbound());
		assertFalse(SyncDirection.OUT.isInbound());
		assertTrue(SyncDirection.OUT.isOutbound());
		assertTrue(SyncDirection.BOTH.isInbound());
		assertTrue(SyncDirection.BOTH.isOutbound());
	}
}
