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
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class FindGtFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {

		return "find.gt";
	}

	@Override
	public String getReplacementHint() {

		return "predicate.gt";
	}

	@Override
	public String getDisplayName(boolean includeParameters) {

		return "predicate.gt";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2);

			if (sources.length == 2) {

				final String key      = sources[0].toString();
				final Object value    = new RangePredicate(sources[1], null, false, false);

				return new SearchParameter(key, value, true);
			}

			if (sources.length == 1) {

				return new RangePredicate(sources[0], null, false, false);
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
			Usage.javaScript("Usage: ${{ $.predicate.gt([ key, ] value) }}"),
			Usage.structrScript("Usage: ${gt([ key, ] value)}")
		);
	}

	@Override
	public String getShortDescription() {

		return "Returns a query predicate that can be used with find() and search().";
	}

	@Override
	public String getLongDescription() {

		return """
			Returns a search predicate to find nodes whose value is greater than the given value in find() and search() functions.
			The optional first parameter is the property key to search for, so `gt(key, value)` is equivalent to `equals(key, gt(value))`.
			""";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
				Example.javaScript("""
				{
					let projects1 = $.find('Project', $.predicate.gt('dueDate', new Date()));

					let projects2 = $.find('Project', $.predicate.equals('dueDate', $.predicate.gt(new Date())));

					let projects3 = $.find('Project', { dueDate: $.predicate.gt(new Date()) });
				}""", "Three equivalent ways to find projects whose dueDate is in the future")
		);
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("[ key, ] value");
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.optional("key", "property key to search for - can be omitted when used as the value of equals() or of a property in a map"),
				Parameter.mandatory("value", "lower bound (exclusive)")
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Predicate;
	}
}
