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
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObjectMap;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Actions;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class CallFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "call";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (sources != null && sources.length >= 1 && sources[0] != null) {

			try {

				final String methodName = sources[0].toString();

				if (sources.length == 1) {

					return Actions.callWithSecurityContext(methodName, getSecurityContext(ctx), Collections.EMPTY_MAP);

				} else if (sources.length == 2 && sources[1] instanceof Map) {

					return Actions.callWithSecurityContext(methodName, getSecurityContext(ctx), ((Map)sources[1]));

				} else if (sources.length == 2 && sources[1] instanceof GraphObjectMap) {

					return Actions.callWithSecurityContext(methodName, getSecurityContext(ctx), ((GraphObjectMap)sources[1]).toMap());

				} else {

					final int parameter_count = sources.length;

					if (parameter_count % 2 == 0) {
						throw new FrameworkException(400, "Invalid number of parameters: " + parameter_count + ". Should be uneven: " + usage(ctx.isJavaScriptContext()));
					}

					final Map<String, Object> newMap = new LinkedHashMap<>();

					for (int c = 1; c < parameter_count; c += 2) {
						newMap.put(sources[c].toString(), sources[c + 1]);
					}

					return Actions.callWithSecurityContext(methodName, getSecurityContext(ctx), newMap);

				}

			} catch (UnlicensedScriptException ex) {

				logger.error(ex.getMessage());
			}

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return "";
	}

	/*
	 * Overridden in CallPrivilegedFunction to return a superuser context
	 */
	public SecurityContext getSecurityContext(final ActionContext ctx) {
		return ctx.getSecurityContext();
	}

	// ----- documentation -----
	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${call(key [, key, value]}. Example ${call('myEvent', 'key1', 'value1', 'key2', 'value2')}"),
			Usage.javaScript("Usage: ${{Structr.call(key [, parameterMap]}}. Example ${{Structr.call('myEvent', {key1: 'value1', key2: 'value2'})}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Calls the given user-defined function in the current users context.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("functionName [, parameterMap ]");
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("functionName", "name of the user-defined function to call"),
			Parameter.mandatory("parameterMap", "map of parameters")
		);
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.structrScript("${call('updateUsers', 'param1', 'value1', 'param2', 'value2')}", "Call the user-defined function `updateUsers` with two key-value pairs as parameters"),
			Example.javaScript("""
			${{
				$.call('updateUsers', {
					param1: 'value1',
					param2: 'value2'
				})
			        }}
			""", "Call the user-defined function `updateUsers` with a map of parameters")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"The method can only be executed if it is visible in the user context it's call()'ed in. This is analogous to calling user-defined functions via REST.",
			"Useful in situations where different types have the same or similar functionality but no common base class so the method can not be attached there",
			"In a StructrScript environment parameters are passed as pairs of `'key1', 'value1'`.",
			"In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter."

		);
	}
}
