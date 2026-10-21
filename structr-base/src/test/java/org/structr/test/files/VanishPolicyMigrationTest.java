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
package org.structr.test.files;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.MigrationService;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.storage.StorageProviderFactory;
import org.structr.storage.providers.local.LocalFSStorageProvider;
import org.structr.storage.sync.VanishPolicy;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.StorageConfiguration;
import org.structr.web.traits.definitions.StorageConfigurationEntryTraitDefinition;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Whether the boolean predecessor of the vanish policy is carried over.
 *
 * An installation upgraded from 7.0 can hold "sync.deleteStale" entries, and nothing reads that name
 * any more, so a mount configured to prune would silently fall back to keeping everything.
 */
public class VanishPolicyMigrationTest extends StructrUiTest {

	private static final String LEGACY_DELETE_STALE_KEY = "sync.deleteStale";

	@Test
	public void testLegacyDeleteStaleEntriesAreRewritten() {

		String enabledUuid  = null;
		String disabledUuid = null;

		try (final Tx tx = app.tx()) {

			// no mountTarget, so neither configuration becomes a sync target
			enabledUuid  = StorageProviderFactory.createConfig("enabledLegacyMount",  LocalFSStorageProvider.class, Map.of(LEGACY_DELETE_STALE_KEY, "true")).getUuid();
			disabledUuid = StorageProviderFactory.createConfig("disabledLegacyMount", LocalFSStorageProvider.class, Map.of(LEGACY_DELETE_STALE_KEY, "false")).getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try {

			MigrationService.execute(false);

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final Map<String, String> enabled = app.getNodeById(StructrTraits.STORAGE_CONFIGURATION, enabledUuid).as(StorageConfiguration.class).getConfiguration();

			assertEquals("An enabled sync.deleteStale should become the delete policy", VanishPolicy.DELETE.value(), enabled.get(VanishPolicy.POLICY_KEY));
			assertFalse("The legacy entry should be gone",                              enabled.containsKey(LEGACY_DELETE_STALE_KEY));

			final Map<String, String> disabled = app.getNodeById(StructrTraits.STORAGE_CONFIGURATION, disabledUuid).as(StorageConfiguration.class).getConfiguration();

			assertFalse("The legacy entry should be gone",                                              disabled.containsKey(LEGACY_DELETE_STALE_KEY));
			assertFalse("A disabled sync.deleteStale carries no information and should not be kept",    disabled.containsKey(VanishPolicy.POLICY_KEY));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testExistingVanishPolicyWinsOverLegacyDeleteStale() {

		String uuid = null;

		try (final Tx tx = app.tx()) {

			// both names on one configuration: the explicit policy is the newer statement
			uuid = StorageProviderFactory.createConfig("bothKeysMount", LocalFSStorageProvider.class,
				Map.of(LEGACY_DELETE_STALE_KEY, "true", VanishPolicy.POLICY_KEY, VanishPolicy.READONLY.value())).getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try {

			MigrationService.execute(false);

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final StorageConfiguration configuration = app.getNodeById(StructrTraits.STORAGE_CONFIGURATION, uuid).as(StorageConfiguration.class);
			final Map<String, String> entries        = configuration.getConfiguration();

			assertEquals("The existing policy should be kept",  VanishPolicy.READONLY.value(), entries.get(VanishPolicy.POLICY_KEY));
			assertFalse("The legacy entry should be gone",      entries.containsKey(LEGACY_DELETE_STALE_KEY));

			// the configuration map collapses duplicate names, so count the entry nodes themselves
			int policyEntries = 0;

			for (final NodeInterface entry : configuration.getEntries()) {

				if (VanishPolicy.POLICY_KEY.equals(entry.getProperty(entry.getTraits().key(StorageConfigurationEntryTraitDefinition.NAME_PROPERTY)))) {
					policyEntries++;
				}
			}

			assertEquals("There should be exactly one sync.onVanished entry", 1, policyEntries);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testUnreadableLegacyValueIsLeftInPlace() {

		String uuid = null;

		try (final Tx tx = app.tx()) {

			// an entry whose value cannot be read: leaving it alone keeps the old name visible instead
			// of quietly turning a configured "delete" into the default
			final StorageConfiguration configuration = StorageProviderFactory.createConfig("unreadableMount", LocalFSStorageProvider.class, Map.of());
			configuration.addEntry(LEGACY_DELETE_STALE_KEY, null);

			uuid = configuration.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try {

			MigrationService.execute(false);

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final Map<String, String> entries = app.getNodeById(StructrTraits.STORAGE_CONFIGURATION, uuid).as(StorageConfiguration.class).getConfiguration();

			assertTrue("An unreadable legacy entry should stay as it is",     entries.containsKey(LEGACY_DELETE_STALE_KEY));
			assertFalse("No policy should be invented for an unreadable one", entries.containsKey(VanishPolicy.POLICY_KEY));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testMigrationLeavesNothingPending() {

		try (final Tx tx = app.tx()) {

			StorageProviderFactory.createConfig("pendingMount", LocalFSStorageProvider.class, Map.of(LEGACY_DELETE_STALE_KEY, "true"));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try {

			assertTrue("A dry run should report the legacy entry as pending", MigrationService.execute(true));

			MigrationService.execute(false);

			// the startup dry run stops an instance whenever anything is pending, so a migration that
			// keeps finding work would keep that instance from starting
			assertFalse("After the migration a dry run should find nothing to do", MigrationService.execute(true));

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}
}
