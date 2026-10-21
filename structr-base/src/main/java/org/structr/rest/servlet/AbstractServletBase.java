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
package org.structr.rest.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.RetryException;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.Tx;
import org.structr.rest.common.StatsCallback;

import java.io.IOException;
import java.util.Arrays;

public abstract class AbstractServletBase extends HttpServlet {

	private static final Logger logger = LoggerFactory.getLogger(AbstractServletBase.class.getName());

	protected StatsCallback stats = null;

	public void registerStatsCallback(final StatsCallback stats) {

		this.stats = stats;
	}

	protected void setCustomResponseHeaders(final HttpServletResponse response) {

		if (response != null) {

			final String customResponseHeadersString = Settings.HtmlCustomResponseHeaders.getValue();
			if (StringUtils.isNotBlank(customResponseHeadersString)) {

				for (final String header : Arrays.asList(customResponseHeadersString.split("[,]+"))) {

					final String[] keyValuePair = header.split("[:]+");
					if (keyValuePair != null && keyValuePair.length == 2) {

						response.setHeader(keyValuePair[0].trim(), keyValuePair[1].trim());

						logger.debug("Set custom response header: {} {}", keyValuePair[0].trim(), keyValuePair[1].trim());
					}
				}
			}
		}
	}

	/**
	 * Examines the request with the given authenticator in a transaction of its own, and repeats that transaction as
	 * long as the database terminates it with a transient error. Examining a request can write, e.g. a login with
	 * credentials in the request headers or a user created by an OAuth callback. A repetition is only possible as long
	 * as no part of the response has been sent, e.g. no redirect of an external authentication. A repeated OAuth
	 * callback finds its flow used up and fails, which is the same result as without the repetition.
	 *
	 * Called in a transaction, which a servlet does not do, the request is examined in it once and the
	 * RetryException goes on to the caller, because only the outermost transaction can be repeated.
	 */
	public static SecurityContext authenticate(final Authenticator authenticator, final HttpServletRequest request, final HttpServletResponse response) throws FrameworkException {

		return authenticate(authenticator, request, response, null);
	}

	/**
	 * Examines the request like {@link #authenticate(Authenticator, HttpServletRequest, HttpServletResponse)}, with the
	 * given prefetch hint for the transaction, or none for null.
	 */
	public static SecurityContext authenticate(final Authenticator authenticator, final HttpServletRequest request, final HttpServletResponse response, final String prefetchHint) throws FrameworkException {

		if (TransactionCommand.inTransaction()) {

			return authenticator.initializeAndExamineRequest(request, response);
		}

		return repeatUnlessCommitted(request, response, () -> {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				if (prefetchHint != null) {

					tx.prefetchHint(prefetchHint);
				}

				final SecurityContext securityContext = authenticator.initializeAndExamineRequest(request, response);

				tx.success();

				return securityContext;
			}
		});
	}

	/**
	 * Runs the given attempt of a request and repeats it as long as the database terminates its transaction with a
	 * transient error and no part of the response has been sent, at most {@link RetryException#MAX_ATTEMPTS} times. A
	 * response that has been handed to an asynchronous context counts as sent, since it is written while the attempt
	 * that started the context still runs. The attempt must contain the whole transaction, and it must not run inside
	 * another transaction, since only the outermost one can be repeated.
	 *
	 * Before a repetition, the buffered body is discarded and the status is reset to the one before the first attempt.
	 * The headers stay, so an attempt sets its headers rather than adding them. A reset of the whole response would
	 * also discard the character encoding, which Jetty does not take from a restored header.
	 *
	 * @return the result of the attempt that succeeded
	 */
	@SuppressWarnings("unchecked")
	public static <T, E extends Exception> T repeatUnlessCommitted(final HttpServletRequest request, final HttpServletResponse response, final RetryException.Computation<T, E> attempt) throws E {

		final int initialStatus = response.getStatus();
		int attempts            = 0;

		while (true) {

			try {

				return attempt.run();

			} catch (RuntimeException rex) {

				retryUnlessCommitted(request, response, rex, ++attempts, initialStatus);

			} catch (Exception ex) {

				// the attempt throws no other checked exception than E
				retryUnlessCommitted(request, response, (E)ex, ++attempts, initialStatus);
			}
		}
	}

	/**
	 * Throws the RetryException of the given exception, if the database terminated the transaction, so that the caller
	 * repeats the transaction instead of answering with the error.
	 */
	public static void throwIfRetryable(final Exception ex) {

		final RetryException rex = RetryException.find(ex);
		if (rex != null) {

			throw rex;
		}
	}

	protected void assertInitialized() throws FrameworkException {

		final Services services = Services.getInstance();
		if (!services.isInitialized()) {

			throw new FrameworkException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, services.getUnavailableMessage());
		}
	}

	protected void sendRedirectHeader(final HttpServletResponse response, final String location) throws IOException {

		sendRedirectHeader(response, location, true);
	}

	/**
	 * Checks the Origin header of a POST request against the server's host
	 * to prevent cross-site request forgery. Returns true if the request
	 * is safe to process, false if it should be rejected.
	 *
	 * A request is considered safe if:
	 * - No Origin header is present (same-origin form submission)
	 * - The Origin matches the request's server name and port
	 */
	protected boolean checkCsrfOrigin(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

		return checkCsrfOrigin(request, response, false);
	}

	/**
	 * Rejects a request whose Origin header names another host. A request without the header passes
	 * unless {@code requireOrigin} is set.
	 *
	 * @param requireOrigin reject a request that carries no Origin header. Browsers omit the header on
	 * top-level GET navigations, so a request without it may be a click on a link anywhere on the web -
	 * a servlet whose pages only ever POST through forms and fetch() can insist on it. A servlet that
	 * serves API clients (curl, SDKs) cannot, they send none.
	 */
	protected boolean checkCsrfOrigin(final HttpServletRequest request, final HttpServletResponse response, final boolean requireOrigin) throws IOException {

		final String origin = request.getHeader("Origin");
		if (origin == null && requireOrigin) {

			logger.warn("CSRF check failed: request to {} without Origin header rejected", request.getRequestURI());
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Request without Origin header rejected");

			return false;
		}

		if (origin != null) {

			try {

				final java.net.URI originUri = java.net.URI.create(origin);
				final String originHost      = originUri.getHost();
				final int originPort         = originUri.getPort();
				final String serverHost      = request.getServerName();
				final int serverPort         = request.getServerPort();

				if (!serverHost.equals(originHost) || (originPort != -1 && originPort != serverPort)) {

					logger.warn("CSRF check failed: Origin '{}' does not match server '{}:{}'", origin, serverHost, serverPort);
					response.sendError(HttpServletResponse.SC_FORBIDDEN, "Cross-origin request rejected");

					return false;
				}

			} catch (IllegalArgumentException e) {

				logger.warn("CSRF check failed: Invalid Origin header '{}'", origin);
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Origin header");

				return false;
			}
		}

		return true;
	}

	protected void sendRedirectHeader(final HttpServletResponse response, final String location, final boolean addPrefix) throws IOException {

		final String locationWithSlash     = ((location.startsWith("/") ? "" : "/") + location);
		final String finalRedirectLocation = (addPrefix ? prefixLocation(locationWithSlash) : locationWithSlash);

		response.resetBuffer();
		response.setHeader("Location", finalRedirectLocation);
		response.setStatus(HttpServletResponse.SC_FOUND);
		response.flushBuffer();
	}

	public static String prefixLocation (final String location) {

		return Settings.ApplicationRootPath.getValue() + ((location.startsWith("/") ? "" : "/") + location);
	}

	/**
	 * Decides whether an attempt of a request that ended with the given exception is repeated, which is only possible
	 * as long as no part of the response has been sent, and discards the buffered body and resets the status before.
	 * For a loop whose attempt assigns variables that are read after it, everything else uses
	 * {@link #repeatUnlessCommitted(HttpServletRequest, HttpServletResponse, RetryException.Computation)}.
	 *
	 * @param initialStatus the status of the response before the first attempt
	 *
	 * @return true if the attempt is to be repeated
	 *
	 * @throws E the given exception, if the attempt is not repeated
	 */
	public static <E extends Exception> boolean retryUnlessCommitted(final HttpServletRequest request, final HttpServletResponse response, final E ex, final int attempts, final int initialStatus) throws E {

		if (response.isCommitted() || request.isAsyncStarted()) {

			throw ex;
		}

		// throws the exception if the database did not terminate the transaction, or after the last attempt
		RetryException.retry(ex, attempts);

		response.resetBuffer();
		response.setStatus(initialStatus);

		return true;
	}
}
