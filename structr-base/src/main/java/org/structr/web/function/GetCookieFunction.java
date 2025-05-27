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


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.schema.action.ActionContext;


public class GetCookieFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_GET_COOKIE    = "Usage: ${get_cookie(name)}. Example: ${get_cookie('cartId')}";
	public static final String ERROR_MESSAGE_GET_COOKIE_JS = "Usage: ${{Structr.getCookie(name)}}. Example: ${{Structr.getCookie('cartId')}}";

	@Override
	public String getName() {
		return "get_cookie";
	}

	@Override
	public String getSignature() {
		return "name";
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

					Cookie[] cookies = request.getCookies();

					if (cookies != null) {
						for (Cookie c : cookies) {
							if (c.getName().equals(name)) {
								return c.getValue();
							}
						}
					}
				}
			}

			if (ctx.isJavaScriptContext()) {
				return null;
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
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_GET_COOKIE_JS : ERROR_MESSAGE_GET_COOKIE);
	}

	@Override
	public String shortDescription() {
		return "Returns the requested cookie if it exists";
	}
}
