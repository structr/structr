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
import org.structr.storage.sync.VanishPolicy;
import org.structr.storage.providers.s3.GenericS3BucketStorageProvider;
import org.structr.storage.providers.s3.S3ClientCache;
import org.structr.storage.providers.s3.S3StorageSynchronizer;
import org.structr.storage.sync.SyncTarget;
import org.structr.test.files.SyncTestBase;
import org.structr.web.entity.AbstractFile;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.testng.AssertJUnit.*;

/**
 * Tests for the S3 storage synchronizer: inbound bucket scanning and
 * outbound propagation of committed virtual filesystem changes, against a
 * rustfs test container.
 */
public class S3StorageSyncTest extends SyncTestBase {

	@Parameters("testDatabaseConnection")
	@BeforeClass(alwaysRun = true)
	@Override
	public void setup(@Optional final String testDatabaseConnection) {

		RustFsTestSupport.start();

		super.setup(testDatabaseConnection);
	}

	@AfterClass(alwaysRun = true)
	@Override
	public void teardown() throws Exception {

		super.teardown();

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

		mountBucket("s3mounted", bucket, "in", VanishPolicy.DELETE);

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
	public void testExternalObjectImport() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		// an externally created object with an arbitrary (non-uuid) key and NO
		// path metadata: the key itself becomes the initial virtual path
		final String content = "external report";

		RustFsTestSupport.putObject(bucket, "docs/report.pdf", content.getBytes(), Map.of());

		// a uuid-keyed object without path metadata stays indistinguishable from
		// a Structr-origin object with a lost node and is still ignored
		final String uuid = NodeServiceCommand.getNextUuid();

		RustFsTestSupport.putObject(bucket, uuid, "orphan".getBytes(), Map.of());

		mountBucket("s3ext", bucket, "in", null);

		waitForNodeAtPath(StructrTraits.FILE, "/s3ext/docs/report.pdf");

		try (final Tx tx = app.tx()) {

			final NodeInterface file = getNodeAtPath("/s3ext/docs/report.pdf");

			assertTrue("Imported external node should be external", file.as(File.class).isExternal());

			// the native object key is persisted so the provider can address it
			assertEquals("Imported node should persist the object key as storageKey", "docs/report.pdf", file.as(AbstractFile.class).getStorageKey());

			// intermediate folder was synthesized from the key's path segments
			assertNotNull("Intermediate folder should have been created", getNodeAtPath("/s3ext/docs"));

			// content is readable through the node (provider reads by storageKey)
			try (final InputStream is = file.as(File.class).getInputStream()) {

				assertEquals("Invalid content of imported external node", content, IOUtils.toString(is, "utf-8"));
			}

			// the uuid-keyed orphan without a node or path is not imported
			assertNull("Uuid-keyed object without a node must not be imported", app.getNodeById(StructrTraits.FILE, uuid));

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testDeletePolicyPruning() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		final String uuid = NodeServiceCommand.getNextUuid();

		RustFsTestSupport.putObject(bucket, uuid, "to be pruned".getBytes(), Map.of("path", "/s3pruned/pruneme.txt"));

		mountBucket("s3pruned", bucket, "in", VanishPolicy.DELETE);

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

		mountBucket("s3outdelete", bucket, "both", null);

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

		mountBucket("s3outrename", bucket, "both", null);

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

		mountBucket("s3rollback", bucket, "both", null);

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

		awaitIdle();

		final HeadObjectResponse head = RustFsTestSupport.head(bucket, fileUuid);

		assertNotNull("Object should still exist", head);
		assertEquals("Rolled-back renames must not touch the path metadata", "/s3rollback/stable.txt", head.metadata().get("path"));
	}

	@Test
	public void testExternalObjectChangeDetection() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		RustFsTestSupport.putObject(bucket, "docs/report.pdf", "short".getBytes(), Map.of());

		mountBucket("s3extchange", bucket, "in", null);

		waitForNodeAtPath(StructrTraits.FILE, "/s3extchange/docs/report.pdf");

		// overwrite the object out-of-band with content of a different length;
		// only a completed rescan can pick up the new size
		final String longer = "a considerably longer body than before";

		RustFsTestSupport.putObject(bucket, "docs/report.pdf", longer.getBytes(), Map.of());

		waitFor("Node size should follow the external overwrite", () -> {

			try (final Tx tx = app.tx()) {

				final NodeInterface node = getNodeAtPath("/s3extchange/docs/report.pdf");
				final boolean refreshed  = node != null && Long.valueOf(longer.getBytes().length).equals(node.as(File.class).getSize());

				tx.success();

				return refreshed;

			} catch (FrameworkException fex) {

				return false;
			}
		});
	}

	@Test
	public void testExternalObjectPruning() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		RustFsTestSupport.putObject(bucket, "docs/report.pdf", "prune me".getBytes(), Map.of());

		mountBucket("s3extprune", bucket, "in", VanishPolicy.DELETE);

		waitForNodeAtPath(StructrTraits.FILE, "/s3extprune/docs/report.pdf");

		RustFsTestSupport.deleteObject(bucket, "docs/report.pdf");

		waitFor("Node of vanished external object should be pruned", () -> getNodeAtPath("/s3extprune/docs/report.pdf") == null);
	}

	@Test
	public void testOutboundDeleteRemovesExternalObject() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		RustFsTestSupport.putObject(bucket, "docs/report.pdf", "delete me".getBytes(), Map.of());

		mountBucket("s3extdelete", bucket, "both", null);

		waitForNodeAtPath(StructrTraits.FILE, "/s3extdelete/docs/report.pdf");

		try (final Tx tx = app.tx()) {

			app.delete(getNodeAtPath("/s3extdelete/docs/report.pdf"));

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");
		}

		// the object must be deleted at its native key, not at the node uuid
		waitFor("External object should be deleted at its native key", () -> RustFsTestSupport.head(bucket, "docs/report.pdf") == null);
	}

	@Test
	public void testOutboundRenameKeepsExternalKey() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		RustFsTestSupport.putObject(bucket, "docs/report.pdf", "renamed content".getBytes(), Map.of());

		mountBucket("s3extrename", bucket, "both", null);

		waitForNodeAtPath(StructrTraits.FILE, "/s3extrename/docs/report.pdf");

		setNodeProperty(getNodeAtPath("/s3extrename/docs/report.pdf"), NodeInterfaceTraitDefinition.NAME_PROPERTY, "renamed.pdf");

		// the object key stays stable; only the "path" metadata is refreshed
		waitFor("Path metadata should follow the rename at the stable native key", () -> {

			final HeadObjectResponse head = RustFsTestSupport.head(bucket, "docs/report.pdf");

			return head != null && "/s3extrename/docs/renamed.pdf".equals(head.metadata().get("path"));
		});

		// no move-back after further complete scans, and the node lives at the new path only
		awaitScanAfter("s3extrename", System.currentTimeMillis());
		awaitScanAfter("s3extrename", System.currentTimeMillis());

		assertNotNull("Object must remain at its original native key", RustFsTestSupport.head(bucket, "docs/report.pdf"));
		assertNotNull("Node should exist at the renamed path", getNodeAtPath("/s3extrename/docs/renamed.pdf"));
		assertNull("No node should remain at the old path", getNodeAtPath("/s3extrename/docs/report.pdf"));
	}

	@Test
	public void testExternalObjectPathConflictSkipped() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		mountBucket("s3conflict", bucket, "in", null);

		// a Structr-origin file materializes an object at its uuid key
		final String content = "structr origin";
		String originUuid    = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface file = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "report.pdf"),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), getNodeAtPath("/s3conflict"))
			);

			originUuid = file.getUuid();

			try (final OutputStream os = file.as(File.class).getOutputStream()) {

				os.write(content.getBytes("utf-8"));
			}

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		// an external object whose key maps to the very same virtual path
		RustFsTestSupport.putObject(bucket, "report.pdf", "foreign".getBytes(), Map.of());

		// two complete scans that started after the foreign object appeared
		awaitScanAfter("s3conflict", System.currentTimeMillis());
		awaitScanAfter("s3conflict", System.currentTimeMillis());

		final String uuid = originUuid;

		try (final Tx tx = app.tx()) {

			final NodeInterface node = getNodeAtPath("/s3conflict/report.pdf");

			assertNotNull("The original Structr node should still exist", node);
			assertEquals("The colliding external object must not hijack the existing node", uuid, node.getUuid());
			assertNull("The existing node must not be bound to the foreign object key", node.as(AbstractFile.class).getStorageKey());

			// content is unchanged (still the Structr-origin object at the uuid key)
			try (final InputStream is = node.as(File.class).getInputStream()) {

				assertEquals("Existing node content must be untouched", content, IOUtils.toString(is, "utf-8"));
			}

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	// ----- private methods -----
	@Test
	public void testListingAMissingBucketOrAnUnreachableEndpointFailsInsteadOfListingNothing() {

		// an empty listing would read as "everything vanished" and be reconciled accordingly
		final SyncTarget target = new SyncTarget(NodeServiceCommand.getNextUuid(), "/s3failing", null, Map.of());

		final S3StorageSynchronizer missingBucket = new S3StorageSynchronizer(target, S3ClientCache.getOrCreate(RustFsTestSupport.getEndpoint(), RustFsTestSupport.REGION, RustFsTestSupport.ACCESS_KEY, RustFsTestSupport.SECRET_KEY), uniqueBucket());
		final S3StorageSynchronizer unreachable   = new S3StorageSynchronizer(target, S3ClientCache.getOrCreate("http://localhost:9", RustFsTestSupport.REGION, RustFsTestSupport.ACCESS_KEY, RustFsTestSupport.SECRET_KEY), uniqueBucket());

		for (final S3StorageSynchronizer synchronizer : List.of(missingBucket, unreachable)) {

			try {

				synchronizer.enumerate(null).hasNext();
				fail("A listing that could not be read must fail");

			} catch (UncheckedIOException expected) {

			} catch (IOException ioex) {

				fail("Unexpected exception.");
			}
		}
	}

	@Test
	public void testFolderPlaceholderKeysAreSkipped() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		// keys with empty or dot segments are skipped as well, but this backend refuses to store them
		RustFsTestSupport.putObject(bucket, "placeholder/", new byte[0],            Map.of());
		RustFsTestSupport.putObject(bucket, "good/ok.txt",  "imported".getBytes(), Map.of());

		mountBucket("s3keys", bucket, "in", null);

		waitForNodeAtPath(StructrTraits.FILE, "/s3keys/good/ok.txt");
		awaitScanAfter("s3keys", 0L);

		assertFalse("A folder placeholder marker is not a file", existsByName("placeholder"));
	}

	@Test
	public void testReadonlyPolicyMarksAVanishedObject() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		RustFsTestSupport.putObject(bucket, "keep.txt",   "kept".getBytes(),   Map.of());
		RustFsTestSupport.putObject(bucket, "vanish.txt", "vanish".getBytes(), Map.of());

		mountBucket("s3readonly", bucket, "in", VanishPolicy.READONLY);

		waitForNodeAtPath(StructrTraits.FILE, "/s3readonly/vanish.txt");

		RustFsTestSupport.deleteObject(bucket, "vanish.txt");

		waitFor("The node of a vanished object should be marked", () -> isExternallyDeleted("vanish.txt"));

		assertNotNull("A marked node should be kept", getNodeAtPath("/s3readonly/vanish.txt"));
		assertFalse("An object that is still there should not be marked", isExternallyDeleted("keep.txt"));
	}

	@Test
	public void testIgnorePolicyKeepsAVanishedObjectUnmarked() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		RustFsTestSupport.putObject(bucket, "vanish.txt", "vanish".getBytes(), Map.of());

		mountBucket("s3ignore", bucket, "in", VanishPolicy.IGNORE);

		waitForNodeAtPath(StructrTraits.FILE, "/s3ignore/vanish.txt");

		RustFsTestSupport.deleteObject(bucket, "vanish.txt");

		final long deleted = System.currentTimeMillis();

		awaitScanAfter("s3ignore", deleted);

		assertNotNull("Under the ignore policy a vanished object's node should be kept", getNodeAtPath("/s3ignore/vanish.txt"));
		assertFalse("Under the ignore policy a vanished object's node should not be marked", isExternallyDeleted("vanish.txt"));
	}

	@Test
	public void testListingBeyondOnePageIsComplete() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		// one listing page holds 1000 keys
		for (int i = 0; i < 1005; i++) {

			RustFsTestSupport.putObject(bucket, String.format("f%04d.txt", i), new byte[] { 1 }, Map.of());
		}

		// a single scan at attach time, so that the assertions see exactly one listing of the bucket
		mountBucket("s3pages", bucket, "in", VanishPolicy.DELETE, null);

		awaitScanAfter("s3pages", 0L);

		assertNotNull("An object on the first page should be imported",  getNodeAtPath("/s3pages/f0000.txt"));
		assertNotNull("An object on the first page should be imported",  getNodeAtPath("/s3pages/f0999.txt"));
		assertNotNull("An object on the second page should be imported", getNodeAtPath("/s3pages/f1004.txt"));
	}

	private boolean existsByName(final String name) {

		try (final Tx tx = app.tx()) {

			final boolean exists = app.nodeQuery(StructrTraits.ABSTRACT_FILE).name(name).getFirst() != null;

			tx.success();

			return exists;

		} catch (FrameworkException fex) {

			fail("Unexpected exception.");

			return false;
		}
	}

	private String uniqueBucket() {

		return "test-" + System.nanoTime();
	}

	/**
	 * Creates a StorageConfiguration for the given bucket and a folder using
	 * it, with a short rescan interval for inbound directions.
	 */
	private void mountBucket(final String folderName, final String bucket, final String direction, final VanishPolicy vanishPolicy) {
		mountBucket(folderName, bucket, direction, vanishPolicy, 2);
	}

	private void mountBucket(final String folderName, final String bucket, final String direction, final VanishPolicy vanishPolicy, final Integer scanInterval) {

		try (final Tx tx = app.tx()) {

			final Map<String, String> entries = new HashMap<>(RustFsTestSupport.providerConfig(bucket));

			entries.put(StorageSyncService.DIRECTION_KEY, direction);

			if (vanishPolicy != null) {

				entries.put(VanishPolicy.POLICY_KEY, vanishPolicy.value());
			}

			mountFolder(folderName, StorageProviderFactory.createConfig("s3-" + folderName + "-" + bucket, GenericS3BucketStorageProvider.class, entries), false, scanInterval);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

}
