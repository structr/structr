/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 */
public class IterablesTest {

	@Test
	public void testFilterIterable() {

		final List<Integer> source       = Arrays.asList(new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
		final Iterable<Integer> filtered = Iterables.filter(i -> { return (i % 2) == 0; }, source);
		final Iterator<Integer> iterator = filtered.iterator();

		// test that hasNext does not change the iterable
		for (int i=0; i<10; i++) {
			assertTrue("Iterator#hasNext should not modify position in Iterable", iterator.hasNext());
		}

		final List<Integer> result = Iterables.toList(filtered);

		assertEquals("Invalid filtering result", Integer.valueOf(0), result.get(0));
		assertEquals("Invalid filtering result", Integer.valueOf(2), result.get(1));
		assertEquals("Invalid filtering result", Integer.valueOf(4), result.get(2));
		assertEquals("Invalid filtering result", Integer.valueOf(6), result.get(3));
		assertEquals("Invalid filtering result", Integer.valueOf(8), result.get(4));
	}

	@Test
	public void testMapIterable() {

		final List<Integer> source       = Arrays.asList(new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
		final Iterable<Integer> mapped   = Iterables.map(i -> { return i * 2; }, source);
		final Iterator<Integer> iterator = mapped.iterator();
		int count                        = 0;

		// test that hasNext does not change the iterable
		for (int i=0; i<10; i++) {
			assertTrue("Iterator#hasNext should not modify position in Iterable", iterator.hasNext());
		}

		for (final Integer i : mapped) {

			final Integer expected = source.get(count++) * 2;
			assertEquals("Invalid mapping result", expected, i);
		}
	}
}
