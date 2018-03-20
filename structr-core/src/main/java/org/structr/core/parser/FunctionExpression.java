/**
 * Copyright (C) 2010-2018 Structr GmbH
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

import java.util.ArrayList;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.function.BatchableFunction;
import org.structr.core.graph.Tx;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 *
 */
public class FunctionExpression extends Expression {

	private Function<Object, Object> function = null;

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		buf.append("function(");

		for (final Expression expr : expressions) {
			buf.append(expr.toString());
		}
		buf.append(")");

		return buf.toString();
	}

	public FunctionExpression(final String name, final Function<Object, Object> function) {

		super(name);

		this.function = function;
	}

	@Override
	public Object evaluate(final ActionContext ctx, final GraphObject entity) throws FrameworkException, UnlicensedException {

		final ArrayList<Object> results = new ArrayList<>();
		for (Expression expr : expressions) {

			final Object result = expr.evaluate(ctx, entity);
			results.add(result);
		}

		if (results.isEmpty() && expressions.size() > 0) {
			return function.usage(ctx.isJavaScriptContext());
		}

		if (function instanceof BatchableFunction) {

			// enable batching if batchable function is found
			((BatchableFunction)function).setBatchSize(getBatchSize());
			((BatchableFunction)function).setBatched(isBatched());

			// batchable functions must create their own transaction when in batched mode
			return function.apply(ctx, entity, results.toArray());

		} else if (isBatched()) {

			// when in batched mode,
			try (final Tx tx = StructrApp.getInstance(ctx.getSecurityContext()).tx()) {

				final Object result = function.apply(ctx, entity, results.toArray());

				tx.success();

				return result;
			}

		} else {

			// default execution path: enclosing transaction exists, no batching
			return function.apply(ctx, entity, results.toArray());
		}

	}

	@Override
	public Object transform(final ActionContext ctx, final GraphObject entity, final Object source) throws FrameworkException, UnlicensedException {
		return source;
	}

	public Function<Object, Object> getFunction() {
		return function;
	}
}
