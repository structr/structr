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
			Usage.structrScript("Usage: ${search(type, key, value)}. Example: ${search(\"User\", \"name\", \"abc\")}"),
			Usage.javaScript("Usage: ${{Structr.search(type, key, value)}}. Example: ${{Structr.search(\"User\", \"name\", \"abc\")}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns a collection of entities of the given type from the database, takes optional key/value pairs. Searches case-insensitve / inexact.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
