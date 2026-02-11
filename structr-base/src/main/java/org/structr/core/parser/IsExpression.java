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

public class IsExpression extends Expression {

	private static final String ERROR_MESSAGE_IS = "Usage: ${is(condition, trueValue)}. Example: ${is(equal(this.name, request.name), 'selected')}";

	public IsExpression(final int row, final int column) {
		super("is", row, column);
	}

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		buf.append("is(");

		for (final Expression expr : expressions) {
			buf.append(expr.toString());
		}
		buf.append(")");

		return buf.toString();
	}

	@Override
	public void add(final Expression expression) throws FrameworkException {

		if (expressions.size() == 2) {
			throw new FrameworkException(422, "Invalid is() expression in builtin function: too many parameters.");
		}

		super.add(expression);

	}

	@Override
	public Object evaluate(final ActionContext ctx, final GraphObject entity, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {

		if (expressions.isEmpty()) {
			return ERROR_MESSAGE_IS;
		}

		final Expression condition = expressions.get(0);

		if (isTrue(condition.evaluate(ctx, entity, hints))) {

			if (expressions.size() > 1) {

				final Expression trueExpression = expressions.get(1);
				return trueExpression.evaluate(ctx, entity, hints);

			} else {

				throw new FrameworkException(422, "Invalid is() expression in builtin function: missing trueExpression.");
			}

		} else {

			return null;

		}
	}

	private boolean isTrue(final Object source) {
		return source != null && (Boolean.TRUE.equals(source) || "true".equals(source));
	}

	@Override
	public Object transform(final ActionContext ctx, final GraphObject entity, final Object source, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {
		return source;
	}

	@Override
	public String getName() {
		return "is";
	}

	@Override
	public String getShortDescription() {
		return "Evaluates a condition and executes an expressions if the result is `true`.";
	}

	@Override
	public String getLongDescription() {
		return null;
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("condition", "condition to evaluate"),
			Parameter.mandatory("trueExpression", "expression to evaluate if condition is `true`")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${is(me.isAdmin, 'background-color-red')}", "Make the background color of an element red if the current user is an admin user")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"This function is only available in StructrScript.",
			"This function is often used in HTML attributes, for example to conditionally output CSS classes or other attributes.",
			"This function is essentially a shortcut for the 'if()` function that only evaluates the trueExpression and does nothing if the condition evaluates to `false`."
		);
	}

	@Override
	public List<Signature> getSignatures() {
		return List.of(
			Signature.structrScript("condition, trueExpression")
		);
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${is(condition, trueExpression)}. Example: ${is(me.isAdmin, 'background-color-red')}")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Logic;
	}
}
