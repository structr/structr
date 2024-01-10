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
package org.structr.core.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.util.Iterables;
import org.structr.common.ContextStore;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.core.function.QueryFunction;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 *
 *
 */

public class SliceExpression extends Expression {

	private static final Logger logger = LoggerFactory.getLogger(SliceExpression.class);

	public static final String ERROR_MESSAGE_SLICE = "Usage: ${slice(collection, start, end)}. Example: ${slice(this.children, 0, 10)}";

	private Expression listExpression  = null;
	private Expression startExpression = null;
	private Expression endExpression   = null;

	public SliceExpression(final int row, final int column) {
		super("slice", row, column);
	}

	@Override
	public void add(final Expression expression) throws FrameworkException {

		// first expression must yield a List
		if (this.listExpression == null) {

			this.listExpression = expression;

		} else if (this.startExpression == null) {

			this.startExpression = expression;

		} else if (this.endExpression == null) {

			this.endExpression = expression;

		} else {

			throw new FrameworkException(422, "Invalid slice() expression in builtin function: too many parameters.");
		}

		expression.parent = this;
		expression.level  = this.level + 1;
	}

	@Override
	public Object evaluate(final ActionContext ctx, final GraphObject entity, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {

		if (listExpression == null || startExpression == null || endExpression == null) {
			return ERROR_MESSAGE_SLICE;
		}

		// load context store
		final SecurityContext securityContext = ctx.getSecurityContext();
		final ContextStore contextStore       = securityContext.getContextStore();

		// evaluate start and end bounds
		final Object startObject = startExpression.evaluate(ctx, entity, hints);
		final Object endObject   = endExpression.evaluate(ctx, entity, hints);

		if (startObject == null) {
			throw new FrameworkException(422, "Error in slice(): invalid start of range: null");
		}

		if (endObject == null) {
			throw new FrameworkException(422, "Error in slice(): invalid end of range: null");
		}

		final Integer start = toNumber(startObject);
		Integer end         = toNumber(endObject);
		boolean valid       = true;

		// null => number format parsing error or invalid source data
		if (start == null || end == null) {
			return null;
		}

		// check bounds BEFORE evaluating list expression
		if (start < 0)    { valid = false; logger.warn("Error in slice(): start index must be >= 0."); }
		if (end < 0)      { valid = false; logger.warn("Error in slice(): end index must be > 0."); }
		if (start >= end) { valid = false; logger.warn("Error in slice(): start index must be < end index."); }

		if (valid) {

			if (listExpression instanceof QueryFunction || (listExpression instanceof FunctionExpression && ((FunctionExpression)listExpression).getFunction() instanceof QueryFunction)) {

				contextStore.setRangeStart(start);
				contextStore.setRangeEnd(end);

				return listExpression.evaluate(ctx, entity, hints);

			} else {

				final Object src = listExpression.evaluate(ctx, entity, hints);
				List list       = null;

				if (src instanceof Iterable) {

					// handle iterable argument
					list = Iterables.toList((Iterable)src);

				} else if (src instanceof List) {

					// handle list argument
					list = (List)src;

				// handle array argument
				} else if (isArray(src)) {

					list = toList((Object[])src);

				// handle collection argument
				} else if (src != null) {

					list = new LinkedList((Collection)src);

				} else {

					return null;
				}

				if (start > list.size())   { valid = false; logger.warn("Error in slice(): start index is out of range."); }
				if (end > list.size())     { end = list.size(); }

				if (valid) {

					return list.subList(start, end);
				}
			}
		}

		return null;
	}

	@Override
	public Object transform(final ActionContext ctx, final GraphObject entity, final Object source, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {
		return source;
	}

	// ----- private methods -----
	private Integer toNumber(final Object src) {

		if (src instanceof Number) {

			return ((Number)src).intValue();
		}

		if (src instanceof String) {

			try {

				return Integer.valueOf(src.toString());

			} catch (NumberFormatException nfex) {
				logger.warn("Error in slice(): Cannot parse number {}.", src);
			}
		}

		return null;
	}

	private boolean isArray(final Object obj) {
		return obj != null && obj.getClass().isArray();
	}

	private List toList(final Object[] obj) {
		return Arrays.asList(obj);
	}
}
