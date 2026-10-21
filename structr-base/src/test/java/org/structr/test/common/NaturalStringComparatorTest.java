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
package org.structr.test.common;

import org.structr.common.NaturalStringComparator;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * The natural order of strings on its own, without a database: numbers by value, text with the
 * collator of a locale, and a total order for strings that are equal in that order.
 */
public class NaturalStringComparatorTest {

	@Test
	public void testUpperAndLowerCaseAreNotSeparated() {

		assertOrder("Upper and lower case of a letter must be next to each other", Locale.ENGLISH, "a", "A", "b", "B", "c", "C");
		assertOrder("Case must not separate words",                                  Locale.ENGLISH, "apple", "Banana", "cherry", "Date");
		assertOrder("Case must not take precedence over a number later in the text",  Locale.ENGLISH, "Item 1", "item 2", "Item 10", "item 20");
		assertOrder("Case decides only between otherwise equal strings",             Locale.ENGLISH, "item 2", "Item 2", "ITEM 2");
	}

	@Test
	public void testNumbersAreComparedByValue() {

		assertOrder("Numbers in text must be compared by value", Locale.ENGLISH, "Item 1", "Item 2", "Item 9", "Item 10", "Item 11", "Item 100");
		assertOrder("Numbers only",                               Locale.ENGLISH, "1", "2", "10", "20", "100");
		assertOrder("Several numbers in one string",              Locale.ENGLISH, "1.2.9", "1.2.10", "1.10.1", "2.0.0");
		assertOrder("Numbers at different positions",             Locale.ENGLISH, "a2b1", "a2b10", "a10b1");
	}

	@Test
	public void testNumbersBeyondLong() {

		assertOrder("Numbers beyond the range of long must still be compared by value", Locale.ENGLISH, "9223372036854775807", "9223372036854775808", "99999999999999999999999");
	}

	@Test
	public void testLeadingZeros() {

		assertOrder("Leading zeros must not change the value", Locale.ENGLISH, "Item 1", "Item 002", "Item 3", "Item 010");

		// equal by value, so the characters decide, which keeps the order total
		assertOrder("Equal numbers with different leading zeros", Locale.ENGLISH, "Item 001", "Item 01", "Item 1");
	}

	@Test
	public void testDigitsComeBeforeLetters() {

		assertOrder("Digits must come before letters at the same position", Locale.ENGLISH, "1a", "2a", "a", "b");
	}

	@Test
	public void testPrefixComesFirst() {

		assertOrder("A prefix must come before the longer string", Locale.ENGLISH, "", "a", "a1", "a1b", "ab");
	}

	@Test
	public void testUmlautsDependOnTheLocale() {

		assertOrder("In German, umlauts belong to their base letter", Locale.GERMAN,   "Apfel", "Äpfel", "Bär", "Zebra");
		assertOrder("In Swedish, ä is a letter of its own after z",   Locale.of("sv"), "Apfel", "Bär", "Zebra", "Äpfel");
		assertOrder("Accents in French belong to their base letter",  Locale.FRENCH,   "cote", "coté", "dé");
	}

	@Test
	public void testEqualStringsAreEqual() {

		final NaturalStringComparator comparator = new NaturalStringComparator(Locale.ENGLISH);

		assertEquals("Identical strings must be equal", 0, comparator.compare("Item 10", "Item 10"));
		assertEquals("Empty strings must be equal",     0, comparator.compare("", ""));
	}

	@Test
	public void testOrderIsConsistent() {

		final NaturalStringComparator comparator = new NaturalStringComparator(Locale.GERMAN);
		final List<String> values                = List.of("a", "A", "ä", "Ä", "b", "1", "01", "10", "a1", "a01", "a10", "Item 2", "item 2", "Item 10", "", "z", "Z");

		for (final String v1 : values) {

			for (final String v2 : values) {

				final int forward  = Integer.signum(comparator.compare(v1, v2));
				final int backward = Integer.signum(comparator.compare(v2, v1));

				assertEquals("Comparing " + v1 + " and " + v2 + " must be antisymmetric", -forward, backward);
				assertEquals("Only identical strings may be equal: " + v1 + ", " + v2, v1.equals(v2), forward == 0);

				for (final String v3 : values) {

					if (forward < 0 && comparator.compare(v2, v3) < 0) {

						assertTrue("The order must be transitive: " + v1 + " < " + v2 + " < " + v3, comparator.compare(v1, v3) < 0);
					}
				}
			}
		}
	}

	@Test
	public void testDefaultLocaleIsUsedWithoutLocale() {

		final NaturalStringComparator comparator = new NaturalStringComparator(null);

		assertTrue("Without a locale, numbers must still be compared by value", comparator.compare("Item 2", "Item 10") < 0);
	}

	// ----- private methods -----
	/**
	 * Sorts the values in several shuffled orders and expects the given order each time.
	 */
	private void assertOrder(final String message, final Locale locale, final String... expected) {

		final NaturalStringComparator comparator = new NaturalStringComparator(locale);
		final List<String> expectedList          = List.of(expected);

		for (int seed = 0; seed < 5; seed++) {

			final List<String> values = new ArrayList<>(expectedList);

			Collections.shuffle(values, new Random(seed));
			values.sort(comparator);

			assertEquals(message, expectedList, values);
		}

		final List<String> reversed = new ArrayList<>(expectedList);

		Collections.reverse(reversed);
		reversed.sort(comparator);

		assertEquals(message + " (from reversed input)", expectedList, reversed);
	}
}
