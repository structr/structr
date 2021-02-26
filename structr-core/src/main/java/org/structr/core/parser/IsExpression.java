/*
 * Copyright (C) 2010-2021 Structr GmbH
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

import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.core.function.Functions;
import org.structr.schema.action.ActionContext;

public class IsExpression extends Expression {

	public static final String ERROR_MESSAGE_IS = "Usage: ${is(condition, trueValue)}. Example: ${is(equal(this.name, request.name), 'selected')}";

	public IsExpression() {
		super("is");
	}

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		buf.append("is(");

		for (final Expression expr : expressions) {
			buf.append(expr.toString());
		}
		buf.append(")");

		return buf.toString();
	}

	@Override
	public void add(final Expression expression) throws FrameworkException {

		if (expressions.size() == 2) {
			throw new FrameworkException(422, "Invalid is() expression in builtin function: too many parameters.");
		}

		super.add(expression);

	}

	@Override
	public Object evaluate(final ActionContext ctx, final GraphObject entity) throws FrameworkException, UnlicensedScriptException {

		if (expressions.isEmpty()) {
			return ERROR_MESSAGE_IS;
		}

		final Expression condition = expressions.get(0);

		if (isTrue(condition.evaluate(ctx, entity))) {

			if (expressions.size() > 1) {

				final Expression trueExpression = expressions.get(1);
				return trueExpression.evaluate(ctx, entity);

			} else {

				throw new FrameworkException(422, "Invalid is() expression in builtin function: missing trueExpression.");
			}

		} else {

			return null;

		}
	}

	private boolean isTrue(final Object source) {
		return source != null && (Boolean.TRUE.equals(source) || "true".equals(source));
	}

	@Override
	public Object transform(final ActionContext ctx, final GraphObject entity, final Object source) throws FrameworkException, UnlicensedScriptException {
		return source;
	}
}
