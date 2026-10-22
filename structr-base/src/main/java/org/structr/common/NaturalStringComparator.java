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
package org.structr.common;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
 * Compares strings the way a reader expects them to be ordered: runs of digits by their numeric
 * value, so "Item 2" comes before "Item 10", and the text in between with the collator of the given
 * locale, so upper and lower case letters are not separated and umlauts are placed according to
 * the language.
 *
 * Like a collator, the comparison has several levels: base letters and numbers first, then accents,
 * then case, so "Item 2" comes before "item 10" although "i" comes before "I". Strings that are
 * equal on all levels but not identical, like "01" and "1", are ordered by their characters, so the
 * order is total.
 *
 * Not thread-safe, because collators are not.
 */
public class NaturalStringComparator implements Comparator<String> {

	private final Collator[] levels;

	public NaturalStringComparator(final Locale locale) {

		final Locale effectiveLocale = locale != null ? locale : Locale.getDefault();

		this.levels = new Collator[] {
			collator(effectiveLocale, Collator.PRIMARY),
			collator(effectiveLocale, Collator.SECONDARY),
			collator(effectiveLocale, Collator.TERTIARY)
		};
	}

	@Override
	public int compare(final String s1, final String s2) {

		for (final Collator collator : levels) {

			final int result = compare(s1, s2, collator);
			if (result != 0) {

				return result;
			}
		}

		return s1.compareTo(s2);
	}

	// ----- private methods -----
	private int compare(final String s1, final String s2, final Collator collator) {

		int pos1 = 0;
		int pos2 = 0;

		while (pos1 < s1.length() && pos2 < s2.length()) {

			final boolean digits1 = isDigit(s1.charAt(pos1));
			final boolean digits2 = isDigit(s2.charAt(pos2));
			final int end1        = endOfRun(s1, pos1, digits1);
			final int end2        = endOfRun(s2, pos2, digits2);
			final String run1     = s1.substring(pos1, end1);
			final String run2     = s2.substring(pos2, end2);

			final int result = (digits1 && digits2) ? compareNumbers(run1, run2) : collator.compare(run1, run2);
			if (result != 0) {

				return result;
			}

			pos1 = end1;
			pos2 = end2;
		}

		// one string is a prefix of the other in this order, the shorter one comes first
		return Integer.compare(s1.length() - pos1, s2.length() - pos2);
	}

	private Collator collator(final Locale locale, final int strength) {

		final Collator collator = Collator.getInstance(locale);

		collator.setStrength(strength);

		return collator;
	}

	private int compareNumbers(final String run1, final String run2) {

		final String number1 = stripLeadingZeros(run1);
		final String number2 = stripLeadingZeros(run2);

		// without leading zeros, the longer run is the larger number, which also works for numbers beyond long
		final int length = Integer.compare(number1.length(), number2.length());
		if (length != 0) {

			return length;
		}

		return number1.compareTo(number2);
	}

	private String stripLeadingZeros(final String run) {

		int pos = 0;

		while (pos < run.length() - 1 && run.charAt(pos) == '0') {

			pos++;
		}

		return run.substring(pos);
	}

	private int endOfRun(final String value, final int start, final boolean digits) {

		int pos = start;

		while (pos < value.length() && isDigit(value.charAt(pos)) == digits) {

			pos++;
		}

		return pos;
	}

	// only ASCII digits, other scripts' digits are text for the collator
	private boolean isDigit(final char c) {

		return c >= '0' && c <= '9';
	}
}
