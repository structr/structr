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

import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

public class ParseDateFunction extends CoreFunction {

	@Override
	public String getName() {
		return "parseDate";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("str, pattern");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources == null || sources.length != 2) {
			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 2);

			final String dateString = sources[0].toString();

			if (StringUtils.isBlank(dateString)) {
				return "";
			}

			final String pattern = sources[1].toString();

			try {
				// parse with format from IS
				return new SimpleDateFormat(pattern).parse(dateString);

			} catch (ParseException ex) {

				logger.debug("{}: Could not parse string \"{}\" with pattern {} in element \"{}\". Parameters: {}", new Object[] { getDisplayName(), dateString, pattern, caller, getParametersAsString(sources) });

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
	public List<Usage> getUsages() {
		return List.of(
				Usage.structrScript("Usage: ${parseDate(value, pattern)}."),
				Usage.javaScript("Usage: ${{ $.parseDate(value, pattern) }}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Parses the given date string using the given format string.";
	}

	@Override
	public String getLongDescription() {
		return "Parses the given string according to the given pattern and returns a date object. This method is the inverse of <a href='#date_format'>date_format()</a>.";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${parseDate('2015-12-12', 'yyyy-MM-dd')}"),
				Example.javaScript("${{ $.parseDate('2015-12-12', 'yyyy-MM-dd') }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("string", "date string"),
				Parameter.mandatory("pattern", "date pattern")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Conversion;
	}
}
