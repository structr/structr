/*
 * Copyright (C) 2010-2026 Structr GmbH
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
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.docs.*;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.List;

/**
 *
 *
 */
public class FunctionValueExpression extends Expression {

	final private FunctionExpression functionExpression;
	final private ValueExpression    valueExpression;

	public FunctionValueExpression(final FunctionExpression functionExpression, final ValueExpression valueExpression, final int row, final int column) {

		super(row, column);
		
		this.functionExpression = functionExpression;
		this.valueExpression    = valueExpression;
	}

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		buf.append(functionExpression.toString()).append(valueExpression.toString());

		return buf.toString();
	}

	@Override
	public Object evaluate(final ActionContext ctx, final GraphObject entity, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {

		Object value = functionExpression.evaluate(ctx, entity, hints);

		value = valueExpression.transform(ctx, entity, value, hints);

		for (final Expression expression : valueExpression.expressions) {

			// evaluate expressions from left to right
			value = expression.transform(ctx, entity, value, hints);
		}

		return value;

	}

	@Override
	public Object transform(ActionContext ctx, GraphObject entity, Object source, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {
		return source;
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

	@Override
	public FunctionCategory getCategory() {
		return null;
	}
}
