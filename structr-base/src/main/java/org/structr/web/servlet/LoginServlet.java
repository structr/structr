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
package org.structr.web.servlet;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.docs.Documentation;
import org.structr.rest.RestMethodResult;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.servlet.AbstractDataServlet;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.entity.dom.Page;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.api.RESTEndpoints;
import org.structr.web.traits.definitions.dom.PageTraitDefinition;

/**
 * Simple login servlet, acts as a bridge for form-base HTTP login.
 */
@Documentation(name="LoginServlet", parent="Servlets", children={ "LoginServlet Settings" })
public class LoginServlet extends AbstractDataServlet implements HttpServiceServlet {

	private static final Logger logger = LoggerFactory.getLogger(LoginServlet.class.getName());

	public LoginServlet() {
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
				response.getOutputStream().write(fex.getMessage().getBytes(StandardCharsets.UTF_8));

			} catch (IOException ioex) {

				logger.warn("Unable to send response", ioex);
			}

			return;
		}

		final SecurityContext securityContext;
		final Authenticator authenticator;

		setCustomResponseHeaders(response);

		try {

			// first thing to do!
			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/html; charset=utf-8");

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				authenticator = config.getAuthenticator();
				securityContext = authenticator.initializeAndExamineRequest(request, response);
				tx.success();
			}

			if (securityContext != null) {

				final RESTCallHandler loginResource = getLoginResource(request);
				if (loginResource != null) {

					final Map<String, Object> properties = new LinkedHashMap<>();

					for (final Entry<String, String[]> entry : request.getParameterMap().entrySet()) {

						final String key = entry.getKey();
						final String[] values = entry.getValue();

						if (values.length > 0) {

							properties.put(key, values[0]);
						}
					}

					final RestMethodResult result = loginResource.doPost(securityContext, properties);

					// send HTTP headers and redirect
					for (final Entry<String, String> entry : result.getHeaders().entrySet()) {

						response.addHeader(entry.getKey(), entry.getValue());
					}

					try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

						switch (result.getResponseCode()) {

							case HttpServletResponse.SC_OK:

								// redirect to requested target page or /
								final String redirectLocation = HtmlServlet.filterMaliciousRedirects(request.getParameter(HtmlServlet.TARGET_PATH_KEY));

								if (StringUtils.isBlank(redirectLocation)) {
									sendRedirectHeader(response, "/", true);
								} else {
									sendRedirectHeader(response, redirectLocation, false);	// user-provided, should be already prefixed
								}
								break;

							default:
								sendRedirectHeader(response, getRedirectPage(request, result.getResponseCode()));
								break;
						}

						tx.success();
					}
				}
			}

		} catch (FrameworkException fex) {

			logger.error("Exception while processing request: {}", fex.getMessage());
			UiAuthenticator.writeFrameworkException(response, fex);

		} catch (IOException ioex) {

			logger.error("Exception while processing request: {}", ioex.getMessage());
			UiAuthenticator.writeInternalServerError(response);
		}
	}

	protected String getUriPart() {
		return "login";
	}

	protected RESTCallHandler getLoginResource(final HttpServletRequest request) throws FrameworkException {
		return RESTEndpoints.resolveRESTCallHandler(request, config.getDefaultPropertyView(), StructrTraits.USER);
	}

	// ----- private methods -----
	private String getRedirectPage(final HttpServletRequest request, final Integer statusCode) throws FrameworkException {

		final NodeInterface errorPage = StructrApp.getInstance().nodeQuery(StructrTraits.PAGE).key(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.SHOW_ON_ERROR_CODES_PROPERTY), statusCode.toString(), false).getFirst();
		if (errorPage != null && HtmlServlet.isVisibleForSite(request, errorPage.as(Page.class))) {

			final String path = errorPage.as(Page.class).getPagePath();
			if (path != null) {

				return path + "?status=" + statusCode;
			}

			return "/" + errorPage.getName() + "?status=" + statusCode;
		}

		return "/";
	}
}
