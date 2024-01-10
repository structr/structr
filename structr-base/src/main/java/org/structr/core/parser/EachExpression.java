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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.Arrays;
import java.util.Iterator;

/**
 *
 *
 */

public class EachExpression extends Expression {

	private static final Logger logger = LoggerFactory.getLogger(EachExpression.class);

	public static final String ERROR_MESSAGE_EACH = "Usage: ${each(collection, expression)}. Example: ${each(this.children, \"set(this, \"email\", lower(get(this.email))))\")}";

	private Expression listExpression = null;
	private Expression eachExpression = null;

	public EachExpression(final int row, final int column) {
		super("each", row, column);
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
	public Object evaluate(final ActionContext ctx, final GraphObject entity, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {

		if (listExpression == null) {
			return ERROR_MESSAGE_EACH;
		}

		Object listSource = listExpression.evaluate(ctx, entity, hints);

		if (listSource != null && listSource.getClass().isArray()) {
			listSource = Arrays.asList((Object[]) listSource);
		}

		if (listSource != null && listSource instanceof Iterable) {

			final Iterable source     = (Iterable)listSource;
			final Object oldDataValue = ctx.getConstant("data");

			if (isBatched()) {

				final App app           = StructrApp.getInstance(ctx.getSecurityContext());
				final Iterator iterator = source.iterator();
				int count               = 0;

				while (iterator.hasNext()) {

					try (final Tx tx = app.tx()) {

						while (iterator.hasNext()) {

							ctx.setConstant("data", iterator.next());
							eachExpression.evaluate(ctx, entity, hints);

							if ((++count % getBatchSize()) == 0) {
								break;
							}
						}

						tx.success();

					} catch (FrameworkException fex) {

						logger.warn(fex.getMessage());
						logger.warn(ExceptionUtils.getStackTrace(fex));
					}

					logger.debug("Committing batch after {} objects", count);

					// reset count
					count = 0;
				}

			} else {

				for (Object obj : source) {

					ctx.setConstant("data", obj);
					eachExpression.evaluate(ctx, entity, hints);
				}
			}

			// restore previous value of data keyword
			ctx.setConstant("data", oldDataValue);
		}

		return null;
	}

	@Override
	public Object transform(final ActionContext ctx, final GraphObject entity, final Object source, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {
		return source;
	}
}
