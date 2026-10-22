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

import org.structr.storage.sync.VanishPolicy;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.testng.AssertJUnit.*;

/**
 * Unit tests for reading the vanish policy from a storage configuration.
 */
public class VanishPolicyTest {

	@Test
	public void testAbsentOrNullConfigurationIsIgnore() {

		assertEquals(VanishPolicy.IGNORE, VanishPolicy.fromConfiguration(null));
		assertEquals(VanishPolicy.IGNORE, VanishPolicy.fromConfiguration(Map.of()));

		final Map<String, String> unset = new HashMap<>();
		unset.put(VanishPolicy.POLICY_KEY, null);

		assertEquals("an entry that exists without a value is the default", VanishPolicy.IGNORE, VanishPolicy.fromConfiguration(unset));
	}

	@Test
	public void testValuesAreReadLeniently() {

		for (final VanishPolicy policy : VanishPolicy.values()) {

			assertEquals(policy, VanishPolicy.fromConfiguration(Map.of(VanishPolicy.POLICY_KEY, policy.value())));
			assertEquals(policy, VanishPolicy.fromConfiguration(Map.of(VanishPolicy.POLICY_KEY, "  " + policy.value().toUpperCase(Locale.ROOT) + " ")));
		}
	}

	@Test
	public void testUnknownValueFallsBackToIgnore() {

		// the safe default: an unreadable setting must never turn into deleting nodes
		assertEquals(VanishPolicy.IGNORE, VanishPolicy.fromConfiguration(Map.of(VanishPolicy.POLICY_KEY, "remove")));
		assertEquals(VanishPolicy.IGNORE, VanishPolicy.fromConfiguration(Map.of(VanishPolicy.POLICY_KEY, "")));
	}

	@Test
	public void testParsingDoesNotDependOnTheDefaultLocale() {

		final Locale previous = Locale.getDefault();

		try {

			// a Turkish locale lowercases "IGNORE" to "ıgnore", with a dotless i
			Locale.setDefault(Locale.forLanguageTag("tr-TR"));

			assertEquals(VanishPolicy.IGNORE,   VanishPolicy.fromConfiguration(Map.of(VanishPolicy.POLICY_KEY, "IGNORE")));
			assertEquals(VanishPolicy.READONLY, VanishPolicy.fromConfiguration(Map.of(VanishPolicy.POLICY_KEY, "READONLY")));
			assertEquals("ignore", VanishPolicy.IGNORE.value());

		} finally {

			Locale.setDefault(previous);
		}
	}

	@Test
	public void testOnlyIgnoreLeavesVanishedNodesAlone() {

		assertFalse(VanishPolicy.IGNORE.reconciles());
		assertTrue(VanishPolicy.DELETE.reconciles());
		assertTrue(VanishPolicy.READONLY.reconciles());
	}
}
