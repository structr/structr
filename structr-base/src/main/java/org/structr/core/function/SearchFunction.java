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
import org.structr.core.traits.Traits;
import org.structr.schema.action.ActionContext;

public class SearchFunction extends AbstractQueryFunction {

	public static final String ERROR_MESSAGE_SEARCH    = "Usage: ${search(type, key, value)}. Example: ${search(\"User\", \"name\", \"abc\")}";
	public static final String ERROR_MESSAGE_SEARCH_JS = "Usage: ${{Structr.search(type, key, value)}}. Example: ${{Structr.search(\"User\", \"name\", \"abc\")}}";

	@Override
	public String getName() {
		return "search";
	}

	@Override
	public String getSignature() {
		return "type, options...";
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
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_SEARCH_JS : ERROR_MESSAGE_SEARCH);
	}

	@Override
	public String shortDescription() {
		return "Returns a collection of entities of the given type from the database, takes optional key/value pairs. Searches case-insensitve / inexact.";
	}
}
