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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

public class RangesTest {

	@Test
	public void testValidRanges() {

		final List<Integer> list = new LinkedList<>();

		// fill list with numbers
		for (int i=1; i<=10; i++) {
			list.add(i);
		}

		testRange(new RangesIterator<>(list.iterator(),         "1"), 1);
		testRange(new RangesIterator<>(list.iterator(),       "2-5"), 2, 3, 4, 5);
		testRange(new RangesIterator<>(list.iterator(), "1,2,5-7,9"), 1, 2, 5, 6, 7, 9);
		testRange(new RangesIterator<>(list.iterator(),      "1-10"), 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
	}

	@Test
	public void testInvalidRanges() {

		final List<Integer> list = new LinkedList<>();

		// fill list with numbers
		for (int i=1; i<=10; i++) {
			list.add(i);
		}

		testRange(new RangesIterator<>(list.iterator(),         "x"));
		testRange(new RangesIterator<>(list.iterator(), "1,2,5-7,x"), 1, 2, 5, 6, 7);
		testRange(new RangesIterator<>(list.iterator(), "          1        ,       3,5,,,,,,,,7   "), 1, 3, 5, 7);
		testRange(new RangesIterator<>(list.iterator(), ",          1"), 1);

		try {
			testRange(new RangesIterator<>(list.iterator(), "3-1"), 1);
			fail("Wrong range boundary order should throw an exception.");

		} catch (IllegalArgumentException iex) {
			assertEquals("Invalid range specification error message", "Range boundaries must be in ascending order", iex.getMessage());
		}

		try {
			testRange(new RangesIterator<>(list.iterator(),      "x-y"));
			fail("Invalid range boundary should throw an exception.");

		} catch (IllegalArgumentException iex) {
			assertEquals("Invalid range specification error message", "Range must have two boundaries", iex.getMessage());
		}

		try {
			testRange(new RangesIterator<>(list.iterator(),       "2-x"));
			fail("Invalid range boundary should throw an exception.");

		} catch (IllegalArgumentException iex) {
			assertEquals("Invalid range specification error message", "Range must have two boundaries", iex.getMessage());
		}

		try {
			testRange(new RangesIterator<>(list.iterator(), "-1"), 1);
			fail("Negative range boundary should throw an exception.");

		} catch (IllegalArgumentException iex) {
			assertEquals("Invalid range specification error message", "Range must have two boundaries", iex.getMessage());
		}

		try {
			testRange(new RangesIterator<>(list.iterator(), null), 1);
			fail("Null range specification should throw an exception.");

		} catch (IllegalArgumentException iex) {
			assertEquals("Invalid range specification error message", "Range specification must not be empty", iex.getMessage());
		}

		try {
			testRange(new RangesIterator<>(list.iterator(), ""), 1);
			fail("Empty range specification should throw an exception.");

		} catch (IllegalArgumentException iex) {
			assertEquals("Invalid range specification error message", "Range specification must not be empty", iex.getMessage());
		}
	}

	// ----- private methods -----
	private void testRange(final Iterator<Integer> result, final int... numbers) {

		for (int number : numbers) {
			assertEquals("Invalid range iterator result", (Integer)number, result.next());
		}
	}

}
