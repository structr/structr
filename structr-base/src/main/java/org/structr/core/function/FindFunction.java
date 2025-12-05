/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.QueryGroup;
import org.structr.core.app.StructrApp;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class FindFunction extends AbstractQueryFunction {

	public static final String ERROR_MESSAGE_FIND_NO_TYPE_SPECIFIED = "Error in find(): no type specified.";
	public static final String ERROR_MESSAGE_FIND_TYPE_NOT_FOUND    = "Error in find(): type not found: ";

	@Override
	public String getNamespaceIdentifier() {
		return "find";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		final SecurityContext securityContext = ctx.getSecurityContext();

		try {

			if (sources == null) {

				throw new IllegalArgumentException();
			}

			final App app          = StructrApp.getInstance(securityContext);
			final QueryGroup query = app.nodeQuery().and();

			// the type to query for
			Traits type = null;

			if (sources.length >= 1 && sources[0] != null) {

				final String typeString = sources[0].toString();

				if (StructrTraits.GRAPH_OBJECT.equals(typeString)) {

					throw new FrameworkException(422, "Type GraphObject not supported in find(), please use type NodeInterface to search for nodes of all types.");
				}

				if (Traits.exists(typeString)) {

					type = Traits.of(typeString);

					query.types(type);

				} else {

					logger.warn("Error in find(): type '{}' not found.", typeString);
					return ERROR_MESSAGE_FIND_TYPE_NOT_FOUND + typeString;

				}
			}

			// exit gracefully instead of crashing..
			if (type == null) {
				logger.warn("Error in find(): no type specified. Parameters: {}", getParametersAsString(sources));
				return ERROR_MESSAGE_FIND_NO_TYPE_SPECIFIED;
			}

			// apply sorting and pagination by surrounding sort() and slice() expressions
			applyQueryParameters(securityContext, query);

			return handleQuerySources(securityContext, type, query, sources, true, usage(ctx.isJavaScriptContext()));

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());

		} finally {

			resetQueryParameters(securityContext);
		}
	}

	@Override
	public String getName() {
		return "find";
	}

	@Override
	public String getShortDescription() {
		return "Returns a collection of entities of the given type from the database.";
	}

	@Override
	public String getLongDescription() {
		return """
		This function is one of the most important and frequently used built-in functions. It returns a collection of entities, which can be empty if none of the existing nodes or relationships matches the given search parameters. `find()` accepts several different predicates (key, value pairs) and other query options like sort order or pagination controls. See the examples below for an overview of the possible parameter combinations for an advanced find() query.
		
		**Predicates**
		
		The following predicates can be specified. Predicates can be combined and mixed to build complex queries. Some  predicates and property keys need to be combined in a different way than others, please refer to the examples below for an overview.
		
		Please note that in JavaScript, the predicates need to be prefixed with `$.predicate` to avoid confusing them with built-in functions of the same name.
		
		|Predicate              |Description|
		|-----------------------|-----------|
		| `and(...)`            | Logical AND |
		| `or(...)`             | Logical OR |
		| `not(...)`            | Logical NOT |
		| `equals(key, value)`  | Returns only those nodes that match the given (key, value) pair |
		| `contains(key, text)` | Returns only those nodes whose value contains the given text |
		| `empty(key)`        | Returns only those nodes that don't have a value set for the given key |
		| `lt(upperBound)`    | Returns nodes where a key is less than `upperBound` [Only available in Structr version > 6.0] |
		| `lte(upperBound)`    | Returns nodes where a key is less than or equal to `upperBound`[Only available in Structr version > 6.0] |
		| `gte(lowerBound)`    | Returns nodes where a key is greater than or equal to `lowerBound` [Only available in Structr version > 6.0]|
		| `gt(lowerBound)`    | Returns nodes where a key is greater than `lowerBound` [Only available in Structr version > 6.0]|
		| `range(start, end [, withStart = true [, withEnd = true ]] )` | Returns only those nodes where the given propertyKey is in the range between start and end |
		| `range(null, end)`    | Unbounded `range()` to emulate "lower than or equal" |
		| `range(start, null)`  | Unbounded `range()` to emulate "greater than or equal" |
		| `startsWith(key, prefix)` | Returns only those nodes whose value for the given key starts with the given prefix |
		| `endsWith(key, suffix)` | Returns only those nodes whose value for the given key ends with the given suffix |
		| `withinDistance(latitude, longitude, distance)`  | Returns only those nodes that are within `distance` meters around the given coordinates. The type that is being searched for needs to extend the built-in type `Location` |
		
		**Options**
		
		|Option   |Description|
		|---------|-----------|
		|`sort(key)`|Sorts the result according to the given property key (ascending)|
		|`sort(key, true)`|Sorts the result according to the given property key (descending)|
		|`page(page, pageSize)`|Limits the result size to `pageSize`, returning the given `page`|
		
		""";
	}

	@Override
	public List<Signature> getSignatures() {

		return List.of(
			Signature.javaScript( "type, map"),
			Signature.structrScript( "type, key, value"),
			Signature.javaScript( "type, uuid"),
			Signature.structrScript( "type, uuid")
		);
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{ $.find(type, key, value); }}. Example: ${{ $.find(\"User\", { eMail: 'tester@test.com' }); }}"),
			Usage.structrScript("Usage: ${find(type, key, value)}. Example: ${find(\"User\", \"email\", \"tester@test.com\")}")
		);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("type", "type to return (includes inherited types)"),
			Parameter.optional("predicates", "list of predicates"),
			Parameter.optional("uuid", "uuid, makes the function return **a single object**")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(

			Example.structrScript("${find('User', 'b3175257898440ff99e78ca8fedfd832')}", "Return the User entity with the UUID `b3175257898440ff99e78ca8fedfd832`"),
			Example.javaScript("${{ $.find('User', 'b3175257898440ff99e78ca8fedfd832'); }}", "Return the User entity with the UUID `b3175257898440ff99e78ca8fedfd832`"),

			Example.structrScript("${find('User', sort('name'))}", "Return all User entities in the database, sorted by name"),
			Example.structrScript("${find('User', sort('name'), page(1, 10))}", "Return the first 10 User entities in the database, sorted by name"),
			Example.structrScript("${find('User', contains('name', 'e'))}", "Return all user entities whose name property contains the letter 'e' (showcases the different ways to use the contains predicate)"),
			Example.structrScript("${find('User', 'age', gte(18))}", "Return all user entities whose age property is greater than or equal to 18"),
			Example.structrScript("${find('User', 'age', range(0, 18))}", "Return all user entities whose age property is between 0 and 18 (inclusive!)"),
			Example.structrScript("${find('User', 'age', range(0, 18, false, false))}", "Return all user entities whose age property is between 0 and 18 (exclusive!)"),
			Example.structrScript("${find('User', equals('name', 'Tester'), equals('age', range(0, 18)))}", "Return all user entities whose name is 'Tester' and whose age is between 0 and 18 (inclusive). Showcases the implicit and explicit logical AND conjunction of root clauses"),
			Example.structrScript("${find('User', and(equals('name', 'Tester'), equals('age', range(0, 18))))}", "Return all user entities whose name is 'Tester' and whose age is between 0 and 18 (inclusive). Showcases the implicit and explicit logical AND conjunction of root clauses"),
			Example.structrScript("${find('BakeryLocation', and(withinDistance(48.858211, 2.294507, 1000)))}", "Return all bakeries (type BakeryLocation extends Location) within 1000 meters around the Eiffel Tower"),

			Example.javaScript("""
			${{
				$.find('User', $.predicate.sort('name'), $.predicate.page(1, 10))
			}}
			""", "Return the first 10 User entities in the database, sorted by name"),

			Example.javaScript("""
			${{
				// Note that the syntax is identical to StructrScript
				$.find('User', 'age', $.predicate.gte(21))
			}}
			""", "Returns all user entities whose `age` property is greater than or equal to 21."),

			Example.javaScript("""
			${{
				// In JavaScript, we can use a map as second parameter
				// to have a more concise and programming-friendly way
				// of query building
			
				$.find('User', {
					'age': $.predicate.gte(21)
				})
			}}
			""", "Return all user entities whose `age` property is greater than or equal to 21."),

			Example.javaScript("""
			${{
				// Instead of a map, we can use a predicate-only approach.
				$.find('User', $.predicate.equals('age', $.predicate.gte(21))
			}}
			""", "Returns all user entities whose `age` property is greater than or equal to 21."),

			Example.javaScript("""
			${{
				let projects = $.find(
					'Project',
					{
						$and: {
							'name': 'structr',
							'age': $.predicate.range(30, 50)
						}
					},
					$.predicate.sort('name', true),
					$.predicate.page(1, 10)
				);

				return users;
			}}
			""", "Return the first 10 projects (sorted descending by name) where `name` equals \"structr\" and `age` is between 30 and 50 (inclusive)"),

			Example.javaScript("""
			${{
				// Showcasing the *limitation* of the MAP SYNTAX: OR on the same property is not possible.
				let users = $.find(
					'User',
					{
						$or: {
							'name': 'jeff',
							'name': 'joe'
						}
					}
				);

				// Note: this returns the WRONG result!
				return users;
			}}
			""", "Only the user \"joe\" will be returned because the map key `name` is used twice (which overrides the first entry)"),

			Example.javaScript("""
			${{
				// Showcasing the *advantage* of the PREDICATE SYNTAX: OR on the same property is possible.
				let users = $.find(
					'User',
					$.predicate.or(
						$.predicate.equals('name', 'jeff'),
						$.predicate.equals('name', 'joe')
					)
				);

				return users;
			}}
			""", "Return all users named \"joe\" or \"jeff\""),

			Example.javaScript("""
			${{
				// More elegant example where predicates are created before of the query
				let nameConditions = [
					$.predicate.equals('name', 'jeff'),
					$.predicate.equals('name', 'joe')
				];

				// This showcases how to create condition predicates before adding them to the query
				let users = $.find('User', $.predicate.or(nameConditions));

				return users;
			}}
			""", "Return all users named \"joe\" or \"jeff\""),

			Example.javaScript("""
			${{
				// More advanced example where predicates are created before of the query
				let nameConditions = [
					$.predicate.equals('name', 'jeff'),
					$.predicate.equals('name', 'joe')
				];

				let rootPredicate = $.predicate.and(
					$.predicate.equal('isAdmin', true),
					$.predicate.or(nameConditions)
				);
				
				// This showcases how to create the complete predicate before the query
				let users = $.find('User', rootPredicate);

				return users;
			}}
			""", "Return all users with the `isAdmin` flag set to true and named \"joe\" or \"jeff\".")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"In a StructrScript environment parameters are passed as pairs of 'key1', 'value1'.",
			"In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter.",
			"The `find()` method will always use **exact** search, if you are interested in inexact / case-insensitive search, use `search()`.",
			"Calling `find()` with only a single parameter will return all the nodes of the given type (which might be problematic if there are so many of them in the database so that they do not fit in the available memory)."
		);
	}
}
