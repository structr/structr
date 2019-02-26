/**
 * Copyright (C) 2010-2019 Structr GmbH
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
package org.structr.cron;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A CRON entry.
 *
 *
 */
public class CronEntry implements Delayed {

	private static final Logger logger = LoggerFactory.getLogger(CronService.class.getName());

	private CronField days    = null;
	private CronField dow     = null;
	private CronField hours   = null;
	private CronField minutes = null;
	private CronField months  = null;
	private CronField seconds = null;
	private String name       = null;

	private CronEntry(String name) {
		this.name = name;
	}

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
		buf.append(months.toString());
		buf.append(" ");
		buf.append(dow.toString());
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
			String monthsField  = fields[4];
			String weeksField   = fields[5];

			try {

				CronField seconds = parseField(secondsField, 0, 59);

				cronEntry.setSeconds(seconds);

			} catch (Throwable t) {
				logger.warn("Invalid cron expression for task {}, field 'seconds': {}", new Object[] { task, t.getMessage() });
			}

			try {

				CronField minutes = parseField(minutesField, 0, 59);

				cronEntry.setMinutes(minutes);

			} catch (Throwable t) {
				logger.warn("Invalid cron expression for task {}, field 'minutes': {}", new Object[] { task, t.getMessage() });
			}

			try {

				CronField hours = parseField(hoursField, 0, 23);

				cronEntry.setHours(hours);

			} catch (Throwable t) {
				logger.warn("Invalid cron expression for task {}, field 'hours': {}", new Object[] { task, t.getMessage() });
			}

			try {

				CronField days = parseField(daysField, 1, 31);

				cronEntry.setDays(days);

			} catch (Throwable t) {
				logger.warn("Invalid cron expression for task {}, field 'days': {}", new Object[] { task, t.getMessage() });
			}

			try {

				CronField weeks = parseField(weeksField, 0, 6);

				cronEntry.setWeeks(weeks);

			} catch (Throwable t) {
				logger.warn("Invalid cron expression for task {}, field 'weeks': {}", new Object[] { task, t.getMessage() });
			}

			try {

				CronField months = parseField(monthsField, 1, 12);

				cronEntry.setMonths(months);

			} catch (Throwable t) {
				logger.warn("Invalid cron expression for task {}, field 'months': {}", new Object[] { task, t.getMessage() });
			}

			return cronEntry;

		} else {

			logger.warn("Invalid cron expression for task {}: invalid number of fields (must be {}).", new Object[] { task, CronService.NUM_FIELDS });

		}

		return null;
	}

	// ----- private methods -----
	private static CronField parseField(String field, int minValue, int maxValue) {

		// asterisk: *
		if ("*".equals(field)) {

			return new CronField(minValue, maxValue, 1, true);

		}

		// asterisk with step: */3
		if (field.startsWith("*/")) {

			int step = Integer.parseInt(field.substring(2));
			if(step > 0 & step <= maxValue) {

			      return new CronField(minValue, maxValue, step);

			} else {

			      throw new IllegalArgumentException("Illegal step: '" + step + "'");
			}
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

		// list: 4,6
		if (field.contains(",")) {

			final String[] listValues  = field.split("[,]+");
			final List<Integer> values = new LinkedList<>();

			for (final String value : listValues) {

				try {
					values.add(Integer.parseInt(value));

				} catch (Throwable t) {
					throw new IllegalArgumentException("Invalid list value: '" + value + "'");
				}
			}

			return new CronField(values);

		}

		// range with step: 4-6/3
		if (field.matches("[0-9]{1,2}-[0-9]{1,2}/[0-9]{1,2}")) {

			throw new UnsupportedOperationException("Steps are not supported yet.");
		}

		throw new IllegalArgumentException("Invalid field: '" + field + "'");
	}

	@Override
	public int compareTo(Delayed o) {

		Long myDelay = getDelay(TimeUnit.MILLISECONDS);
		Long oDelay  = o.getDelay(TimeUnit.MILLISECONDS);

		return myDelay.compareTo(oDelay);
	}

	public long getDelayToNextExecutionInMillis() {

		Calendar now       = GregorianCalendar.getInstance();
		int nowSeconds     = now.get(Calendar.SECOND);
		int nowMinutes     = now.get(Calendar.MINUTE);
		int nowHours       = now.get(Calendar.HOUR_OF_DAY);
		int nowDays        = now.get(Calendar.DAY_OF_MONTH);		// DAY_OF_MONTH starts with 1
		int nowDow         = now.get(Calendar.DAY_OF_WEEK) - 1;		// DAY_OF_WEEK starts with 1 (sunday)
		int nowMonths      = now.get(Calendar.MONTH) + 1;		// MONTH starts with 0 (why???)
		boolean modified   = true;
		int maxTries       = 10000;
		int numTries       = 0;

		while(modified && numTries++ < maxTries) {

			modified = false;

			if(!modified && !seconds.isInside(nowSeconds)) {
				now.add(Calendar.SECOND, 1);
				modified = true;
			}

			if(!modified && !minutes.isInside(nowMinutes)) {
				now.add(Calendar.MINUTE, 1);
				modified = true;
			}

			if(!modified && !hours.isInside(nowHours)) {
				now.add(Calendar.HOUR_OF_DAY, 1);
				modified = true;
			}

			// exclude day of week and day from each other (both can match)
			if(!dow.isIsWildcard() && !days.isIsWildcard()) {

				if(!modified && !(dow.isInside(nowDow) || days.isInside(nowDays))) {
					now.add(Calendar.DAY_OF_MONTH, 1);
					modified = true;
				}

			} else if(!dow.isIsWildcard()) {

				if(!modified && !dow.isInside(nowDow)) {
					now.add(Calendar.DAY_OF_MONTH, 1);
					modified = true;
				}

			} else if(!days.isIsWildcard()) {

				if(!modified && !days.isInside(nowDays)) {
					now.add(Calendar.DAY_OF_MONTH, 1);
					modified = true;
				}
			}

			if(!modified && !months.isInside(nowMonths)) {
				now.add(Calendar.MONTH, 1);
				modified = true;
			}

			nowSeconds     = now.get(Calendar.SECOND);
			nowMinutes     = now.get(Calendar.MINUTE);
			nowHours       = now.get(Calendar.HOUR_OF_DAY);
			nowDays        = now.get(Calendar.DAY_OF_MONTH);	// DAY_OF_MONTH starts with 1
			nowDow         = now.get(Calendar.DAY_OF_WEEK) - 1;	// DAY_OF_WEEK starts with 1 (sunday)
			nowMonths      = now.get(Calendar.MONTH) + 1;		// MONTH starts with 0 (why???)
		}

		if(numTries == maxTries) {
			throw new IllegalArgumentException("Unable to determine next cron date for task " + name + ", aborting.");
		}

		return now.getTimeInMillis() - System.currentTimeMillis();
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

		logger.info("{} ms until start of task {}", new Object[] { next, name });

		return next;
	}

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
