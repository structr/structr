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

import com.sun.management.UnixOperatingSystemMXBean;
import org.structr.storage.providers.local.LocalFSStorageSynchronizer;
import org.structr.storage.sync.ExternalEntry;
import org.structr.storage.sync.SyncTarget;
import org.structr.storage.sync.VirtualChangeEvent;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.FileSystemLoopException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import static org.testng.AssertJUnit.*;

/**
 * Unit tests for the local file system synchronizer on its own, without a running service: how its
 * enumeration ends when the tree cannot be walked, and that outbound changes stay inside the mount.
 */
public class LocalFSStorageSynchronizerTest {

	private static final String UUID = "0123456789abcdef0123456789abcdef";

	private Path base;
	private Path root;

	@BeforeMethod
	public void createTree() throws IOException {

		base = Files.createTempDirectory("structr-localfs-synchronizer-test");
		root = Files.createDirectory(base.resolve("mount"));

		Files.createDirectory(root.resolve("a"));
		Files.writeString(root.resolve("a/x.txt"), "content");
	}

	@AfterMethod
	public void deleteTree() throws IOException {

		try (final Stream<Path> paths = Files.walk(base)) {

			// symbolic links are deleted, not followed
			paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
		}
	}

	@Test
	public void testSymlinkLoopFailsTheEnumeration() throws IOException {

		Files.createSymbolicLink(root.resolve("a/loop"), root);

		try {

			drain(synchronizer().enumerate(null));
			fail("A tree that loops back on itself cannot be walked completely, so the enumeration must fail");

		} catch (UncheckedIOException expected) {

			assertTrue("The enumeration should fail because of the loop", expected.getCause() instanceof FileSystemLoopException);
		}
	}

	@Test
	public void testFailedEnumerationReleasesItsFileHandles() throws IOException {

		final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		if (!(os instanceof UnixOperatingSystemMXBean unix)) {

			throw new SkipException("Open file descriptors can only be counted on Unix");
		}

		Files.createSymbolicLink(root.resolve("a/loop"), root);

		final LocalFSStorageSynchronizer synchronizer = synchronizer();
		final long before                             = unix.getOpenFileDescriptorCount();

		for (int i = 0; i < 100; i++) {

			try {

				drain(synchronizer.enumerate(null));

			} catch (UncheckedIOException expected) {}
		}

		assertTrue("A failed enumeration must not leave the directories it walked open", unix.getOpenFileDescriptorCount() - before < 50);
	}

	@Test
	public void testOutboundChangesStayInsideTheMount() throws IOException {

		final LocalFSStorageSynchronizer synchronizer = synchronizer();

		Files.writeString(base.resolve("outside.txt"), "not part of the mount");

		synchronizer.onVirtualChange(VirtualChangeEvent.created(UUID, true, "../escaped", null));
		synchronizer.onVirtualChange(VirtualChangeEvent.moved(UUID, true, "a", "../../moved", null));
		synchronizer.onVirtualChange(VirtualChangeEvent.deleted(UUID, false, "../outside.txt", null));

		assertFalse("A created directory must not escape the mount", Files.exists(base.resolve("escaped")));
		assertFalse("A move must not escape the mount",               Files.exists(base.getParent().resolve("moved")));
		assertTrue("A move that would escape must leave the source",  Files.isDirectory(root.resolve("a")));
		assertTrue("A delete must not reach outside the mount",       Files.exists(base.resolve("outside.txt")));
	}

	private LocalFSStorageSynchronizer synchronizer() {
		return new LocalFSStorageSynchronizer(new SyncTarget(UUID, "/mount", true, null, Map.of()), root);
	}

	private static void drain(final Iterator<ExternalEntry> it) {

		while (it.hasNext()) {

			it.next();
		}
	}
}
