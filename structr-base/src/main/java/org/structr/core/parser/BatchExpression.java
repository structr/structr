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
import org.structr.core.StaticValue;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

/**
 *
 */

public class BatchExpression extends Expression {

	private static final Logger logger = LoggerFactory.getLogger(BatchExpression.class);

	public static final String ERROR_MESSAGE_BATCH = "Usage: ${batch(statement, batchSize)}. Example: ${batch(delete(find('User')), 1000)}";

	private Expression batchExpression = null;
	private Expression sizeExpression  = null;
	private boolean background         = false;
	private int batchSize              = -1;

	public BatchExpression(final int row, final int column) {
		super("batch", row, column);
	}

	@Override
	public void add(final Expression expression) throws FrameworkException {

		if (this.batchExpression == null) {

			this.batchExpression = expression;

		} else if (this.sizeExpression == null) {

			this.sizeExpression = expression;

		} else {

			throw new FrameworkException(422, "Invalid batch() expression in builtin function: too many parameters.");
		}

		expression.parent = this;
		expression.level  = this.level + 1;
	}

	@Override
	public Object evaluate(final ActionContext ctx, final GraphObject entity, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {

		if (batchExpression == null || sizeExpression == null) {
			return ERROR_MESSAGE_BATCH;
		}

		final Object value = sizeExpression.evaluate(ctx, entity, hints);
		if (value != null && value instanceof Number) {

			// store batch size for children to use
			this.batchSize = ((Number)value).intValue();

			// initialize holders to store results from worker thread (must be final)
			final StaticValue<FrameworkException> exception = new StaticValue<>(null);
			final StaticValue result                        = new StaticValue(null);

			final Thread workerThread = new Thread(() -> {

				try {
					result.set(null, batchExpression.evaluate(ctx, entity, hints));

				} catch (FrameworkException fex) {
					exception.set(null, fex);
				}

			});

			workerThread.start();

			try { workerThread.join(); } catch (Throwable t) {
				logger.error(ExceptionUtils.getStackTrace(t));
			}

			if (exception.get(null) != null) {
				throw exception.get(null);
			}

			// result holder
			return result.get(null);

		} else {

			throw new FrameworkException(422, "Error in batch(): invalid batch size, expecting number.");
		}
	}

	@Override
	public Object transform(final ActionContext ctx, final GraphObject entity, final Object source, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {
		return source;
	}

	@Override
	public boolean isBatched() {
		return true;
	}

	@Override
	public int getBatchSize() {
		return batchSize;
	}
}
