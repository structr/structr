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
import org.structr.core.function.AdvancedScriptingFunction;
import org.structr.docs.Example;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class FindLtFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {

		return "find.lt";
	}

	@Override
	public String getReplacementHint() {

		return "predicate.lt";
	}

	@Override
	public String getDisplayName(boolean includeParameters) {

		return "predicate.lt";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			if (sources.length == 2) {

				final String key      = sources[0].toString();
				final Object value    = new RangePredicate(null, sources[1], false, false);

				return new SearchParameter(key, value, true);
			}

			if (sources.length == 1) {

				return new RangePredicate(null, sources[0], false, false);
			}

			throw new FrameworkException(422, getName() + "(): Unsupported number of arguments.");

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			throw new FrameworkException(422, getName() + "(): " + e.getMessage());
		}
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.javaScript("Usage: ${{ $.predicate.lt(value)}}"),
			Usage.structrScript("Usage: ${lt(value)}")
		);
	}

	@Override
	public String getShortDescription() {

		return "Returns a query predicate that can be used with find() and search().";
	}

	@Override
	public String getLongDescription() {

		return "";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
				Example.javaScript("""
				{
					let projects1 = $.find('Project', $.predicate.lt('budget', 1000000));

					let projects2 = $.find('Project', $.predicate.equals('budget', $.predicate.lt(1000000)));

					let projects3 = $.find('Project', { budget: $.predicate.lt(1000000) });

				}""", "Three equivalent ways to find projects whose budget is less than a million")
		);
	}

	@Override
	public List<Signature> getSignatures() {

		return List.of(
				Signature.structrScript("key, value"),
				Signature.structrScript("value")
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Predicate;

	}
}
