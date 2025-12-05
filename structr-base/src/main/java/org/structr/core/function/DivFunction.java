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
import org.structr.docs.Language;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class DivFunction extends CoreFunction {

	@Override
	public String getName() {
		return "div";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("value1, value2");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 2);

			try {

				return Double.parseDouble(sources[0].toString()) / Double.parseDouble(sources[1].toString());

			} catch (NumberFormatException nfe) {

				logException(nfe, "{}: NumberFormatException in element \"{}\" for parameters: {}", new Object[] { getDisplayName(), caller, getParametersAsString(sources) });
				return nfe.getMessage();

			}

		} catch (ArgumentNullException pe) {

			if (sources.length > 0 && sources[0] != null) {

				try {

					return Double.parseDouble(sources[0].toString());

				} catch (NumberFormatException nfe) {

					logException(nfe, "{}: NumberFormatException in element \"{}\" for parameters: {}", new Object[] { getDisplayName(), caller, getParametersAsString(sources) });
					return nfe.getMessage();
				}
			}

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${div(value1, value2)}. Example: ${div(5, 2)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the result of value1 divided by value2.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"This function tries to convert its parameter objects into numerical values, i.e. you can use strings as arguments."
		);
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}
}