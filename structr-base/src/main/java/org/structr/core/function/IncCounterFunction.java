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
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class IncCounterFunction extends CoreFunction {

	@Override
	public String getName() {
		return "inc_counter";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("level [, resetLowerLevels=false ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			try {

				final int level = parseInt(sources[0]);

				ctx.incrementCounter(level);

				// reset lower levels?
				if (sources.length == 2 && "true".equals(sources[1].toString())) {

					// reset lower levels
					for (int i = level + 1; i < 10; i++) {
						ctx.resetCounter(i);
					}
				}

			} catch (NumberFormatException nfe) {

				logException(nfe, "{}: NumberFormatException parsing counter level \"{}\" in element \"{}\". Parameters: {}", new Object[] { getReplacement(), sources[0].toString(), caller, getParametersAsString(sources) });
			}

		} catch (ArgumentNullException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${inc_counter(level[, resetLowerLevels])}. Example: ${inc_counter(1, true)}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Increases the value of the counter with the given index.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
