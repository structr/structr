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

import jakarta.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.Collections;
import java.util.List;

public class GetRequestHeaderNamesFunction extends UiAdvancedFunction {

	@Override
	public String getName() {

		return "getRequestHeaderNames";
	}

	@Override
	public List<Signature> getSignatures() {

		return Signature.forAllScriptingLanguages("");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		final SecurityContext securityContext = ctx.getSecurityContext();
		if (securityContext != null) {

			final HttpServletRequest request = securityContext.getRequest();
			if (request != null) {

				return Collections.list(request.getHeaderNames());
			}
		}

		return List.of();
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.structrScript("Usage: ${getRequestHeaderNames()}. Example: ${getRequestHeaderNames()}"),
			Usage.javaScript("Usage: ${{ $.getRequestHeaderNames() }}. Example: ${{ $.getRequestHeaderNames()}}")
		);
	}

	@Override
	public String getShortDescription() {

		return "Returns a collection of all the header names the current request contains.";
	}

	@Override
	public String getLongDescription() {

		return "This function can be used both in Entity Callback Functions and in the Page Rendering process to obtain the names of all the HTTP headers the requesting client has sent.";
	}

	@Override
	public List<Example> getExamples() {

		return List.of(Example.structrScript("${getRequestHeaderNames()}"), Example.javaScript("${{ $.getRequestHeaderNames() }}"));
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of();
	}

	@Override
	public FunctionCategory getCategory() {

		return FunctionCategory.Http;
	}
}
