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
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.List;


public class RemoveResponseHeaderFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "removeResponseHeader";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("field");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			final String name = sources[0].toString();

			final SecurityContext securityContext = ctx.getSecurityContext();
			if (securityContext != null) {

				final HttpServletResponse response = securityContext.getResponse();
				if (response != null) {

					response.setHeader(name, null);
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
			Usage.structrScript("Usage: ${removeResponseHeader(field)}. Example: ${removeResponseHeader('X-Frame-Options'}"),
			Usage.javaScript("Usage: ${{Structr.removeResponseHeader(field)}}. Example: ${{Structr.removeResponseHeader('X-Frame-Options')}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Removes the given header field from the server response.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Http;
	}
}
