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
import org.structr.schema.action.ActionContext;

public class IntFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_INT = "Usage: ${int(parameter)}. Example: ${int(this.numericalStringValue)} or ${int(5.8)}";

	@Override
	public String getName() {
		return "int";
	}

	@Override
	public String getSignature() {
		return "value";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof Number) {
				return ((Number)sources[0]).intValue();
			}

			final Double dbl = getDoubleOrNull(sources[0]);

			if (dbl != null) {

				return dbl.intValue();

			} else {

				logParameterError(caller, sources, ctx.isJavaScriptContext());
				return null;
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());

		} catch (Throwable t) {

			logException(caller, t, sources);
			return null;
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_INT;
	}

	@Override
	public String shortDescription() {
		return "Converts the given string to an integer";
	}
}
