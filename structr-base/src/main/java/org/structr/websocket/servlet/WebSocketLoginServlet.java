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
package org.structr.websocket.servlet;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.graph.Identity;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.exception.SessionLimitExceededException;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.auth.SessionHelper;
import org.structr.rest.servlet.AbstractServletBase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Completes a login that was made over the websocket.
 *
 * <p>A websocket cannot set a cookie, so the LOGIN command cannot give the browser a new session id,
 * and binding the id the browser already had is session fixation: whoever planted that id beforehand
 * - a cookie from a sibling subdomain, plain HTTP - holds an authenticated session of the account
 * the moment the login succeeds. LOGIN therefore binds nothing. It answers with a ticket, which the
 * page redeems here, over HTTP, where {@link AuthHelper#doLogin} rotates the session id and binds the
 * new one, and the page then opens a new websocket whose handshake carries it.</p>
 *
 * <p>A ticket is good for one redemption within a minute, and only by the browser that logged in:
 * the request has to present the session id the websocket login was made with.</p>
 */
public class WebSocketLoginServlet extends AbstractServletBase {

	private static final Logger logger                 = LoggerFactory.getLogger(WebSocketLoginServlet.class);
	private static final SecureRandom secureRandom     = new SecureRandom();
	private static final Cache<String, Ticket> tickets = CacheBuilder.newBuilder()
		.maximumSize(10_000)
		.expireAfterWrite(1, TimeUnit.MINUTES)
		.build();

	public static final String PATH             = "/structr/ws/login";
	public static final String TICKET_PARAMETER = "ticket";

	private record Ticket(String userId, String sessionId) {}

	/**
	 * Issues a ticket that logs the given user in over HTTP.
	 *
	 * @param sessionId the session id the websocket login was made with, which the redeeming request has to present
	 */
	public static String issueTicket(final Principal user, final String sessionId) {

		final byte[] bytes = new byte[32];

		secureRandom.nextBytes(bytes);

		final String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

		tickets.put(ticket, new Ticket(user.getUuid(), SessionHelper.getShortSessionId(sessionId)));

		return ticket;
	}

	@Override
	protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

		setCustomResponseHeaders(response);

		// the Admin UI redeems the ticket with fetch(), which always sends Origin
		if (!checkCsrfOrigin(request, response, true)) {

			return;
		}

		final String value  = request.getParameter(TICKET_PARAMETER);
		final Ticket ticket = StringUtils.isNotBlank(value) ? tickets.asMap().remove(value) : null;

		if (ticket == null || !StringUtils.equals(ticket.sessionId(), SessionHelper.getShortSessionId(request.getRequestedSessionId()))) {

			sendReason(response, HttpServletResponse.SC_UNAUTHORIZED, "invalidLoginTicket");

			return;
		}

		final App app = StructrApp.getInstance();
		Identity<?> userId = null;

		try (final Tx tx = app.tx()) {

			final NodeInterface node = app.getNodeById(StructrTraits.PRINCIPAL, ticket.userId());
			if (node != null) {

				AuthHelper.doLogin(request, node.as(Principal.class));

				userId = node.getNode().getId();
			}

			tx.success();

		} catch (SessionLimitExceededException ex) {

			sendReason(response, HttpServletResponse.SC_FORBIDDEN, ex.getReason());

			return;

		} catch (FrameworkException fex) {

			logger.warn("Unable to complete websocket login: {}", fex.getMessage());
		}

		if (userId == null) {

			sendReason(response, HttpServletResponse.SC_UNAUTHORIZED, "invalidLoginTicket");

			return;
		}

		// send broadcast to cluster members to refresh user from db
		Services.getInstance().broadcastLogin(userId.hash());

		response.setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	// ----- private methods -----
	private void sendReason(final HttpServletResponse response, final int status, final String reason) throws IOException {

		response.setStatus(status);
		response.setContentType("application/json; charset=utf-8");
		response.getOutputStream().write(("{\"reason\":\"" + reason + "\"}").getBytes(StandardCharsets.UTF_8));
	}
}
