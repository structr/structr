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
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.storage.StorageProvider;
import org.structr.storage.StorageProviderFactory;
import org.structr.storage.providers.local.LocalFSStorageProvider;
import org.structr.storage.providers.s3.GenericS3BucketStorageProvider;
import org.structr.storage.providers.s3.S3ClientCache;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.StorageConfiguration;
import org.structr.web.traits.definitions.AbstractFileTraitDefinition;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Set;

import static org.testng.AssertJUnit.*;

/**
 * Validates the GenericS3BucketStorageProvider against a real S3-compatible
 * endpoint (rustfs test container).
 */
public class S3StorageProviderTest extends StructrUiTest {

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
	public void testReadBeforeWriteReturnsEmpty() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		String fileUuid = null;

		// creating a File in an S3 mount triggers AfterCreation metadata
		// computation, which reads the (not-yet-written) object - this must
		// not fail or log an error, but present the object as empty
		try (final Tx tx = app.tx()) {

			final NodeInterface folder = createS3Folder("s3empty", bucket);
			final NodeInterface file = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "unwritten.txt"),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder)
			);

			fileUuid = file.getUuid();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Creating an S3-backed file without content must not fail.");
		}

		try (final Tx tx = app.tx()) {

			final File file                = app.getNodeById(StructrTraits.FILE, fileUuid).as(File.class);
			final StorageProvider provider = StorageProviderFactory.getStorageProvider(file);

			// object was never written -> present as empty, no error
			assertFalse("Object must not exist before the first write", ((GenericS3BucketStorageProvider)provider).exists());
			assertEquals("Size of an unwritten object must be 0", 0L, provider.size());

			try (final InputStream is = file.getInputStream()) {

				assertNotNull("Reading an unwritten S3 object must not return null", is);
				assertEquals("Reading an unwritten S3 object must yield empty content", "", IOUtils.toString(is, "utf-8"));
			}

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Reading an unwritten S3 object must not fail.");
		}
	}

	@Test
	public void testStreamRoundTrip() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		final String content = "s3 stream content";
		String fileUuid      = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface folder = createS3Folder("s3stream", bucket);
			final NodeInterface file = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "test.txt"),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder)
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

		try (final Tx tx = app.tx()) {

			final NodeInterface file = app.getNodeById(StructrTraits.FILE, fileUuid);

			// content round-trip through the node
			try (final InputStream is = file.as(File.class).getInputStream()) {

				assertEquals("Invalid content after S3 round-trip", content, IOUtils.toString(is, "utf-8"));
			}

			// provider-level assertions
			final StorageProvider provider = StorageProviderFactory.getStorageProvider(file.as(org.structr.web.entity.AbstractFile.class));

			assertEquals("Invalid size reported by S3 provider", content.getBytes("utf-8").length, provider.size());
			assertTrue("Object should exist in the bucket", ((GenericS3BucketStorageProvider)provider).exists());

			// the object key is the node uuid, and the virtual path is stored as user metadata
			final HeadObjectResponse head = RustFsTestSupport.head(bucket, fileUuid);

			assertNotNull("Object should be stored under the node uuid", head);
			assertEquals("Invalid path metadata", "/s3stream/test.txt", head.metadata().get("path"));

			// physical delete through the provider
			provider.delete();

			assertNull("Object should be gone after provider delete", RustFsTestSupport.head(bucket, fileUuid));

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testContentType() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		String fileUuid = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface folder = createS3Folder("s3contenttype", bucket);
			final NodeInterface file = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "typed.txt"),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder)
			);

			fileUuid = file.getUuid();

			final GenericS3BucketStorageProvider provider = (GenericS3BucketStorageProvider)StorageProviderFactory.getStorageProvider(file.as(org.structr.web.entity.AbstractFile.class));

			provider.setContentType("text/plain");

			try (final OutputStream os = provider.getOutputStream()) {

				os.write("typed content".getBytes("utf-8"));
			}

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		final HeadObjectResponse head = RustFsTestSupport.head(bucket, fileUuid);

		assertNotNull("Object should exist", head);
		assertEquals("Invalid content type on S3 object", "text/plain", head.contentType());
	}

	@Test
	public void testChannelWriteFiresUploadCompletion() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		final String content = "channel content";
		String fileUuid      = null;
		Integer versionBefore = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface folder = createS3Folder("s3channel", bucket);
			final NodeInterface file = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "channel.txt"),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder)
			);

			fileUuid      = file.getUuid();
			versionBefore = file.as(File.class).getVersion();

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface file       = app.getNodeById(StructrTraits.FILE, fileUuid);
			final StorageProvider provider = StorageProviderFactory.getStorageProvider(file.as(org.structr.web.entity.AbstractFile.class));

			try (final SeekableByteChannel channel = provider.getSeekableByteChannel()) {

				channel.write(ByteBuffer.wrap(content.getBytes("utf-8")));
			}

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface file = app.getNodeById(StructrTraits.FILE, fileUuid);

			// content arrived
			try (final InputStream is = file.as(File.class).getInputStream()) {

				assertEquals("Invalid content after channel write", content, IOUtils.toString(is, "utf-8"));
			}

			// the fixed channel path stores the same path metadata as the stream path
			final HeadObjectResponse head = RustFsTestSupport.head(bucket, fileUuid);

			assertNotNull("Object should exist", head);
			assertEquals("Channel write should store path metadata", "/s3channel/channel.txt", head.metadata().get("path"));

			// VirtualFileChannel wrapping fired notifyUploadCompletion -> version increment
			final Integer versionAfter = file.as(File.class).getVersion();

			assertNotNull("Version should be set after channel write", versionAfter);
			assertTrue("Channel write should increase the file version", versionBefore == null || versionAfter > versionBefore);

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testBinaryMigrationLocalToS3AndBack() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		final String content = "migrating content";
		String fileUuid      = null;
		String localFolderId = null;
		String s3FolderId    = null;

		try (final Tx tx = app.tx()) {

			final StorageConfiguration localConfig = StorageProviderFactory.createConfig("local-" + bucket, LocalFSStorageProvider.class, Map.of());
			final NodeInterface localFolder = app.create(StructrTraits.FOLDER,
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "localsrc"),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), localConfig)
			);

			final NodeInterface s3Folder = createS3Folder("s3dst", bucket);

			localFolderId = localFolder.getUuid();
			s3FolderId    = s3Folder.getUuid();

			final NodeInterface file = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "migrate.txt"),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), localFolder)
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

		// move to the S3 folder - binary content must follow
		try (final Tx tx = app.tx()) {

			final NodeInterface file     = app.getNodeById(StructrTraits.FILE, fileUuid);
			final NodeInterface s3Folder = app.getNodeById(StructrTraits.FOLDER, s3FolderId);

			file.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), s3Folder);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface file = app.getNodeById(StructrTraits.FILE, fileUuid);

			try (final InputStream is = file.as(File.class).getInputStream()) {

				assertEquals("Invalid content after migration to S3", content, IOUtils.toString(is, "utf-8"));
			}

			assertNotNull("Object should exist in the bucket after migration", RustFsTestSupport.head(bucket, fileUuid));

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		// and back to the local folder - the S3 object must be cleaned up
		try (final Tx tx = app.tx()) {

			final NodeInterface file        = app.getNodeById(StructrTraits.FILE, fileUuid);
			final NodeInterface localFolder = app.getNodeById(StructrTraits.FOLDER, localFolderId);

			file.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), localFolder);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface file = app.getNodeById(StructrTraits.FILE, fileUuid);

			try (final InputStream is = file.as(File.class).getInputStream()) {

				assertEquals("Invalid content after migration back", content, IOUtils.toString(is, "utf-8"));
			}

			assertNull("S3 object should be deleted after migration back", RustFsTestSupport.head(bucket, fileUuid));

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testMoveToMissingBucketFailsAndPreservesSource() {

		// deliberately do NOT create the bucket - the destination upload will 404
		final String bucket  = uniqueBucket();
		final String content = "must survive a failed move";
		String fileUuid      = null;

		try (final Tx tx = app.tx()) {

			final StorageConfiguration localConfig = StorageProviderFactory.createConfig("local-" + bucket, LocalFSStorageProvider.class, Map.of());
			final NodeInterface localFolder = app.create(StructrTraits.FOLDER,
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "localsource"),
				new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), localConfig)
			);

			createS3Folder("s3missing", bucket);

			final NodeInterface file = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "keepme.txt"),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), localFolder)
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

		// moving into the S3 folder whose bucket does not exist must fail the
		// transaction, not silently commit a broken metadata change
		boolean moveRejected = false;

		try (final Tx tx = app.tx()) {

			final NodeInterface file     = app.getNodeById(StructrTraits.FILE, fileUuid);
			final NodeInterface s3Folder = app.nodeQuery(StructrTraits.FOLDER).key(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.PATH_PROPERTY), "/s3missing").getFirst();

			file.setProperty(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), s3Folder);

			tx.success();

		} catch (FrameworkException expected) {

			moveRejected = true;
		}

		assertTrue("Moving a file to a provider with a missing bucket must be rejected", moveRejected);

		// the transaction rolled back: node still under the local folder, content intact
		try (final Tx tx = app.tx()) {

			final NodeInterface file = app.getNodeById(StructrTraits.FILE, fileUuid);

			assertEquals("File must still be under the original local folder", "/localsource/keepme.txt", file.as(File.class).getPath());

			try (final InputStream is = file.as(File.class).getInputStream()) {

				assertEquals("Source content must be preserved after a failed move", content, IOUtils.toString(is, "utf-8"));
			}

			// nothing was written to the (nonexistent) bucket
			assertNull("No S3 object should exist after the failed move", RustFsTestSupport.head(bucket, fileUuid));

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testClientCacheSharing() {

		final String endpoint = RustFsTestSupport.getEndpoint();

		assertSame("Identical connection settings must share one client",
			S3ClientCache.getOrCreate(endpoint, RustFsTestSupport.REGION, RustFsTestSupport.ACCESS_KEY, RustFsTestSupport.SECRET_KEY),
			S3ClientCache.getOrCreate(endpoint, RustFsTestSupport.REGION, RustFsTestSupport.ACCESS_KEY, RustFsTestSupport.SECRET_KEY));

		assertNotSame("Different connection settings must not share a client",
			S3ClientCache.getOrCreate(endpoint, RustFsTestSupport.REGION, RustFsTestSupport.ACCESS_KEY, RustFsTestSupport.SECRET_KEY),
			S3ClientCache.getOrCreate(endpoint, RustFsTestSupport.REGION, RustFsTestSupport.ACCESS_KEY, "other-secret"));

		assertNull("Incomplete settings must not create a client", S3ClientCache.getOrCreate(endpoint, null, RustFsTestSupport.ACCESS_KEY, RustFsTestSupport.SECRET_KEY));
	}

	/**
	 * The upload path the UI actually uses. {@code FileUploadHandler.handleChunk()} seeks to
	 * {@code sequenceNumber * chunkSize} before writing every chunk - chunk 0 included, so even a
	 * single-chunk upload seeks - and keeps one channel open for the whole upload. Nothing in the suite
	 * covered that, which is how a change to position() that reads correctly on its own can break every
	 * upload to an S3 mount and still leave all S3 tests green. This pins the sequence.
	 */
	@Test
	public void testChunkedWriteThroughSeekingChannelKeepsChunkOrder() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		final String[] chunks = { "chunk-00", "chunk-01", "chunk-02" };
		final int chunkSize   = chunks[0].length();
		final String expected = String.join("", chunks);
		final String fileUuid = createS3File("s3chunked", "chunked.txt", bucket);

		try (final Tx tx = app.tx()) {

			final NodeInterface file       = app.getNodeById(StructrTraits.FILE, fileUuid);
			final StorageProvider provider = StorageProviderFactory.getStorageProvider(file.as(AbstractFile.class));

			try (final SeekableByteChannel channel = provider.getSeekableByteChannel()) {

				for (int sequenceNumber = 0; sequenceNumber < chunks.length; sequenceNumber++) {

					channel.position((long) sequenceNumber * chunkSize);
					channel.write(ByteBuffer.wrap(chunks[sequenceNumber].getBytes(StandardCharsets.UTF_8)));

					assertEquals("Channel size must follow the chunks written so far", (long) (sequenceNumber + 1) * chunkSize, channel.size());
				}
			}

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("A chunked upload through a seeking channel must not fail.");
		}

		assertEquals("Chunks must arrive in order and without gaps", expected, new String(RustFsTestSupport.getObject(bucket, fileUuid), StandardCharsets.UTF_8));
	}

	/**
	 * A writable S3 channel appends into a buffer and uploads it in one putObject on close, so the only
	 * position it can honestly be at is the end of what has been written. It used to accept ANY position,
	 * remember it, and then write the next bytes at the end regardless - so a caller seeking backwards to
	 * overwrite got its data appended instead, and nothing said so. Refusing the impossible seek is what
	 * turns that into an error the caller sees.
	 */
	@Test
	public void testSeekAwayFromTheAppendPointIsRejectedWhileWriting() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		final String content  = "HELLO WORLD";
		final String fileUuid = createS3File("s3seekwrite", "seek-write.txt", bucket);

		try (final Tx tx = app.tx()) {

			final NodeInterface file       = app.getNodeById(StructrTraits.FILE, fileUuid);
			final StorageProvider provider = StorageProviderFactory.getStorageProvider(file.as(AbstractFile.class));

			try (final SeekableByteChannel channel = provider.getSeekableByteChannel()) {

				channel.write(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)));

				try {

					channel.position(0);

					fail("Seeking back to 0 after writing must be refused, not silently ignored.");

				} catch (IOException expected) {

					// this is the point of the test: the channel says no instead of misplacing the data
				}

				// the append point itself stays acceptable - that is what the chunked upload relies on
				channel.position(content.length());
			}

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		assertEquals("A refused seek must leave the object exactly as it was written", content, new String(RustFsTestSupport.getObject(bucket, fileUuid), StandardCharsets.UTF_8));
	}

	/**
	 * The read side of the same channel, which was untested as well: a readable channel holds the whole
	 * object, so a seek has to move the read position and size() has to report the object's length.
	 */
	@Test
	public void testSeekWhileReadingReturnsDataFromThatOffset() {

		final String bucket = uniqueBucket();

		RustFsTestSupport.createBucket(bucket);

		final String content  = "HELLO WORLD";
		final String fileUuid = createS3File("s3seekread", "seek-read.txt", bucket);

		try (final Tx tx = app.tx()) {

			final NodeInterface file       = app.getNodeById(StructrTraits.FILE, fileUuid);
			final StorageProvider provider = StorageProviderFactory.getStorageProvider(file.as(AbstractFile.class));

			try (final SeekableByteChannel channel = provider.getSeekableByteChannel()) {

				channel.write(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)));
			}

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			final NodeInterface file       = app.getNodeById(StructrTraits.FILE, fileUuid);
			final StorageProvider provider = StorageProviderFactory.getStorageProvider(file.as(AbstractFile.class));

			try (final SeekableByteChannel channel = provider.getSeekableByteChannel(Set.of(StandardOpenOption.READ))) {

				assertEquals("A readable channel must report the object's length", (long) content.length(), channel.size());

				final ByteBuffer target = ByteBuffer.allocate(5);

				channel.position(6);

				assertEquals("Reading after a seek must return the bytes at that offset", 5, channel.read(target));
				assertEquals("Reading after a seek must return the bytes at that offset", "WORLD", new String(target.array(), StandardCharsets.UTF_8));

				try {

					channel.position(content.length() + 1);

					fail("Seeking past the end of the object must be refused.");

				} catch (IllegalArgumentException expected) {

					// a position beyond the object is not a position
				}
			}

			tx.success();

		} catch (Exception ex) {

			ex.printStackTrace();
			fail("Unexpected exception.");
		}
	}

	// ----- private methods -----
	private String uniqueBucket() {

		return "test-" + System.nanoTime();
	}

	private String createS3File(final String folderName, final String fileName, final String bucket) {

		try (final Tx tx = app.tx()) {

			final NodeInterface folder = createS3Folder(folderName, bucket);
			final NodeInterface file   = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), fileName),
				new NodeAttribute<>(Traits.of(StructrTraits.FILE).key(AbstractFileTraitDefinition.PARENT_PROPERTY), folder)
			);

			final String uuid = file.getUuid();

			tx.success();

			return uuid;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unable to create an S3-backed file.");

			return null;
		}
	}

	private NodeInterface createS3Folder(final String name, final String bucket) throws FrameworkException {

		final StorageConfiguration config = StorageProviderFactory.createConfig("s3-" + name + "-" + bucket, GenericS3BucketStorageProvider.class, RustFsTestSupport.providerConfig(bucket));

		return app.create(StructrTraits.FOLDER,
			new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
			new NodeAttribute<>(Traits.of(StructrTraits.FOLDER).key(AbstractFileTraitDefinition.STORAGE_CONFIGURATION_PROPERTY), config)
		);
	}
}
