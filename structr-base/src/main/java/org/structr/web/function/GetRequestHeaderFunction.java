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

import jakarta.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;



public class GetRequestHeaderFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "getRequestHeader";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("name");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final SecurityContext securityContext = ctx.getSecurityContext();
			final String name = sources[0].toString();

			if (securityContext != null) {

				final HttpServletRequest request = securityContext.getRequest();
				if (request != null) {

					return request.getHeader(name);
				}
			}

			return "";

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${getRequestHeader(name)}. Example: ${getRequestHeader('User-Agent')}"),
			Usage.javaScript("Usage: ${{ $.getRequestHeader(name) }}. Example: ${{ $.getRequestHeader('User-Agent')}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the value of the given request header field.";
	}

	@Override
	public String getLongDescription() {
		return """
		This method can be used both in [Entity Callback Functions](/article/Entity%20Callback%20Functions)  
		and in the [Page Rendering](/article/Page%20Rendering) process to obtain the value of a given HTTP header, 
		allowing the user to use HTTP headers from their web application clients to control features of the application.""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${getRequestHeader('User-Agent')}"),
				Example.javaScript("${{ $.getRequestHeader('User-Agent') }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
				Parameter.mandatory("name", "name of request header field")
				);
	}
}
