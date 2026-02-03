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

import java.util.LinkedList;
import java.util.List;

/**
 *
 *
 */

public class FilterExpression extends Expression {

	private static final String ERROR_MESSAGE_FILTER = "Usage: ${filter(list, expression)}. Example: ${filter(this.children, gt(size(data.children), 0))}";

	private Expression listExpression   = null;
	private Expression filterExpression = null;

	public FilterExpression(final int row, final int column) {
		super("filter", row, column);
	}

	@Override
	public void add(final Expression expression) throws FrameworkException {

		// first expression must yield a List
		if (this.listExpression == null) {

			this.listExpression = expression;

		} else if (this.filterExpression == null) {

			this.filterExpression = expression;

		} else {

			throw new FrameworkException(422, "Invalid filter() expression in builtin function: too many parameters.");
		}

		expression.parent = this;
		expression.level  = this.level + 1;
	}

	@Override
	public Object evaluate(final ActionContext ctx, final GraphObject entity, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {

		if (listExpression == null || filterExpression == null) {
			return ERROR_MESSAGE_FILTER;
		}

		final Object listSource = listExpression.evaluate(ctx, entity, hints);
		final List target       = new LinkedList<>();

		if (listSource != null && listSource instanceof Iterable) {

			final Iterable source     = (Iterable)listSource;
			final Object oldDataValue = ctx.getConstant("data");

			for (Object obj : source) {

				ctx.setConstant("data", obj);
				final Object result = filterExpression.evaluate(ctx, entity, hints);
				if (result instanceof Boolean) {

					if ((Boolean)result) {

						target.add(obj);
					}
				}
			}

			ctx.setConstant("data", oldDataValue);
		}

		return target;
	}

	@Override
	public Object transform(final ActionContext ctx, final GraphObject entity, final Object source, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {
		return source;
	}

	@Override
	public String getName() {
		return "filter";
	}

	@Override
	public String getShortDescription() {
		return "Filters a list using a StructrScript expression.";
	}

	@Override
	public String getLongDescription() {
		return "The function will remove any object from the list for which the filter expression returns **false**, and return the filtered list. The filter expression can be any valid expression that returns a boolean value. Inside the expression function, the keyword `data` refers to the current element.";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("list", "list of elements to loop over"),
			Parameter.mandatory("filterExpression", "expression to evaluate for each element")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${filter(find('User'), not(data.isAdmin))}", "Remove admin users from a list of users")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"This function is only available in StructrScript because there is a native language feature in JavaScript that does the same (`Array.prototype.filter()`)."
		);
	}

	@Override
	public List<Signature> getSignatures() {
		return List.of(
			Signature.structrScript("list, filterExpression")
		);
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${filter(list, filterExpression)}. Example: ${filter(find('User'), data.isAdmin)}")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Collection;
	}
}
