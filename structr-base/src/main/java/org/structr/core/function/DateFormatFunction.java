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
import java.util.Date;
import java.util.List;

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

		if (sources == null || sources.length != 2 || sources[1] == null) {
			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 2);

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
						return sources[0];
					}
				}
			}

			// format with given pattern
			return new SimpleDateFormat(sources[1].toString(), ctx.getLocale()).format(date);

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return "";

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
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
				This function uses the Java SimpleDateFormat class which provides the following pattern chars:

				| Letter | Date or Time Component |
				| --- | --- |
				| G | Era designator |
				| y | Year |
				| Y | Week year |
				| M | Month in year |
				| w | Week in year |
				| W | Week in month |
				| D | Day in year |
				| d | Day in month |
				| F | Day of week in month |
				| E | Day name in week |
				| u | Day number of week (1 = Monday, ..., 7 = Sunday) |
				| a | AM/PM marker |
				| H | Hour in day (0-23) |
				| k | Hour in day (1-24) |
				| K | Hour in AM/PM (0-11) |
				| h | Hour in AM/PM (1-12) |
				| m | Minute in hour |
				| s | Second in minute |
				| S | Millisecond |
				| z | General time zone |
				| Z | RFC 822 time zone |
				| X | ISO 8601 time zone |

				Each character can be repeated multiple times to control the output format.

				| Pattern | Description |
				| --- | --- |
				| d | prints one or two numbers (e.g. "1", "5" or "20") |
				| dd | prints two numbers (e.g. "01", "05" or "20") |
				| EEE | prints the shortened name of the weekday (e.g. "Mon") |
				| EEEE | prints the long name of the weekday (e.g. "Monday") |
				""";
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"Some format options are locale-specific. See the examples or the `locale` keyword for information about locales."
		);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("date", "date to format"),
				Parameter.mandatory("pattern", "format pattern")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${dateFormat(toDate(1585504800000), 'yyyy-MM-dd')}", "2020-03-29"),
				Example.structrScript("${dateFormat(toDate(1585504800000), 'EEEE')}", "Sunday"),
				Example.structrScript("${(setLocale('de'), dateFormat(toDate(1585504800000), 'EEEE'))}", "Sonntag")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Conversion;
	}
}
