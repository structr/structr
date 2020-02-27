/**
 * Copyright (C) 2010-2020 Structr GmbH
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

import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 *
 */
public class ValueExpression extends Expression {

	private String keyword = null;

	public ValueExpression(final String keyword) {
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
	public Object evaluate(final ActionContext ctx, final GraphObject entity) throws FrameworkException, UnlicensedScriptException {

		Object value = ctx.getReferencedProperty(entity, keyword, null, 0);

		for (final Expression expression : expressions) {

			// evaluate expressions from left to right
			value = expression.transform(ctx, entity, value);
		}

		return value;
	}

	@Override
	public Object transform(final ActionContext ctx, final GraphObject entity, final Object value) throws FrameworkException, UnlicensedScriptException {

		// evaluate dot syntax
		if (keyword.startsWith(".")) {

			final String key = keyword.substring(1);

			if (value instanceof GraphObject) {

				// use evaluation depth > 0 so that any data key that is registered in the
				// context can NOT be used
				return ctx.getReferencedProperty(entity, key, value, 1);

			} else if (value instanceof Map) {

				return ((Map)value).get(key);
			}
		}

		return value;
	}
}
