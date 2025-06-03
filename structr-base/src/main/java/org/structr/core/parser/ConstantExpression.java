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

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

/**
 *
 *
 */
public class ConstantExpression extends Expression {

	private String quoteChar = null;
	private Object value     = null;

	public ConstantExpression(final Object value, final int row, final int column) {
		super(null, row, column);

		this.value = value;
	}

	@Override
	public String toString() {

		if (value != null) {
			return value.toString();
		}

		return "null";
	}

	public Object getValue() {
		return value;
	}

	@Override
	public Object evaluate(final ActionContext ctx, final GraphObject entity, final EvaluationHints hints) throws FrameworkException {
		return value;
	}

	@Override
	public Object transform(final ActionContext ctx, final GraphObject entity, final Object source, final EvaluationHints hints) throws FrameworkException {
		return source;
	}

	public void setQuoteChar(final String quoteChar) {
		this.quoteChar = quoteChar;
	}

	public String getQuoteChar() {
		return quoteChar;
	}

}
