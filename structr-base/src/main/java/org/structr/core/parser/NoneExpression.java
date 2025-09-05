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
package org.structr.core.parser;

import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.List;

/**
 *
 *
 */

public class NoneExpression extends Expression {

	public static final String ERROR_MESSAGE_ALL = "Usage: ${none(collection, expression)}. Example: ${none(user.groups, is_allowed(data, current, 'read'))}";

	private Expression listExpression = null;
	private Expression noneExpression = null;

	public NoneExpression(final int row, final int column) {
		super("none", row, column);
	}

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		buf.append("none(");

		for (final Expression expr : expressions) {
			buf.append(expr.toString());
		}
		buf.append(")");

		return buf.toString();
	}

	@Override
	public void add(final Expression expression) throws FrameworkException {

		// first expression must yield a List
		if (this.listExpression == null) {

			this.listExpression = expression;

		} else if (this.noneExpression == null) {

			this.noneExpression = expression;

		} else {

			throw new FrameworkException(422, "Invalid none() expression in builtin function: too many parameters.");
		}

		expression.parent = this;
		expression.level  = this.level + 1;
	}

	@Override
	public Object evaluate(final ActionContext ctx, final GraphObject entity, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {

		if (listExpression == null) {
			return ERROR_MESSAGE_ALL;
		}

		final Object listSource = listExpression.evaluate(ctx, entity, hints);

		if (listSource != null && listSource instanceof Iterable) {

			final List source         = Iterables.toList((Iterable)listSource);
			final Object oldDataValue = ctx.getConstant("data");

			for (Object obj : source) {

				ctx.setConstant("data", obj);

				final Object resultObject = noneExpression.evaluate(ctx, entity, hints);
				if (resultObject != null) {

					if (resultObject instanceof Boolean) {

						if ((Boolean)resultObject) {
							return false;
						}

					} else {

						if (Boolean.valueOf(resultObject.toString())) {
							return false;
						}
					}
				}
			}

			ctx.setConstant("data", oldDataValue);
		}

		return true;
	}

	@Override
	public Object transform(final ActionContext ctx, final GraphObject entity, final Object source, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {
		return source;
	}
}
