/**
 * Copyright (C) 2010-2019 Structr GmbH
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

import java.util.LinkedHashMap;
import java.util.Map;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

public class CypherFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_CYPHER    = "Usage: ${cypher(query)}. Example ${cypher('MATCH (n) RETURN n')}";
	public static final String ERROR_MESSAGE_CYPHER_JS = "Usage: ${{Structr.cypher(query)}}. Example ${{Structr.cypher('MATCH (n) RETURN n')}}";

	@Override
	public String getName() {
		return "cypher";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			final Map<String, Object> params = new LinkedHashMap<>();
			final String query = sources[0].toString();

			// parameters?
			if (sources.length > 1 && sources[1] != null && sources[1] instanceof Map) {
				params.putAll((Map)sources[1]);
			}

			return StructrApp.getInstance(ctx.getSecurityContext()).query(query, params);

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_CYPHER_JS : ERROR_MESSAGE_CYPHER);
	}

	@Override
	public String shortDescription() {
		return "Returns the result of the given Cypher query";
	}

}
