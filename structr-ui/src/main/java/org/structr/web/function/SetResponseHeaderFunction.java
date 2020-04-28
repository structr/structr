/**
 * Copyright (C) 2010-2020 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.function;

import javax.servlet.http.HttpServletResponse;
import org.structr.common.SecurityContext;
import org.structr.schema.action.ActionContext;

public class SetResponseHeaderFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_SET_RESPONSE_HEADER    = "Usage: ${set_response_header(field, value)}. Example: ${set_response_header('X-User', 'johndoe')}";
	public static final String ERROR_MESSAGE_SET_RESPONSE_HEADER_JS = "Usage: ${{Structr.setResponseHeader(field, value)}}. Example: ${{Structr.setResponseHeader('X-User', 'johndoe')}}";

	@Override
	public String getName() {
		return "set_response_header";
	}

	@Override
	public String getSignature() {
		return "name, value";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		if (sources != null && sources.length == 2) {

			final String name = sources[0].toString();
			final String value = sources[1].toString();

			final SecurityContext securityContext = ctx.getSecurityContext();
			if (securityContext != null) {

				final HttpServletResponse response = securityContext.getResponse();
				if (response != null) {

					response.addHeader(name, value);
				}
			}

			return "";

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());

		}

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
