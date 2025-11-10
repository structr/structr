/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

public class WeekDaysFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_WEEK_DAYS    = "Usage: ${week_days(from, to)}. Example: ${week_days(parse_date(\"2014-01-01\", \"yyyy-MM-dd\"), parse_date(\"2014-01-15\", \"yyyy-MM-dd\"))}";
	public static final String ERROR_MESSAGE_WEEK_DAYS_JS = "Usage: ${{Structr.weekDays(from, to)}}. Example: ${{Structr.weekDays(Structr.parseDate(\"2014-01-01\", \"yyyy-MM-dd\"), Structr.parseDate(\"2014-01-15\", \"yyyy-MM-dd\"))}}";

	@Override
	public String getName() {
		return "week_days";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("from, to");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources == null || sources.length != 2) {
			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 2);

			final LocalDate fromDate = ((Date) sources[0]).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			final LocalDate toDate   = ((Date) sources[1]).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

			try {
				final DayOfWeek startWeek = toDate.getDayOfWeek();
				final DayOfWeek endWeek = toDate.getDayOfWeek();

				final long days = ChronoUnit.DAYS.between(fromDate, toDate);
				final long daysWithoutWeekends = days - 2 * ((days + startWeek.getValue())/7);

				//adjust for starting and ending on a Sunday:
				return daysWithoutWeekends + (startWeek == DayOfWeek.SUNDAY ? 1 : 0) + (endWeek == DayOfWeek.SUNDAY ? 1 : 0);

			} catch (Exception ex) {

				logger.warn("{}: Could not calculate week days. Parameters: {}", new Object[] { getReplacement(), caller, getParametersAsString(sources) });

			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_WEEK_DAYS_JS : ERROR_MESSAGE_WEEK_DAYS);
	}

	@Override
	public String getShortDescription() {
		return "Calculates the number of week days (working days) between given dates.";
	}
}
