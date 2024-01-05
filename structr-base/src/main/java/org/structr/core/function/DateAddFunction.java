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
package org.structr.core.function;

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateAddFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_DATE_FORMAT    = "Usage: ${date_add(date, years[, months[, days[, hours[, minutes[, seconds]]]]])}. Example: ${date_add(this.createdDate, 1, -1, 0, 0, 0, 0)}";
	public static final String ERROR_MESSAGE_DATE_FORMAT_JS = "Usage: ${{Structr.date_add(date, years[, months[, days[, hours[, minutes[, seconds]]]]])}}. Example: ${{Structr.date_add(Structr.this.createdDate, 1, -1, 0, 0, 0, 0)}}";

	@Override
	public String getName() {
		return "date_add";
	}

	@Override
	public String getSignature() {
		return "date, years[, months[, days[, hours[, minutes[, seconds]]]]]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			Date date = null;

			if (sources[0] instanceof Date) {

				date = (Date)sources[0];

			} else if (sources[0] instanceof Number) {

				date = new Date(((Number)sources[0]).longValue());

			} else {

				try {
					// parse with format from IS
					date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(sources[0].toString());

				} catch (ParseException ex) {

					logger.warn("{}: Could not parse string \"{}\" with pattern {} in element \"{}\". Parameters: {}", new Object[] { getReplacement(), sources[0].toString(), "yyyy-MM-dd'T'HH:mm:ssZ", caller, getParametersAsString(sources) });
					return "";
				}
			}

			final Calendar givenDate = Calendar.getInstance();
			givenDate.setTime(date);

			givenDate.add(Calendar.YEAR,        getIntParameter(sources, 1, "year"));
			givenDate.add(Calendar.MONTH,       getIntParameter(sources, 2, "month"));
			givenDate.add(Calendar.DAY_OF_YEAR, getIntParameter(sources, 3, "day"));
			givenDate.add(Calendar.HOUR_OF_DAY, getIntParameter(sources, 4, "hour"));
			givenDate.add(Calendar.MINUTE,      getIntParameter(sources, 5, "minute"));
			givenDate.add(Calendar.SECOND,      getIntParameter(sources, 6, "second"));

			return givenDate.getTime();

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return "";

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	private int getIntParameter(final Object[] sources, final int index, final String name) {

		if (sources.length > index) {
			if (sources[index] instanceof Number) {
				return ((Number)sources[index]).intValue();
			} else {
				logger.warn("{}: Parameter \"{}\" must be integer! Provided: {} - using 0 as value. (Parameters: {})", getReplacement(), name, sources[index], getParametersAsString(sources));
			}
		}

		return 0;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_DATE_FORMAT_JS : ERROR_MESSAGE_DATE_FORMAT);
	}

	@Override
	public String shortDescription() {
		return "Adds the given values to a date";
	}
}
