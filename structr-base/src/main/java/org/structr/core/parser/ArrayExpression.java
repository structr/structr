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
package org.structr.core.parser;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.util.Iterables;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.docs.*;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;
import org.structr.schema.action.Function;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 *
 */

public class ArrayExpression extends Expression {

	public ArrayExpression(final int row, final int column) {
		super(row, column);
	}

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		buf.append("[");
		buf.append(StringUtils.join(expressions.stream().map(Expression::toString).collect(Collectors.toList()), ", "));
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
	public Object evaluate(final ActionContext ctx, final GraphObject entity, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {

		switch (expressions.size()) {

			case 0:
				throw new FrameworkException(422, "Invalid expression: expected expression, found ].");

			case 1:
				final Object value  = expressions.get(0).evaluate(ctx, entity, hints);
				final Object parsed = Function.parseInt(value);
				if (parsed instanceof Number) {

					return ((Number)parsed).intValue();
				}
		}

		return null;
	}

	@Override
	public Object transform(final ActionContext ctx, final GraphObject entity, final Object value, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {

		if (value == null) {
			return null;
		}

		final Integer index = (Integer)evaluate(ctx, entity, hints);
		if (index != null) {

			if (value instanceof Collection || value.getClass().isArray()) {

				try {

					// silently ignore array index errors
					return CollectionUtils.get(value, index);

				} catch (Throwable t) {}

			} else if (value instanceof Iterable) {

				try {

					return Iterables.nth((Iterable)value, index);

				} catch (Throwable t) {}

			} else {

				throw new FrameworkException(422, "Invalid expression: expected collection, found " + value.getClass().getSimpleName() + ".");
			}

		} else {

			throw new FrameworkException(422, "Invalid expression: invalid array index: null.");
		}

		return null;
	}

	// ----- documentation (unused) -----
	@Override
	public String getShortDescription() {
		return "";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of();
	}

	@Override
	public List<Example> getExamples() {
		return List.of();
	}

	@Override
	public List<String> getNotes() {
		return List.of();
	}

	@Override
	public List<Signature> getSignatures() {
		return List.of();
	}

	@Override
	public List<Language> getLanguages() {
		return List.of();
	}

	@Override
	public List<Usage> getUsages() {
		return List.of();
	}
}
