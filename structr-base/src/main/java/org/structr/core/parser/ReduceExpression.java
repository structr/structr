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
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.docs.*;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.Arrays;
import java.util.List;

/**
 *
 *
 */

public class ReduceExpression extends Expression {

	private static final String ERROR_MESSAGE_MAP = "Usage: ${reduce(list, initialValue, reduceExpression)}. Example: ${reduce(this.children, 0, sum(accumulator, data.value)))}";

	private Expression listExpression         = null;
	private Expression initialValueExpression = null;
	private Expression reduceExpression       = null;

	public ReduceExpression(final int row, final int column) {
		super("reduce", row, column);
	}

	@Override
	public void add(final Expression expression) throws FrameworkException {

		// first expression must yield a List
		if (this.listExpression == null) {

			this.listExpression = expression;

		} else if (this.initialValueExpression == null) {

			this.initialValueExpression = expression;

		} else if (this.reduceExpression == null) {

			this.reduceExpression = expression;

		} else {

			throw new FrameworkException(422, "Invalid reduce() expression in builtin function: too many parameters.");
		}

		expression.parent = this;
		expression.level  = this.level + 1;
	}

	@Override
	public Object evaluate(final ActionContext ctx, final GraphObject entity, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {

		if (listExpression == null || initialValueExpression == null || reduceExpression == null) {
			return ERROR_MESSAGE_MAP;
		}

		Object accumulator = initialValueExpression.evaluate(ctx, entity, hints);
		Object listSource = listExpression.evaluate(ctx, entity, hints);

		if (listSource != null && listSource.getClass().isArray()) {
			listSource = Arrays.asList((Object[]) listSource);
		}

		if (listSource != null && listSource instanceof Iterable && accumulator != null) {

			final Iterable source     = (Iterable)listSource;
			final Object oldAccValue  = ctx.getConstant("accumulator");
			final Object oldDataValue = ctx.getConstant("data");

			for (Object obj : source) {

				ctx.setConstant("accumulator", accumulator);
				ctx.setConstant("data", obj);

				accumulator = reduceExpression.evaluate(ctx, entity, hints);
			}

			ctx.setConstant("accumulator", oldAccValue);
			ctx.setConstant("data",        oldDataValue);
		}

		return accumulator;
	}

	@Override
	public Object transform(final ActionContext ctx, final GraphObject entity, final Object source, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {
		return source;
	}

	@Override
	public String getName() {
		return "map";
	}

	@Override
	public String getShortDescription() {
		return "Returns a single result from all elements of a list by applying a reduction function.";
	}

	@Override
	public String getLongDescription() {
		return "This function evaluates the reductionExpression for each element of the list and returns a single value. Inside the reduction expression, the keyword `accumulator` refers to the result of the previous reduction, and `data` refers to the current element. See also: `map()`, `each()` and `filter()`.";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("list", "list of elements to loop over"),
			Parameter.mandatory("initialValue", "expression that creates the initial value, e.g. 0"),
			Parameter.mandatory("reductionExpression", "reduce expression that gets `accumulator` and `data` to reduce the two to a single value")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${reduce(merge(1, 2, 3, 4), 0, add(accumulator, data))}", "Add")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"This function is only available in StructrScript because there is a native language feature in JavaScript that does the same (`Array.prototype.reduce()`).",
			"The collection can also be a list of strings or numbers (see example 2)."
		);
	}

	@Override
	public List<Signature> getSignatures() {
		return List.of(
			Signature.structrScript("list, initialValue, reductionExpression")
		);
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${reduce(list, initialValue, reduceExpression)}. Example: ${reduce(this.children, 0, sum(accumulator, data.value)))}")
		);
	}
}
