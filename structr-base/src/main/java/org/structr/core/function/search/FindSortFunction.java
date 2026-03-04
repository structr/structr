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

public class FindSortFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "find.sort";
	}

	@Override
	public String getDisplayName(boolean includeParameters) {
		return "predicate.sort";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		// use String here because the actual type of the query is not known yet
		String sortKey         = "name";
		boolean sortDescending = false;

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			switch (sources.length) {

				case 2: sortDescending = "true".equals(sources[1].toString().toLowerCase()); // no break here
				case 1: sortKey        = sources[0].toString();
			}

			if (sortKey.contains(".")) {

				return new SortPathPredicate(sortKey, sortDescending);
			}

			return new SortPredicate(sortKey, sortDescending);

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{ $.predicate.sort(key [, descending]) }}. Example: ${{ $.find('Group', $.predicate.sort('name')) }}"),
			Usage.structrScript("Usage: ${sort(key [, descending]). Example: ${find('Group', sort('name'))}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns a query predicate that can be used with find() or search().";
	}

	@Override
	public String getLongDescription() {
		return """
			This predicate sorts the results of a query by the given key. It supports "transitive sorting", i.e. you can sort nodes by properties of related nodes.

			Default is ascending order; items with a null key sort last. If the parameter `descending` is true, the sort order is descending and items with a null key sort first.
			""";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("key", "name of the sort key"),
				Parameter.optional("descending", "If true, sorts in descending order; otherwise sorts in ascending order. (boolean, default `false`)")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.javaScript("""
				{
					let projects = $.find('Project', $.predicate.sort('name'));
				}""", "Fetch the list of projects sorted name"),
				Example.javaScript("""
				{
					let projects = $.find('Project', $.predicate.sort('owner.name'));
				}""", "Fetch the list of projects sorted by owner name (transitive sorting)"),
				Example.javaScript("""
				{
					let projects = $.find('Project', $.predicate.sort('budget', true));
				}""", "Fetch the list of projects sorted by budget in descending order")
		);
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("key [, descending = false]");
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Predicate;
	}
}
