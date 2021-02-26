/*
 * Copyright (C) 2010-2021 Structr GmbH
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
package org.structr.web.servlet;


import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.graph.Tx;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.servlet.AbstractDataServlet;
import org.structr.web.auth.UiAuthenticator;

/**
 * Simple login servlet, acts as a bridge for form-base HTTP login.
 */
public class LogoutServlet extends AbstractDataServlet implements HttpServiceServlet {

	private static final Logger logger = LoggerFactory.getLogger(LogoutServlet.class.getName());

	public LogoutServlet() {
	}

	@Override
	public String getModuleName() {
		return "ui";
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException {

		try {

			assertInitialized();

		} catch (FrameworkException fex) {

			try {
				response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
				response.getOutputStream().write(fex.getMessage().getBytes("UTF-8"));

			} catch (IOException ioex) {

				logger.warn("Unable to send response", ioex);
			}

			return;
		}

		setCustomResponseHeaders(response);

		try {

			// first thing to do!
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/html; charset=utf-8");

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {

				final Authenticator authenticator     = config.getAuthenticator();
				final SecurityContext securityContext = authenticator.initializeAndExamineRequest(request, response);

				if (securityContext != null) {

					securityContext.getAuthenticator().doLogout(securityContext.getRequest());
				}

				tx.success();
			}

			// redirect to requested target page or /
			response.sendRedirect(coalesce(HtmlServlet.filterMaliciousRedirects(request.getParameter(HtmlServlet.TARGET_PAGE_KEY)), "/"));

		} catch (FrameworkException fex) {

			logger.error("Exception while processing request: {}", fex.getMessage());
			UiAuthenticator.writeFrameworkException(response, fex);

		} catch (IOException ioex) {

			logger.error("Exception while processing request: {}", ioex.getMessage());
			UiAuthenticator.writeInternalServerError(response);
		}
	}
}
