/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.Date;
import java.util.logging.Logger;
import java.util.Calendar;


/**
 *
 *
 */
public class RecurringDateHelperTest extends StructrTest {
	
	private static final Logger logger = Logger.getLogger(DeleteGraphObjectsTest.class.getName());

	@Override
	public void test00DbAvailable() {
		super.test00DbAvailable();
	}

	public void test01AppointmentsSize(){
		Calendar cal = Calendar.getInstance();
		cal.set(2013, 07, 01);
		Date startDate = new Date(cal.getTimeInMillis());
		cal.set(2013, 07,02);
		Date endDate = new Date(cal.getTimeInMillis());
		String weekdays = "Mo,Di,Mi,Do,Fr,Sa,So";
		String startTimeString = "14:00";
		String endTimeString = "17:00";

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
}