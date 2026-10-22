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
package org.structr.core.function;

import org.structr.api.util.Iterables;
import org.structr.common.SortKeysComparator;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.function.SortKeyArguments.SortKey;
import org.structr.core.function.search.SortKeysPredicate;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SortFunction extends CoreFunction {

	@Override
	public String getName() {

		return "sort";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("collection [, sortKey1 [, sortKey2 ... ]]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources == null || sources.length == 0) {

			return null;
		}

		if (!(sources[0] instanceof Iterable)) {

			return sources[0];
		}

		final List list = Iterables.toList((Iterable)sources[0]);

		// an empty collection, or one of nulls only, has nothing to sort
		if (!hasElements(list)) {

			return sources[0];
		}

		final List<SortKey> keys            = SortKeyArguments.parse(getName(), Arrays.asList(sources).subList(1, sources.length));
		final SortKeysComparator comparator = new SortKeysComparator(ctx);

		if (containsOnly(list, GraphObject.class)) {

			final List<GraphObject> sortCollection = (List<GraphObject>)list;

			for (final SortKey spec : keys) {

				if (spec.key().contains(".")) {

					assertPathIsKnown(sortCollection, spec.key());

					comparator.addPath(spec.key(), spec.descending(), spec.natural(), spec.nulls());

				} else {

					comparator.addKey(resolveSortKey(sortCollection, spec.key()), spec.descending(), spec.natural(), spec.nulls());
				}
			}

			sortCollection.sort(comparator);

			return sortCollection;
		}

		if (containsOnly(list, String.class)) {

			// strings have no keys, so only the order, natural order and position of nulls of the first sort key apply
			final SortKey order = keys.get(0);

			list.sort(comparator.forValues(order.descending(), order.natural(), order.nulls()));

			return list;
		}

		logger.error("{}(): Only collections of nodes or strings are supported. Returning input as-is. Caller: {}, class: {}", getName(), caller, sources[0].getClass().getSimpleName());
		logger.debug("{}(): Parameters: {}", getName(), getParametersAsString(sources));

		return sources[0];
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
				Usage.structrScript("Usage: ${sort(collection [, sortKey1 [, sortKey2 ... ]])}."),
				Usage.javaScript("Usage: ${{ $.sort(collection [, sortKey1 [, sortKey2 ... ]]) }}.")
		);
	}

	@Override
	public String getShortDescription() {

		return "Sorts a collection and returns a new sorted collection.";
	}

	@Override
	public String getLongDescription() {

		return """
			Supported collection types:

			* **Collection of nodes**: Sorted by one or more sort keys, `name` by default. A sort key that none of the nodes has raises an error.
			* **Collection of strings**: Sorted as text. A key does not apply, only the order, natural order and position of nulls of the first sort key.

			Each sort key can be given in one of these forms, and the forms can be combined freely:

			* The name of a property, like `'lastName'`, sorted in ascending order.
			* The name followed by its order: `true` or `'desc'` for descending, `false` or `'asc'` for ascending, like `'lastName', 'desc'`.
			* An object with the following options, all of them optional:

			| Option | Description |
			| --- | --- |
			| `key` | Name of the property to sort by, or a path through related nodes. Default: `name` |
			| `descending` or `desc` | If `true`, sorts in descending order. Default: `false` |
			| `natural` | If `true`, strings are sorted in natural order: numbers by their value, so `Item 2` comes before `Item 10`, upper and lower case letters together, and accented letters according to the language of the request. Default: `false`, which sorts by character codes, so all upper case letters come before all lower case letters. |
			| `nulls` | `first` or `last` places nodes without a value, and `null` elements of a collection, at the beginning or the end, regardless of the order. Without this option, they come last in ascending and first in descending order. |

			With several sort keys, the first one decides, and each further key only decides between nodes that are equal in all keys before it. For example, `sort(list, 'lastName', 'desc', { key: 'firstName', natural: true })` sorts by last name in descending order, and nodes with the same last name by first name in natural order.

			An order without a name before it belongs to the default key, so `sort(list, true)` sorts by name in descending order.

			A key can also be a path through related nodes, like `owner.name`. If the path leads through a collection, like `projects.name`, a node can have several values. Ascending order then uses the smallest of them and descending order the largest. A path with a key that does not exist raises an error, like an unknown sort key.

			In StructrScript, you can create a sort key object with `fromJson()`, for example `fromJson('{"key": "title", "natural": true}')`.

			To sort the result of a query, use the sort predicate of `find()` instead, which takes the same sort keys and lets the database sort where possible.
			""";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
				Example.javaScript("${{ $.sort($.this.projects) }}", "Sort the projects of the current object by name"),
				Example.javaScript("${{ $.sort($.this.members, 'lastName', 'firstName') }}", "Sort members by last name and, for the same last name, by first name"),
				Example.javaScript("${{ $.sort($.this.projects, 'owner.name', 'desc', 'name') }}", "Sort projects by the name of their owner in descending order, and projects of the same owner by name"),
				Example.javaScript("${{ $.sort($.this.documents, { key: 'title', natural: true }) }}", "Sort documents by title in natural order, so that Chapter 2 comes before Chapter 10"),
				Example.javaScript("${{ $.sort($.this.tasks, { key: 'dueDate', nulls: 'first' }, 'name') }}", "Sort tasks by due date, tasks without a due date first, and tasks of the same date by name"),
				Example.javaScript("${{ $.sort(['b', 'A', 'a', 'B'], { natural: true, desc: true }) }}", "Sort strings in natural order, descending"),
				Example.structrScript("${extract(sort(this.members, 'lastName', 'firstName'), 'name')}", "Sort members by last and first name"),
				Example.structrScript("${extract(sort(this.documents, fromJson('{\"key\": \"title\", \"natural\": true}')), 'name')}", "Natural order in StructrScript")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("collection", "collection to be sorted"),
				Parameter.optional("sortKey1, sortKey2, ...", "names of properties, each optionally followed by its order, or objects with the options `key`, `descending` (or `desc`), `natural` and `nulls`, see above. Default: `name`")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
				"A sort key that is a path is resolved for each node of the collection and traverses its relationships, which takes noticeably longer than a plain property for large collections. Prefer the sort predicate of `find()` where the database can sort, or sort by a property of the node itself.",
				"The `sort()` and `find()` functions are often used in repeater elements in a function query, see Repeater Elements.",
				"Do not use JavaScript built-in function `sort` on remote collections for a node because it mutates the collection which leads to database writes and can lead to undesired effects."
		);
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Collection;
	}

	// ----- private methods -----
	private boolean hasElements(final List list) {

		for (final Object element : list) {

			if (element != null) {

				return true;
			}
		}

		return false;
	}

	// null elements have no value and are allowed in any collection
	private boolean containsOnly(final List list, final Class<?> type) {

		for (final Object element : list) {

			if (element != null && !type.isInstance(element)) {

				return false;
			}
		}

		return true;
	}

	/**
	 * Rejects a path with an unknown key, like find() does. Only the elements with a type of the schema
	 * can be checked, maps created from JSON or by a query have none.
	 */
	private void assertPathIsKnown(final List<GraphObject> collection, final String path) throws FrameworkException {

		final Set<String> types = new LinkedHashSet<>();

		for (final GraphObject obj : collection) {

			if (obj != null && obj.getType() != null && Traits.exists(obj.getType())) {

				types.add(obj.getType());
			}
		}

		if (!types.isEmpty()) {

			SortKeysPredicate.assertPathIsKnown(types, path);
		}
	}

	private Set<String> getTypes(final List<GraphObject> collection) {

		final Set<String> types = new LinkedHashSet<>();

		for (final GraphObject obj : collection) {

			if (obj != null && obj.getType() != null) {

				types.add(obj.getType());
			}
		}

		return types;
	}

	/**
	 * Resolves the sort key from the first element that has it, so a key that only some elements of a list with
	 * mixed types have still counts as known. Rejects a key that no element has, like the sort predicate of find() does,
	 * instead of returning the collection unsorted.
	 */
	private PropertyKey resolveSortKey(final List<GraphObject> collection, final String name) throws FrameworkException {

		for (final GraphObject obj : collection) {

			if (obj != null && obj.getTraits().hasKey(name)) {

				return obj.getTraits().key(name);
			}
		}

		throw new FrameworkException(422, "Unknown sort key '" + name + "' for type(s) " + String.join(", ", getTypes(collection)));
	}
}
