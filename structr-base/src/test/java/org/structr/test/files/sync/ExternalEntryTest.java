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

import org.structr.storage.sync.ExternalEntry;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

/**
 * Unit tests for how an external entry normalizes and splits its path, and what it refuses.
 */
public class ExternalEntryTest {

	@Test
	public void testPathsAreNormalizedToRelativeForwardSlashPaths() {

		assertEquals("a/b/c.txt", ExternalEntry.file("/a/b/c.txt", 1L, 1L).relativePath());
		assertEquals("a/b/c.txt", ExternalEntry.file("a\\b\\c.txt", 1L, 1L).relativePath());
		assertEquals("a/b",       ExternalEntry.directory("//a/b//", 1L).relativePath());
		assertEquals("",          ExternalEntry.directory("/", 1L).relativePath());
	}

	@Test
	public void testNameAndParentPath() {

		final ExternalEntry nested   = ExternalEntry.file("a/b/c.txt", 1L, 1L);
		final ExternalEntry topLevel = ExternalEntry.file("c.txt", 1L, 1L);
		final ExternalEntry byUuid   = ExternalEntry.byUuid("0123456789abcdef0123456789abcdef", false, 1L, 1L);

		assertEquals("c.txt", nested.name());
		assertEquals("a/b",   nested.parentPath());
		assertEquals("c.txt", topLevel.name());
		assertEquals("",      topLevel.parentPath());
		assertNull(byUuid.name());
		assertNull(byUuid.parentPath());
		assertFalse(byUuid.hasPath());
		assertTrue(byUuid.hasUuid());
	}

	@Test
	public void testAnEntryNeedsAPathOrAUuid() {

		try {

			new ExternalEntry(null, null, false, 1L, 1L, null, false);
			fail("An entry without a path or a uuid should be refused");

		} catch (IllegalArgumentException expected) {}
	}

	@Test
	public void testBindingANativeKeyNeedsOne() {

		try {

			ExternalEntry.externalFile(null, "a.txt", 1L, 1L);
			fail("An entry bound to a native key should need one");

		} catch (IllegalArgumentException expected) {}

		assertEquals("key-1", ExternalEntry.externalFile("key-1", "a.txt", 1L, 1L).withNativeKey("key-1").nativeKey());
	}
}
