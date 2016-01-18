/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.function;

import javax.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;

/**
 *
 */
public class GetRequestHeaderFunction extends UiFunction {

	public static final String ERROR_MESSAGE_GET_REQUEST_HEADER    = "Usage: ${get_request_header(name)}. Example: ${get_request_header('User-Agent')}";
	public static final String ERROR_MESSAGE_GET_REQUEST_HEADER_JS = "Usage: ${{Structr.getRequestHeader(name)}}. Example: ${{Structr.getRequestHeader('User-Agent')}}";

	@Override
	public String getName() {
		return "get_request_header()";
	}

	@Override
	public Object apply(ActionContext ctx, final GraphObject entity, final Object[] sources) {

		if (sources != null && arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

			final SecurityContext securityContext = ctx.getSecurityContext();
			final String name = sources[0].toString();

			if (securityContext != null) {

				final HttpServletRequest request = securityContext.getRequest();
				if (request != null) {

					return request.getHeader(name);
				}
			}

			return "";
		}

		return usage(ctx.isJavaScriptContext());
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_GET_REQUEST_HEADER_JS : ERROR_MESSAGE_GET_REQUEST_HEADER);
	}

	@Override
	public String shortDescription() {
		return "Returns the value of the given request header field";
	}

}
