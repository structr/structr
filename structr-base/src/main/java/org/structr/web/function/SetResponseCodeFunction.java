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


import jakarta.servlet.http.HttpServletResponse;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentNullException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;


public class SetResponseCodeFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "set_response_code";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("code");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			if (!(sources[0] instanceof Number statusCode)) {

				throw new IllegalArgumentException("Parameter must be a number!");
			}

			final SecurityContext securityContext = ctx.getSecurityContext();
			if (securityContext != null) {

				final HttpServletResponse response = securityContext.getResponse();
				if (response != null) {

					response.setStatus(statusCode.intValue());
				}
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (IllegalArgumentException iae) {

			logParameterError(caller, sources, iae.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}

		return "";
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${set_response_code(int)}. Example: ${set_response_header(302)}"),
			Usage.javaScript("Usage: ${{Structr.setResponseCode(int)}}. Example: ${{Structr.setResponseHeader(302)}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Sets the response code of the current rendering run.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
