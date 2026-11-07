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
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class FindOverlapsFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {

		return "find.overlaps";
	}

	@Override
	public String getReplacementHint() {

		return "predicate.overlaps";
	}

	@Override
	public String getDisplayName(boolean includeParameters) {

		return "predicate.overlaps";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		String startKey      = null;
		String endKey        = null;
		Object rangeStart    = null;
		Object rangeEnd      = null;
		boolean includeStart = false;
		boolean includeEnd   = false;

		try {

			assertArrayHasMinLengthAndMaxLength(sources, 4, 6);

			if (sources[0] == null || sources[1] == null) {
				throw new FrameworkException(422, getDisplayName(false) + ": startKey and endKey must not be null");
			}

			switch (sources.length) {

				case 6: includeEnd   = Boolean.parseBoolean(String.valueOf(sources[5]));
				case 5: includeStart = Boolean.parseBoolean(String.valueOf(sources[4]));
				case 4: rangeEnd     = sources[3];
				case 3: rangeStart   = sources[2];
				case 2: endKey       = sources[1].toString();
				case 1: startKey     = sources[0].toString();
				default: break;
			}

			final AndPredicate andPredicate = new AndPredicate();

			if (rangeEnd != null) {

				final OrPredicate orPredicate = new OrPredicate();
				andPredicate.addPredicate(orPredicate);

				// includeEnd = true creates lte, otherwise lt
				orPredicate.addParameter(new SearchParameter(startKey, new RangePredicate(null, rangeEnd, false, includeEnd), true));

				// support open-ended intervals
				orPredicate.addParameter(new EmptyParameter(startKey));
			}

			if (rangeStart != null) {

				final OrPredicate orPredicate = new OrPredicate();
				andPredicate.addPredicate(orPredicate);

				// includeStart = true creates gte, otherwise gt
				orPredicate.addParameter(new SearchParameter(endKey, new RangePredicate(rangeStart, null, includeStart, false), true));

				// support open-ended intervals
				orPredicate.addParameter(new EmptyParameter(endKey));
			}

			return andPredicate;

		} catch (final IllegalArgumentException e) {

			throw new FrameworkException(422, getDisplayName(false) + ": " + e.getMessage());
		}
	}

	@Override
	public String getShortDescription() {

		return "Returns a query predicate that can be used with find() and search().";
	}

	@Override
	public String getLongDescription() {

		return """
				Returns a search predicate to find nodes whose interval overlaps a given search interval in find() and search() functions.
				An interval is defined by two properties of the same type on each node, for example `beginDate` and `endDate` for a time span
				or `minAmount` and `maxAmount` for a numeric range. The first two parameters are the names of these properties.
				The next two parameters represent the start and end of the search interval. A node matches if its interval and the search interval
				overlap in any way: the node starts inside the search interval, ends inside it, contains it completely, or lies completely inside it.

				Both start and end of the search interval can be set to `null` to create an open-ended search interval (e.g. everything from a given value onward).
				If both are `null`, the search interval is unbounded and every node matches.
				Note that the predicate then adds no condition at all, so wrapping it in `not()` also matches every node instead of none.
				Check for this case before negating if you need to exclude everything.

				A `null` value in a node's start or end property is also treated as open-ended, so nodes without an end value (e.g. ongoing tasks)
				match any search interval that ends after their start.

				By default, intervals that only **touch** do not overlap: a node ending exactly at the start of the search interval, or starting exactly
				at its end, does not match. Set the optional boolean parameters `includeStart` and `includeEnd` to `true` to include those nodes.
				Note that this also applies to zero-length intervals (start equals end): a zero-length node that sits exactly on a boundary of the search
				interval, or a zero-length search interval that sits exactly on a boundary of a node, only matches if the corresponding flag is set.
				""";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("startKey, endKey, start, end [, includeStart, includeEnd ]");
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
				Example.javaScript("""
						${{
							// find all other tasks in the same project whose time span overlaps this task
							let currentTask = $.this;

							let conflicts = $.find('Task', $.predicate.and(
								$.predicate.equals('project', currentTask.project),
								$.predicate.not($.predicate.equals('id', currentTask.id)),
								$.predicate.overlaps('beginDate', 'endDate', currentTask.beginDate, currentTask.endDate)
							));

						}}""", "Find tasks that overlap the current task")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("startKey", "property holding the start of each node's interval"),
				Parameter.mandatory("endKey", "property holding the end of each node's interval"),
				Parameter.mandatory("start", "start of search interval"),
				Parameter.mandatory("end", "end of search interval"),
				Parameter.optional("includeStart", "true to include nodes that end exactly at the start of the search interval - default: false"),
				Parameter.optional("includeEnd", "true to include nodes that start exactly at the end of the search interval - default: false")
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Predicate;
	}
}
