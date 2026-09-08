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

import org.structr.files.ssh.filesystem.StructrFilesystemProvider;
import org.testng.annotations.Test;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * The registration layer of the virtual filesystem, the part that makes it reachable through the URI.
 *
 * None of this needs a database: a filesystem for the superuser is an object, and what is under test is
 * the addressing, not the files. The operations themselves are covered by the SSH and FTP tests, which
 * drive the same provider through the other entry point.
 */
public class StructrFilesystemProviderRegistrationTest {

	@Test
	public void testTheProviderIsFoundByServiceLoader() {

		// the whole point of the META-INF/services entry: a caller with only a URI in hand can reach the
		// virtual filesystem without ever naming a Structr class
		for (final FileSystemProvider provider : FileSystemProvider.installedProviders()) {

			if (StructrFilesystemProvider.SCHEME.equals(provider.getScheme())) {

				return;
			}
		}

		fail("no installed provider answers to the '" + StructrFilesystemProvider.SCHEME + "' scheme");
	}

	@Test
	public void testTheSuperuserFilesystemIsAddressableWithoutAnAuthority() {

		final StructrFilesystemProvider provider = new StructrFilesystemProvider();
		final FileSystem fs                      = provider.fileSystemFor("", null);

		assertNotNull(fs);
		assertSame("a filesystem must report the provider that created it", provider, fs.provider());

		// asking twice must not build a second one, or two Paths for the same file would differ
		assertSame(fs, provider.fileSystemFor("", null));
	}

	@Test
	public void testNewFileSystemRefusesToReplaceAnExistingOne() throws Exception {

		final StructrFilesystemProvider provider = new StructrFilesystemProvider();
		final URI uri                            = URI.create(StructrFilesystemProvider.SCHEME + ":///");

		assertNotNull(provider.newFileSystem(uri, null));

		try {

			provider.newFileSystem(uri, null);
			fail("a second newFileSystem for the same user must be refused");

		} catch (FileSystemAlreadyExistsException expected) {
		}
	}

	@Test
	public void testGetFileSystemDoesNotCreateOneOnDemand() {

		final StructrFilesystemProvider provider = new StructrFilesystemProvider();

		try {

			provider.getFileSystem(URI.create(StructrFilesystemProvider.SCHEME + ":///"));
			fail("getFileSystem must not create a filesystem that was never opened");

		} catch (FileSystemNotFoundException expected) {
		}
	}

	@Test
	public void testGetPathCreatesTheFilesystemAndKeepsTheWholePath() {

		final StructrFilesystemProvider provider = new StructrFilesystemProvider();
		final Path path                          = provider.getPath(URI.create(StructrFilesystemProvider.SCHEME + ":///dir/file.txt"));

		assertNotNull(path);
		assertTrue("the path must survive the URI, was: " + path, path.toString().endsWith("dir/file.txt"));
	}

	@Test
	public void testAForeignSchemeIsRejected() {

		final StructrFilesystemProvider provider = new StructrFilesystemProvider();

		try {

			provider.getPath(URI.create("file:///tmp/x"));
			fail("a URI of another scheme must be rejected rather than silently served");

		} catch (IllegalArgumentException expected) {
		}
	}

	@Test
	public void testARelativeUriIsRejected() {

		final StructrFilesystemProvider provider = new StructrFilesystemProvider();

		try {

			provider.getPath(URI.create(StructrFilesystemProvider.SCHEME + ":dir/file.txt"));
			fail("a URI without an absolute path must be rejected: there is no working directory to resolve it against");

		} catch (IllegalArgumentException expected) {
		}
	}

	@Test
	public void testTheSchemeIsWhatTheProviderAnswersTo() {

		assertEquals(StructrFilesystemProvider.SCHEME, new StructrFilesystemProvider().getScheme());
	}
}
