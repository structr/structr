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
package org.structr.web.function;

import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class AddHeaderFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "add_header";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		if (sources != null && sources.length == 2) {

			if (sources[0] != null) {

				final String name = sources[0].toString();

				if (sources[1] == null) {
					ctx.removeHeader(name);

				} else {

					final String value = sources[1].toString();

					ctx.addHeader(name, value);
				}
			}

			return "";

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("name, value");
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${add_header(name, value)}. Example: ${add_header('X-User', 'johndoe')}"),
			Usage.javaScript("Usage: ${{ $.addHeader(name, value)}}. Example: ${{ $.addHeader('X-User', 'johndoe')}}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("name", "name of the header field"),
			Parameter.mandatory("value", "value of the header field")
		);
	}

	@Override
	public String getShortDescription() {
		return "Temporarily adds the given (key, value) tuple to the local list of request headers.";
	}

	@Override
	public String getLongDescription() {
		return "All headers added with this function will be sent with any subsequent `GET()`, `HEAD()`, `POST()`, `PUT()` or `DELETE()` call in the same request (meaning the request from the client to Structr).";
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"Prior to 3.5.0 it was not possible to remove headers. In 3.5.0 this function was changed to remove a header if `value = null` was provided as an argument."
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript(
				"""
				${
				    (
					add_header('X-User', 'tester1'),
					add_header('X-Password', 'test'),
					GET('http://localhost:8082/structr/rest/User')
				    )
				}
				""", "Authenticate an HTTP GET request with add_header (StructrScript version)"),
			Example.javaScript(
				"""
					${{
					    $.addHeader('X-User', 'tester1');
					    $.addHeader('X-Password', 'test');
					    let result = $.GET('http://localhost:8082/structr/rest/User');
					}}
				""", "Authenticate an HTTP GET request with addHeader (JavaScript version)"
			)
		);
	}
}
