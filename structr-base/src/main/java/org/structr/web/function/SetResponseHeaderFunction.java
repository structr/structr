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
package org.structr.web.function;


import jakarta.servlet.http.HttpServletResponse;
import org.structr.common.SecurityContext;
import org.structr.schema.action.ActionContext;



public class SetResponseHeaderFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_SET_RESPONSE_HEADER    = "Usage: ${set_response_header(field, value [, override = false ])}. Example: ${set_response_header('X-User', 'johndoe', true)}";
	public static final String ERROR_MESSAGE_SET_RESPONSE_HEADER_JS = "Usage: ${{Structr.setResponseHeader(field, value [, override = false ])}}. Example: ${{Structr.setResponseHeader('X-User', 'johndoe', true)}}";

	@Override
	public String getName() {
		return "set_response_header";
	}

	@Override
	public String getSignature() {
		return "name, value [, override = false ]";
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
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_SET_RESPONSE_HEADER_JS : ERROR_MESSAGE_SET_RESPONSE_HEADER);
	}

	@Override
	public String shortDescription() {
		return "Adds the given header field and value to the response of the current rendering run";
	}

}
