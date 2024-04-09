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

import org.structr.api.util.Iterables;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class FirstFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_FIRST = "Usage: ${first(collection)}. Example: ${first(this.children)}";

	@Override
	public String getName() {
		return "first";
	}

	@Override
	public String getSignature() {
		return "list";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof Iterable) {

				return Iterables.first((Iterable)sources[0]);
			}

			if (sources[0] instanceof List && !((List)sources[0]).isEmpty()) {
				return ((List)sources[0]).get(0);
			}

			if (sources[0].getClass().isArray()) {

				final Object[] arr = (Object[])sources[0];
				if (arr.length > 0) {

					return arr[0];
				}
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_FIRST;
	}

	@Override
	public String shortDescription() {
		return "Returns the first element of the given collection";
	}
}
