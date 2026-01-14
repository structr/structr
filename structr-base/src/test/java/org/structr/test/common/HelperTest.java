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

import org.structr.common.helper.CaseHelper;
import org.structr.common.helper.RecurringDateHelper;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Date;

import static org.testng.AssertJUnit.assertEquals;

/**
 *
 */
public class HelperTest {

	@Test
	public void test01AppointmentsSize(){

		final Calendar cal = Calendar.getInstance();
		cal.set(2013, 07, 01);

		final Date startDate = new Date(cal.getTimeInMillis());
		cal.set(2013, 07,02);

		final Date endDate = new Date(cal.getTimeInMillis());

		final String weekdays        = "Mo,Di,Mi,Do,Fr,Sa,So";
		final String startTimeString = "14:00";
		final String endTimeString   = "17:00";

		int expResult = 0;

		int result1 = RecurringDateHelper.generateAppointments(null, null, null, null, null).size();
		assertEquals("result1: Appointments not empty",expResult, result1);

		int result2 = RecurringDateHelper.generateAppointments(null, null, "", "", "").size();
		assertEquals("result2: Appointments not empty",expResult, result2);

		int result3 = RecurringDateHelper.generateAppointments(startDate, endDate, weekdays, startTimeString, "").size();
		assertEquals("result3: Appointments not empty",expResult, result3);

		int result4 = RecurringDateHelper.generateAppointments(startDate, endDate, weekdays, "", endTimeString).size();
		assertEquals("result4: Appointments not empty",expResult, result4);

		int result5 = RecurringDateHelper.generateAppointments(startDate, endDate, "", startTimeString, endTimeString).size();
		assertEquals("result5: Appointments not empty",expResult, result5);
	}

	@Test
	public void testCaseConversion() {

		assertEquals("CheckIns", CaseHelper.toUpperCamelCase("check_ins"));
		assertEquals("check_ins", CaseHelper.toUnderscore("check_ins", true));
		assertEquals("check_ins", CaseHelper.toUnderscore("check_ins", false));

		assertEquals("CheckIns", CaseHelper.toUpperCamelCase("CheckIns"));
		assertEquals("check_ins", CaseHelper.toUnderscore("CheckIns", true));
		assertEquals("check_ins", CaseHelper.toUnderscore("CheckIns", false));

		assertEquals("CheckIn", CaseHelper.toUpperCamelCase("CheckIn"));
		assertEquals("check_ins", CaseHelper.toUnderscore("CheckIn", true));
		assertEquals("check_in", CaseHelper.toUnderscore("CheckIn", false));

		assertEquals("BlogEntry", CaseHelper.toUpperCamelCase("blog_entry"));
		assertEquals("blog_entries", CaseHelper.toUnderscore("blog_entry", true));
		assertEquals("blog_entry", CaseHelper.toUnderscore("blog_entry", false));

		assertEquals("BlogEntry", CaseHelper.toUpperCamelCase("BlogEntry"));
		assertEquals("blog_entries", CaseHelper.toUnderscore("BlogEntry", true));
		assertEquals("blog_entry", CaseHelper.toUnderscore("BlogEntry", false));

		assertEquals("BlogEntries", CaseHelper.toUpperCamelCase("blog_entries"));
		assertEquals("blog_entries", CaseHelper.toUnderscore("blog_entries", true));
		assertEquals("blog_entries", CaseHelper.toUnderscore("blog_entries", false));

		assertEquals("BlogEntries", CaseHelper.toUpperCamelCase("BlogEntries"));
		assertEquals("blog_entries", CaseHelper.toUnderscore("BlogEntries", true));
		assertEquals("blog_entries", CaseHelper.toUnderscore("BlogEntries", false));

		assertEquals("Blogentries", CaseHelper.toUpperCamelCase("blogentries"));
		assertEquals("blogentries", CaseHelper.toUnderscore("blogentries", true));
		assertEquals("blogentries", CaseHelper.toUnderscore("blogentries", false));

		assertEquals("Blogentries", CaseHelper.toUpperCamelCase("Blogentries"));
		assertEquals("blogentries", CaseHelper.toUnderscore("Blogentries", true));
		assertEquals("blogentries", CaseHelper.toUnderscore("Blogentries", false));
	}
}
