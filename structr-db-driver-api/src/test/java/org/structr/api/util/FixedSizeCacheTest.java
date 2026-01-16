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
package org.structr.api.util;

import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;

/**
 *
 */
public class FixedSizeCacheTest {

	public FixedSizeCacheTest() {
	}

	@Test
	public void testFixedSizeCache() {

		final FixedSizeCache<Long, Long> test = new FixedSizeCache<>("Test cache", 10);

		for (int i=0; i<100; i++) {

			final Long value = Long.valueOf(i);
			test.put(value, value);
		}

		assertEquals("Invalid FixedSizeCache size", 10, test.size());

		// check that only the eldest entries are kept
		for (int i=0; i<90; i++) {

			final Long value = Long.valueOf(i);
			assertNull("Invalid FixedSizeCache contents", test.get(value));
		}

		// check that only the eldest entries are kept
		for (int i=90; i<100; i++) {

			final Long value = Long.valueOf(i);
			assertEquals("Invalid FixedSizeCache contents", test.get(value), value);
		}

	}
}
