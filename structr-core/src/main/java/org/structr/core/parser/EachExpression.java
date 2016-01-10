/**
 * Copyright (C) 2010-2016 Structr GmbH
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

import java.util.List;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 *
 */

public class EachExpression extends Expression {

	public static final String ERROR_MESSAGE_EACH = "Usage: ${each(collection, expression)}. Example: ${each(this.children, \"set(this, \"email\", lower(get(this.email))))\")}";

	private Expression listExpression = null;
	private Expression eachExpression = null;

	public EachExpression() {
		super("each");
	}

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		buf.append("each(");

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

		} else if (this.eachExpression == null) {

			this.eachExpression = expression;

		} else {

			throw new FrameworkException(422, "Invalid each() expression in builtin function: too many parameters.");
		}

		expression.parent = this;
		expression.level  = this.level + 1;
	}

	@Override
	public Object evaluate(final ActionContext ctx, final GraphObject entity) throws FrameworkException {

		if (listExpression == null) {
			return ERROR_MESSAGE_EACH;
		}

		final Object listSource = listExpression.evaluate(ctx, entity);
		if (listSource != null && listSource instanceof List) {

			final List source         = (List)listSource;
			final Object oldDataValue = ctx.getConstant("data");

			for (Object obj : source) {

				ctx.setConstant("data", obj);
				eachExpression.evaluate(ctx, entity);
			}

			ctx.setConstant("data", oldDataValue);
		}

		return null;
	}

	@Override
	public Object transform(final ActionContext ctx, final GraphObject entity, final Object source) throws FrameworkException {
		return source;
	}
}
