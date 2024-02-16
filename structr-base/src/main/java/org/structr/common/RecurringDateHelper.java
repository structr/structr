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
package org.structr.common;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Utility class for recurring dates.
 */
public class RecurringDateHelper {

	private static final Logger logger = LoggerFactory.getLogger(RecurringDateHelper.class.getName());

	private enum ShortWeekday {

		Mo, Di, Mi, Do, Fr, Sa, So
	}

	public static class Appointment {

		public Date startDate;
		public Date endDate;

		public Appointment(final Date startDate, final Date endDate) {

			this.startDate = startDate;
			this.endDate   = endDate;

		}

		@Override
		public String toString() {
			return this.startDate + " - " + this.endDate;
		}

	}

	public static List<Appointment> generateAppointments(final Date startDate, final Date endDate, final String weekdays, final String startTimeString, final String endTimeString) {


		List<Appointment> appointments = new ArrayList();
		//check if a Date is empty
		if(	startDate == null || startDate.getTime() == 0 ||
			endDate == null || endDate.getTime() == 0 ||
			weekdays == null || weekdays.equals("") ||
			startTimeString == null || startTimeString.equals("") ||
			endTimeString == null || endTimeString.equals(""))
			return appointments;

		String[] wd      = StringUtils.split(weekdays, ",");
		Date start       = dateFromDateAndTimeString(startDate, wd[0], startTimeString);
		Calendar cal     = GregorianCalendar.getInstance();

		while (start.before(startDate) && start.before(endDate)) {

			cal.setTime(start);
			cal.add(Calendar.DAY_OF_WEEK, 1);

			start = cal.getTime();

		}

		while (start.before(endDate)) {

			String shortWeekday = getShortWeekday(cal.get(Calendar.DAY_OF_WEEK));
			if (ArrayUtils.contains(wd, shortWeekday)) {

				Date end = dateFromDateAndTimeString(start, shortWeekday, endTimeString);

				appointments.add(new Appointment(start, end));

			}

			cal.setTime(start);
			cal.add(Calendar.DAY_OF_WEEK, 1);

			start = cal.getTime();

		}

		return appointments;

	}

	private static Date dateFromDateAndTimeString(final Date date, final String shortWeekday, final String timeString) {

		String[] hourMinute = StringUtils.split(timeString, ":");
		Calendar cal        = GregorianCalendar.getInstance();

		cal.setTime(date);

		cal.set(Calendar.DAY_OF_WEEK, getDayOfWeek(shortWeekday));

		try {
			cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hourMinute[0]));
			cal.set(Calendar.MINUTE, Integer.parseInt(hourMinute[1]));

		} catch (Throwable t) {

			logger.warn("Unable to parse time from string {}", timeString);
		}

		cal.set(Calendar.SECOND, 0);

		return cal.getTime();

	}

	private static int getDayOfWeek(final String shortWeekday) {

		if (shortWeekday != null && !shortWeekday.equals(""))
			try {
				ShortWeekday wd = ShortWeekday.valueOf(shortWeekday);

				switch (wd) {

					case Mo :
						return Calendar.MONDAY;

					case Di :
						return Calendar.TUESDAY;

					case Mi :
						return Calendar.WEDNESDAY;

					case Do :
						return Calendar.THURSDAY;

					case Fr :
						return Calendar.FRIDAY;

					case Sa :
						return Calendar.SATURDAY;

					case So :
						return Calendar.SUNDAY;

				}

			} catch (Throwable t) {

				logger.warn("Unable to parse day of week for string {}", shortWeekday);
			}

		return 0;

	}

	private static String getShortWeekday(final int wd) {

		switch (wd) {

			case Calendar.MONDAY :
				return ShortWeekday.Mo.name();

			case Calendar.TUESDAY :
				return ShortWeekday.Di.name();

			case Calendar.WEDNESDAY :
				return ShortWeekday.Mi.name();

			case Calendar.THURSDAY :
				return ShortWeekday.Do.name();

			case Calendar.FRIDAY :
				return ShortWeekday.Fr.name();

			case Calendar.SATURDAY :
				return ShortWeekday.Sa.name();

			case Calendar.SUNDAY :
				return ShortWeekday.So.name();

		}

		return "";

	}

}
