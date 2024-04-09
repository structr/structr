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

import org.structr.common.ContextStore;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 *
 */
public class ValueExpression extends Expression {

	private String keyword = null;

	public ValueExpression(final String keyword, final int row, final int column) {

		super(row, column);

		this.keyword = keyword;
	}

	public String getKeyword() {
		return keyword;
	}

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		buf.append(keyword);
		buf.append("(");

		for (final Expression expr : expressions) {
			buf.append(expr.toString());
		}
		buf.append(")");

		return buf.toString();
	}

	@Override
	public Object evaluate(final ActionContext ctx, final GraphObject entity, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {

		Object value = null;

		if (this.expressions.isEmpty()) {

			// no nested expressions, this is not a function call
			value = ctx.getReferencedProperty(entity, keyword, null, 0, hints, row, column);

		} else {

			// nested expressions detected, handle this as a function call
			final ContextStore contextStore  = ctx.getContextStore();
			final Map<String, Object> tmp    = contextStore.getTemporaryParameters();
			final Map<String, Object> params = new LinkedHashMap<>();

			// evaluate child expressions to get parameters
			handleParameters(ctx, entity, hints, params);

			// install new parameters for possible method call
			contextStore.setTemporaryParameters(params);

			// evaluate
			value = ctx.getReferencedProperty(entity, keyword, null, 0, hints, row, column);

			// restore previous parameters
			contextStore.setTemporaryParameters(tmp);
		}

		for (final Expression expression : expressions) {

			// evaluate expressions from left to right
			value = expression.transform(ctx, entity, value, hints);
		}

		return value;
	}

	@Override
	public Object transform(final ActionContext ctx, final GraphObject entity, final Object value, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {

		// evaluate dot syntax
		if (keyword.startsWith(".")) {

			Object extractedValue = value;

			final String[] keys = keyword.split("\\.");

			for (final String key : keys) {

				if (key.length() == 0) {
					continue;
				}

				if (extractedValue instanceof GraphObject) {

					// use evaluation depth > 0 so that any data key that is registered in the
					// context can NOT be used
					extractedValue = ctx.getReferencedProperty(entity, key, extractedValue, 1, hints, row, column);

					// Treat enums as string values in StructrScript contexts
					if (extractedValue instanceof Enum<?>) {

						extractedValue = extractedValue.toString();
					}

				} else if (value instanceof Map) {

					extractedValue = ((Map)extractedValue).get(key);
				}
			}


			return extractedValue;
		}

		return value;
	}

	// ----- private methods -----
	private void handleParameters(final ActionContext ctx, final GraphObject entity, final EvaluationHints hints, final Map<String, Object> dest) throws FrameworkException {

		Object key   = null;
		Object value = null;

		for (final Expression param : this.expressions) {

			if (key == null) {

				key = param.evaluate(ctx, entity, hints);

				if (key instanceof Map) {

					dest.putAll((Map)key);

					// reset params, allows map mixed with key-value pairs
					key = null;

				}

			} else if (value == null) {

				value = param.evaluate(ctx, entity, hints);

				dest.put(key.toString(), value);

				// reset params for the next key-value pair
				key   = null;
				value = null;
			}
		}
	}
}
