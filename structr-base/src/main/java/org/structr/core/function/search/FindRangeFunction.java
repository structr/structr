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

import java.util.Arrays;
import java.util.List;

public class FindRangeFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {

		return "find.range";
	}

	@Override
	public String getReplacementHint() {

		return "predicate.range";
	}

	@Override
	public String getDisplayName(boolean includeParameters) {

		return "predicate.range";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndMaxLength(sources, 2, 5);

			// The flags always follow the end value, so position 2 holds a flag in range(start, end, includeStart [, includeEnd ])
			// and the end value in range(key, start, end [, includeStart [, includeEnd ]]). We can not decide by the type of the
			// first parameter alone because start and end can be strings as well.
			final boolean hasKey = sources.length == 5 || (sources.length >= 3 && sources[0] instanceof String && !(sources[2] instanceof Boolean));
			if (hasKey) {

				if (sources[0] == null) {
					throw new FrameworkException(422, getName() + "(): key must not be null");
				}

				final String key   = sources[0].toString();
				final Object value = createRangePredicate(Arrays.copyOfRange(sources, 1, sources.length));

				return new SearchParameter(key, value, true);
			}

			return createRangePredicate(sources);

		} catch (final IllegalArgumentException e) {

			throw new FrameworkException(422, getName() + "(): " + e.getMessage());
		}
	}

	private RangePredicate createRangePredicate(final Object[] sources) {

		Object rangeStart    = null;
		Object rangeEnd      = null;
		boolean includeStart = true;
		boolean includeEnd   = true;

		switch (sources.length) {

			case 4: includeEnd   = Boolean.parseBoolean(String.valueOf(sources[3]));
			case 3: includeStart = Boolean.parseBoolean(String.valueOf(sources[2]));
			case 2: rangeEnd     = sources[1];
			case 1: rangeStart   = sources[0];
			default: break;
		}

		return new RangePredicate(rangeStart, rangeEnd, includeStart, includeEnd);
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.javaScript("Usage: ${{ $.predicate.range([ key, ] start, end [, includeStart, includeEnd ]) }}"),
			Usage.structrScript("Usage: ${range([ key, ] start, end [, includeStart, includeEnd ])}")
		);
	}

	@Override
	public String getShortDescription() {

		return "Returns a query predicate that can be used with find() and search().";
	}

	@Override
	public String getLongDescription() {

		return """
			Returns a search predicate to specify value ranges, greater and less-than searches in find() and search() functions.
			The optional first parameter is the property key to search for, so `range(key, start, end)` is equivalent to `equals(key, range(start, end))`.
			The parameters `start` and `end` represent the first and the last element of the desired query range. Both start and end of the range can be
			set to `null` to allow the use of `range()` for `<`, `<=`. `>` and `>=` queries, although there are dedicated predicate functions for that (lt, lte, gt, gte).

			The two optional boolean parameters, `includeStart` and `includeEnd` control whether the search range should **include** or **exclude** the endpoints of the interval.
			""";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("[ key, ] start, end [, includeStart, includeEnd ]");
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
				Example.structrScript("${find('Project', 'taskCount', range(0, 10))}"),
				Example.structrScript("${find('Project', 'taskCount', range(0, 10, true, false))}"),
				Example.structrScript("${find('Project', 'taskCount', range(0, 10, false, false))}"),
				Example.structrScript("${find('Project', range('taskCount', 0, 10))}"),
				Example.javaScript("""
				{
					let projects1 = $.find('Project', $.predicate.range('taskCount', 0, 10));

					let projects2 = $.find('Project', $.predicate.equals('taskCount', $.predicate.range(0, 10)));

					let projects3 = $.find('Project', { taskCount: $.predicate.range(0, 10) });
				}""", "Three equivalent ways to find projects with 0 to 10 tasks"),
				Example.javaScript("""
						${{ $.find('Project').forEach(p => $.print(p.index + ", ")) }}
						> 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
						"""),
				Example.javaScript("""
						${{ $.find('Project', { index: $.predicate.range(2, 5) }).forEach(p => $.print(p.index + ", ")); }}
						> 2, 3, 4, 5,
						"""),
				Example.javaScript("""
						${{ $.find('Project', { index: $.predicate.range(2, 5, false, false) }).forEach(p => $.print(p.index + ", ")); }}
						> 3, 4,
						"""),
				Example.javaScript("""
						${{ $.find('Article', 'createdDate', $.predicate.range(
									$.parseDate('01.12.2024 00:00', 'dd.MM.yyyy hh:mm'),
									$.parseDate('03.12.2024 23:59', 'dd.MM.yyyy hh:mm')
							)
						); }}
						""", "Range on datetime attribute")

		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.optional("key", "property key to search for - can be omitted when used as the value of equals() or of a property in a map"),
				Parameter.mandatory("start", "start of search interval"),
				Parameter.mandatory("end", "end of search interval"),
				Parameter.optional("includeStart", "true to include startpoint of given interval - default: true"),
				Parameter.optional("includeEnd", "true to include endpoint of given interval - default: true")
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Predicate;
	}
}
