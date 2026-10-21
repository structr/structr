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
package org.structr.test.web.advanced;

import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.files.sync.StorageSyncService;
import org.structr.storage.StorageProviderFactory;
import org.structr.storage.providers.local.LocalFSStorageProvider;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.StorageConfiguration;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.StorageConfigurationTraitDefinition;
import org.structr.web.maintenance.deploy.StorageConfigurationDeploymentHandler;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.testng.AssertJUnit.*;

/**
 * Verifies that StorageConfiguration nodes, their key/value entries and the
 * linkage to the folders they configure survive a deployment export/import
 * roundtrip (handled via the ui module's deployment data).
 */
public class DeploymentStorageConfigurationTest extends DeploymentTestBase {

	@Test
	public void testStorageConfigurationRoundtrip() {

		final String configName          = "test-storage-config";
		final String provider            = LocalFSStorageProvider.class.getName();
		final Map<String, String> config = new LinkedHashMap<>();

		config.put("mountTarget",   "/tmp/structr-storage-config-test");
		config.put("customSetting", "custom-value");
		config.put("secretSetting", "s3cr3t-value");

		String configUuid = null;
		String folderUuid = null;

		// setup: a configuration with entries plus a folder linked to it
		try (final Tx tx = app.tx()) {

			final StorageConfiguration storageConfig = StorageProviderFactory.createConfig(configName, LocalFSStorageProvider.class, config);

			configUuid = storageConfig.getUuid();

			final NodeInterface folder = app.create(StructrTraits.FOLDER,
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),                    "mountedConfig"),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY),    storageConfig)
			);

			folderUuid = folder.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// export -> clean database -> import
		doImportExportRoundtrip(true);

		// verify everything survived
		try (final Tx tx = app.tx()) {

			final NodeInterface configNode = app.nodeQuery(StructrTraits.STORAGE_CONFIGURATION)
				.key(Traits.of(StructrTraits.STORAGE_CONFIGURATION).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), configName)
				.getFirst();

			assertNotNull("StorageConfiguration must survive the deployment roundtrip", configNode);
			assertEquals("StorageConfiguration uuid must be preserved", configUuid, configNode.getUuid());

			final StorageConfiguration storageConfig = configNode.as(StorageConfiguration.class);

			assertEquals("StorageConfiguration provider must be preserved", provider, storageConfig.getProvider());

			// all key/value entries intact (values decrypted through the accessor)
			final Map<String, String> restored = storageConfig.getConfiguration();

			assertEquals("All configuration entries must be preserved", config.size(), restored.size());

			for (final Map.Entry<String, String> expected : config.entrySet()) {

				assertEquals("Entry '" + expected.getKey() + "' must be preserved intact", expected.getValue(), restored.get(expected.getKey()));
			}

			// the folder was re-created and its linkage to the configuration restored
			final NodeInterface folderNode = app.getNodeById(StructrTraits.ABSTRACT_FILE, folderUuid);

			assertNotNull("Linked folder must survive the deployment roundtrip", folderNode);

			final StorageConfiguration linkedConfig = folderNode.as(AbstractFile.class).getStorageConfiguration();

			assertNotNull("Folder must be re-linked to its storage configuration", linkedConfig);
			assertEquals("Folder must be linked to the same storage configuration", configUuid, linkedConfig.getUuid());

			// and the reverse relationship resolves as well
			final Iterable<NodeInterface> linkedFolders = configNode.getProperty(Traits.of(StructrTraits.STORAGE_CONFIGURATION).key(StorageConfigurationTraitDefinition.FOLDERS_PROPERTY));
			boolean folderFound = false;

			for (final NodeInterface linked : linkedFolders) {

				if (folderUuid.equals(linked.getUuid())) {

					folderFound = true;
					break;
				}
			}

			assertTrue("Configuration's folder back-reference must include the linked folder", folderFound);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testStorageConfigIsNotExportedByFileDeployment() {

		// guards against a regression where AbstractFile/File/Folder deployment
		// would also serialize the storageConfiguration/entries - which would
		// double up with (or conflict against) the ui module deployment data

		final Map<String, String> config = new LinkedHashMap<>();

		config.put("mountTarget",   "/tmp/structr-storage-config-notexported");
		config.put("secretSetting", "must-not-appear-in-files-json");

		try (final Tx tx = app.tx()) {

			final StorageConfiguration storageConfig = StorageProviderFactory.createConfig("notexported-config", LocalFSStorageProvider.class, config);

			app.create(StructrTraits.FOLDER,
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),                      "notExportedMount"),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY),      storageConfig)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		Path exportPath = null;

		try {

			exportPath = doExport();

			final String filesJson = Files.readString(exportPath.resolve("files.json"));

			// the folder node itself is exported (so the mount point survives)...
			assertTrue("Mount folder node must be present in files.json", filesJson.contains("/notExportedMount"));

			// ...but none of the storage-configuration data may leak into it
			assertFalse("files.json must not contain the storageConfiguration relationship", filesJson.contains("storageConfiguration"));
			assertFalse("files.json must not contain the storageKey property",              filesJson.contains("storageKey"));
			assertFalse("files.json must not contain configuration entry keys",             filesJson.contains("mountTarget"));
			assertFalse("files.json must not contain configuration entry values",           filesJson.contains("must-not-appear-in-files-json"));

			// the storage configuration is instead handled by the ui module data
			final Path moduleConf = exportPath.resolve("modules").resolve("ui").resolve(StorageConfigurationDeploymentHandler.STORAGE_CONFIGURATIONS_FILE);

			assertTrue("Storage configuration must be exported via the ui module deployment data", Files.exists(moduleConf));

			final String moduleJson = Files.readString(moduleConf);

			assertTrue("Module data must contain the configuration entries", moduleJson.contains("mountTarget"));

		} catch (FrameworkException | IOException ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");

		} finally {

			if (exportPath != null) {

				try { deleteExportAt(exportPath); } catch (IOException ignore) {}
			}
		}
	}

	@Test
	public void testMountedFolderContentsAreNotExportedWhileTheSyncServiceIsNotRunning() {

		final Services services = Services.getInstance();

		Path mountTarget = null;
		Path exportPath  = null;

		try {

			// an instance whose saved service list does not include the storage sync service
			services.shutdownService("StorageSyncService.default");

			assertNull("The storage sync service should not be running", StructrApp.getInstance().getService(StorageSyncService.class));

			mountTarget = Files.createTempDirectory("structr-deployment-mount-test");

			try (final Tx tx = app.tx()) {

				final StorageConfiguration storageConfig = StorageProviderFactory.createConfig("contents-config", LocalFSStorageProvider.class, Map.of("mountTarget", mountTarget.toString()));

				final NodeInterface folder = app.create(StructrTraits.FOLDER,
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),                      "contentsMount"),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY),      storageConfig)
				);

				// what an earlier run of the sync service would have imported
				app.create(StructrTraits.FILE,
					new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),                      "inside.txt"),
					new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true),
					new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY),                     folder)
				);

				tx.success();

			} catch (FrameworkException fex) {

				fex.printStackTrace();
				fail("Unexpected exception.");
			}

			exportPath = doExport();

			final String filesJson = Files.readString(exportPath.resolve("files.json"));

			assertTrue("The mount folder itself should be exported",                       filesJson.contains("/contentsMount"));
			assertFalse("The contents of a mounted folder belong to its storage, not to the export", filesJson.contains("/contentsMount/inside.txt"));

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");

		} finally {

			try {

				services.startService("StorageSyncService.default");

			} catch (Exception ex) {

				ex.printStackTrace();
				fail("Unexpected exception while restarting the service: " + ex.getMessage());
			}

			try {

				if (exportPath != null) {

					deleteExportAt(exportPath);
				}

				if (mountTarget != null) {

					deleteExportAt(mountTarget);
				}

			} catch (IOException ignore) {}
		}
	}

	@Test
	public void testMountIsLiveAgainAfterADeploymentImport() {

		Path mountTarget = null;
		String folderUuid = null;

		try {

			mountTarget = Files.createTempDirectory("structr-deployment-live-mount-test");

			Files.writeString(mountTarget.resolve("external.txt"), "lives in the mount");

			try (final Tx tx = app.tx()) {

				final StorageConfiguration storageConfig = StorageProviderFactory.createConfig("live-config", LocalFSStorageProvider.class, Map.of("mountTarget", mountTarget.toString()));

				folderUuid = app.create(StructrTraits.FOLDER,
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY),                      "liveMount"),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.INCLUDE_IN_FRONTEND_EXPORT_PROPERTY), true),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY),      storageConfig)
				).getUuid();

				tx.success();

			} catch (FrameworkException fex) {

				fex.printStackTrace();
				fail("Unexpected exception.");
			}

			// export -> clean database -> import
			doImportExportRoundtrip(true);

			final String uuid = folderUuid;

			waitUntil("The imported mount should be attached again", () -> {

				final StorageSyncService service = StructrApp.getInstance().getService(StorageSyncService.class);

				return service != null && service.isSynchronized(uuid);
			});

			waitUntil("The imported mount should be scanned again", () -> {

				try (final Tx tx = app.tx()) {

					final boolean found = app.nodeQuery(StructrTraits.FILE).name("external.txt").getFirst() != null;

					tx.success();

					return found;

				} catch (FrameworkException fex) {

					return false;
				}
			});

		} catch (IOException ioex) {

			ioex.printStackTrace();
			fail("Unexpected exception.");

		} finally {

			try {

				if (mountTarget != null) {

					deleteExportAt(mountTarget);
				}

			} catch (IOException ignore) {}
		}
	}

	private void waitUntil(final String message, final java.util.function.BooleanSupplier condition) {

		final long deadline = System.currentTimeMillis() + 15000;

		while (System.currentTimeMillis() < deadline) {

			if (condition.getAsBoolean()) {

				return;
			}

			try { Thread.sleep(200); } catch (InterruptedException ignore) {}
		}

		fail(message);
	}
}
