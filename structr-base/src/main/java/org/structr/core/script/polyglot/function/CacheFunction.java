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
package org.structr.core.script.polyglot.function;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.parser.CacheExpression;
import org.structr.core.parser.ConstantExpression;
import org.structr.core.parser.LazyEvaluatedFunctionExpression;
import org.structr.core.script.polyglot.PolyglotWrapper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.schema.action.Function;

import java.util.Arrays;

public class CacheFunction implements ProxyExecutable {
	private static final Logger logger = LoggerFactory.getLogger(CacheFunction.class);
	private final ActionContext actionContext;
	private final GraphObject entity;

	public CacheFunction(final ActionContext actionContext, final GraphObject entity) {

		this.actionContext = actionContext;
		this.entity = entity;
	}

	@Override
	public Object execute(Value... arguments) {
		final CacheExpression cacheExpr = new CacheExpression(1, 1);

		Object retVal = null;

		Object[] parameters = Arrays.stream(arguments).map(a -> PolyglotWrapper.unwrap(actionContext, a)).toArray();

		try {
			for (Object parameter : parameters) {
				if (parameter instanceof PolyglotWrapper.FunctionWrapper) {

					cacheExpr.add(new LazyEvaluatedFunctionExpression(((PolyglotWrapper.FunctionWrapper) parameter)::execute, 1, 1));
				} else {

					cacheExpr.add(new ConstantExpression(parameter, 1, 1));
				}
			}

			retVal = PolyglotWrapper.wrap(actionContext, cacheExpr.evaluate(actionContext, entity, new EvaluationHints()));

		} catch (FrameworkException ex) {

			Function.logException(logger, ex, "Exception in CacheFunction: {}", new Object[]{ex.getMessage()});
		}

		return retVal;
	}
}
