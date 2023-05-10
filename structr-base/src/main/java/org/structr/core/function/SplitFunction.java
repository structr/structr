/*
 * Copyright (C) 2010-2023 Structr GmbH
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
import org.structr.schema.action.ActionContext;

import java.util.Arrays;

public class SplitFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_SPLIT = "Usage: ${split(str[, separator])}. Example: ${split(this.commaSeparatedItems)}";

	@Override
	public String getName() {
		return "split";
	}

	@Override
	public String getSignature() {
		return "str [, separator ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			final String toSplit = sources[0].toString();
			String splitExpr = "[,;\\s]+";

			if (sources.length >= 2) {
				splitExpr = sources[1].toString();
				return Arrays.asList(StringUtils.splitByWholeSeparator(toSplit, splitExpr));
			} else {

				return Arrays.asList(toSplit.split(splitExpr));
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_SPLIT;
	}

	@Override
	public String shortDescription() {
		return "Splits the given string by the whole separator string";
	}
}
