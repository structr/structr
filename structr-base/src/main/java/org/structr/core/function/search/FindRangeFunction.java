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

public class FindRangeFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "find.range";
	}

	@Override
	public String getDisplayName(boolean includeParameters) {
		return "predicate.range";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		Object rangeStart    = null;
		Object rangeEnd      = null;
		boolean includeStart = true;
		boolean includeEnd   = true;

		try {

			if (sources == null || sources.length < 2) {

				throw new IllegalArgumentException();
			}

			switch (sources.length) {

				case 4: includeEnd   = Boolean.valueOf(sources[3].toString());
				case 3: includeStart = Boolean.valueOf(sources[2].toString());
				case 2: rangeEnd     = sources[1];
				case 1: rangeStart   = sources[0];
				default: break;
			}

			return new RangePredicate(rangeStart, rangeEnd, includeStart, includeEnd);

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{ $.predicate.range(start, end) }}. Example: ${{ $.find('Event', { date: $.predicate.range('2018-12-31', '2019-01-01') }); }}"),
			Usage.structrScript("Usage: ${range(start, end)}. Example: ${find('Event', 'date', range('2018-12-31', '2019-01-01'))}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns a range predicate that can be used in find() function calls.";
	}

	@Override
	public String getLongDescription() {
		return """
		Returns a search predicate to specify value ranges, greater and less-than searches in [find()](53)  and [search()](109) functions. 
		The first two parameters represent the first and the last element of the desired query range. Both start and end of the range can be 
		set to `null` to allow the use of `range()` for `<`, `<=`. `>` and `>=` queries.
		There are two optional boolean parameters, `includeStart` and `includeEnd` that control whether the search range 
		should **include** or **exclude** the endpoints of the interval.
		""";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("key, value");
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${find('Project', 'taskCount', range(0, 10))}"),
				Example.structrScript("${find('Project', 'taskCount', range(0, 10, true, false))}"),
				Example.structrScript("${find('Project', 'taskCount', range(0, 10, false, false))}"),
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
				Parameter.mandatory("start", "start of range interval"),
				Parameter.mandatory("end", "end of range interval"),
				Parameter.optional("includeStart", "true to include startpoint of given interval - default: false"),
				Parameter.optional("includeEnd", "true to include endpoint of given interval - default: false")
				);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Database;
	}
}
