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
package org.structr.web.servlet;


import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.rest.common.HttpHelper;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.service.StructrHttpServiceConfig;
import org.structr.rest.servlet.AbstractServletBase;
import org.structr.web.auth.UiAuthenticator;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

/**
 * Servlet for proxy requests.
 */
public class ProxyServlet extends AbstractServletBase implements HttpServiceServlet {

	private static final Logger logger = LoggerFactory.getLogger(ProxyServlet.class.getName());

	private final StructrHttpServiceConfig config = new StructrHttpServiceConfig();

	@Override
	public StructrHttpServiceConfig getConfig() {
		return config;
	}

	@Override
	public String getModuleName() {
		return "proxy";
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) {

		if (Settings.ProxyServletMode.getValue().equals("disabled")) {

			logger.info("Proxy servlet disabled.");
			response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

			return;

		}


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

		final Traits traits                        = Traits.of(StructrTraits.USER);
		final PropertyKey<String> proxyUrlKey      = traits.key(PrincipalTraitDefinition.PROXY_URL_PROPERTY);
		final PropertyKey<String> proxyUsernameKey = traits.key(PrincipalTraitDefinition.PROXY_USERNAME_PROPERTY);
		final PropertyKey<String> proxyPasswordKey = traits.key(PrincipalTraitDefinition.PROXY_PASSWORD_PROPERTY);

		final Authenticator auth = getConfig().getAuthenticator();

		SecurityContext securityContext;
		String content;

		if (auth == null) {

			final String errorMessage = "No authenticator class found. Check log for 'Missing authenticator key " + this.getClass().getSimpleName() + ".authenticator'";
			logger.error(errorMessage);

			try {
				final ServletOutputStream out = response.getOutputStream();
				content = errorPage(new Throwable(errorMessage));
				IOUtils.write(content, out);

			} catch (IOException ex) {
				logger.error("Could not write to response", ex);
			}

			return;
		}

		try {

			boolean hasUser;

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {

				securityContext = auth.initializeAndExamineRequest(request, response);
				hasUser         = securityContext.getUser(false) != null;
				tx.success();
			}

			// Ensure access mode is frontend
			securityContext.setAccessMode(AccessMode.Frontend);

			if (Settings.ProxyServletMode.getValue().equals("protected") && !hasUser) {
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				logger.error("Authorization required in 'protected' mode");
				return;
			}

			final String address = request.getParameter("url");
			if (StringUtils.isBlank(address)) {

				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				logger.error("Empty request parameter 'url'");

				return;
			}

			final URI url  = URI.create(address);

			String proxyUrl      = request.getParameter("proxyUrl");
			String proxyUsername = request.getParameter("proxyUsername");
			String proxyPassword = request.getParameter("proxyPassword");
			String authUsername  = request.getParameter("authUsername");
			String authPassword  = request.getParameter("authPassword");
			String cookie        = request.getParameter("cookie");

			String contentType   = request.getContentType();
			String charset       = null;

			// Extract character set from contentType if given
			if (StringUtils.isNotBlank(contentType)) {
				final String[] contentTypeParts = contentType.split(";");
				if (contentTypeParts.length == 2) {
					charset = org.apache.commons.lang.StringUtils.trim(contentTypeParts[1]);
				}
			}


			if (StringUtils.isBlank(proxyUrl)) {

				final Principal user = securityContext.getCachedUser();
				if (user != null) {

					try (final Tx tx = StructrApp.getInstance().tx()) {

						proxyUrl =      user.getProxyUrl();
						proxyUsername = user.getProxyUsername();
						proxyPassword = user.getProxyPassword();

						tx.success();

					} catch (FrameworkException fex) {
					}
				}
			}

			final Map<String, Object> responseData = HttpHelper.get(address, charset, authUsername, authPassword, proxyUrl, proxyUsername, proxyPassword, cookie, Collections.EMPTY_MAP, true);
			final String body = responseData.get(HttpHelper.FIELD_BODY) != null ? (String) responseData.get(HttpHelper.FIELD_BODY) : null;

			if (body == null) {
				throw new FrameworkException(422, "Request returned empty body");
			}

			content =  body.replace("<head>", "<head>\n  <base href=\"" + url + "\">");

		} catch (Throwable t) {

			logger.error("Exception while processing request", t);
			content = errorPage(t);
		}

		try {
			final ServletOutputStream out = response.getOutputStream();
			IOUtils.write(content, out, "utf-8");
		} catch (IOException ex) {
			logger.error("Could not write to response", ex);
		}
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	@Override
	protected void doOptions(final HttpServletRequest request, final HttpServletResponse response) {

		if (Settings.ProxyServletMode.getValue().equals("disabled")) {

			logger.info("Proxy servlet disabled.");
			response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);

			return;
		}

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

		try {

			response.setContentLength(0);
			response.setHeader("Allow", "GET,HEAD,OPTIONS");

		} catch (Throwable t) {

			logger.error("Exception while processing request", t);
			UiAuthenticator.writeInternalServerError(response);
		}
	}

	private String errorPage(final Throwable t) {
		return "<html><head><title>Error in Structr Proxy</title></head><body><h1>Error in Proxy</h1><p>" + t.getMessage() + "</p>\n<!--" + ExceptionUtils.getStackTrace(t) + "--></body></html>";
	}

}
