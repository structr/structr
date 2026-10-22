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

import io.restassured.RestAssured;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.schema.JsonSchema;
import org.structr.api.schema.JsonType;
import org.structr.common.AccessControllable;
import org.structr.common.Permission;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.ResourceAccessTraitDefinition;
import org.structr.core.property.PropertyKey;
import org.structr.files.sync.ExternalFileSyncHandler;
import org.structr.files.sync.StorageSyncService;
import org.structr.schema.export.StructrSchema;
import org.structr.storage.StorageProviderFactory;
import org.structr.storage.providers.local.LocalFSStorageProvider;
import org.structr.storage.sync.ExternalEntry;
import org.structr.storage.sync.SyncTarget;
import org.structr.storage.sync.VirtualChangeEvent;
import org.structr.test.web.StructrUiTest;
import org.structr.storage.sync.SyncSettings;
import org.structr.storage.sync.VanishPolicy;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;
import org.structr.web.entity.StorageConfiguration;
import org.structr.web.entity.User;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.StorageConfigurationEntryTraitDefinition;
import org.structr.web.traits.definitions.FolderTraitDefinition;
import org.structr.web.traits.definitions.StorageConfigurationTraitDefinition;
import org.structr.web.traits.definitions.ImageTraitDefinition;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.testng.AssertJUnit.*;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Common ground for the tests of the StorageSyncService: the services it needs, a clean slate of sync
 * targets for every test, and helpers that wait for the service instead of sleeping.
 */
public abstract class SyncTestBase extends StructrUiTest {

	private static final Logger syncTestLogger = LoggerFactory.getLogger(SyncTestBase.class.getName());

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	@Override
	public void setup(@Optional final String testDatabaseConnection) {

		Settings.Services.setValue("NodeService SchemaService HttpService StorageSyncService");

		super.setup(testDatabaseConnection);
	}

	@BeforeMethod(alwaysRun = true)
	public void resetRecordedEvents() {

		RecordingStorageProvider.reset();
		ScriptedStorageProvider.reset();
	}

	/**
	 * The database is wiped below the lifecycle before the next test, which would leave every
	 * synchronizer, watcher and scan schedule of this one attached for the rest of the class. Deleting
	 * the mount roots through the lifecycle detaches them.
	 */
	@AfterMethod(alwaysRun = true)
	public void detachMounts() {

		final List<String> roots = new ArrayList<>();

		// unlinking detaches every mount through the lifecycle, and a scan still running stops at its next
		// batch; the nodes themselves go with the database cleanup that follows
		retryCleanup("unlink the mount roots", () -> {

			roots.clear();

			try (final Tx tx = app.tx()) {

				// filtered here rather than in the query, so that a query that cannot match relationship
				// properties cannot silently leave every mount attached
				for (final NodeInterface node : app.nodeQuery(StructrTraits.ABSTRACT_FILE).getAsList()) {

					if (node.as(AbstractFile.class).getStorageConfiguration() != null) {

						node.setProperty(Traits.of(StructrTraits.ABSTRACT_FILE).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), null);
						roots.add(node.getUuid());
					}
				}

				tx.success();
			}
		});

		syncTestLogger.info("Detached {} mount roots after {}", roots.size(), getClass().getSimpleName());

		SyncServiceProbe.awaitIdle(60000);
	}

	/**
	 * Runs a cleanup step, retrying it a few times: on a database with locks, a write can collide with
	 * the sync service's own and be terminated as a deadlock. A cleanup that still fails is logged and
	 * not thrown, so that the database cleanup after it runs in any case.
	 */
	private void retryCleanup(final String what, final CleanupStep step) {

		for (int attempt = 1; attempt <= 5; attempt++) {

			try {

				step.run();

				return;

			} catch (Throwable t) {

				syncTestLogger.warn("Unable to {} after {} (attempt {}): {}", what, getClass().getSimpleName(), attempt, t.getMessage());

				SyncServiceProbe.awaitIdle(10000);
			}
		}
	}

	@FunctionalInterface
	private interface CleanupStep {
		void run() throws Exception;
	}

	/**
	 * Waits until the service has no queued or in-flight event, no pending outbound change and no
	 * running scan. This is the signal a test needs before asserting that something did NOT happen.
	 */
	protected void awaitIdle() {

		if (!SyncServiceProbe.awaitIdle(60000)) {

			fail("The storage sync service should have become idle, but has " + SyncServiceProbe.describeActivity());
		}
	}

	/**
	 * @return the number of external change events the service has handled so far; a test that
	 * expects an event to change nothing waits for this to move instead of sleeping
	 */
	protected long handledEvents() {

		return SyncServiceProbe.handledEvents();
	}

	protected void awaitHandledEventsBeyond(final long count) {

		waitFor("The storage sync service should have handled the external change", () -> SyncServiceProbe.handledEvents() > count);
	}

	protected NodeInterface getNodeAtPath(final String path) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.nodeQuery(StructrTraits.ABSTRACT_FILE).key(Traits.of(StructrTraits.ABSTRACT_FILE).key(AbstractFileTraitDefinition.PATH_PROPERTY), path).getFirst();

			tx.success();

			return node;

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");

			return null;
		}
	}

	protected void waitForNodeAtPath(final String type, final String path) {

		waitFor("Node at " + path + " should have been created", () -> {

			try (final Tx tx = app.tx()) {

				final NodeInterface node = app.nodeQuery(type).key(Traits.of(type).key(AbstractFileTraitDefinition.PATH_PROPERTY), path).getFirst();

				tx.success();

				return node != null;

			} catch (FrameworkException fex) {

				return false;
			}
		});
	}

	protected void setNodeProperty(final NodeInterface node, final String keyName, final Object value) {

		try (final Tx tx = app.tx()) {

			node.setProperty(Traits.of(StructrTraits.ABSTRACT_FILE).key(keyName), value);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	/**
	 * Creates a mount root for the given storage configuration, inside the caller's transaction, with
	 * the per-mount settings watch and scan interval.
	 */
	protected NodeInterface mountFolder(final String folderName, final StorageConfiguration configuration, final boolean watchContents, final Integer scanInterval) throws FrameworkException {

		final Traits folderTraits = Traits.of(StructrTraits.FOLDER);

		syncSettings(configuration, watchContents, scanInterval);

		return app.create(StructrTraits.FOLDER,
			new NodeAttribute<>(folderTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), folderName),
			new NodeAttribute<>(folderTraits.key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), configuration)
		);
	}

	/**
	 * Stores the per-mount settings watch and scan interval as entries of the given storage
	 * configuration, inside the caller's transaction. A setting at its default gets no entry.
	 */
	protected void syncSettings(final StorageConfiguration configuration, final boolean watchContents, final Integer scanInterval) throws FrameworkException {

		if (watchContents) {

			configuration.addEntry(SyncSettings.WATCH_CONTENTS_KEY, "true");
		}

		if (scanInterval != null) {

			configuration.addEntry(SyncSettings.SCAN_INTERVAL_KEY, Integer.toString(scanInterval));
		}
	}

	/**
	 * Mounts a folder on a ScriptedStorageProvider scenario, with optional further configuration entries.
	 */
	protected void mountScripted(final String folderName, final ScriptedStorageProvider.Scenario scenario, final boolean watchContents, final Integer scanInterval, final Map<String, String> extraEntries) {

		try (final Tx tx = app.tx()) {

			final Map<String, String> entries = new HashMap<>(extraEntries);

			entries.put(ScriptedStorageProvider.SCENARIO_KEY, scenario.name());

			mountFolder(folderName, StorageProviderFactory.createConfig("scripted-" + folderName, ScriptedStorageProvider.class, entries), watchContents, scanInterval);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	protected void mount(final String configurationName, final Path target, final String folderName, final boolean watchContents, final Integer scanInterval, final VanishPolicy policy) {

		try (final Tx tx = app.tx()) {

			final Map<String, String> entries = (policy == null)
				? Map.of("mountTarget", target.toString())
				: Map.of("mountTarget", target.toString(), VanishPolicy.POLICY_KEY, policy.value());

			mountFolder(folderName, StorageProviderFactory.createConfig(configurationName, LocalFSStorageProvider.class, entries), watchContents, scanInterval);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	/**
	 * Waits until the named mount root records a complete scan that started after the given time.
	 * The timestamp is written only after a scan that enumerated everything, so this is the signal
	 * that reconciliation has run.
	 */
	protected void awaitScanAfter(final String folderName, final long timestamp) {

		waitFor("Mount " + folderName + " should have completed a scan", () -> {

			final Long lastScanned = getLastScanned(folderName);

			return lastScanned != null && lastScanned > timestamp;
		});
	}

	/**
	 * @return the time the last complete scan of the named mount root started, recorded on its storage
	 * configuration, or null
	 */
	protected Long getLastScanned(final String folderName) {

		try (final Tx tx = app.tx()) {

			final NodeInterface folder       = app.nodeQuery(StructrTraits.FOLDER).name(folderName).getFirst();
			final StorageConfiguration config = folder != null ? folder.as(AbstractFile.class).getStorageConfiguration() : null;
			final Long lastScanned           = config != null ? config.getProperty(Traits.of(StructrTraits.STORAGE_CONFIGURATION).key(StorageConfigurationTraitDefinition.LAST_SCANNED_PROPERTY)) : null;

			tx.success();

			return lastScanned;

		} catch (FrameworkException fex) {

			return null;
		}
	}

	protected Object getFileProperty(final String name, final String key) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.nodeQuery(StructrTraits.FILE).name(name).getFirst();
			final Object value       = node != null ? node.getProperty(Traits.of(StructrTraits.FILE).key(key)) : null;

			tx.success();

			return value;

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");

			return null;
		}
	}

	protected boolean isExternallyDeleted(final String name) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.nodeQuery(StructrTraits.ABSTRACT_FILE).name(name).getFirst();

			tx.success();

			return node != null && node.as(AbstractFile.class).isExternallyDeleted();

		} catch (FrameworkException fex) {

			return false;
		}
	}

	protected void waitFor(final String message, final BooleanSupplier condition) {

		// generous, because these are waits for something to happen, and a large scan on a database
		// with locks takes a while
		final long deadline = System.currentTimeMillis() + 60000;

		while (System.currentTimeMillis() < deadline) {

			if (condition.getAsBoolean()) {

				return;
			}

			try { Thread.sleep(200); } catch (InterruptedException ignore) {}
		}

		fail(message);
	}

	protected void createTestFile(final Path path, final String content) throws IOException {

		try (final FileWriter writer = new FileWriter(path.toFile())) {

			writer.append(content);
			writer.flush();
		}
	}

	protected String getContent(final File file) {

		try (final InputStream is = file.getInputStream()) {

			return IOUtils.toString(is, "utf-8");

		} catch (IOException ioex) {

			ioex.printStackTrace();
			fail("Unexpected exception.");
		}

		return null;
	}

	protected void writeFile(final java.io.File file, final String content) throws IOException {

		try (final FileOutputStream os = new FileOutputStream(file)) {

			os.write(content.getBytes("utf-8"));
			os.flush();
		}
	}

	protected String readFile(final File file) throws IOException {

		try (final InputStream is = file.getInputStream()) {

			final String content = IOUtils.toString(is, "utf-8");

			return content;
		}
	}

	protected void cleanupDirectory(final Path root) {

		if (root == null) {

			return;
		}

		try {

			Files.walkFileTree(root, new FileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

					try {

						Files.delete(file);

					} catch (Throwable t) {

						t.printStackTrace();
					}

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {

					try {

						Files.delete(dir);

					} catch (Throwable t) {

						t.printStackTrace();
					}

					return FileVisitResult.CONTINUE;
				}
			});

		} catch (Throwable ex) {

			ex.printStackTrace();
		}
	}
}
