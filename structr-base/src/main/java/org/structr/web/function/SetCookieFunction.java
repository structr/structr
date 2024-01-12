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


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.structr.common.SecurityContext;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.schema.action.ActionContext;



public class SetCookieFunction extends UiAdvancedFunction {

	public static final String ERROR_MESSAGE_SET_COOKIE    = "Usage: ${set_cookie(name, value[, secure[, httpOnly[, maxAge[, domain[, path]]]]])}. Example: ${get_cookie('cartId', 'abcdef123', true, false, 1800, 'www.structr.com', '/')}";
	public static final String ERROR_MESSAGE_SET_COOKIE_JS = "Usage: ${{Structr.getCookie(name, value[, secure[, httpOnly[, maxAge[, domain[, path]]]]])}}. Example: ${{Structr.getCookie('cartId', 'abcdef123', true, false, 1800, 'www.structr.com', '/')}}";

	@Override
	public String getName() {
		return "set_cookie";
	}

	@Override
	public String getSignature() {
		return "name, value[, secure[, httpOnly[, maxAge[, domain[, path]]]]]";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 2);

			final SecurityContext securityContext = ctx.getSecurityContext();

			if (securityContext != null) {

				final HttpServletResponse response = securityContext.getResponse();
				if (response != null) {

					try {

						final String name  = sources[0].toString();
						final String value = sources[1].toString();

						final Cookie c = new Cookie(name, value);

						if (sources.length >= 3) {
							c.setSecure((Boolean)sources[2]);
						}

						if (sources.length >= 4) {
							c.setHttpOnly((Boolean)sources[3]);
						}

						if (sources.length >= 5) {
							c.setMaxAge(((Number)sources[4]).intValue());
						}

						if (sources.length >= 6) {
							c.setDomain(sources[5].toString());
						}

						if (sources.length >= 7) {
							c.setPath(sources[6].toString());
						}

						response.addCookie(c);

					} catch (IllegalArgumentException iae) {
						logger.warn("{}: Exception creating cookie: {}", getDisplayName(), iae.getMessage());
					}
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
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_SET_COOKIE_JS : ERROR_MESSAGE_SET_COOKIE);
	}

	@Override
	public String shortDescription() {
		return "Sets the given cookie";
	}
}
