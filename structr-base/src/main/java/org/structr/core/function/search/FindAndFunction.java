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
package org.structr.core.function.search;

import org.structr.common.error.FrameworkException;
import org.structr.core.function.CoreFunction;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class FindAndFunction extends CoreFunction {

	@Override
	public String getName() {
		return "find.and";
	}

	@Override
	public String getDisplayName(boolean includeParameters) {
		return "predicate.and";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final AndPredicate andPredicate = new AndPredicate();

		if (sources != null) {

			for (Object param : sources) {

				handleParameter(andPredicate, param);
			}

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return andPredicate;
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{ $.predicate.and(predicates...) }}. Example: ${{ $.find('Group', $.predicate.and($.predicate.equals('name', 'Test'))) }}"),
			Usage.structrScript("Usage: ${and(predicate, ...). Example: ${find('Group', and(equals('name', 'Test')))}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns a query predicate that can be used with find() or search().";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("predicates");
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Predicate;
	}

	// ----- private methods -----
	private void handleParameter(final AndPredicate andPredicate, final Object param) {

		if (param instanceof SearchParameter) {

			andPredicate.addParameter((SearchParameter)param);

		} else if (param instanceof SearchFunctionPredicate) {

			andPredicate.addPredicate((SearchFunctionPredicate)param);

		} else if (param instanceof List) {

			for (final Object o : (List)param) {

				handleParameter(andPredicate, o);
			}
		}
	}
}
