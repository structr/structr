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

public class FindOrFunction extends CoreFunction {

	@Override
	public String getName() {

		return "find.or";
	}

	@Override
	public String getReplacementHint() {

		return "predicate.or";
	}

	@Override
	public String getDisplayName(boolean includeParameters) {

		return "predicate.or";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final OrPredicate orPredicate = new OrPredicate();

		if (sources != null) {

			for (Object param : sources) {

				handleParameter(orPredicate, param);
			}

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return orPredicate;
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.javaScript("Usage: ${{ $.predicate.or(predicate, ...)}}"),
			Usage.structrScript("Usage: ${or(predicate, ...)}")
		);
	}

	@Override
	public String getShortDescription() {

		return "Combines the given predicates using logical OR, returning a predicate usable with find() and search().";
	}

	@Override
	public String getLongDescription() {

		return "The predicates can be passed as separate parameters, as lists, or as any mix of both, e.g. `or([a, b], c)`.";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
				Example.javaScript("""
				{
					let projects = $.find('Project', $.predicate.or(
						$.predicate.equals('status', 'FINISHED'),
						$.predicate.equals('dueDate', $.predicate.lt(new Date())),
					));
				}""", "Find projects that are finished or were already due.")
		);
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("predicates");
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("predicates", "one or more predicates or lists of predicates, in any combination - a node matches if it matches at least one of them")
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Predicate;
	}

	// ----- private methods -----
	private void handleParameter(final OrPredicate orPredicate, final Object param) {

		if (param instanceof SearchParameter searchParam) {

			orPredicate.addParameter(searchParam);

		} else if (param instanceof SearchFunctionPredicate searchFnPredicate) {

			orPredicate.addPredicate(searchFnPredicate);

		} else if (param instanceof List list) {

			for (final Object o : list) {

				handleParameter(orPredicate, o);
			}
		}
	}
}
