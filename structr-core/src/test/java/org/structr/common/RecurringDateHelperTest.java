package org.structr.common;

import java.util.Date;
import java.util.logging.Logger;
import java.util.Calendar;
import static junit.framework.Assert.assertEquals;

/**
 *
 * @author alex
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