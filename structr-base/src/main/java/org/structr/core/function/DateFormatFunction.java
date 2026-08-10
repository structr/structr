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

import org.structr.api.config.Settings;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.FrameworkException;
import org.structr.core.converter.TemporalDateConverter;
import org.structr.core.property.ZonedDateTimeProperty;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DateFormatFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_DATE_FORMAT = "Usage: ${dateFormat(value, pattern)}. Example: ${dateFormat(this.createdDate, \"yyyy-MM-dd'T'HH:mm:ssZ\")}";

	@Override
	public String getName() {

		return "dateFormat";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("value, pattern");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLength(sources, 2, 2);

			// a null date value is a normal data condition, not an error: pass it through
			if (sources[0] == null) {

				return null;
			}

			if (sources[1] == null) {

				return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, "%s: Pattern must not be null: %s".formatted(getDisplayName(), getParametersAsString(sources)));
			}

			Date date = null;

			if (sources[0] instanceof Date) {

				date = (Date) sources[0];

			} else if (sources[0] instanceof ZonedDateTime zdt) {

				final DateTimeFormatter dateTimeFormatter = ZonedDateTimeProperty.getDateTimeFormatter(sources[1].toString()).withLocale(ctx.getLocale());

				return dateTimeFormatter.format(zdt);

			} else if (sources[0] instanceof Number) {

				date = new Date(((Number)sources[0]).longValue());

			} else {

				date = TemporalDateConverter.convert(sources[0]);

				if (date == null) {

					try {

						// parse with format from IS
						date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(sources[0].toString());

					} catch (ParseException ex) {

						logger.warn("{}: Could not parse string \"{}\" with pattern {} in element \"{}\". Parameters: {}", getDisplayName(), sources[0].toString(), "yyyy-MM-dd'T'HH:mm:ssZ", caller, getParametersAsString(sources));

						return sources[0];
					}
				}
			}

			// format with given pattern

			return new SimpleDateFormat(sources[1].toString(), ctx.getLocale()).format(date);

		} catch (ArgumentCountException ace) {

			return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, "%s: Wrong number of arguments: %s".formatted(getDisplayName(), getParametersAsString(sources)), ace);

		} catch (Throwable t) {

			return throwExceptionIfSupportedElseLogWarningAndReturnNull(ctx, "%s: %s: %s".formatted(getDisplayName(), t.getMessage(), getParametersAsString(sources)), t);
		}
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.structrScript(ERROR_MESSAGE_DATE_FORMAT),
			Usage.javaScript("Usage: ${{ $.dateFormat(value, pattern); }}. Example: ${{ $.dateFormat($.this.createdDate, \"yyyy-MM-dd'T'HH:mm:ssZ\"); }}")
		);
	}

	@Override
	public String getShortDescription() {

		return "Formats the given date object according to the given pattern, using the current locale (language/country settings).";
	}

	@Override
	public String getLongDescription() {

		return """
				This function supports Date objects and ZonedDateTime objects. The tables below list the supported patterns for those types.
				
				### Date objects
				
				%s
				
				----
				
				### ZonedDateTime objects
				
				%s
				""".formatted(Settings.DefaultDateFormat.getLongDescription(), Settings.ZonedDateTimeFormatOverride.getLongDescription());
	}

	@Override
	public List<String> getNotes() {

		return List.of(
				"Some format options are locale-specific. See the examples or the `locale` keyword for information about locales.",
				"If the date value is null, the function returns null. A null pattern is an error."
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(Parameter.mandatory("date", "date to format"), Parameter.mandatory("pattern", "format pattern"));
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
				Example.structrScript("${dateFormat(toDate(1585504800000), 'yyyy-MM-dd')}", "2020-03-29"),
				Example.structrScript("${dateFormat(toDate(1585504800000), 'EEEE')}", "Sunday"),
				Example.structrScript("${(setLocale('de'), dateFormat(toDate(1585504800000), 'EEEE'))}", "Sonntag"),
				Example.javaScript("""
					$.setLocale('de');
					$.dateFormat(Temporal.ZonedDateTime.from({
						timeZone: "Europe/Berlin",
						year: 2026, month: 7, day: 15,
						hour: 12, minute: 34, second: 56,
					}), 'EEEE, HH:mm:ss');
					""", "Mittwoch, 12:34:56")
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Conversion;
	}
}
