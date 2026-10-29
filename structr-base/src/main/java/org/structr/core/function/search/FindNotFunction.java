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
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class FindNotFunction extends CoreFunction {

	@Override
	public String getName() {

		return "find.not";
	}

	@Override
	public String getReplacementHint() {

		return "predicate.not";
	}

	@Override
	public String getDisplayName(boolean includeParameters) {

		return "predicate.not";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final NotPredicate notPredicate = new NotPredicate();

		if (sources != null) {

			for (Object param : sources) {

				handleParameter(notPredicate, param);
			}

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return notPredicate;
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.javaScript("Usage: ${{ $.predicate.not(predicate, ...) }}"),
			Usage.structrScript("Usage: ${not(predicate, ...)}")
		);
	}

	@Override
	public String getShortDescription() {

		return "Returns a query predicate that can be used with find() and search().";
	}

	@Override
	public String getLongDescription() {

		return """
			Returns a search predicate that negates the given predicates in find() and search() functions.
			With a single predicate, it matches all nodes that do not match that predicate.
			With several predicates, it matches all nodes that match **none** of them, so `not(a, b)` is equivalent to `and(not(a), not(b))`.
			To exclude only the nodes that match all of the predicates at the same time, combine them with `and()` first: `not(and(a, b))`.

			The predicates can be passed as separate parameters, as lists, or as any mix of both, e.g. `not([a, b], c)`.
			""";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
				Example.structrScript("${find('Group', not(equals('name', 'Test')))}", "Find all groups that are not named 'Test'"),
				Example.javaScript("""
				{
					let projects1 = $.find('Project', $.predicate.not($.predicate.equals('status', 'archived'), $.predicate.equals('status', 'cancelled')));

					let projects2 = $.find('Project', $.predicate.and($.predicate.not($.predicate.equals('status', 'archived')), $.predicate.not($.predicate.equals('status', 'cancelled'))));

				}""", "Two equivalent ways to find projects whose status is neither 'archived' nor 'cancelled'")
		);
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("predicates");
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("predicates", "one or more predicates or lists of predicates, in any combination - a node matches if it matches none of them")
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Predicate;
	}

	// ----- private methods -----
	private void handleParameter(final NotPredicate notPredicate, final Object param) {

		if (param instanceof SearchParameter searchParam) {

			notPredicate.addParameter(searchParam);

		} else if (param instanceof SearchFunctionPredicate searchFnPredicate) {

			notPredicate.addPredicate(searchFnPredicate);

		} else if (param instanceof List list) {

			for (final Object o : list) {

				handleParameter(notPredicate, o);
			}
		}
	}
}
