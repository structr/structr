/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.files.nio;

import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.files.ssh.filesystem.StructrFilesystemProvider;
import org.structr.storage.StorageProviderFactory;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.AbstractFile;
import org.structr.web.traits.definitions.FileTraitDefinition;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

/**
 * What actually happens to a file node when the channel that wrote it is closed.
 *
 * The channel a caller receives outlives the transaction that produced it by design: newChannel()
 * commits and closes its own transaction, and VirtualFileChannel updates size, version and checksum from
 * close(), in a transaction of its own. These tests pin that, and then ask what the surrounding
 * transaction does to it, because the inner transaction is thread-local and joins an enclosing one
 * instead of standing alone.
 *
 * The invariant every case is measured against: what the node claims about the file has to match what
 * the storage provider actually holds. A node that says zero bytes over content that exists is worse
 * than a failed write, because nothing reports it.
 */
public class VirtualFileChannelLifetimeTest extends StructrUiTest {

	private static final String CONTENT = "the quick brown fox";

	@Test
	public void testCloseOutsideAnyTransactionUpdatesTheNode() {

		final String id = createFile("outside.txt");

		// the designed path: the writer is done with the transaction long before it is done writing
		writeAndClose(pathFor("/outside.txt"), CONTENT);

		assertNodeMatchesStorage(id, CONTENT.length());
	}

	@Test
	public void testCloseInsideACommittedTransactionUpdatesTheNode() {

		final String id            = createFile("committed.txt");
		final SeekableByteChannel c = open(pathFor("/committed.txt"));

		write(c, CONTENT);

		// close from inside a transaction that commits. The metadata update joins this transaction
		// rather than opening its own, which is invisible as long as the join commits too.
		try (final Tx tx = app.tx()) {

			close(c);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}

		assertNodeMatchesStorage(id, CONTENT.length());
	}

	@Test
	public void testCloseInsideARolledBackTransactionDoesNotRecordTheWrite() {

		final String id             = createFile("rolledback.txt");
		final SeekableByteChannel c = open(pathFor("/rolledback.txt"));

		write(c, CONTENT);

		// the same close, in a transaction that does NOT commit
		try (final Tx tx = app.tx()) {

			close(c);

			// deliberately no tx.success()

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}

		// The agreed contract, and it is worth being explicit that it is a divergence and not a repair:
		// the bytes reached the storage provider and no rollback can reach them, so the file is larger
		// than the node says. What the deferral changes is that the metadata write is now conditional on
		// the caller's work having committed, instead of being made and then silently rolled back.
		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(StructrTraits.FILE, id);

			assertNotNull("file " + id + " not found", node);

			assertEquals("the storage provider keeps what was written, a rollback does not reach it",
				CONTENT.length(), StorageProviderFactory.getStorageProvider(node.as(AbstractFile.class)).size());

			assertEquals("the node must NOT record a write whose transaction was rolled back",
				Long.valueOf(0), node.getProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.SIZE_PROPERTY)));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	// ----- private methods -----
	/**
	 * The one assertion that matters: the node and the storage provider agree.
	 *
	 * Read both inside one transaction, so the comparison cannot be split across a commit.
	 */
	private void assertNodeMatchesStorage(final String id, final long expectedLength) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(StructrTraits.FILE, id);

			assertNotNull("file " + id + " not found", node);

			final long storedSize = StorageProviderFactory.getStorageProvider(node.as(AbstractFile.class)).size();
			final Long nodeSize   = node.getProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.SIZE_PROPERTY));
			final Integer version = node.getProperty(Traits.of(StructrTraits.FILE).key(FileTraitDefinition.VERSION_PROPERTY));

			assertEquals("the storage provider must hold what was written", expectedLength, storedSize);
			assertEquals("the node must report the size the storage provider holds", Long.valueOf(expectedLength), nodeSize);
			assertNotNull("the version must have been set when the channel closed", version);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception: " + fex.getMessage());
		}
	}

	private String createFile(final String name) {

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.create(StructrTraits.FILE,
				new NodeAttribute<>(Traits.of(StructrTraits.NODE_INTERFACE).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name)
			);

			tx.success();

			return node.getUuid();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception while creating " + name);
		}

		return null;
	}

	private Path pathFor(final String path) {

		final StructrFilesystemProvider provider = new StructrFilesystemProvider();
		final FileSystem fs                      = provider.fileSystemFor("", Map.of(StructrFilesystemProvider.SECURITY_CONTEXT_KEY, securityContext));

		return fs.getPath(path);
	}

	private void writeAndClose(final Path path, final String content) {

		final SeekableByteChannel channel = open(path);

		write(channel, content);
		close(channel);
	}

	private SeekableByteChannel open(final Path path) {

		try {

			return Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE);

		} catch (IOException ioex) {

			ioex.printStackTrace();
			fail("Unable to open a channel for " + path + ": " + ioex.getMessage());
		}

		return null;
	}

	private void write(final SeekableByteChannel channel, final String content) {

		try {

			channel.write(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)));

		} catch (IOException ioex) {

			ioex.printStackTrace();
			fail("Unable to write: " + ioex.getMessage());
		}
	}

	private void close(final SeekableByteChannel channel) {

		try {

			channel.close();

		} catch (IOException ioex) {

			ioex.printStackTrace();
			fail("Unable to close the channel: " + ioex.getMessage());
		}
	}
}
