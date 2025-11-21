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
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class CallPrivilegedFunction extends CallFunction {

	@Override
	public String getName() {
		return "call_privileged";
	}

	@Override
	public SecurityContext getSecurityContext(final ActionContext ctx) {

		final SecurityContext superuserSecurityContext = SecurityContext.getSuperUserInstance();
		superuserSecurityContext.setContextStore(ctx.getContextStore());

		return superuserSecurityContext;
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("functionName [, parameterMap ]");
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${call_privileged(key [, key, value]}. Example ${call_privileged('myEvent', 'key1', 'value1', 'key2', 'value2')}"),
			Usage.javaScript("Usage: ${{ $.callPrivileged(key [, parameterMap]}}. Example ${{ $.callPrivileged('myEvent', {key1: 'value1', key2: 'value2'})}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Calls the given user-defined function **in a superuser context**.";
	}

	@Override
	public String getLongDescription() {
		return "";
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
			"Useful in situations where different types have the same or similar functionality but no common base class so the method can not be attached there",
			"In a StructrScript environment parameters are passed as pairs of `'key1', 'value1'`.",
			"In a JavaScript environment, the function can be used just as in a StructrScript environment. Alternatively it can take a map as the second parameter."

		);
	}
}
