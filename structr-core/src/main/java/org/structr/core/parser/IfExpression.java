/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 * @author Christian Morgner
 */

public class IfExpression extends Expression {

	private Expression condition       = null;
	private Expression falseExpression = null;
	private Expression trueExpression  = null;

	public IfExpression() {
		super("if");
	}

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		buf.append("if(");

		for (final Expression expr : expressions) {
			buf.append(expr.toString());
		}
		buf.append(")");

		return buf.toString();
	}

	@Override
	public void add(final Expression expression) throws FrameworkException {

		// first expression is the if condition
		if (this.condition == null) {

			this.condition = expression;

		} else if (this.trueExpression == null) {

			this.trueExpression = expression;

		} else if (this.falseExpression == null) {

			this.falseExpression = expression;

		} else {

			throw new FrameworkException(422, "Invalid if() expression in builtin function: too many parameters.");
		}

		expression.parent = this;
		expression.level  = this.level + 1;
	}

	@Override
	public Object evaluate(final ActionContext ctx, final GraphObject entity) throws FrameworkException {


		if (condition == null) {
			return Functions.ERROR_MESSAGE_IF;
		}

		if (isTrue(condition.evaluate(ctx, entity))) {

			if (trueExpression != null) {

				return trueExpression.evaluate(ctx, entity);

			} else {

				throw new FrameworkException(422, "Invalid if() expression in builtin function: missing trueExpression.");
			}

		} else {

			if (falseExpression != null) {

				return falseExpression.evaluate(ctx, entity);

			} else {

				throw new FrameworkException(422, "Invalid if() expression in builtin function: missing falseExpression.");
			}
		}
	}

	private boolean isTrue(final Object source) {
		return source != null && (Boolean.TRUE.equals(source) || "true".equals(source));
	}

	@Override
	public Object transform(final ActionContext ctx, final GraphObject entity, final Object source) throws FrameworkException {
		return source;
	}
}
