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
package org.structr.core.function.search;

import org.structr.common.error.FrameworkException;
import org.structr.core.function.AdvancedScriptingFunction;
import org.structr.schema.action.ActionContext;

public class FindGteFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_GTE = "Usage: ${gte(start, end)}. Example: ${find(\"User\", \"age\", gte(\"42\"))}";

	@Override
	public String getName() {
		return "find.gte";
	}

	@Override
	public String getSignature() {
		return null;
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			if (sources == null || sources.length > 1) {

				throw new IllegalArgumentException();
			}

			return new RangePredicate(sources[0], null, true, false);

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_GTE;
	}

	@Override
	public String shortDescription() {
		return "Returns a gte predicate that can be used in find() function calls";
	}

	@Override
	public boolean isHidden() {
		return true;
	}
}
