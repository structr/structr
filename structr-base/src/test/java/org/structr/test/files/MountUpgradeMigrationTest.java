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
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.GenericProperty;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.storage.StorageProviderFactory;
import org.structr.storage.providers.local.LocalFSStorageProvider;
import org.structr.storage.sync.ExternalEntry;
import org.structr.storage.sync.SyncSettings;
import org.structr.storage.sync.VanishPolicy;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.StorageConfiguration;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.StorageConfigurationTraitDefinition;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.testng.AssertJUnit.*;

/**
 * Upgrades of instances whose folders still carry the mount settings Folder used to declare: the
 * shapes a 6.x and a 7.0.0 database can have, written with the properties' raw names.
 */
public class MountUpgradeMigrationTest extends SyncTestBase {

	private static final String WATCH_CONTENTS = "mountWatchContents";
	private static final String SCAN_INTERVAL  = "mountScanInterval";
	private static final String FILE_TYPE      = "mountTargetFileType";
	private static final String FOLDER_TYPE    = "mountTargetFolderType";
	private static final String LAST_SCANNED   = "mountLastScanned";
	private static final String MOUNT_TARGET   = "mountTarget";

	@Test
	public void testSixXFolderBecomesAMountWithItsSettings() {

		Path target = null;

		try {

			target = Files.createTempDirectory("structr-6x-mount");

			final String uuid = legacyFolder("sixX", null, null, Map.of(MOUNT_TARGET, target.toString(), WATCH_CONTENTS, true, SCAN_INTERVAL, 30, FILE_TYPE, StructrTraits.IMAGE, FOLDER_TYPE, StructrTraits.FOLDER));

			migrate();

			final Map<String, String> entries = entriesOf(uuid);

			assertEquals("The mount target should become an entry", target.toString(), entries.get(MOUNT_TARGET));
			assertEquals("true",                    entries.get(SyncSettings.WATCH_CONTENTS_KEY));
			assertEquals("30",                      entries.get(SyncSettings.SCAN_INTERVAL_KEY));
			assertEquals(StructrTraits.IMAGE,       entries.get(SyncSettings.FILE_TYPE_KEY));
			assertEquals(StructrTraits.FOLDER,      entries.get(SyncSettings.FOLDER_TYPE_KEY));
			assertNoLegacyValues(uuid);

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");

		} finally {

			cleanupDirectory(target);
		}
	}

	@Test
	public void testSevenZeroFolderSettingsMoveOntoItsConfiguration() {

		// no mount target, so the configuration does not attach and nothing scans while the test reads it
		final String uuid = legacyFolder("sevenZero", "sevenZeroConfig", Map.of("sync.deleteStale", "true"), Map.of(SCAN_INTERVAL, 10, WATCH_CONTENTS, false, LAST_SCANNED, 12345L));

		migrate();

		final Map<String, String> entries = entriesOf(uuid);

		assertEquals(VanishPolicy.DELETE.value(), entries.get(VanishPolicy.POLICY_KEY));
		assertEquals("10",                        entries.get(SyncSettings.SCAN_INTERVAL_KEY));
		assertFalse("A setting at its default should not become an entry", entries.containsKey(SyncSettings.WATCH_CONTENTS_KEY));
		assertEquals("The last scan time should move to the configuration", Long.valueOf(12345L), lastScannedOf(uuid));
		assertNoLegacyValues(uuid);
	}

	@Test
	public void testLeftoversOnAnUnmountedFolderAreOnlyCleared() {

		final String uuid = legacyFolder("unmounted", null, null, Map.of(WATCH_CONTENTS, true, SCAN_INTERVAL, 5));

		migrate();

		assertNull("A folder without a mount gets no configuration", configurationOf(uuid));
		assertNoLegacyValues(uuid);
	}

	@Test
	public void testAnExistingEntryIsNeverOverwritten() {

		final String uuid = legacyFolder("existing", "existingConfig", Map.of(SyncSettings.SCAN_INTERVAL_KEY, "99"), Map.of(SCAN_INTERVAL, 10));

		migrate();

		assertEquals("The entry that was already there should win", "99", entriesOf(uuid).get(SyncSettings.SCAN_INTERVAL_KEY));
		assertNoLegacyValues(uuid);
	}

	@Test
	public void testASharedConfigurationTakesTheFirstFolderByPath() {

		String first  = null;
		String second = null;

		try (final Tx tx = app.tx()) {

			final StorageConfiguration shared = StorageProviderFactory.createConfig("sharedConfig", LocalFSStorageProvider.class, Map.of());

			first  = createLegacyFolder("aShared", shared, Map.of(SCAN_INTERVAL, 10)).getUuid();
			second = createLegacyFolder("bShared", shared, Map.of(SCAN_INTERVAL, 20)).getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		migrate();

		assertEquals("The first folder by path should decide", "10", entriesOf(first).get(SyncSettings.SCAN_INTERVAL_KEY));
		assertNoLegacyValues(first);
		assertNoLegacyValues(second);
	}

	@Test
	public void testNestedMountsEachKeepTheirOwnSettings() {

		String outer = null;
		String inner = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface outerFolder = createLegacyFolder("outerLegacy", StorageProviderFactory.createConfig("outerConfig", LocalFSStorageProvider.class, Map.of()), Map.of(SCAN_INTERVAL, 10));
			final NodeInterface innerFolder = createLegacyFolder("innerLegacy", StorageProviderFactory.createConfig("innerConfig", LocalFSStorageProvider.class, Map.of()), Map.of(SCAN_INTERVAL, 20, FILE_TYPE, StructrTraits.IMAGE));

			innerFolder.setProperty(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.PARENT_PROPERTY), outerFolder);

			outer = outerFolder.getUuid();
			inner = innerFolder.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		migrate();

		assertEquals("10", entriesOf(outer).get(SyncSettings.SCAN_INTERVAL_KEY));
		assertFalse("The outer mount should not take the inner one's settings", entriesOf(outer).containsKey(SyncSettings.FILE_TYPE_KEY));
		assertEquals("20", entriesOf(inner).get(SyncSettings.SCAN_INTERVAL_KEY));
		assertEquals(StructrTraits.IMAGE, entriesOf(inner).get(SyncSettings.FILE_TYPE_KEY));
	}

	@Test
	public void testDryRunReportsAndChangesNothingAndASecondRunFindsNothing() {

		final String uuid = legacyFolder("dryRun", "dryRunConfig", Map.of(), Map.of(SCAN_INTERVAL, 10));

		assertTrue("A dry run on legacy data should report pending changes", dryRun());
		assertFalse("A dry run should not write entries", entriesOf(uuid).containsKey(SyncSettings.SCAN_INTERVAL_KEY));
		assertEquals("A dry run should not clear the legacy value", 10, ((Number) legacyValue(uuid, SCAN_INTERVAL)).intValue());

		migrate();
		migrate();

		assertEquals("A second run should not duplicate anything", "10", entriesOf(uuid).get(SyncSettings.SCAN_INTERVAL_KEY));
		assertFalse("A dry run after the migration should find nothing", dryRun());
	}

	@Test
	public void testAMigratedMountAttachesWithItsSettings() {

		final ScriptedStorageProvider.Scenario scenario = ScriptedStorageProvider.scenario("migratedMount").entries(ExternalEntry.file("a.txt", 10L, 1_700_000_000_000L));

		final String uuid = legacyFolder("migratedMount", "migratedConfig", Map.of(ScriptedStorageProvider.SCENARIO_KEY, "migratedMount"), Map.of(WATCH_CONTENTS, true), ScriptedStorageProvider.class);

		waitFor("The legacy mount should be attached", () -> scenario.synchronizersCreated() >= 1);
		awaitIdle();

		assertEquals("Before the migration the legacy setting is not read", 0, scenario.watchStarts());

		migrate();

		waitFor("The migrated mount should be watched", () -> scenario.watchStarts() >= 1);
		waitForNodeAtPath(StructrTraits.FILE, "/migratedMount/a.txt");
		assertNoLegacyValues(uuid);
	}

	// ----- private methods -----
	private String legacyFolder(final String name, final String configurationName, final Map<String, String> entries, final Map<String, Object> legacy) {
		return legacyFolder(name, configurationName, entries, legacy, LocalFSStorageProvider.class);
	}

	private String legacyFolder(final String name, final String configurationName, final Map<String, String> entries, final Map<String, Object> legacy, final Class provider) {

		try (final Tx tx = app.tx()) {

			final StorageConfiguration configuration = configurationName != null ? StorageProviderFactory.createConfig(configurationName, provider, entries) : null;
			final String uuid                        = createLegacyFolder(name, configuration, legacy).getUuid();

			tx.success();

			return uuid;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");

			return null;
		}
	}

	private NodeInterface createLegacyFolder(final String name, final StorageConfiguration configuration, final Map<String, Object> legacy) throws FrameworkException {

		final NodeInterface folder = app.create(StructrTraits.FOLDER,
			new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
			new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), configuration)
		);

		for (final Map.Entry<String, Object> value : legacy.entrySet()) {

			folder.setProperty(new GenericProperty<>(value.getKey()), value.getValue());
		}

		return folder;
	}

	private void migrate() {

		try {

			MigrationService.execute(false);

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	private boolean dryRun() {

		try {

			return MigrationService.execute(true);

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");

			return false;
		}
	}

	private StorageConfiguration configurationOf(final String uuid) {

		try (final Tx tx = app.tx()) {

			final StorageConfiguration configuration = app.getNodeById(StructrTraits.FOLDER, uuid).as(AbstractFile.class).getStorageConfiguration();

			tx.success();

			return configuration;

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");

			return null;
		}
	}

	private Map<String, String> entriesOf(final String uuid) {

		try (final Tx tx = app.tx()) {

			final StorageConfiguration configuration = app.getNodeById(StructrTraits.FOLDER, uuid).as(AbstractFile.class).getStorageConfiguration();
			final Map<String, String> entries        = configuration != null ? configuration.getConfiguration() : Map.of();

			tx.success();

			return entries;

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");

			return null;
		}
	}

	private Long lastScannedOf(final String uuid) {

		try (final Tx tx = app.tx()) {

			final StorageConfiguration configuration = app.getNodeById(StructrTraits.FOLDER, uuid).as(AbstractFile.class).getStorageConfiguration();
			final Long lastScanned                   = configuration.getProperty(Traits.of(StructrTraits.STORAGE_CONFIGURATION).key(StorageConfigurationTraitDefinition.LAST_SCANNED_PROPERTY));

			tx.success();

			return lastScanned;

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");

			return null;
		}
	}

	private Object legacyValue(final String uuid, final String key) {

		try (final Tx tx = app.tx()) {

			final Object value = app.getNodeById(StructrTraits.FOLDER, uuid).getProperty(new GenericProperty<>(key));

			tx.success();

			return value;

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");

			return null;
		}
	}

	private void assertNoLegacyValues(final String uuid) {

		for (final String key : new String[] { WATCH_CONTENTS, SCAN_INTERVAL, FILE_TYPE, FOLDER_TYPE, LAST_SCANNED, MOUNT_TARGET }) {

			assertNull("The legacy value " + key + " should be cleared", legacyValue(uuid, key));
		}
	}
}
