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
package org.structr.core.function.search;

import org.structr.common.error.FrameworkException;
import org.structr.core.function.AdvancedScriptingFunction;
import org.structr.schema.action.ActionContext;

public class FindRangeFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_RANGE = "Usage: ${range(start, end)}. Example: ${find(\"Event\", \"date\", range(\"2018-12-31\", \"2019-01-01\"))}";

	@Override
	public String getName() {
		return "find.range";
	}

	@Override
	public String getSignature() {
		return null;
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		Object rangeStart    = null;
		Object rangeEnd      = null;
		boolean includeStart = true;
		boolean includeEnd   = true;

		try {

			if (sources == null || sources.length < 2) {

				throw new IllegalArgumentException();
			}

			switch (sources.length) {

				case 4: includeEnd   = Boolean.valueOf(sources[3].toString());
				case 3: includeStart = Boolean.valueOf(sources[2].toString());
				case 2: rangeEnd     = sources[1];
				case 1: rangeStart   = sources[0];
				default: break;
			}

			return new RangePredicate(rangeStart, rangeEnd, includeStart, includeEnd);

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_RANGE;
	}

	@Override
	public String shortDescription() {
		return "Returns a range predicate that can be used in find() function calls";
	}

	@Override
	public boolean isHidden() {
		return true;
	}
}
