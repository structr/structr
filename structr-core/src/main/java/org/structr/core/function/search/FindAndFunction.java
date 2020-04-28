/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import org.structr.core.function.*;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

public class FindAndFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_AND = "Usage: ${and(predicate, ...). Example: ${find('Group', and(equals('name', 'Test')))}";

	@Override
	public String getName() {
		return "find.and";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final AndPredicate andPredicate = new AndPredicate();

		if (sources != null) {

			for (Object i : sources) {

				if (i instanceof SearchParameter) {

					andPredicate.addParameter((SearchParameter)i);

				} else if (i instanceof SearchFunctionPredicate) {

					andPredicate.addPredicate((SearchFunctionPredicate)i);
				}
			}

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return andPredicate;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_AND;
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
}
