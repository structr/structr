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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 * @author Christian Morgner
 */
public class ValueExpression extends Expression {

	private String keyword = null;

	public ValueExpression(final String keyword) {
		this.keyword = keyword;
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
	public Object evaluate(final SecurityContext securityContext, final ActionContext ctx, final GraphObject entity) throws FrameworkException {

		Object value = ctx.getReferencedProperty(securityContext, entity, keyword, null);

		for (final Expression expression : expressions) {

			// evaluate expressions from left to right
			value = expression.transform(securityContext, ctx, entity, value);
		}

		return value;
	}

	@Override
	public Object transform(final SecurityContext securityContext, final ActionContext ctx, final GraphObject entity, final Object value) throws FrameworkException {

		// evaluate dot syntax
		if (keyword.startsWith(".") && value instanceof GraphObject) {

			return ctx.getReferencedProperty(securityContext, entity, keyword.substring(1), value);
		}

		return value;
	}
}
