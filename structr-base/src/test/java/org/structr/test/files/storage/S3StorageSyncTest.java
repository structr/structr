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
package org.structr.test.files.storage;

import org.apache.commons.io.IOUtils;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.NodeServiceCommand;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.files.sync.StorageSyncService;
import org.structr.storage.StorageProviderFactory;
import org.structr.storage.providers.s3.GenericS3BucketStorageProvider;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.File;
import org.structr.web.entity.StorageConfiguration;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.structr.web.traits.definitions.FolderTraitDefinition;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.testng.AssertJUnit.*;

/**
 * Tests for the S3 storage synchronizer: inbound bucket scanning and
 * outbound propagation of committed virtual filesystem changes, against a
 * rustfs test container.
 */
public class S3StorageSyncTest extends StructrUiTest {

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	@Override
	public void setup(@Optional final String testDatabaseConnection) {

		RustFsTestSupport.start();

		Settings.Services.setValue("NodeService SchemaService HttpService StorageSyncService");

		super.setup(testDatabaseConnection);
	}

	@AfterClass(alwaysRun = true)
	@Override
	public void stop() throws Exception {

		super.stop();

		RustFsTestSupport.stop();
	}

	@Test
	public void testInboundImportByUuidKey() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		// out-of-band object with a valid node uuid as key and the virtual
		// path stored as user metadata - put BEFORE mounting so the initial
		// scan imports it
		final String uuid    = NodeServiceCommand.getNextUuid();
		final String content = "imported from the bucket";

		RustFsTestSupport.putObject(bucket, uuid, content.getBytes(), Map.of("path", "/s3mounted/sub/external.txt"));

		mountBucket("s3mounted", bucket, "in", true);

		waitForNodeAtPath(StructrTraits.FILE, "/s3mounted/sub/external.txt");

		try (final Tx tx = app.tx()) {

			final NodeInterface file = getNodeAtPath("/s3mounted/sub/external.txt");

			// node is bound to the object's key
			assertEquals("Imported node should carry the object's uuid key", uuid, file.getUuid());
			assertTrue("Imported node should be external", file.as(File.class).isExternal());

			// intermediate folder was synthesized from the path metadata
			assertNotNull("Intermediate folder should have been created", getNodeAtPath("/s3mounted/sub"));

			// content is readable through the node (provider reads by uuid key)
			try (final InputStream is = file.as(File.class).getInputStream()) {

				assertEquals("Invalid content of imported S3 node", content, IOUtils.toString(is, "utf-8"));
			}

			tx.success();

		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testNonUuidKeySkipped() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		// a foreign object with a non-uuid key and a valid sibling in the same scan
		final String uuid = NodeServiceCommand.getNextUuid();

		RustFsTestSupport.putObject(bucket, "not-a-uuid.txt", "foreign".getBytes(), Map.of("path", "/s3skipped/foreign.txt"));
		RustFsTestSupport.putObject(bucket, uuid, "valid".getBytes(), Map.of("path", "/s3skipped/valid.txt"));

		mountBucket("s3skipped", bucket, "in", false);

		// the valid sibling imports - proving the scan completed and skipped, not aborted
		waitForNodeAtPath(StructrTraits.FILE, "/s3skipped/valid.txt");

		try (final Tx tx = app.tx()) {

			assertNull("Objects with non-uuid keys must not be imported", app.nodeQuery(StructrTraits.FILE).name("foreign.txt").getFirst());
			assertNull("Objects with non-uuid keys must not be imported", app.nodeQuery(StructrTraits.FILE).name("not-a-uuid.txt").getFirst());

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testDeleteStalePruning() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		final String uuid = NodeServiceCommand.getNextUuid();

		RustFsTestSupport.putObject(bucket, uuid, "to be pruned".getBytes(), Map.of("path", "/s3pruned/pruneme.txt"));

		mountBucket("s3pruned", bucket, "in", true);

		waitForNodeAtPath(StructrTraits.FILE, "/s3pruned/pruneme.txt");

		// remove the object out-of-band: only a completed rescan can detect this
		RustFsTestSupport.deleteObject(bucket, uuid);

		waitFor("Node of vanished object should be pruned", () -> {

			try (final Tx tx = app.tx()) {

				final boolean gone = app.getNodeById(StructrTraits.FILE, uuid) == null;

				tx.success();

				return gone;

			} catch (FrameworkException fex) {
				return false;
			}
		});
	}

	@Test
	public void testOutboundDeleteRemovesObject() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		mountBucket("s3outdelete", bucket, "both", false);

		final String content = "delete me";
		String fileUuid      = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface file = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "doomed.txt"),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), getNodeAtPath("/s3outdelete"))
			);

			fileUuid = file.getUuid();

			try (final OutputStream os = file.as(File.class).getOutputStream()) {

				os.write(content.getBytes("utf-8"));
			}

			tx.success();

		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		assertNotNull("Object should exist after write", RustFsTestSupport.head(bucket, fileUuid));

		// deleting the node must delete the object - the direct provider
		// delete is skipped for outbound-governed nodes, the synchronizer's
		// DELETED handling is the only mechanism
		try (final Tx tx = app.tx()) {

			app.delete(getNodeAtPath("/s3outdelete/doomed.txt"));

			tx.success();

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		final String uuid = fileUuid;
		waitFor("Object should be deleted after virtual deletion", () -> RustFsTestSupport.head(bucket, uuid) == null);
	}

	@Test
	public void testOutboundRenameRefreshesPathMetadata() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		mountBucket("s3outrename", bucket, "both", false);

		final String content = "renamed content";
		String fileUuid      = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface file = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "original.txt"),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), getNodeAtPath("/s3outrename"))
			);

			fileUuid = file.getUuid();

			try (final OutputStream os = file.as(File.class).getOutputStream()) {

				os.write(content.getBytes("utf-8"));
			}

			tx.success();

		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		setNodeProperty(getNodeAtPath("/s3outrename/original.txt"), NodeInterfaceTraitDefinition.NAME_PROPERTY, "renamed.txt");

		// the synchronizer refreshes the object's path metadata via server-side self-copy
		final String uuid = fileUuid;
		waitFor("Path metadata should follow the rename", () -> {

			final HeadObjectResponse head = RustFsTestSupport.head(bucket, uuid);

			return head != null && "/s3outrename/renamed.txt".equals(head.metadata().get("path"));
		});

		// content survived the metadata copy
		assertEquals("Content should be intact after metadata refresh", content, new String(RustFsTestSupport.getObject(bucket, fileUuid)));
	}

	@Test
	public void testRolledBackRenameSendsNothing() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		mountBucket("s3rollback", bucket, "both", false);

		String fileUuid = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface file = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "stable.txt"),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), getNodeAtPath("/s3rollback"))
			);

			fileUuid = file.getUuid();

			try (final OutputStream os = file.as(File.class).getOutputStream()) {

				os.write("stable".getBytes("utf-8"));
			}

			tx.success();

		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		// modify without tx.success(): the transaction rolls back (the node
		// must be fetched within the SAME transaction - a nested transaction
		// would mark the shared toplevel transaction successful)
		try (final Tx tx = app.tx()) {

			final NodeInterface file = app.nodeQuery(StructrTraits.ABSTRACT_FILE).key(Traits.of(StructrTraits.ABSTRACT_FILE).key(AbstractFileTraitDefinition.PATH_PROPERTY), "/s3rollback/stable.txt").getFirst();

			file.setProperty(Traits.of(StructrTraits.ABSTRACT_FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "discarded.txt");

			// no tx.success()

		} catch (FrameworkException fex) {
			fail("Unexpected exception.");
		}

		try { Thread.sleep(1500); } catch (InterruptedException ignore) {}

		final HeadObjectResponse head = RustFsTestSupport.head(bucket, fileUuid);

		assertNotNull("Object should still exist", head);
		assertEquals("Rolled-back renames must not touch the path metadata", "/s3rollback/stable.txt", head.metadata().get("path"));
	}

	// ----- private methods -----
	private String uniqueBucket() {
		return "test-" + System.nanoTime();
	}

	/**
	 * Creates a StorageConfiguration for the given bucket and a folder using
	 * it, with a short rescan interval for inbound directions.
	 */
	private void mountBucket(final String folderName, final String bucket, final String direction, final boolean deleteStale) {

		try (final Tx tx = app.tx()) {

			final Map<String, String> entries = new HashMap<>(RustFsTestSupport.providerConfig(bucket));

			entries.put(StorageSyncService.DIRECTION_KEY, direction);

			if (deleteStale) {
				entries.put(StorageSyncService.DELETE_STALE_KEY, "true");
			}

			final StorageConfiguration config = StorageProviderFactory.createConfig("s3-" + folderName + "-" + bucket, GenericS3BucketStorageProvider.class, entries);

			app.create(StructrTraits.FOLDER,
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), folderName),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(FolderTraitDefinition.MOUNT_SCAN_INTERVAL_PROPERTY), 2),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), config)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

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
