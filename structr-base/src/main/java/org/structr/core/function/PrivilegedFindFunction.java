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
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class PrivilegedFindFunction extends AbstractQueryFunction {

	public static final String ERROR_MESSAGE_PRIVILEGEDFIND = "Usage: ${find_privileged(type, key, value)}. Example: ${find_privileged(\"User\", \"email\", \"tester@test.com\"}";
	public static final String ERROR_MESSAGE_PRIVILEGEDFIND_NO_TYPE_SPECIFIED = "Error in find_privileged(): no type specified.";
	public static final String ERROR_MESSAGE_PRIVILEGEDFIND_TYPE_NOT_FOUND = "Error in find_privileged(): type not found: ";
	@Override
	public String getName() {
		return "find_privileged";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("type, options...");
	}

	@Override
	public String getNamespaceIdentifier() {
		return "find";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, Object[] sources) throws FrameworkException {

		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();

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

					throw new FrameworkException(422, "Type GraphObject not supported in find_privileged(), please use type NodeInterface to search for nodes of all types.");
				}

				type = Traits.of(typeString);

				if (type != null) {

					query.types(type);

				} else {

					logger.warn("Error in find_privileged(): type '{}' not found.", typeString);
					return ERROR_MESSAGE_PRIVILEGEDFIND_TYPE_NOT_FOUND + typeString;

				}
			}

			// exit gracefully instead of crashing..
			if (type == null) {
				logger.warn("Error in find_privileged(): no type specified. Parameters: {}", getParametersAsString(sources));
				return ERROR_MESSAGE_PRIVILEGEDFIND_NO_TYPE_SPECIFIED;
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
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_PRIVILEGEDFIND;
	}

	@Override
	public String getShortDescription() {
		return "Returns a collection of entities of the given type from the database, takes optional key/value pairs. Executed in a super user context.";
	}
}