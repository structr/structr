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
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.EvaluationHints;

import java.util.List;

/**
 *
 *
 */

public class IfExpression extends Expression {

	private static final String ERROR_MESSAGE_IF = "Usage: ${if(condition, trueValue, falseValue)}. Example: ${if(empty(this.name), this.nickName, this.name)}";

	public IfExpression(final int row, final int column) {
		super("if", row, column);
	}

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		buf.append("if(");

		for (final Expression expr : expressions) {
			buf.append(expr.toString());
		}
		buf.append(")");

		return buf.toString();
	}

	@Override
	public void add(final Expression expression) throws FrameworkException {

		if (expressions.size() == 3) {
			throw new FrameworkException(422, "Invalid if() expression in builtin function: too many parameters.");
		}

		super.add(expression);

	}

	@Override
	public Object evaluate(final ActionContext ctx, final GraphObject entity, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {

		if (expressions.isEmpty()) {
			return ERROR_MESSAGE_IF;
		}

		final Expression condition = expressions.get(0);

		if (isTrue(condition.evaluate(ctx, entity, hints))) {

			if (expressions.size() > 1) {

				final Expression trueExpression = expressions.get(1);
				return trueExpression.evaluate(ctx, entity, hints);

			} else {

				throw new FrameworkException(422, "Invalid if() expression in builtin function: missing trueExpression.");
			}

		} else {

			if (expressions.size() > 2) {

				final Expression falseExpression = expressions.get(2);
				return falseExpression.evaluate(ctx, entity, hints);

			} else {

				throw new FrameworkException(422, "Invalid if() expression in builtin function: missing falseExpression.");
			}
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
		return "if";
	}

	@Override
	public String getShortDescription() {
		return "Evaluates a condition and executes different expressions depending on the result.";
	}

	@Override
	public String getLongDescription() {
		return null;
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("condition", "condition to evaluate"),
			Parameter.mandatory("trueExpression", "expression to evaluate if condition is `true`"),
			Parameter.mandatory("falseExpression", "expression to evaluate if condition is `false`")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${if(me.isAdmin, 'background-color-red', 'background-color-white')}", "Make the background color of an element red if the current user is an admin user"),
			Example.structrScript("${if(me.isAdmin, 'You have admin rights.', 'You do not have admin rights.')}", "Display different strings depending on the status of a user")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"This function is only available in StructrScript.",
			"This function is often used in HTML attributes, for example to conditionally output CSS classes etc.",
			"The `is()` function is a shortcut for `if(condition, trueExpression, null)`."
		);
	}

	@Override
	public List<Signature> getSignatures() {
		return List.of(
			Signature.structrScript("condition, trueExpression, falseExpression")
		);
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${if(condition, trueExpression, falseExpression)}. Example: ${if(me.isAdmin, 'background-color-red', 'background-color-white')}")
		);
	}
}
