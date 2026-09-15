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

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * The name algebra of a StructrPath, measured against the platform's own.
 *
 * Every case here asserts the same thing twice: once on a structr: path and once on the real filesystem's
 * path for the same string. The platform is the specification — relativize and startsWith are easy to
 * write plausibly and get subtly wrong, and a test that only asserted what this implementation happens to
 * do would pin the mistake instead of the rule.
 *
 * No database is involved: this is string algebra over path names, and none of it touches a file.
 */
public class StructrPathAlgebraTest {

	@Test
	public void testNameCountAndElements() {

		bothAgree(p -> String.valueOf(p.getNameCount()), "/a/b/c");
		bothAgree(p -> p.getName(0).toString(),          "/a/b/c");
		bothAgree(p -> p.getName(2).toString(),          "/a/b/c");
		bothAgree(p -> p.getFileName().toString(),       "/a/b/c");
	}

	@Test
	public void testSubpath() {

		bothAgree(p -> p.subpath(0, 2).toString(), "/a/b/c");
		bothAgree(p -> p.subpath(1, 3).toString(), "/a/b/c");
		bothAgree(p -> p.subpath(2, 3).toString(), "/a/b/c");
	}

	@Test
	public void testStartsWithAndEndsWith() {

		bothAgree(p -> String.valueOf(p.startsWith("/a")),     "/a/b/c");
		bothAgree(p -> String.valueOf(p.startsWith("/a/b")),   "/a/b/c");
		bothAgree(p -> String.valueOf(p.startsWith("/b")),     "/a/b/c");
		// a prefix of a name, not a name: /ab is not a prefix of /a/b/c
		bothAgree(p -> String.valueOf(p.startsWith("/ab")),    "/a/b/c");
		bothAgree(p -> String.valueOf(p.endsWith("c")),        "/a/b/c");
		bothAgree(p -> String.valueOf(p.endsWith("b/c")),      "/a/b/c");
		bothAgree(p -> String.valueOf(p.endsWith("/a/b/c")),   "/a/b/c");
		// an absolute path only ends with an absolute path when they are equal
		bothAgree(p -> String.valueOf(p.endsWith("/b/c")),     "/a/b/c");
	}

	@Test
	public void testRelativize() {

		bothAgree2((a, b) -> a.relativize(b).toString(), "/a/b", "/a/b/c/d");
		bothAgree2((a, b) -> a.relativize(b).toString(), "/a/b/c/d", "/a/b");
		bothAgree2((a, b) -> a.relativize(b).toString(), "/a/b/c", "/a/x/y");
		// two equal paths relativize to the empty path
		bothAgree2((a, b) -> a.relativize(b).toString(), "/a/b", "/a/b");
	}

	@Test
	public void testResolveAndResolveSibling() {

		bothAgree2((a, b) -> a.resolve(b).toString(),        "/a/b", "c/d");
		bothAgree2((a, b) -> a.resolve(b).toString(),        "/a/b", "/x/y");
		bothAgree2((a, b) -> a.resolveSibling(b).toString(), "/a/b", "z");
	}

	@Test
	public void testNormalize() {

		bothAgree(p -> p.normalize().toString(), "/a/b/../c");
		bothAgree(p -> p.normalize().toString(), "/a/./b");
		bothAgree(p -> p.normalize().toString(), "/a/b/../..");
		// above the root is still the root
		bothAgree(p -> p.normalize().toString(), "/a/../..");
	}

	@Test
	public void testIteratorYieldsSingleNameElements() {

		final StringBuilder structr = new StringBuilder();
		final StringBuilder unix    = new StringBuilder();

		for (final Path p : structrPath("/a/b/c")) {

			structr.append("[").append(p).append("]");
		}

		for (final Path p : Paths.get("/a/b/c")) {

			unix.append("[").append(p).append("]");
		}

		assertEquals("iterator must yield the name elements, not the ancestor paths", unix.toString(), structr.toString());
	}

	@Test
	public void testEqualityAndOrdering() {

		assertEquals("two paths naming the same file must be equal", structrPath("/a/b"), structrPath("/a/b"));
		assertEquals("equal paths must have equal hash codes", structrPath("/a/b").hashCode(), structrPath("/a/b").hashCode());
		assertFalse("different paths must not be equal", structrPath("/a/b").equals(structrPath("/a/c")));

		assertEquals("ordering must follow the platform's", Integer.signum(Paths.get("/a/b").compareTo(Paths.get("/a/c"))), Integer.signum(structrPath("/a/b").compareTo(structrPath("/a/c"))));

		try {

			structrPath("/a/b").compareTo(Paths.get("/a/b"));
			fail("comparing across providers must throw ClassCastException, not return an ordering");

		} catch (ClassCastException expected) {
		}
	}

	@Test
	public void testRootAndAbsoluteness() {

		assertTrue("an absolute path must say so", structrPath("/a/b").isAbsolute());
		assertEquals("an absolute path must report the filesystem root", "/", structrPath("/a/b").getRoot().toString());
		assertNull("a relative path has no root", structrPath("/a/b").relativize(structrPath("/a/b/c")).getRoot());
	}

	@Test
	public void testUriNamesTheSchemeAndRoundTrips() {

		final Path path = structrPath("/a/b");

		assertEquals("structr", path.toUri().getScheme());
		assertEquals("/a/b", path.toUri().getPath());

		// the whole point of the URI: it comes back as the same path
		assertEquals(path, Paths.get(path.toUri()));
	}

	// ----- private methods -----
	private interface OneArg  { String apply(final Path p); }
	private interface TwoArgs { String apply(final Path a, final Path b); }

	/** Runs the same expression on both filesystems and requires the same answer. */
	private void bothAgree(final OneArg op, final String path) {

		assertEquals("differs from the platform for " + path, op.apply(Paths.get(path)), op.apply(structrPath(path)));
	}

	private void bothAgree2(final TwoArgs op, final String a, final String b) {

		assertEquals("differs from the platform for " + a + " and " + b, op.apply(Paths.get(a), Paths.get(b)), op.apply(structrPath(a), structrPath(b)));
	}

	/**
	 * A path of the installed provider, not of a freshly built one.
	 *
	 * There is exactly one provider instance in a running VM, and paths of two different filesystems are
	 * legitimately unequal. Constructing a provider per call here would make that difference show up as
	 * a failure of equals() and of the URI round-trip, neither of which is what is under test.
	 */
	private Path structrPath(final String path) {

		for (final FileSystemProvider provider : FileSystemProvider.installedProviders()) {

			if (StructrFilesystemProvider.SCHEME.equals(provider.getScheme())) {

				return ((StructrFilesystemProvider)provider).fileSystemFor("", null).getPath(path);
			}
		}

		fail("the structr provider is not installed");

		return null;
	}
}
