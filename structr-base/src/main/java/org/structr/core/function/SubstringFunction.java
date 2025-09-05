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

public class SubstringFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_SUBSTRING = "Usage: ${substring(string, start [, length ])}. Example: ${substring(this.name, 19, 3)}";

	@Override
	public String getName() {
		return "substring";
	}

	@Override
	public String getSignature() {
		return "str, start [, length ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 2, 3);

			final String source = sources[0].toString();
			final int sourceLength = source.length();
			final int beginIndex = parseInt(sources[1]);
			final int length = sources.length == 3 ? parseInt(sources[2]) : sourceLength - beginIndex;
			final int endIndex = Math.min(beginIndex + length, sourceLength);

			if (beginIndex >= 0 && beginIndex < sourceLength && endIndex >= beginIndex && endIndex <= sourceLength) {

				return source.substring(beginIndex, endIndex);
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
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_SUBSTRING;
	}

	@Override
	public String shortDescription() {
		return "Returns the substring of the given string";
	}
}
