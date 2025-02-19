/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.structr.api.SyntaxErrorException;
import org.structr.api.UnknownClientException;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NativeQueryCommand;
import org.structr.core.graph.Tx;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashMap;
import java.util.Map;

public class CypherFunction extends CoreFunction {

	public static final String ERROR_MESSAGE_CYPHER    = "Usage: ${cypher(query)}. Example ${cypher('MATCH (n) RETURN n')}";
	public static final String ERROR_MESSAGE_CYPHER_JS = "Usage: ${{Structr.cypher(query)}}. Example ${{Structr.cypher('MATCH (n) RETURN n')}}";

	@Override
	public String getName() {
		return "cypher";
	}

	@Override
	public String getSignature() {
		return "query [, parameterMap ]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try(final Tx tx = StructrApp.getInstance().tx()) {

			if (sources.length < 1) {
				throw ArgumentCountException.tooFew(sources.length, 1);
			}
			if (sources[0] == null) {
				throw new ArgumentNullException();
			}

			final Map<String, Object> params = new LinkedHashMap<>();
			final String query = sources[0].toString();

			boolean dontFlushCaches = false;

			// parameters?
			if (sources.length > 1) {

				if (sources[1] instanceof Map) {

					params.putAll((Map)sources[1]);

					if (sources.length > 2 && sources[2] instanceof Boolean) {
						dontFlushCaches = ((boolean)sources[2]);
					}

				} else if (sources[1] instanceof GraphObjectMap) {

					params.putAll(((GraphObjectMap)sources[1]).toMap());

					if (sources.length > 2 && sources[2] instanceof Boolean) {
						dontFlushCaches = ((boolean)sources[2]);
					}

				} else {

					int parameter_count = sources.length;

					if (parameter_count % 2 == 0) {

						// count indicates trailing parameter. If this is a boolean, interpret it as dontFlushCaches flag. otherwise throw error
						if (sources[parameter_count - 1] instanceof Boolean) {

							dontFlushCaches = ((boolean)sources[parameter_count - 1]);
							parameter_count--;

						} else {

							throw new FrameworkException(400, "Invalid number of parameters: " + parameter_count + ". Should be uneven: " + usage(ctx.isJavaScriptContext()));
						}
					}

					for (int c = 1; c < parameter_count; c += 2) {

						params.put(sources[c].toString(), sources[c + 1]);
					}
				}
			}

			final NativeQueryCommand nqc = StructrApp.getInstance(ctx.getSecurityContext()).command(NativeQueryCommand.class);

			if (Boolean.TRUE.equals(dontFlushCaches)) {
				nqc.setDontFlushCachesIfKeywordsInQuery(dontFlushCaches);
			}

			tx.success();
			final Iterable result = nqc.execute(query, params);
			return result;

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		} catch (SyntaxErrorException ex) {

			throw new FrameworkException(422, "%s: SyntaxError (Cause: %s)".formatted(getReplacement(), ex.getMessage()));
		} catch (UnknownClientException ex) {

			logException(ex, "{}: Exception in '{}' for parameters: {} (Cause: {})", new Object[] {getReplacement(), caller, sources, ex.getMessage()});
			return null;
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
