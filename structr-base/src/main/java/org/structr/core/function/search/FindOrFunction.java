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
import org.structr.core.function.CoreFunction;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class FindOrFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_OR = "Usage: ${or(predicate, ...). Example: ${find('Group', or(equals('name', 'Test1'), equals('name', 'Test2')))}";

	@Override
	public String getName() {
		return "find.or";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final OrPredicate orPredicate = new OrPredicate();

		if (sources != null) {

			for (Object param : sources) {

				handleParameter(orPredicate, param);
			}

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return orPredicate;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_OR;
	}

	@Override
	public String shortDescription() {
		return "Returns a query predicate that can be used with find() or search().";
	}

	@Override
	public boolean isHidden() {
		return true;
	}

	@Override
	public String getSignature() {
		return null;
	}

	// ----- private methods -----
	private void handleParameter(final OrPredicate orPredicate, final Object param) {

		if (param instanceof SearchParameter) {

			orPredicate.addParameter((SearchParameter)param);

		} else if (param instanceof SearchFunctionPredicate) {

			orPredicate.addPredicate((SearchFunctionPredicate)param);

		} else if (param instanceof List) {

			for (final Object o : (List)param) {

				handleParameter(orPredicate, o);
			}
		}
	}
}
