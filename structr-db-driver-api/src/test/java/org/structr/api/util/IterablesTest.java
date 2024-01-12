/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 *
 */
public class IterablesTest {

	@Test
	public void testFilterIterable() {

		final List<Integer> source       = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9 );
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

		final List<Integer> source       = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9 );
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

	@Test
	public void testFlatteningIterable() {

		final Iterable<Integer> source1 = Arrays.asList(   1,  2,    3,  4,    5,  6,  7,  8);
		final Iterable<Integer> source2 = Arrays.asList(null, null);
		final Iterable<Integer> source3 = Arrays.asList(null, 10, null, 12, null, 14, 15, 16);
		final Iterable<Integer> source4 = Arrays.asList(  17, 18,   19, 20,   21, 22, 23, 24);

		final List<Iterable<Integer>> sources = new LinkedList<>();

		sources.add(Iterables.map(e -> { return e; }, source1));
		sources.add(Iterables.filter(e -> { return true; }, source2));
		sources.add(source3);
		sources.add(source4);

		final Iterable<Integer> flat = Iterables.flatten(sources);
		final List<Integer> result   = Iterables.toList(flat);

		assertEquals(21, result.size());

		assertEquals(Integer.valueOf( 1), result.get( 0));
		assertEquals(Integer.valueOf( 2), result.get( 1));
		assertEquals(Integer.valueOf( 3), result.get( 2));
		assertEquals(Integer.valueOf( 4), result.get( 3));
		assertEquals(Integer.valueOf( 5), result.get( 4));
		assertEquals(Integer.valueOf( 6), result.get( 5));
		assertEquals(Integer.valueOf( 7), result.get( 6));
		assertEquals(Integer.valueOf( 8), result.get( 7));
		assertEquals(Integer.valueOf(10), result.get( 8));
		assertEquals(Integer.valueOf(12), result.get( 9));
		assertEquals(Integer.valueOf(14), result.get(10));
		assertEquals(Integer.valueOf(15), result.get(11));
		assertEquals(Integer.valueOf(16), result.get(12));
		assertEquals(Integer.valueOf(17), result.get(13));
		assertEquals(Integer.valueOf(18), result.get(14));
		assertEquals(Integer.valueOf(19), result.get(15));
		assertEquals(Integer.valueOf(20), result.get(16));
		assertEquals(Integer.valueOf(21), result.get(17));
		assertEquals(Integer.valueOf(22), result.get(18));
		assertEquals(Integer.valueOf(23), result.get(19));
		assertEquals(Integer.valueOf(24), result.get(20));
	}
}
