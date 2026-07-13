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
import org.structr.files.sync.ExternalFileSyncHandler;
import org.structr.files.sync.StorageSyncService;
import org.structr.schema.export.StructrSchema;
import org.structr.storage.StorageProviderFactory;
import org.structr.storage.providers.local.LocalFSStorageProvider;
import org.structr.storage.sync.ExternalEntry;
import org.structr.storage.sync.SyncTarget;
import org.structr.storage.sync.VirtualChangeEvent;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;
import org.structr.web.entity.StorageConfiguration;
import org.structr.web.entity.User;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.FolderTraitDefinition;
import org.structr.web.traits.definitions.ImageTraitDefinition;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.testng.AssertJUnit.*;


/**
 * Tests for the StorageSyncService, ported from the former
 * DirectoryWatchServiceTest and extended with sync-specific cases
 * (stale-node pruning, nested mounts, uuid-addressed entries).
 */
public class StorageSyncServiceTest extends StructrUiTest {

	private static final Logger logger = LoggerFactory.getLogger(StorageSyncServiceTest.class.getName());

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	@Override
	public void setup(@Optional String testDatabaseConnection) {

		Settings.Services.setValue("NodeService SchemaService HttpService StorageSyncService");

		super.setup(testDatabaseConnection);
	}

	@Test
	public void testServiceNameAlias() {

		// existing structr.conf files may still list the replaced DirectoryWatchService
		assertEquals("DirectoryWatchService should resolve to StorageSyncService", StorageSyncService.class, Services.getInstance().getServiceClassForName("DirectoryWatchService"));
	}

	@Test
	public void testMountedFolderInRoot() {

		final String dirName = "mountServiceTest1";
		final Path base      = Paths.get(basePath);
		Path testDir         = null;

		try {

			testDir = Files.createDirectory(base.resolve(dirName));

			createTestFile(testDir.resolve(Paths.get("test1.txt")), "test file content 1");
			createTestFile(testDir.resolve(Paths.get("test2.txt")), "test file content 2");
			createTestFile(testDir.resolve(Paths.get("test3.txt")), "test file content 3");

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		// mount directory
		try (final Tx tx = app.tx()) {

			// create folder to mount
			final StorageConfiguration testMount = StorageProviderFactory.createConfig("testMount", LocalFSStorageProvider.class, Map.of("mountTarget", testDir.toString()));

			app.create(StructrTraits.FOLDER,
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "mounted1"),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_WATCH_CONTENTS_PROPERTY), true),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), testMount)
			);

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		// wait for StorageSyncService to start and scan
		try { Thread.sleep(5000); } catch (InterruptedException ex) {}

		// verify mount point
		try (final Tx tx = app.tx()) {

			assertNotNull("Folder should have been created by import", app.nodeQuery(StructrTraits.FOLDER).key(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PATH_PROPERTY), "/mounted1").getFirst());

			final File file1 = app.nodeQuery(StructrTraits.FILE).key(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PATH_PROPERTY), "/mounted1/test1.txt").getFirst().as(File.class);
			final File file2 = app.nodeQuery(StructrTraits.FILE).key(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PATH_PROPERTY), "/mounted1/test2.txt").getFirst().as(File.class);
			final File file3 = app.nodeQuery(StructrTraits.FILE).key(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PATH_PROPERTY), "/mounted1/test3.txt").getFirst().as(File.class);

			assertNotNull("Test file should have been created by import", file1);
			assertNotNull("Test file should have been created by import", file2);
			assertNotNull("Test file should have been created by import", file3);

			assertEquals("Imported test file content does not match source", "test file content 1", getContent(file1));
			assertEquals("Imported test file content does not match source", "test file content 2", getContent(file2));
			assertEquals("Imported test file content does not match source", "test file content 3", getContent(file3));

			// files below a mounted folder must be mounted and readable
			assertTrue("Mounted file should report isMounted", file1.isMounted());

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testNestedMountedFolder() {

		final String dirName = "mountServiceTest2";
		final Path base      = Paths.get(basePath);
		Path testDir         = null;

		try {

			testDir = Files.createDirectory(base.resolve(dirName));

			createTestFile(testDir.resolve(Paths.get("test1.txt")), "test file content 1");
			createTestFile(testDir.resolve(Paths.get("test2.txt")), "test file content 2");
			createTestFile(testDir.resolve(Paths.get("test3.txt")), "test file content 3");

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		// mount directory
		try (final Tx tx = app.tx()) {

			final NodeInterface parent1 = app.create(StructrTraits.FOLDER, "parent");

			final NodeInterface parent2 = app.create(StructrTraits.FOLDER,
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "parent"),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.PARENT_PROPERTY), parent1)
			);

			// create folder to mount
			final StorageConfiguration testMount = StorageProviderFactory.createConfig("testMount", LocalFSStorageProvider.class, Map.of("mountTarget", testDir.toString()));

			app.create(StructrTraits.FOLDER,
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "mounted2"),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.PARENT_PROPERTY), parent2),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_WATCH_CONTENTS_PROPERTY), true),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), testMount)
			);

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		// wait for StorageSyncService to start and scan
		try { Thread.sleep(5000); } catch (InterruptedException ex) {}

		// verify mount point
		try (final Tx tx = app.tx()) {

			assertNotNull("Folder should have been created by import", app.nodeQuery(StructrTraits.FOLDER).key(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PATH_PROPERTY), "/parent/parent/mounted2").getFirst());

			final File file1 = app.nodeQuery(StructrTraits.FILE).key(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PATH_PROPERTY), "/parent/parent/mounted2/test1.txt").getFirst().as(File.class);
			final File file2 = app.nodeQuery(StructrTraits.FILE).key(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PATH_PROPERTY), "/parent/parent/mounted2/test2.txt").getFirst().as(File.class);
			final File file3 = app.nodeQuery(StructrTraits.FILE).key(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PATH_PROPERTY), "/parent/parent/mounted2/test3.txt").getFirst().as(File.class);

			assertNotNull("Test file should have been created by import", file1);
			assertNotNull("Test file should have been created by import", file2);
			assertNotNull("Test file should have been created by import", file3);

			assertEquals("Imported test file content does not match source", "test file content 1", getContent(file1));
			assertEquals("Imported test file content does not match source", "test file content 2", getContent(file2));
			assertEquals("Imported test file content does not match source", "test file content 3", getContent(file3));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testExternalChangesOfMountedDirectory() {

		Path root          = null;
		java.io.File file1 = null;
		java.io.File file2 = null;
		java.io.File file3 = null;

		try {

			logger.info("Creating directory to mount..");

			// create some files and folders on disk
			root = Files.createTempDirectory("structr-mount-test");

			root.resolve("parent1/child1/grandchild1").toFile().mkdirs();
			root.resolve("parent2/child1/grandchild1").toFile().mkdirs();
			root.resolve("parent3/child1/grandchild1").toFile().mkdirs();

			logger.info("Creating files to mount..");

			file1 = root.resolve("parent1/child1/grandchild1/test1.txt").toFile();
			file2 = root.resolve("parent2/child1/grandchild1/test2.txt").toFile();
			file3 = root.resolve("parent3/child1/grandchild1/test3.txt").toFile();

			writeFile(file1, "test1 - before change");
			writeFile(file2, "test2 - before change");
			writeFile(file3, "test3 - before change");

			// mount folder
			try (final Tx tx = app.tx()) {

				logger.info("Mounting directory..");

				// create folder to mount
				final StorageConfiguration testMount = StorageProviderFactory.createConfig("testMount", LocalFSStorageProvider.class, Map.of("mountTarget", root.toString()));

				app.create(StructrTraits.FOLDER,
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "mounted3"),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_WATCH_CONTENTS_PROPERTY),   true),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), testMount),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_SCAN_INTERVAL_PROPERTY),    2)
				);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}


			// wait some time
			try { Thread.sleep(5000); } catch (Throwable t) {}

			// check that all files and folders exist
			try (final Tx tx = app.tx()) {

				logger.info("Checking directory..");

				final File check1 = app.nodeQuery(StructrTraits.FILE).name("test1.txt").getFirst().as(File.class);
				final File check2 = app.nodeQuery(StructrTraits.FILE).name("test2.txt").getFirst().as(File.class);
				final File check3 = app.nodeQuery(StructrTraits.FILE).name("test3.txt").getFirst().as(File.class);

				assertEquals("Invalid mount result", "/mounted3/parent1/child1/grandchild1/test1.txt", check1.getPath());
				assertEquals("Invalid mount result", "/mounted3/parent2/child1/grandchild1/test2.txt", check2.getPath());
				assertEquals("Invalid mount result", "/mounted3/parent3/child1/grandchild1/test3.txt", check3.getPath());

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			// test external changes to files
			writeFile(file2, "test2 - AFTER change");

			// wait some time
			try { Thread.sleep(4000); } catch (InterruptedException ignore) {}

			// check that external changes are present in the file
			try (final Tx tx = app.tx()) {

				logger.info("Checking directory..");

				final File check2 = app.nodeQuery(StructrTraits.FILE).name("test2.txt").getFirst().as(File.class);

				assertEquals("Invalid checksum of externally modified file", FileHelper.getChecksum(file2), check2.getChecksum());
				assertEquals("Invalid content of externally modified file", "test2 - AFTER change", readFile(check2));

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			// unmount folder
			try (final Tx tx = app.tx()) {

				logger.info("Unmounting directory..");

				final NodeInterface mounted = app.nodeQuery(StructrTraits.FOLDER).key(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "mounted3").getFirst();

				mounted.setProperty(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_TARGET_PROPERTY), null);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

		} catch (IOException ioex) {

			fail("Unexpected exception.");

		} finally {

			cleanupDirectory(root);
		}

	}

	@Test
	public void testDisableWatchKeyRegistration() {

		Path root          = null;
		java.io.File file1 = null;
		java.io.File file2 = null;
		java.io.File file3 = null;

		try {

			logger.info("Creating directory to mount..");

			// create some files and folders on disk
			root = Files.createTempDirectory("structr-mount-test");

			root.resolve("parent1/child1/grandchild1").toFile().mkdirs();
			root.resolve("parent2/child1/grandchild1").toFile().mkdirs();
			root.resolve("parent3/child1/grandchild1").toFile().mkdirs();

			logger.info("Creating files to mount..");

			file1 = root.resolve("parent1/child1/grandchild1/test1.txt").toFile();
			file2 = root.resolve("parent2/child1/grandchild1/test2.txt").toFile();
			file3 = root.resolve("parent3/child1/grandchild1/test3.txt").toFile();

			writeFile(file1, "test1 - before change");
			writeFile(file2, "test2 - before change");
			writeFile(file3, "test3 - before change");

			// mount folder
			try (final Tx tx = app.tx()) {

				logger.info("Mounting directory..");

				final StorageConfiguration testMount = StorageProviderFactory.createConfig("testMount", LocalFSStorageProvider.class, Map.of("mountTarget", root.toString()));

				app.create(StructrTraits.FOLDER,
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "mounted3"),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), testMount),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_WATCH_CONTENTS_PROPERTY), false)
				);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}


			// wait some time
			try { Thread.sleep(5000); } catch (Throwable t) {}

			// check that all files and folders exist
			try (final Tx tx = app.tx()) {

				logger.info("Checking directory..");

				final File check1 = app.nodeQuery(StructrTraits.FILE).name("test1.txt").getFirst().as(File.class);
				final File check2 = app.nodeQuery(StructrTraits.FILE).name("test2.txt").getFirst().as(File.class);
				final File check3 = app.nodeQuery(StructrTraits.FILE).name("test3.txt").getFirst().as(File.class);

				assertEquals("Invalid mount result", "/mounted3/parent1/child1/grandchild1/test1.txt", check1.getPath());
				assertEquals("Invalid mount result", "/mounted3/parent2/child1/grandchild1/test2.txt", check2.getPath());
				assertEquals("Invalid mount result", "/mounted3/parent3/child1/grandchild1/test3.txt", check3.getPath());

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			// test external changes to files
			writeFile(file2, "test2 - AFTER change");

			// wait some time
			try { Thread.sleep(1000); } catch (InterruptedException ignore) {}

			// check that external changes are NOT recorded
			try (final Tx tx = app.tx()) {

				logger.info("Checking directory..");

				final File check2 = app.nodeQuery(StructrTraits.FILE).name("test2.txt").getFirst().as(File.class);

				assertFalse("Invalid checksum of externally modified file", FileHelper.getMD5Checksum(file2).equals(check2.getMd5()));
				assertEquals("Invalid content of externally modified file", "test2 - AFTER change", readFile(check2));

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			// unmount folder
			try (final Tx tx = app.tx()) {

				logger.info("Unmounting directory..");

				final NodeInterface mounted = app.nodeQuery(StructrTraits.FOLDER).key(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "mounted3").getFirst();

				mounted.setProperty(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_TARGET_PROPERTY), null);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

		} catch (IOException ioex) {

			fail("Unexpected exception.");

		} finally {

			cleanupDirectory(root);
		}
	}

	@Test
	public void testStaleNodePruning() {

		Path root = null;

		try {

			root = Files.createTempDirectory("structr-prune-test");

			writeFile(root.resolve("keep.txt").toFile(),  "this file stays");
			writeFile(root.resolve("prune.txt").toFile(), "this file will vanish");

			// mount folder with pruning enabled and a short rescan interval, without live watching
			try (final Tx tx = app.tx()) {

				final StorageConfiguration testMount = StorageProviderFactory.createConfig("pruneMount", LocalFSStorageProvider.class,
					Map.of("mountTarget", root.toString(), StorageSyncService.DELETE_STALE_KEY, "true"));

				app.create(StructrTraits.FOLDER,
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "mounted4"),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_WATCH_CONTENTS_PROPERTY), false),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_SCAN_INTERVAL_PROPERTY), 2),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), testMount)
				);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			// wait for the initial scan
			try { Thread.sleep(5000); } catch (InterruptedException ignore) {}

			try (final Tx tx = app.tx()) {

				assertNotNull("File should have been created by initial scan", app.nodeQuery(StructrTraits.FILE).name("keep.txt").getFirst());
				assertNotNull("File should have been created by initial scan", app.nodeQuery(StructrTraits.FILE).name("prune.txt").getFirst());

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			// delete one file externally - without live watching, only a rescan can detect this
			Files.delete(root.resolve("prune.txt"));

			// wait for the next rescan
			try { Thread.sleep(6000); } catch (InterruptedException ignore) {}

			try (final Tx tx = app.tx()) {

				assertNotNull("Unchanged file should survive pruning",              app.nodeQuery(StructrTraits.FILE).name("keep.txt").getFirst());
				assertNull("Node of externally deleted file should be pruned",     app.nodeQuery(StructrTraits.FILE).name("prune.txt").getFirst());

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

		} catch (IOException ioex) {

			fail("Unexpected exception.");

		} finally {

			cleanupDirectory(root);
		}
	}

	@Test
	public void testNestedMountBoundary() {

		Path outerDir = null;
		Path innerDir = null;

		try {

			outerDir = Files.createTempDirectory("structr-outer-mount");
			innerDir = Files.createTempDirectory("structr-inner-mount");

			writeFile(outerDir.resolve("outer.txt").toFile(), "outer content");
			writeFile(innerDir.resolve("inner.txt").toFile(), "inner content");

			// mount the outer directory with pruning and rescans, and mount a
			// second directory on a subfolder inside the outer mount
			try (final Tx tx = app.tx()) {

				final StorageConfiguration outerMount = StorageProviderFactory.createConfig("outerMount", LocalFSStorageProvider.class,
					Map.of("mountTarget", outerDir.toString(), StorageSyncService.DELETE_STALE_KEY, "true"));

				final StorageConfiguration innerMount = StorageProviderFactory.createConfig("innerMount", LocalFSStorageProvider.class,
					Map.of("mountTarget", innerDir.toString()));

				final NodeInterface outer = app.create(StructrTraits.FOLDER,
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "outer"),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_WATCH_CONTENTS_PROPERTY), false),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_SCAN_INTERVAL_PROPERTY), 2),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), outerMount)
				);

				app.create(StructrTraits.FOLDER,
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "inner"),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.PARENT_PROPERTY), outer),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_WATCH_CONTENTS_PROPERTY), false),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), innerMount)
				);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			// wait for the initial scans and at least one pruning rescan of the outer mount
			try { Thread.sleep(8000); } catch (InterruptedException ignore) {}

			try (final Tx tx = app.tx()) {

				final NodeInterface outerFile = app.nodeQuery(StructrTraits.FILE).key(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PATH_PROPERTY), "/outer/outer.txt").getFirst();
				final NodeInterface innerFile = app.nodeQuery(StructrTraits.FILE).key(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PATH_PROPERTY), "/outer/inner/inner.txt").getFirst();
				final NodeInterface inner     = app.nodeQuery(StructrTraits.FOLDER).key(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.PATH_PROPERTY), "/outer/inner").getFirst();

				assertNotNull("Outer mount should contain its file", outerFile);
				assertNotNull("Nested mount root should still exist", inner);

				// the nested mount is a boundary: the outer mount's pruning rescans must
				// not delete the inner mount's content even though the outer enumeration
				// never sees it
				assertNotNull("Nested mount content must not be pruned by the outer mount", innerFile);

				assertEquals("Invalid content of file in nested mount", "inner content", getContent(innerFile.as(File.class)));

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

		} catch (IOException ioex) {

			fail("Unexpected exception.");

		} finally {

			cleanupDirectory(outerDir);
			cleanupDirectory(innerDir);
		}
	}

	@Test
	public void testUuidAddressedEntries() {

		Path root = null;

		try {

			root = Files.createTempDirectory("structr-uuid-test");

			String folderUuid = null;
			String configUuid = null;

			try (final Tx tx = app.tx()) {

				final StorageConfiguration mount = StorageProviderFactory.createConfig("uuidMount", LocalFSStorageProvider.class, Map.of("mountTarget", root.toString()));

				final NodeInterface folder = app.create(StructrTraits.FOLDER,
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "mounted5"),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_WATCH_CONTENTS_PROPERTY), false),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), mount)
				);

				folderUuid = folder.getUuid();
				configUuid = mount.getUuid();

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			final SyncTarget target = new SyncTarget(folderUuid, "/mounted5", true, configUuid, Map.of("mountTarget", root.toString()));

			// the handler always runs in sync-originated transactions (see StorageSyncService)
			final App syncApp = StructrApp.getInstance(StorageSyncService.createSyncContext());

			// uuid+path entry with an unknown uuid: node is created WITH the given uuid
			final String givenUuid = "ffffffffffffffffffffffffffffffff";

			try (final Tx tx = syncApp.tx()) {

				final ExternalFileSyncHandler handler = new ExternalFileSyncHandler(target);

				handler.handleCreatedOrModified(ExternalEntry.byUuidAndPath(givenUuid, "sub/created-by-uuid.txt", false, 123L, System.currentTimeMillis()));

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception.");
			}

			try (final Tx tx = syncApp.tx()) {

				final NodeInterface created = app.getNodeById(StructrTraits.FILE, givenUuid);

				assertNotNull("Node should have been created with the provider-supplied UUID", created);
				assertEquals("Invalid path of node created by uuid+path entry", "/mounted5/sub/created-by-uuid.txt", created.as(File.class).getPath());

				// uuid-addressed event with a differing path: node follows the external move
				final ExternalFileSyncHandler handler = new ExternalFileSyncHandler(target);

				handler.handleCreatedOrModified(ExternalEntry.byUuidAndPath(givenUuid, "sub2/renamed.txt", false, 123L, System.currentTimeMillis()));

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception.");
			}

			try (final Tx tx = syncApp.tx()) {

				final NodeInterface moved = app.getNodeById(StructrTraits.FILE, givenUuid);

				assertNotNull("Node should still exist after uuid-addressed move", moved);
				assertEquals("Node should have followed the external move", "/mounted5/sub2/renamed.txt", moved.as(File.class).getPath());

				// uuid-only entry for an unknown uuid: no node can be created (no name/path), must be ignored
				final ExternalFileSyncHandler handler = new ExternalFileSyncHandler(target);

				assertNull("Unknown uuid-only entry must be ignored", handler.handleCreatedOrModified(ExternalEntry.byUuid("0000000000000000000000000000000a", false, 1L, 1L)));

				// uuid-addressed deletion resolves and deletes the node
				handler.handleDeleted(ExternalEntry.byUuid(givenUuid, false, null, null));

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception.");
			}

			try (final Tx tx = app.tx()) {

				assertNull("Node should have been deleted by uuid-addressed deletion", app.getNodeById(StructrTraits.FILE, givenUuid));

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

		} catch (IOException ioex) {

			fail("Unexpected exception.");

		} finally {

			cleanupDirectory(root);
		}
	}

	@Test
	public void testImageDataUploadToMountedFolder() {

		final String base64Data = "iVBORw0KGgoAAAANSUhEUgAAAFYAAAAXCAYAAAHmVYioAAAAAXNSR0IArs4c6QAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB9sEBxYRJKsLwDYAAAAZdEVYdENvbW1lbnQAQ3JlYXRlZCB3aXRoIEdJTVBXgQ4XAAAD00lEQVRYw+1Y3XXyMAy9zuE1XSALhAHsBVigXsAL4AUyAQOQBeoF0gHIANECZIB6gTCAvhecY0wClNLv0HO4L4Cw/CNL8pWElBIAUFUVbzYbgUvouo632y0zM0spsd1uWUqJruv44+ODpZRYr9fcdR0vrLWCiDgoO+fG2eu6FgBARAIAi7CNOTjn2BgjACAzxjAAKKW4bVsOvwHAGMNlWY7fs7AsEYn9fp/OKuLvZ9sgIlZKXbZKgimdTGvNVVXxlEJd1xy2m8rmoJRiAMiGYYD3/kQhnggADocDjDFsjGHv/TgmtVusc/VGriHPcz4cDqdmwA+htT6TZUTExhherVYc26ZpmvGIwRHjz9gEQSfIF6l7HD0VRVGcyCccVQR7Bp0gz5RSQmvNbdsyHoTMGMN5niPPcwCAtZbzPD9bIL3hcPzgHd+6/XsC47swxrC1FlfXkVLi6+uLY3Rdx1JKSCkRy4Ms1QnpLB2TzhXGpHOm8jD+/f39RL6o65qLorh4qvQ/ImIigtb6Wxa/Zr30v6ZpYK0dnT2z1oqjgImItdaMJ0UWwlQpJZRSwnuP+AF6Jiw2mw2vVqsTYVVV8aMwbj6+kql8s1wuOT5o3/ew1oqpueKrT3XnXOViNlBKsdYaVVX9ejbo+37WEDe9MXVdj3nyN2Gtvd1n/wzmcl+aY2N5rDMMA4ex6/X6LM/O5WZm5kA1p2RSSux2uxP5wlqLtm2Rcr44SOq6Prs25xyGYbjZKE3TcFEUJ3MR0YnL9X2PQLy01rxarcbxRITFMfpnHXsYhknHDzz6VhRFASKaZTBpgIVYiceHPMtFUfCzu2ymlBJEhKZp8Eja9WsvmLVWKKXE8conqdzTbDbi5WKOnz/Fc5sS3BjeeyilRhJ8id6HLLHb7RgAlsvlxblChPd9L2Jd7z3atp1+bpmZ06gMxW+ccuI3e4qQl2XJzrkzjhEvHM8V0pVzThRFwU3TjAew1oopQv6jOjGqxk4Iy19EMM41wnZXfL3wwFzwiEne3t7GnkF4oEOM/0UcW2bjefb7PdLuzVXDHptct4T9bLiXZXlGy5RSs6F2qay9NSQvpaFra6V581olcBx30iZzziHOwen4RVwo3tt1uSXHhs3+j3z807Wu5Vit9WjUubHZVEv1hQeQQ2utCG4e2g+hVf+sjPZPGDZUfaHJZa2F9x6h3CUids5xWZYvI/+EbhGRMMaMhm6aBmVZwjmHS9XEC9/ksW3bvqx0B0TXdZxSo0uvofde3EJr0tfy1irtGpWZqJjOOPM9dCvPc/78/Jxs7KZ7voUS/gORqW62f9bxTgAAAABJRU5ErkJggg==";
		final String dirName    = "mountServiceTest";
		final Path base         = Paths.get(basePath);
		Path testDir            = null;

		try {

			testDir = Files.createDirectory(base.resolve(dirName));

		} catch (IOException ioex) {
			fail("Unable to create test files.");
		}

		// mount directory
		try (final Tx tx = app.tx()) {

			// create test user
			final User tester = app.create(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "tester"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), "tester")
			).as(User.class);

			// create folder to mount
			final StorageConfiguration testMount = StorageProviderFactory.createConfig("testMount", LocalFSStorageProvider.class, Map.of("mountTarget", testDir.toString()));

			final AccessControllable folder = app.create(StructrTraits.FOLDER,
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "mounted"),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_WATCH_CONTENTS_PROPERTY), false),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), testMount)
			).as(AccessControllable.class);

			// make folder writable for user
			folder.grant(Permission.read, tester);
			folder.grant(Permission.write, tester);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			// set resource access flags to be able to POS to /Image
			final NodeInterface grant = app.nodeQuery(StructrTraits.RESOURCE_ACCESS).key(Traits.of(StructrTraits.RESOURCE_ACCESS).key(ResourceAccessTraitDefinition.SIGNATURE_PROPERTY), StructrTraits.IMAGE).getFirst();
			if (grant != null) {

				grant.setProperty(Traits.of(StructrTraits.RESOURCE_ACCESS).key(ResourceAccessTraitDefinition.FLAGS_PROPERTY),                       4L);
				grant.setProperty(Traits.of(StructrTraits.RESOURCE_ACCESS).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true);

			} else {

				app.create(StructrTraits.RESOURCE_ACCESS,
					new NodeAttribute<>(Traits.of(StructrTraits.RESOURCE_ACCESS).key(ResourceAccessTraitDefinition.SIGNATURE_PROPERTY),                   StructrTraits.IMAGE),
					new NodeAttribute<>(Traits.of(StructrTraits.RESOURCE_ACCESS).key(ResourceAccessTraitDefinition.FLAGS_PROPERTY),                       4L),
					new NodeAttribute<>(Traits.of(StructrTraits.RESOURCE_ACCESS).key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), true)
				);
			}

			// add onCreate method that sets the parent of an uploaded image
			final JsonSchema schema  = StructrSchema.createFromDatabase(app);
			final JsonType imageType = schema.getType(StructrTraits.IMAGE);
			imageType.addMethod("onCreation", "set(this, 'parent', first(find('Folder', 'name', 'mounted')))");
			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
			fail("Unexpected exception.");
		}

		RestAssured

			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, "tester" , X_PASSWORD_HEADER, "tester")
				.body("{ name: 'test.png', imageData: '" + base64Data + "' }")

			.expect()

				.statusCode(201)

			.when()
				.post("/Image");

		try { Thread.sleep(5000); } catch (Throwable t) {}

		try (final Tx tx = app.tx()) {

			final List<NodeInterface> images = app.nodeQuery(StructrTraits.IMAGE).getAsList();

			assertEquals("Only one image should exist", 1, images.size());

			final Image image = images.get(0).as(Image.class);

			assertNotNull(image);
			assertEquals("Invalid name of uploaded image", "test.png", image.getName());
			assertEquals("Invalid binary data of uploaded image", base64Data, image.getProperty(Traits.of(StructrTraits.IMAGE).key(ImageTraitDefinition.IMAGE_DATA_PROPERTY)));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testOutboundRenameMoveDeletePropagation() {

		Path root = null;

		try {

			root = Files.createTempDirectory("structr-outbound-test");

			Files.createDirectory(root.resolve("a"));
			writeFile(root.resolve("a/file.txt").toFile(), "outbound content");

			// mount with two-way sync, no live watching (echo behavior is tested separately)
			try (final Tx tx = app.tx()) {

				final StorageConfiguration mount = StorageProviderFactory.createConfig("outboundMount", LocalFSStorageProvider.class,
					Map.of("mountTarget", root.toString(), StorageSyncService.DIRECTION_KEY, "both"));

				app.create(StructrTraits.FOLDER,
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "outbound1"),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_WATCH_CONTENTS_PROPERTY), false),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), mount)
				);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			waitForNodeAtPath(StructrTraits.FILE, "/outbound1/a/file.txt");

			// rename the (external) file node
			setNodeProperty(getNodeAtPath("/outbound1/a/file.txt"), NodeInterfaceTraitDefinition.NAME_PROPERTY, "renamed.txt");

			final Path rootPath = root;
			waitFor("Physical file should have been renamed", () -> Files.exists(rootPath.resolve("a/renamed.txt")) && !Files.exists(rootPath.resolve("a/file.txt")));

			// create a folder inside Structr - the physical directory must materialize
			try (final Tx tx = app.tx()) {

				app.create(StructrTraits.FOLDER,
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "b"),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.PARENT_PROPERTY), getNodeAtPath("/outbound1"))
				);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			waitFor("Physical directory should have been created", () -> Files.isDirectory(rootPath.resolve("b")));

			// move the file into the new folder
			setNodeProperty(getNodeAtPath("/outbound1/a/renamed.txt"), AbstractFileTraitDefinition.PARENT_PROPERTY, getNodeAtPath("/outbound1/b"));

			waitFor("Physical file should have been moved", () -> Files.exists(rootPath.resolve("b/renamed.txt")) && !Files.exists(rootPath.resolve("a/renamed.txt")));

			// rename the (external) folder - one physical directory move
			setNodeProperty(getNodeAtPath("/outbound1/a"), NodeInterfaceTraitDefinition.NAME_PROPERTY, "a2");

			waitFor("Physical directory should have been renamed", () -> Files.isDirectory(rootPath.resolve("a2")) && !Files.exists(rootPath.resolve("a")));

			// delete file and folder nodes - physical entries must vanish
			try (final Tx tx = app.tx()) {

				app.delete(getNodeAtPath("/outbound1/b/renamed.txt"));

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			waitFor("Physical file should have been deleted", () -> !Files.exists(rootPath.resolve("b/renamed.txt")));

			try (final Tx tx = app.tx()) {

				app.delete(getNodeAtPath("/outbound1/b"));

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			waitFor("Physical directory should have been deleted", () -> !Files.exists(rootPath.resolve("b")));

		} catch (IOException ioex) {

			fail("Unexpected exception.");

		} finally {

			cleanupDirectory(root);
		}
	}

	@Test
	public void testOutboundForNonExternalNodeCreatedInStructr() {

		Path root = null;

		try {

			root = Files.createTempDirectory("structr-outbound-created-test");

			try (final Tx tx = app.tx()) {

				final StorageConfiguration mount = StorageProviderFactory.createConfig("outboundCreatedMount", LocalFSStorageProvider.class,
					Map.of("mountTarget", root.toString(), StorageSyncService.DIRECTION_KEY, "both"));

				final NodeInterface folder = app.create(StructrTraits.FOLDER,
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "outbound2"),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_WATCH_CONTENTS_PROPERTY), false),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), mount)
				);

				final NodeInterface sub = app.create(StructrTraits.FOLDER,
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "sub"),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder)
				);

				app.create(StructrTraits.FILE,
					new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "created.txt"),
					new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), sub)
				);

				tx.success();

			} catch (FrameworkException fex) {
				fex.printStackTrace();
				fail("Unexpected exception.");
			}

			final Path rootPath = root;
			waitFor("Physical directory and file should have materialized", () -> Files.isDirectory(rootPath.resolve("sub")) && Files.exists(rootPath.resolve("sub/created.txt")));

			// the file node is NOT external - renaming it must still move the physical file
			try (final Tx tx = app.tx()) {

				assertFalse("Node created in Structr must not be external", getNodeAtPath("/outbound2/sub/created.txt").as(File.class).isExternal());

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			setNodeProperty(getNodeAtPath("/outbound2/sub/created.txt"), NodeInterfaceTraitDefinition.NAME_PROPERTY, "renamed.txt");

			waitFor("Physical file of non-external node should follow the rename", () -> Files.exists(rootPath.resolve("sub/renamed.txt")) && !Files.exists(rootPath.resolve("sub/created.txt")));

		} catch (IOException ioex) {

			fail("Unexpected exception.");

		} finally {

			cleanupDirectory(root);
		}
	}

	@Test
	public void testInboundOnlyDirectionKeepsRenameGuard() {

		Path root = null;

		try {

			root = Files.createTempDirectory("structr-inbound-guard-test");

			writeFile(root.resolve("guarded.txt").toFile(), "guarded content");

			// mount WITHOUT a sync.direction entry - defaults to inbound-only
			try (final Tx tx = app.tx()) {

				final StorageConfiguration mount = StorageProviderFactory.createConfig("guardMount", LocalFSStorageProvider.class, Map.of("mountTarget", root.toString()));

				app.create(StructrTraits.FOLDER,
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "guarded"),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_WATCH_CONTENTS_PROPERTY), false),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), mount)
				);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			waitForNodeAtPath(StructrTraits.FILE, "/guarded/guarded.txt");

			// renaming an external node without outbound synchronization must still be rejected
			try (final Tx tx = app.tx()) {

				getNodeAtPath("/guarded/guarded.txt").setProperty(Traits.of(StructrTraits.ABSTRACT_FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "reject.txt");

				tx.success();

				fail("Renaming an external node without outbound sync should be rejected");

			} catch (FrameworkException fex) {

				fail("Unexpected exception type.");

			} catch (UnsupportedOperationException expected) {
			}

			// and the physical file is untouched
			assertTrue("Physical file must be untouched", Files.exists(root.resolve("guarded.txt")));
			assertFalse("No physical file with the rejected name may exist", Files.exists(root.resolve("reject.txt")));

		} catch (IOException ioex) {

			fail("Unexpected exception.");

		} finally {

			cleanupDirectory(root);
		}
	}

	@Test
	public void testOutboundOnlyDirection() {

		Path root = null;

		try {

			root = Files.createTempDirectory("structr-outbound-only-test");

			writeFile(root.resolve("external.txt").toFile(), "never imported");

			// outbound-only: no scans, no watching - even though both are configured
			try (final Tx tx = app.tx()) {

				final StorageConfiguration mount = StorageProviderFactory.createConfig("outboundOnlyMount", LocalFSStorageProvider.class,
					Map.of("mountTarget", root.toString(), StorageSyncService.DIRECTION_KEY, "out"));

				app.create(StructrTraits.FOLDER,
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "outboundOnly"),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_WATCH_CONTENTS_PROPERTY), true),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_SCAN_INTERVAL_PROPERTY), 2),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), mount)
				);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			// the folder must still count as synchronized (it is an outbound target)
			try (final Tx tx = app.tx()) {

				assertTrue("Outbound-only sync root should report isMounted", getNodeAtPath("/outboundOnly").as(Folder.class).isMounted());

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			// wait past scan interval + debounce: the external file must NOT be imported
			try { Thread.sleep(5000); } catch (InterruptedException ignore) {}

			try (final Tx tx = app.tx()) {

				assertNull("External file must not be imported with direction=out", app.nodeQuery(StructrTraits.FILE).name("external.txt").getFirst());

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			// but Structr-side changes propagate out
			try (final Tx tx = app.tx()) {

				app.create(StructrTraits.FOLDER,
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "created"),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.PARENT_PROPERTY), getNodeAtPath("/outboundOnly"))
				);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			final Path rootPath = root;
			waitFor("Physical directory should have been created", () -> Files.isDirectory(rootPath.resolve("created")));

			setNodeProperty(getNodeAtPath("/outboundOnly/created"), NodeInterfaceTraitDefinition.NAME_PROPERTY, "renamed");

			waitFor("Physical directory should have been renamed", () -> Files.isDirectory(rootPath.resolve("renamed")) && !Files.exists(rootPath.resolve("created")));

		} catch (IOException ioex) {

			fail("Unexpected exception.");

		} finally {

			cleanupDirectory(root);
		}
	}

	@Test
	public void testOutboundEchoDoesNotLoop() {

		Path root = null;

		try {

			root = Files.createTempDirectory("structr-echo-test");

			writeFile(root.resolve("echo.txt").toFile(), "echo content");

			// two-way sync WITH live watching: outbound disk changes echo back as watch events
			try (final Tx tx = app.tx()) {

				final StorageConfiguration mount = StorageProviderFactory.createConfig("echoMount", LocalFSStorageProvider.class,
					Map.of("mountTarget", root.toString(), StorageSyncService.DIRECTION_KEY, "both"));

				app.create(StructrTraits.FOLDER,
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "echo"),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_WATCH_CONTENTS_PROPERTY), true),
					new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), mount)
				);

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			waitForNodeAtPath(StructrTraits.FILE, "/echo/echo.txt");

			setNodeProperty(getNodeAtPath("/echo/echo.txt"), NodeInterfaceTraitDefinition.NAME_PROPERTY, "echoRenamed.txt");

			final Path rootPath = root;
			waitFor("Physical file should have been renamed", () -> Files.exists(rootPath.resolve("echoRenamed.txt")) && !Files.exists(rootPath.resolve("echo.txt")));

			// wait past the inbound debounce window so all echo events are processed
			try { Thread.sleep(6000); } catch (InterruptedException ignore) {}

			try (final Tx tx = app.tx()) {

				assertEquals("Exactly one file node should exist after the echo settled", 1, app.nodeQuery(StructrTraits.FILE).getAsList().size());
				assertNotNull("Renamed node should still exist", app.nodeQuery(StructrTraits.FILE).name("echoRenamed.txt").getFirst());
				assertNull("No node with the old name may reappear", app.nodeQuery(StructrTraits.FILE).name("echo.txt").getFirst());

				tx.success();

			} catch (FrameworkException fex) {
				fail("Unexpected exception.");
			}

			assertTrue("Physical file must keep the new name", Files.exists(root.resolve("echoRenamed.txt")));
			assertFalse("Physical file with the old name must not reappear", Files.exists(root.resolve("echo.txt")));

		} catch (IOException ioex) {

			fail("Unexpected exception.");

		} finally {

			cleanupDirectory(root);
		}
	}

	@Test
	public void testUuidKeyedProviderMayIgnoreOutboundEvents() {

		RecordingStorageProvider.reset();

		// no mountTarget: the recording provider always creates a synchronizer
		try (final Tx tx = app.tx()) {

			final StorageConfiguration config = StorageProviderFactory.createConfig("recordingConfig", RecordingStorageProvider.class,
				Map.of(StorageSyncService.DIRECTION_KEY, "both"));

			app.create(StructrTraits.FOLDER,
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "recorded"),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), config)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		String childUuid = null;

		try (final Tx tx = app.tx()) {

			childUuid = app.create(StructrTraits.FOLDER,
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "x"),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.PARENT_PROPERTY), getNodeAtPath("/recorded"))
			).getUuid();

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		setNodeProperty(getNodeAtPath("/recorded/x"), NodeInterfaceTraitDefinition.NAME_PROPERTY, "y");

		try (final Tx tx = app.tx()) {

			app.delete(getNodeAtPath("/recorded/y"));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		waitFor("Three outbound events should have been dispatched", () -> RecordingStorageProvider.RECORDED_EVENTS.size() >= 3);

		final List<VirtualChangeEvent> events = RecordingStorageProvider.RECORDED_EVENTS;

		assertEquals("Unexpected number of outbound events", 3, events.size());

		assertEquals("First event should be the folder creation",  VirtualChangeEvent.Type.CREATED, events.get(0).type());
		assertEquals("Invalid path of creation event",             "x", events.get(0).relativePath());
		assertTrue("Creation event should mark a directory",       events.get(0).directory());
		assertEquals("Invalid node uuid of creation event",        childUuid, events.get(0).nodeUuid());

		assertEquals("Second event should be the rename",          VirtualChangeEvent.Type.MOVED, events.get(1).type());
		assertEquals("Invalid old path of rename event",           "x", events.get(1).previousRelativePath());
		assertEquals("Invalid new path of rename event",           "y", events.get(1).relativePath());

		assertEquals("Third event should be the deletion",         VirtualChangeEvent.Type.DELETED, events.get(2).type());
		assertEquals("Invalid path of deletion event",             "y", events.get(2).previousRelativePath());

		// ignoring the events has no graph side effects
		try (final Tx tx = app.tx()) {

			assertNull("Deleted node must stay deleted", app.getNodeById(StructrTraits.FOLDER, childUuid));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testRolledBackTransactionSendsNoOutboundEvents() {

		// recording provider target with one committed child
		try (final Tx tx = app.tx()) {

			final StorageConfiguration config = StorageProviderFactory.createConfig("rollbackConfig", RecordingStorageProvider.class,
				Map.of(StorageSyncService.DIRECTION_KEY, "both"));

			final NodeInterface folder = app.create(StructrTraits.FOLDER,
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "rollback"),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), config)
			);

			app.create(StructrTraits.FOLDER,
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "child"),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		// the setup transaction's creation event is dispatched asynchronously -
		// wait for it before resetting, so it cannot pollute the assertion below
		waitFor("Setup creation event should have been dispatched", () -> !RecordingStorageProvider.RECORDED_EVENTS.isEmpty());

		RecordingStorageProvider.reset();

		// modify without tx.success(): the transaction rolls back (the node must
		// be fetched within the same transaction - a nested transaction would
		// mark the shared toplevel transaction successful)
		try (final Tx tx = app.tx()) {

			final NodeInterface child = app.nodeQuery(StructrTraits.ABSTRACT_FILE).key(Traits.of(StructrTraits.ABSTRACT_FILE).key(AbstractFileTraitDefinition.PATH_PROPERTY), "/rollback/child").getFirst();

			child.setProperty(Traits.of(StructrTraits.ABSTRACT_FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "discarded");

			// no tx.success()

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try { Thread.sleep(1500); } catch (InterruptedException ignore) {}

		assertTrue("Rolled-back transactions must not produce outbound events", RecordingStorageProvider.RECORDED_EVENTS.isEmpty());

		// a committed rename afterwards still works and produces exactly one event
		setNodeProperty(getNodeAtPath("/rollback/child"), NodeInterfaceTraitDefinition.NAME_PROPERTY, "committed");

		waitFor("Committed rename should produce an outbound event", () -> RecordingStorageProvider.RECORDED_EVENTS.size() == 1);

		final VirtualChangeEvent event = RecordingStorageProvider.RECORDED_EVENTS.get(0);

		assertEquals("Invalid event type",     VirtualChangeEvent.Type.MOVED, event.type());
		assertEquals("Invalid old path",       "child", event.previousRelativePath());
		assertEquals("Invalid new path",       "committed", event.relativePath());
	}

	// ----- private methods -----
	private NodeInterface getNodeAtPath(final String path) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.nodeQuery(StructrTraits.ABSTRACT_FILE).key(Traits.of(StructrTraits.ABSTRACT_FILE).key(AbstractFileTraitDefinition.PATH_PROPERTY), path).getFirst();

			tx.success();

			return node;

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");
			return null;
		}
	}

	private void waitForNodeAtPath(final String type, final String path) {

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

	private void setNodeProperty(final NodeInterface node, final String keyName, final Object value) {

		try (final Tx tx = app.tx()) {

			node.setProperty(Traits.of(StructrTraits.ABSTRACT_FILE).key(keyName), value);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	private void waitFor(final String message, final BooleanSupplier condition) {

		final long deadline = System.currentTimeMillis() + 10000;

		while (System.currentTimeMillis() < deadline) {

			if (condition.getAsBoolean()) {
				return;
			}

			try { Thread.sleep(200); } catch (InterruptedException ignore) {}
		}

		fail(message);
	}

	private void createTestFile(final Path path, final String content) throws IOException {

		try (final FileWriter writer = new FileWriter(path.toFile())) {

			writer.append(content);
			writer.flush();
		}
	}

	private String getContent(final File file) {

		try (final InputStream is = file.getInputStream()) {

			return IOUtils.toString(is, "utf-8");

		} catch (IOException ioex) {
			ioex.printStackTrace();
			fail("Unexpected exception.");
		}

		return null;
	}

	private void writeFile(final java.io.File file, final String content) throws IOException {

		try (final FileOutputStream os = new FileOutputStream(file)) {

			os.write(content.getBytes("utf-8"));
			os.flush();
		}
	}

	private String readFile(final File file) throws IOException {

		try (final InputStream is = file.getInputStream()) {

			final String content = IOUtils.toString(is, "utf-8");
			return content;
		}
	}

	private void cleanupDirectory(final Path root) {

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
