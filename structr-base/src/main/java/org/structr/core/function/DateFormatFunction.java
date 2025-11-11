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
import org.structr.core.converter.TemporalDateConverter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DateFormatFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_DATE_FORMAT = "Usage: ${date_format(value, pattern)}. Example: ${date_format(this.creationDate, \"yyyy-MM-dd'T'HH:mm:ssZ\")}";

	@Override
	public String getName() {
		return "date_format";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("value, pattern");
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

						logger.warn("{}: Could not parse string \"{}\" with pattern {} in element \"{}\". Parameters: {}", new Object[]{getReplacement(), sources[0].toString(), "yyyy-MM-dd'T'HH:mm:ssZ", caller, getParametersAsString(sources)});
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
			Usage.structrScript("Usage: ${date_format(value, pattern)}. Example: ${date_format(this.creationDate, \"yyyy-MM-dd'T'HH:mm:ssZ\")}"),
			Usage.javaScript("Usage: ${{Structr.date_format(value, pattern)}}. Example: ${{Structr.date_format(Structr.get('this').creationDate, \"yyyy-MM-dd'T'HH:mm:ssZ\")}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Formats the given value as a date string with the given format string";
	}
}
