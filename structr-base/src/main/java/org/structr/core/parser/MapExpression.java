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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 *
 *
 */

public class MapExpression extends Expression {

	private static final String ERROR_MESSAGE_MAP = "Usage: ${map(list, expression)}. Example: ${map(this.children, data.name)}";

	private Expression listExpression = null;
	private Expression mapExpression  = null;

	public MapExpression(final int row, final int column) {
		super("map", row, column);
	}

	@Override
	public void add(final Expression expression) throws FrameworkException {

		// first expression must yield a List
		if (this.listExpression == null) {

			this.listExpression = expression;

		} else if (this.mapExpression == null) {

			this.mapExpression = expression;

		} else {

			throw new FrameworkException(422, "Invalid map() expression in builtin function: too many parameters.");
		}

		expression.parent = this;
		expression.level  = this.level + 1;
	}

	@Override
	public Object evaluate(final ActionContext ctx, final GraphObject entity, final EvaluationHints hints) throws FrameworkException, UnlicensedScriptException {

		if (listExpression == null || mapExpression == null) {
			return ERROR_MESSAGE_MAP;
		}

		final List target = new LinkedList<>();
		Object listSource = listExpression.evaluate(ctx, entity, hints);

		if (listSource != null && listSource.getClass().isArray()) {
			listSource = Arrays.asList((Object[]) listSource);
		}

		if (listSource != null && listSource instanceof Iterable) {

			final Iterable source     = (Iterable)listSource;
			final Object oldDataValue = ctx.getConstant("data");

			for (Object obj : source) {

				ctx.setConstant("data", obj);

				target.add(mapExpression.evaluate(ctx, entity, hints));
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
		return "map";
	}

	@Override
	public String getShortDescription() {
		return "Transforms a list using a transformation expression.";
	}

	@Override
	public String getLongDescription() {
		return "This function evaluates the transformationExpression for each element of the list and returns the list of transformed values. Inside the expression function, the keyword `data` refers to the current element. See also: `each()` and `filter()`.";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("list", "list of elements to loop over"),
			Parameter.mandatory("transformationExpression", "transformation expression that is applied to each element")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${map(find('User'), data.name)}", "Return only the names of all users")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"This function is only available in StructrScript because there is a native language feature in JavaScript that does the same (`Array.prototype.map()`).",
			"The collection can also be a list of strings or numbers (see example 2)."
		);
	}

	@Override
	public List<Signature> getSignatures() {
		return List.of(
			Signature.structrScript("list, transformationExpression")
		);
	}

	@Override
	public List<Language> getLanguages() {
		return List.of(Language.StructrScript);
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${map(list, transformationExpression)}. Example: ${map(user.groups, data.name)}")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Collection;
	}
}
