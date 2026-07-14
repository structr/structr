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

import org.structr.storage.sync.SyncTarget;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.*;

/**
 * Unit tests for the SyncTarget configuration snapshot.
 */
public class SyncTargetTest {

	@Test
	public void testConfigurationWithNullValuesAndKeys() {

		// a StorageConfiguration entry may exist with a name but no value yet
		// (or vice versa); the snapshot must not fail like Map.copyOf would
		final Map<String, String> configuration = new HashMap<>();

		configuration.put("mountTarget", "/data");
		configuration.put("sync.direction", null);   // created but not set
		configuration.put(null, "orphan");           // entry without a name

		final SyncTarget target = new SyncTarget("uuid", "/mounted", true, "configUuid", configuration);

		assertEquals("Set entries must be preserved", "/data", target.configuration().get("mountTarget"));
		assertFalse("Null-valued entries must be dropped", target.configuration().containsKey("sync.direction"));
		assertFalse("Null-keyed entries must be dropped", target.configuration().containsKey(null));
		assertEquals("Only the usable entry should remain", 1, target.configuration().size());
	}

	@Test
	public void testNullConfigurationYieldsEmptyMap() {

		final SyncTarget target = new SyncTarget("uuid", "/mounted", true, "configUuid", null);

		assertNotNull("Configuration must never be null", target.configuration());
		assertTrue("Null configuration must yield an empty map", target.configuration().isEmpty());
	}

	@Test
	public void testConfigurationSnapshotIsImmutable() {

		final SyncTarget target = new SyncTarget("uuid", "/mounted", true, "configUuid", Map.of("region", "us-east-1"));

		try {

			target.configuration().put("secretKey", "leak");
			fail("SyncTarget configuration must be immutable");

		} catch (UnsupportedOperationException expected) {
		}
	}

	@Test
	public void testSyncRootRequired() {

		try {

			new SyncTarget(null, "/mounted", true, "configUuid", Map.of());
			fail("SyncTarget must reject a null sync root UUID");

		} catch (IllegalArgumentException expected) {
		}

		try {

			new SyncTarget("uuid", null, true, "configUuid", Map.of());
			fail("SyncTarget must reject a null sync root path");

		} catch (IllegalArgumentException expected) {
		}
	}
}
