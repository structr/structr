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

import org.structr.api.SyntaxErrorException;
import org.structr.api.UnknownClientException;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NativeQueryCommand;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CypherFunction extends CoreFunction {

	@Override
	public String getName() {
		return "cypher";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("query [, parameterMap, runInNewTransaction]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			if (sources.length < 1) {
				throw ArgumentCountException.tooFew(sources.length, 1);
			}
			if (sources[0] == null) {
				throw new ArgumentNullException();
			}

			final Map<String, Object> params = new LinkedHashMap<>();
			final String query = sources[0].toString();

			boolean runInNewTransaction = (sources.length > 2 && sources[2] instanceof Boolean && (Boolean)sources[2]);

			// parameters?
			if (sources.length > 1) {

				if (sources[1] instanceof Map) {

					params.putAll((Map)sources[1]);
				} else if (sources[1] instanceof GraphObjectMap) {

					params.putAll(((GraphObjectMap)sources[1]).toMap());
				} else {

					int parameterCount = sources.length;

					if (parameterCount % 2 == 0) {

						throw new FrameworkException(400, "Invalid number of parameters: " + parameterCount + ". Should be uneven: " + usage(ctx.isJavaScriptContext()));
					}

					for (int c = 1; c < parameterCount; c += 2) {

						params.put(sources[c].toString(), sources[c + 1]);
					}
				}
			}

			final NativeQueryCommand nqc = StructrApp.getInstance(ctx.getSecurityContext()).command(NativeQueryCommand.class);
			nqc.setRunInNewTransaction(runInNewTransaction);

			return nqc.execute(query, params);

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		} catch (SyntaxErrorException ex) {

			throw new FrameworkException(422, "%s: SyntaxError (Cause: %s)".formatted(getDisplayName(), ex.getMessage()));
		} catch (UnknownClientException ex) {

			throw new FrameworkException(422, "%s: UnknownClientException (Cause: %s)".formatted(getDisplayName(), ex.getMessage()));
		}
    }

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${cypher(query)}. Example ${cypher('MATCH (n) RETURN n')}"),
			Usage.javaScript("Usage: ${{ $.cypher(query); }}. Example ${{ $.cypher('MATCH (n) RETURN n'); }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Executes the given Cypher query directly on the database and returns the results as Structr entities.";
	}

	@Override
	public String getLongDescription() {
		return "Structr will automatically convert all query results into the corresponding Structr objects, i.e. Neo4j nodes will be instantiated to Structr node entities, Neo4j relationships will be instantiated to Structr relationship entities, and maps will converted into Structr maps that can be accessed using the dot notation (`map.entry.subentry`).";
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("query", "query to execute"),
			Parameter.optional("parameters", "map to supply parameters for the variables in the query"),
			Parameter.optional("runInNewTransaction", "whether the Cypher query should be run in a new transaction - see notes about the implications of that flag")

		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript("${cypher('MATCH (n:User) RETURN n')}", "Run a simple query"),
			Example.javaScript("""
			${{
				let query = "MATCH (user:User) WHERE user.name = $userName RETURN user";
				let users = $.cypher(query, {userName: 'admin'});
			}}
			""", "Run a query with variables in it")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"If the `runInNewTransaction` parameter is set to `true`, the query runs in a new transaction, **which means that you cannot access use objects created in the current context due to transaction isolation**.",
			"The `cypher()` function always returns a collection of objects, even if `LIMIT 1` is specified!",
			"In a StructrScript environment parameters are passed as pairs of `'key1', 'value1'`.",
			"In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter."
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Database;
	}
}
