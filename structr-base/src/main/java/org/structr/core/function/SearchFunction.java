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
import org.structr.core.app.QueryGroup;
import org.structr.core.app.StructrApp;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class SearchFunction extends AbstractQueryFunction {

	@Override
	public String getName() {
		return "search";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("type, options...");
	}

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

			final QueryGroup query = StructrApp.getInstance(securityContext).nodeQuery().and();

			applyQueryParameters(securityContext, query);

			Traits type = null;

			if (sources.length >= 1 && sources[0] != null) {

				final String typeString = sources[0].toString();

				if (StructrTraits.GRAPH_OBJECT.equals(typeString)) {

					throw new FrameworkException(422, "Type GraphObject not supported in search(), please use type NodeInterface to search for nodes of all types.");
				}

				if (Traits.exists(typeString)) {

					type = Traits.of(typeString);

					query.types(type);

				} else {

					logger.warn("Error in search(): type '{}' not found.", typeString);
					return "Error in search(): type " + typeString + " not found.";
				}
			}

			// exit gracefully instead of crashing..
			if (type == null) {

				logger.warn("Error in search(): no type specified. Parameters: {}", getParametersAsString(sources));
				return "Error in search(): no type specified.";
			}

			// apply sorting and pagination by surrounding sort() and slice() expressions
			applyQueryParameters(securityContext, query);

			return handleQuerySources(securityContext, type, query, sources, false, usage(ctx.isJavaScriptContext()));

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());

		} finally {

			resetQueryParameters(securityContext);
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${search(type, key, value)}."),
			Usage.javaScript("Usage: ${{Structr.search(type, key, value)}}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns a collection of entities of the given type from the database, takes optional key/value pairs. Searches case-insensitve / inexact.";
	}

	@Override
	public String getLongDescription() {
		return """
		The `search()` method is very similar to `find()`, except that it is case-insensitive / inexact. It returns a collection of entities, 
		which can be empty if none of the existing nodes or relationships matches the given search parameters.
		`search()` accepts several different parameter combinations, whereas the first parameter is always the name of 
		the type to retrieve from the database. The second parameter can either be a map (e.g. a result from nested function calls)
		or a list of (key, value) pairs. Calling `search()` with only a single parameter will return all the nodes of the  
		given type (which might be dangerous if there are many of them in the database).
		
		For more examples see `find()`.
		""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("""
				// For this example, we assume that there are three users in the database: [admin, tester1, tester2]
				${search('User')}
				> [7379af469cd645aebe1a3f8d52b105bd, a05c044697d648aefe3ae4589af305bd, 505d0d469cd645aebe1a3f8d52b105bd]
				${search('User', 'name', 'test')}
				> [a05c044697d648aefe3ae4589af305bd, 505d0d469cd645aebe1a3f8d52b105bd]
				"""),
				Example.javaScript("${{ $.search('User', 'name', 'abc')} }")
		);
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
				Parameter.mandatory("type", "type to return (includes inherited types"),
				Parameter.optional("predicates", "list of predicates"),
				Parameter.optional("uuid", "uuid, makes the function return **a single object**")
		);
	}
}
