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
package org.structr.test.cron;

import org.structr.cron.CronEntry;
import org.structr.cron.CronField;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/** The same cases the configuration interface checks in config.js, so both agree on what is a valid expression. */
public class CronEntryTest {

	@Test
	public void testSupportedExpressions() {

		for (final String expression : new String[] { "*/10 * * * * *", "0 */10 * * * *", "0 0 3 * * 1-5", "0 0 0 1,15 * *", "  */5   *  * * * *  ",
			// the examples in the Scheduled Tasks article
			"* * * * * *", "*/10 * * * * *", "0 * * * * *", "0 */5 * * * *", "30 */15 * * * *", "0 0 * * * *", "0 0 */2 * * *", "0 0 8-18 * * 1-5", "0 0 0 * * *", "0 30 6 * * *", "0 0 8 * * 1-5", "0 0 3 * * 0", "0 0 9 * * 1,3,5", "0 0 0 1 * *", "0 0 0 1,15 * *", "0 0 6 1 */3 *", "0 0 0 1 1 *", "0 0 12 1 * 1" }) {

			final CronEntry entry = CronEntry.parse("test", expression);

			assertNotNull("must be accepted: '" + expression + "'", entry);
			assertTrue(entry.getDelayToNextExecutionInMillis() >= 0);
		}
	}

	@Test
	public void testStepsCountFromTheStartOfTheField() {

		assertEquals(List.of(0, 15, 30, 45), matching(new CronField(0, 59, 15), 0, 59));
		assertEquals(List.of(1, 4, 7, 10), matching(new CronField(1, 12, 3), 1, 12));
		assertEquals(List.of(1, 11, 21, 31), matching(new CronField(1, 31, 10), 1, 31));
		assertEquals(List.of(0, 2, 4, 6), matching(new CronField(0, 6, 2), 0, 6));
	}

	@Test
	public void testUnsupportedExpressions() {

		for (final String expression : new String[] { "/10 * * * * *", "/10 * * * *", "0 0 3 * * 7", "0 0 24 * * *", "*/0 * * * * *", "*/60 * * * * *", "0 0 0 0 * *", "0-30/10 * * * * *", "0 0 9 * * MON", "0 0 0 1,32 * *", "0,60 * * * * *" }) {

			assertNull("must be refused: '" + expression + "'", CronEntry.parse("test", expression));
		}
	}

	@Test
	public void testNextExecution() {

		// strictly after the given time
		assertNext("*/10 * * * * *", at(2026, 10, 18, 12, 0, 3), at(2026, 10, 18, 12, 0, 10));
		assertNext("*/10 * * * * *", at(2026, 10, 18, 12, 0, 10), at(2026, 10, 18, 12, 0, 20));

		// quarterly, also across the end of the year
		assertNext("0 0 6 1 */3 *", at(2026, 2, 15, 10, 0, 0), at(2026, 4, 1, 6, 0, 0));
		assertNext("0 0 6 1 */3 *", at(2026, 11, 20, 10, 0, 0), at(2027, 1, 1, 6, 0, 0));
		assertNext("0 30 6 * * *", at(2026, 12, 31, 7, 0, 0), at(2027, 1, 1, 6, 30, 0));

		// months without the day are skipped
		assertNext("0 0 0 31 * *", at(2026, 4, 1, 0, 0, 0), at(2026, 5, 31, 0, 0, 0));
		assertNext("0 0 0 29 2 *", at(2026, 3, 1, 0, 0, 0), at(2028, 2, 29, 0, 0, 0));

		// day of month OR day of week: the next Monday, or the 1st when it comes first
		assertNext("0 0 12 1 * 1", at(2026, 10, 18, 13, 0, 0), at(2026, 10, 19, 12, 0, 0));
		assertNext("0 0 12 1 * 1", at(2026, 10, 30, 13, 0, 0), at(2026, 11, 1, 12, 0, 0));

		// weekdays only, from a Friday over the weekend and the end of daylight saving time
		assertNext("0 0 8 * * 1-5", at(2026, 10, 23, 9, 0, 0), at(2026, 10, 26, 8, 0, 0));
	}

	// ----- private methods -----
	private List<Integer> matching(final CronField field, final int min, final int max) {

		final List<Integer> values = new ArrayList<>();

		for (int i = min; i <= max; i++) {

			if (field.isInside(i)) {

				values.add(i);
			}
		}

		return values;
	}

	private void assertNext(final String expression, final long from, final long expected) {

		final CronEntry entry = CronEntry.parse("test", expression);

		assertNotNull(expression, entry);
		assertEquals(expression + " after " + new Date(from), new Date(expected), new Date(entry.getNextExecutionAfter(from)));
	}

	private long at(final int year, final int month, final int day, final int hour, final int minute, final int second) {

		final Calendar calendar = GregorianCalendar.getInstance();

		calendar.clear();
		calendar.set(year, month - 1, day, hour, minute, second);

		return calendar.getTimeInMillis();
	}
}
