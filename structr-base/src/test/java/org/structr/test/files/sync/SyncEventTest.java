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

import org.structr.storage.sync.ExternalChangeEvent;
import org.structr.storage.sync.ExternalEntry;
import org.structr.storage.sync.VirtualChangeEvent;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

/**
 * Unit tests for the shapes the inbound and outbound change events refuse.
 */
public class SyncEventTest {

	private static final String UUID = "0123456789abcdef0123456789abcdef";

	@Test
	public void testExternalChangeEventNeedsATypeAndAnEntry() {

		assertRefused(() -> new ExternalChangeEvent(null, ExternalEntry.file("a.txt", 1L, 1L), null));
		assertRefused(() -> new ExternalChangeEvent(ExternalChangeEvent.Type.CREATED, null, null));
	}

	@Test
	public void testExternalMoveNeedsWhereItCameFrom() {

		assertRefused(() -> new ExternalChangeEvent(ExternalChangeEvent.Type.MOVED, ExternalEntry.file("b.txt", 1L, 1L), null));

		assertEquals("a.txt", ExternalChangeEvent.moved("a.txt", ExternalEntry.file("b.txt", 1L, 1L)).previousRelativePath());
	}

	@Test
	public void testVirtualChangeEventPathsMatchItsType() {

		assertRefused(() -> new VirtualChangeEvent(null, UUID, false, null, "a.txt", null));
		assertRefused(() -> new VirtualChangeEvent(VirtualChangeEvent.Type.CREATED, null, false, null, "a.txt", null));

		assertRefused(() -> new VirtualChangeEvent(VirtualChangeEvent.Type.CREATED, UUID, false, null, null, null));
		assertRefused(() -> new VirtualChangeEvent(VirtualChangeEvent.Type.CREATED, UUID, false, "a.txt", "b.txt", null));

		assertRefused(() -> new VirtualChangeEvent(VirtualChangeEvent.Type.MOVED, UUID, false, null, "b.txt", null));
		assertRefused(() -> new VirtualChangeEvent(VirtualChangeEvent.Type.MOVED, UUID, false, "a.txt", null, null));

		assertRefused(() -> new VirtualChangeEvent(VirtualChangeEvent.Type.DELETED, UUID, false, null, null, null));
		assertRefused(() -> new VirtualChangeEvent(VirtualChangeEvent.Type.DELETED, UUID, false, "a.txt", "b.txt", null));

		assertEquals("b.txt", VirtualChangeEvent.created(UUID, false, "b.txt", null).relativePath());
		assertEquals("a.txt", VirtualChangeEvent.moved(UUID, false, "a.txt", "b.txt", null).previousRelativePath());
		assertEquals("a.txt", VirtualChangeEvent.deleted(UUID, false, "a.txt", null).previousRelativePath());
	}

	private static void assertRefused(final Runnable construction) {

		try {

			construction.run();
			fail("The event should have been refused");

		} catch (IllegalArgumentException expected) {}
	}
}
