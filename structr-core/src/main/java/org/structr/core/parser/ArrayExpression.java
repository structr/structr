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

import java.util.Collection;
import org.apache.commons.collections.CollectionUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 *
 */

public class ArrayExpression extends Expression {

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		buf.append("[");

		for (final Expression expr : expressions) {
			buf.append(expr.toString());
		}
		buf.append("]");

		return buf.toString();
	}


	@Override
	public void add(final Expression expression) throws FrameworkException {

		if (!expressions.isEmpty()) {
			throw new FrameworkException(422, "Invalid expression: expected ], found another expression.");
		}

		super.add(expression);
	}

	@Override
	public Object evaluate(final ActionContext ctx, final GraphObject entity) throws FrameworkException {

		switch (expressions.size()) {

			case 0:
				throw new FrameworkException(422, "Invalid expression: expected expression, found ].");

			case 1:
				final Object value = expressions.get(0).evaluate(ctx, entity);
				if (value instanceof Number) {

					return ((Number)value).intValue();
				}
		}

		return null;
	}

	@Override
	public Object transform(final ActionContext ctx, final GraphObject entity, final Object value) throws FrameworkException {

		if (value == null) {
			return null;
		}

		final Integer index = (Integer)evaluate(ctx, entity);
		if (index != null) {

			if (value instanceof Collection || value.getClass().isArray()) {

				try {

					// silently ignore array index errors
					return CollectionUtils.get(value, index);

				} catch (Throwable t) {}

			} else {

				throw new FrameworkException(422, "Invalid expression: expected collection, found " + value.getClass().getSimpleName() + ".");
			}

		} else {

			throw new FrameworkException(422, "Invalid expression: invalid array index: null.");
		}

		return null;
	}
}
