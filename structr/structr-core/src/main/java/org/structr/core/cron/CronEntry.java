/*
 *  Copyright (C) 2011 Axel Morgner
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.core.cron;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Christian Morgner
 */
public class CronEntry implements Delayed {

	private static final Logger logger = Logger.getLogger(CronService.class.getName());

	//~--- fields ---------------------------------------------------------

	private CronField days    = null;
	private CronField dow     = null;
	private CronField hours   = null;
	private CronField minutes = null;
	private CronField months  = null;
	private String name       = null;
	private CronField seconds = null;

	//~--- constructors ---------------------------------------------------

	private CronEntry(String name) {
		this.name = name;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public String toString() {

		StringBuilder buf = new StringBuilder();

		buf.append(seconds.toString());
		buf.append(" ");
		buf.append(minutes.toString());
		buf.append(" ");
		buf.append(hours.toString());
		buf.append(" ");
		buf.append(days.toString());
		buf.append(" ");
		buf.append(dow.toString());
		buf.append(" ");
		buf.append(months.toString());
		buf.append(" ");

		return buf.toString();
	}

	// ----- static methods -----
	public static CronEntry parse(String task, String expression) {

		String[] fields = expression.split("[ \\t]+");

		if (fields.length == CronService.NUM_FIELDS) {

			CronEntry cronEntry = new CronEntry(task);
			String secondsField = fields[0];
			String minutesField = fields[1];
			String hoursField   = fields[2];
			String daysField    = fields[3];
			String weeksField   = fields[4];
			String monthsField  = fields[5];

			try {

				CronField seconds = parseField(secondsField, 0, 59);

				cronEntry.setSeconds(seconds);

			} catch (Throwable t) {
				logger.log(Level.WARNING, "Invalid cron expression for task {0}, field 'seconds': {1}", new Object[] { task, t.getMessage() });
			}

			try {

				CronField minutes = parseField(minutesField, 0, 59);

				cronEntry.setMinutes(minutes);

			} catch (Throwable t) {
				logger.log(Level.WARNING, "Invalid cron expression for task {0}, field 'minutes': {1}", new Object[] { task, t.getMessage() });
			}

			try {

				CronField hours = parseField(hoursField, 0, 23);

				cronEntry.setHours(hours);

			} catch (Throwable t) {
				logger.log(Level.WARNING, "Invalid cron expression for task {0}, field 'hours': {1}", new Object[] { task, t.getMessage() });
			}

			try {

				CronField days = parseField(daysField, 0, 31);

				cronEntry.setDays(days);

			} catch (Throwable t) {
				logger.log(Level.WARNING, "Invalid cron expression for task {0}, field 'days': {1}", new Object[] { task, t.getMessage() });
			}

			try {

				CronField weeks = parseField(weeksField, 0, 6);

				cronEntry.setWeeks(weeks);

			} catch (Throwable t) {
				logger.log(Level.WARNING, "Invalid cron expression for task {0}, field 'weeks': {1}", new Object[] { task, t.getMessage() });
			}

			try {

				CronField months = parseField(monthsField, 0, 11);

				cronEntry.setMonths(months);

			} catch (Throwable t) {
				logger.log(Level.WARNING, "Invalid cron expression for task {0}, field 'months': {1}", new Object[] { task, t.getMessage() });
			}

			return cronEntry;

		} else {

			logger.log(Level.WARNING, "Invalid cron expression for task {0}: invalid number of fields (must be {1}).", new Object[] { task, CronService.NUM_FIELDS });

		}

		return null;
	}

	// ----- private methods -----
	private static CronField parseField(String field, int minValue, int maxValue) {

		// asterisk: *
		if ("*".equals(field)) {

			return new CronField(minValue, maxValue, 1);

		}

		// asterisk with step: */3
		if (field.startsWith("*/")) {

			throw new UnsupportedOperationException("Steps are not supported yet.");

			/*
			 * int step = Integer.parseInt(field.substring(2));
			 * if(step > 0 & step <= maxValue) {
			 *
			 *       return new CronField(minValue, maxValue, step);
			 *
			 * } else {
			 *
			 *       throw new IllegalArgumentException("Illegal step: '" + step + "'");
			 * }
			 */

		}

		// simple number: 2
		if (field.matches("[0-9]{1,2}")) {

			int value = Integer.parseInt(field);

			if ((value >= minValue) && (value <= maxValue)) {

				return new CronField(value, value, 1);

			} else {

				throw new IllegalArgumentException("Parameter not within range: '" + field + "'");

			}

		}

		// range: 4-6
		if (field.matches("[0-9]{1,2}-[0-9]{1,2}")) {

			String[] rangeValues = field.split("[-]+");

			if (rangeValues.length == 2) {

				int start = Integer.parseInt(rangeValues[0]);
				int end   = Integer.parseInt(rangeValues[1]);

				if ((start >= minValue) && (start <= maxValue) && (end >= minValue) && (end <= maxValue)) {

					return new CronField(start, end, 1);

				} else {

					throw new IllegalArgumentException("Parameters not within range: '" + field + "'");

				}

			} else {

				throw new IllegalArgumentException("Invalid range: '" + field + "'");

			}

		}

		// range with step: 4-6/3
		if (field.matches("[0-9]{1,2}-[0-9]{1,2}/[0-9]{1,2}")) {

			throw new UnsupportedOperationException("Steps are not supported yet.");

			/*
			 * String[] rangeValues = field.split("[-]{1}");
			 * if(rangeValues.length == 2) {
			 *
			 *       int start = Integer.parseInt(rangeValues[0]);
			 *       String[] stepValues = rangeValues[1].split("[/]{1}");
			 *
			 *       if(stepValues.length == 2) {
			 *
			 *               int end = Integer.parseInt(stepValues[0]);
			 *               int step = Integer.parseInt(stepValues[1]);
			 *
			 *               if(step > 0 && step <= maxValue) {
			 *                       if(start >= minValue && start <= maxValue && end >= minValue && end <= maxValue) {
			 *
			 *
			 *                               return new CronField(start, end, step);
			 *
			 *                       } else {
			 *
			 *                               throw new IllegalArgumentException("Parameters not within range: '" + field + "'");
			 *                       }
			 *
			 *               } else {
			 *
			 *                       throw new IllegalArgumentException("Illegal step: '" + step + "'");
			 *               }
			 *
			 *       } else {
			 *
			 *               throw new IllegalArgumentException("Invalid step: '" + field + "'");
			 *       }
			 * } else {
			 *
			 *       throw new IllegalArgumentException("Invalid range: '" + field + "'");
			 * }
			 */

		}

		throw new IllegalArgumentException("Invalid field: '" + field + "'");
	}

	@Override
	public int compareTo(Delayed o) {

		Long myDelay = Long.valueOf(getDelay(TimeUnit.MILLISECONDS));
		Long oDelay  = Long.valueOf(o.getDelay(TimeUnit.MILLISECONDS));

		return myDelay.compareTo(oDelay);
	}

	//~--- get methods ----------------------------------------------------

	public long getDelayToNextExecutionInMillis() {

		Calendar now       = GregorianCalendar.getInstance();
		int nowSeconds     = now.get(Calendar.SECOND);
		int nowMinutes     = now.get(Calendar.MINUTE);
		int nowHours       = now.get(Calendar.HOUR_OF_DAY);
		int nowDays        = now.get(Calendar.DAY_OF_MONTH);
		int nowDow         = now.get(Calendar.DAY_OF_WEEK);
		int nowMonths      = now.get(Calendar.MONTH);
		
		int minSecondsNext = seconds.getStartValue();
		int minMinutesNext = minutes.getStartValue();
		int minHoursNext   = hours.getStartValue();
		int minDaysNext    = days.getStartValue();
		int minDowNext     = dow.getStartValue();
		int minMonthsNext  = months.getStartValue();

		// Not implemented yet
//              int stepSeconds = seconds.getStep();
//              int stepMinutes = minutes.getStep();
//              int stepHours   = hours.getStep();
//              int stepDays    = days.getStep();
//              int stepDow     = dow.getStep();
//              int stepMonths  = months.getStep();
		
		int diffSeconds = minSecondsNext - nowSeconds;
		int diffMinutes = minMinutesNext - nowMinutes;
		int diffHours   = minHoursNext - nowHours;
		int diffDays    = minDaysNext - nowDays;
		int diffDow     = minDowNext - nowDow;
		int diffMonths  = minMonthsNext - nowMonths;

		// FIXME: implement step
		if (diffSeconds < 0) {

			diffSeconds += 60;

		}

		if (diffMinutes < 0) {

			diffMinutes += 60;

		}

		if (diffHours < 0) {

			diffHours += 24;

		}

		if (diffDays < 0) {

			diffDays += 1;

		}

		if (diffDow < 0) {

			diffDow += 7;

		}

		if (diffMonths < 0) {

			diffMonths += 30;

		}

		long next = 0;

		if (!seconds.isInside(nowSeconds)) {

			next += (diffSeconds) * (1000L);

		}

		if (!minutes.isInside(nowMinutes)) {

			next += (diffMinutes) * (1000L * 60L);

		}

		if (!hours.isInside(nowHours)) {

			next += (diffHours) * (1000L * 60L * 60L);

		}

		if (!days.isInside(nowDays)) {

			next += (diffDays) * (1000L * 60L * 60L * 24L);

		}

		if (!dow.isInside(nowDow)) {

			next += (diffDow) * (1000L * 60L * 60L * 24L * 7L);

		}

		if (!months.isInside(nowMonths)) {

			next += (diffMonths) * (1000L * 60L * 60L * 24L * 30L);

		}

		return next;
	}

	public CronField getSeconds() {
		return seconds;
	}

	public CronField getMinutes() {
		return minutes;
	}

	public CronField getHours() {
		return hours;
	}

	public CronField getDays() {
		return days;
	}

	public CronField getWeeks() {
		return dow;
	}

	public CronField getMonths() {
		return months;
	}

	public String getName() {
		return name;
	}

	// ----- interface Delayed -----
	@Override
	public long getDelay(TimeUnit unit) {

		long next = TimeUnit.MILLISECONDS.convert(getDelayToNextExecutionInMillis(), unit);

		logger.log(Level.INFO, "{0} ms until start of task {1}", new Object[] { next, name });

		return next;
	}

	//~--- set methods ----------------------------------------------------

	public void setSeconds(CronField seconds) {
		this.seconds = seconds;
	}

	public void setMinutes(CronField minutes) {
		this.minutes = minutes;
	}

	public void setHours(CronField hours) {
		this.hours = hours;
	}

	public void setDays(CronField days) {
		this.days = days;
	}

	public void setWeeks(CronField weeks) {
		this.dow = weeks;
	}

	public void setMonths(CronField months) {
		this.months = months;
	}

	public void setName(String name) {
		this.name = name;
	}
}
