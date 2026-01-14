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
package org.structr.web.function;


import jakarta.servlet.http.HttpServletResponse;
import org.structr.common.SecurityContext;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;


public class SetResponseHeaderFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "setResponseHeader";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("name, value [, override = false ]");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			final String name      = sources[0].toString();
			final String value     = sources[1].toString();
			final Boolean override = sources.length > 2 ? (Boolean) sources[2] : false;

			final SecurityContext securityContext = ctx.getSecurityContext();
			if (securityContext != null) {

				final HttpServletResponse response = securityContext.getResponse();
				if (response != null) {

					if (override) {
						response.setHeader(name, value);
					} else {
						response.addHeader(name, value);
					}
				}
			}

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${setResponseHeader(field, value [, override = false ])}."),
			Usage.javaScript("Usage: ${{$.setResponseHeader(field, value [, override = false ])}}.")
		);
	}

	@Override
	public String getShortDescription() {
		return "Adds the given header field and value to the response of the current rendering run.";
	}

	@Override
	public String getLongDescription() {
		return """
		Sets the value of the HTTP response header with the given name to the given value. 
		This function can be used to set and/or override HTTP response headers in the Structr server implementation to 
		control certain aspects of browser / client behaviour.
		""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${setResponseHeader('Content-Type', 'text/csv')}"),
				Example.javaScript("${{ $.setResponseHeader('Content-Type', 'text/csv') }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("name", "HTTP header name"),
				Parameter.mandatory("value", "HTTP header value"),
				Parameter.optional("override", "override previous header")
				);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
				"The following example will cause the browser to display a 'Save as...' dialog when visiting the page, because the response content type is set to `text/csv`."
		);
	}

}
