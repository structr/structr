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
package org.structr.core.function;

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.converter.TemporalDateConverter;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DateAddFunction extends CoreFunction {

	@Override
	public String getName() {
		return "dateAdd";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("date, years[, months[, days[, hours[, minutes[, seconds]]]]]");
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

				date = TemporalDateConverter.convert(sources[0]);

				if (date == null) {

					try {
						// parse with format from IS
						date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(sources[0].toString());

					} catch (ParseException ex) {

						logger.warn("{}: Could not parse string \"{}\" with pattern {} in element \"{}\". Parameters: {}", new Object[]{getDisplayName(), sources[0].toString(), "yyyy-MM-dd'T'HH:mm:ssZ", caller, getParametersAsString(sources)});
						return "";
					}
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
				logger.warn("{}: Parameter \"{}\" must be integer! Provided: {} - using 0 as value. (Parameters: {})", getDisplayName(), name, sources[index], getParametersAsString(sources));
			}
		}

		return 0;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${dateAdd(date, years[, months[, days[, hours[, minutes[, seconds]]]]])}. Example: ${dateAdd(this.createdDate, 1, -1, 0, 0, 0, 0)}"),
			Usage.javaScript("Usage: ${{ $.dateAdd(date, years[, months[, days[, hours[, minutes[, seconds]]]]]); }}. Example: ${{ $.dateAdd($.this.createdDate, 1, -1, 0, 0, 0, 0); }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Adds the given values to a date.";
	}

	@Override
	public String getLongDescription() {
		return "The result is returned as new date object, leaving the original date untouched.";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("date", "date to manipulate"),
				Parameter.mandatory("years", "number of years to add"),
				Parameter.optional("months", "number of months to add"),
				Parameter.optional("days", "number of days to add"),
				Parameter.optional("hours", "number of hours to add"),
				Parameter.optional("minutes", "number of minutes to add"),
				Parameter.optional("seconds", "number of seconds to add")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"The `date` parameter accepts actual date objects, numbers (interpreted as ms after 1970) and strings (formatted as `yyyy-MM-dd'T'HH:mm:ssZ`)",
				"All other parameters must be provided as numbers"
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${dateAdd(now, 1)}", "Adds one year to the current date"),
				Example.structrScript("${dateAdd(now, 0, 0, 7)}", "Adds one week to the current date"),
				Example.structrScript("${dateAdd(now, 0, 0, -7)}", "Subtracts one week from the current date")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Miscellaneous;
	}
}