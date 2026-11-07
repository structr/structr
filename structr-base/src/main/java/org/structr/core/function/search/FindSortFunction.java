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
import org.structr.core.function.SortKeyArguments;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.Arrays;
import java.util.List;

public class FindSortFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {

		return "find.sort";
	}

	@Override
	public String getReplacementHint() {

		return "predicate.sort";
	}

	@Override
	public String getDisplayName(boolean includeParameters) {

		return "predicate.sort";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		// the keys are checked against the type of the query, which is not known yet
		return new SortKeysPredicate(SortKeyArguments.parse(getName(), sources != null ? Arrays.asList(sources) : List.of()), ctx.getLocale());
	}

	@Override
	public String getShortDescription() {

		return "Returns a query predicate that can be used with find() and search().";
	}

	@Override
	public String getLongDescription() {

		return """
			This predicate sorts the results of a query by one or more sort keys, `name` by default. It takes the same sort keys as the `sort()` function, in any combination:

			* The name of a property, like `'name'`, sorted in ascending order.
			* The name followed by `true` or `false` for its order, like `'name', true` for descending order.
			* An object with the options `key`, `descending` (or `desc`), `natural` and `nulls`, like `{ key: 'title', natural: true }`. See the `sort()` function for a description of the options.

			With several sort keys, the first one decides, and each further key only decides between results that are equal in all keys before it. A query can also have several sort predicates, which work like one predicate with all of their keys, in the same order.

			A key can be a path through related nodes, like `owner.name`. If the path leads through a collection, like `projects.name`, a result can have several values. Ascending order then uses the smallest of them and descending order the largest, so a result in the projects "B" and "D" sorts as "B" in ascending and as "D" in descending order.

			If a key in a path exists neither on the type at that point of the path nor on one of its subtypes, the query fails with an error. The check ends at the first property that does not reference objects of a known type, like a function property or a string property: the keys behind it are not checked, and results without a value for them sort like results with a null key.

			The database sorts by keys that are properties of the results themselves. If one of the keys is a path, is sorted in natural order or places nulls first or last, Structr loads the complete result and sorts it before applying the page, which takes longer for large results.
			""";
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.optional("sortKey1, sortKey2, ...", "names of properties, each optionally followed by `true` or `false` for descending order, or objects with the options `key`, `descending` (or `desc`), `natural` and `nulls`. Default: `name`")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
				Example.javaScript("""
				{
					let projects = $.find('Project', $.predicate.sort('name'));
				}""", "Fetch the list of projects sorted by name"),
				Example.javaScript("""
				{
					let projects = $.find('Project', $.predicate.sort('budget', true));
				}""", "Fetch the list of projects sorted by budget in descending order"),
				Example.javaScript("""
				{
					let users = $.find('User', $.predicate.sort('lastName', 'firstName'));
				}""", "Fetch the list of users sorted by last name and, for the same last name, by first name"),
				Example.javaScript("""
				{
					let projects = $.find('Project', $.predicate.sort({ key: 'owner.name', desc: true }, 'name'));
				}""", "Fetch the list of projects sorted by owner name in descending order (transitive sorting), and projects of the same owner by name"),
				Example.javaScript("""
				{
					let documents = $.find('Document', $.predicate.sort({ key: 'title', natural: true }), $.predicate.page(1, 20));
				}""", "Fetch the first page of documents sorted by title in natural order, so that Chapter 2 comes before Chapter 10"),
				Example.javaScript("""
				{
					let tasks = $.find('Task', $.predicate.sort({ key: 'dueDate', nulls: 'first' }));
				}""", "Fetch the list of tasks sorted by due date, tasks without a due date first"),
				Example.structrScript("${find('User', sort('lastName', false, 'firstName'))}", "Sort by last and first name in StructrScript"),
				Example.structrScript("${find('Document', sort(fromJson('{\"key\": \"title\", \"natural\": true}')))}", "Natural order in StructrScript")
		);
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("[ sortKey1 [, sortKey2 ... ]]");
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Predicate;
	}
}
