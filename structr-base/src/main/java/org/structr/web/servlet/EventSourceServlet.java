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
package org.structr.web.servlet;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlets.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.entity.Group;
import org.structr.core.entity.Principal;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.common.StatsCallback;
import org.structr.rest.service.HttpServiceServlet;
import org.structr.rest.service.StructrHttpServiceConfig;
import org.structr.web.entity.User;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;


public class EventSourceServlet extends org.eclipse.jetty.ee10.servlets.EventSourceServlet implements HttpServiceServlet {

	private static final Logger logger = LoggerFactory.getLogger(EventSourceServlet.class.getName());

	private static final BlockingDeque<StructrEventSource> eventSources = new LinkedBlockingDeque<>();

	protected final StructrHttpServiceConfig config                     = new StructrHttpServiceConfig();
	protected StatsCallback stats                                       = null;

	private SecurityContext securityContext;

	@Override
	protected EventSource newEventSource(HttpServletRequest hsr) {
		return new StructrEventSource(securityContext.getSessionId());
	}

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

		try {

			Authenticator authenticator = null;

			assertInitialized();

			// isolate request authentication in a transaction
			try (final Tx tx = StructrApp.getInstance().tx()) {
				authenticator = config.getAuthenticator();
				securityContext = authenticator.initializeAndExamineRequest(request, response);
				tx.success();
			}

			final App app = StructrApp.getInstance(securityContext);

			request.setCharacterEncoding("UTF-8");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/event-stream");

			// isolate resource authentication
			try (final Tx tx = app.tx()) {

				authenticator.checkResourceAccess(securityContext, request, "_eventSource", "");
				tx.success();
			}

			newEventSource(request);

			super.doGet(request, response);

		} catch (FrameworkException frameworkException) {

			// set status
			response.setStatus(frameworkException.getStatus());

		} catch (Throwable t) {

			logger.warn("Exception in GET", t);

			int code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

			response.setStatus(code);
		}
	}

	@Override
	public StructrHttpServiceConfig getConfig() {
		return config;
	}

	@Override
	public void registerStatsCallback(final StatsCallback stats) {
		this.stats = stats;
	}

	public static void broadcastEvent(final String name, final String data) {

		for (StructrEventSource es : eventSources) {

			es.sendEvent(name, data);
		}
	}

	public static void broadcastEvent(final String name, final String data, final boolean authenticated, final boolean anonymous) {

		if (anonymous && authenticated) {

			broadcastEvent(name, data);

		} else if (anonymous) {

			// account for multiple open tabs/connections for one sessionIds and save result
			final Map<String, Boolean> checkedSessionIds = new HashMap<>();

			for (StructrEventSource es : eventSources) {

				final String sessionId      = es.getSessionId();
				final Boolean shouldReceive = checkedSessionIds.get(sessionId);

				if (shouldReceive == null) {

					final Principal user = AuthHelper.getPrincipalForSessionId(sessionId);

					if (user == null) {
						checkedSessionIds.put(sessionId, true);
						es.sendEvent(name, data);
					} else {
						checkedSessionIds.put(sessionId, false);
					}

				} else if (shouldReceive) {

					es.sendEvent(name, data);
				}
			}

		} else if (authenticated) {

			// account for multiple open tabs/connections for one sessionIds and save result
			final Map<String, Boolean> checkedSessionIds = new HashMap<>();

			for (StructrEventSource es : eventSources) {

				final String sessionId      = es.getSessionId();
				final Boolean shouldReceive = checkedSessionIds.get(sessionId);

				if (shouldReceive == null) {

					final Principal user = AuthHelper.getPrincipalForSessionId(sessionId);

					if (user != null) {
						checkedSessionIds.put(sessionId, true);
						es.sendEvent(name, data);
					} else {
						checkedSessionIds.put(sessionId, false);
					}

				} else if (shouldReceive) {

					es.sendEvent(name, data);
				}
			}
		}
	}

	public static boolean sendEvent(final String name, final String data, final Set<Principal> targets) {

		final Set<User> uniqueUsers = new HashSet<>();
		final Set<Principal> seenGroups  = new HashSet<>();

		for (Principal principal : targets) {

			if (principal.is(StructrTraits.USER)) {

				uniqueUsers.add(principal.as(User.class));

			} else {

				uniqueUsers.addAll(getUniqueUsersForGroup(principal.as(Group.class), seenGroups, true));
			}
		}

		boolean oneTargetSeen = false;

		for (User user : uniqueUsers) {
			boolean seen = sendEvent(name, data, user);
			oneTargetSeen = seen || oneTargetSeen;
		}

		return oneTargetSeen;
	}

	private static Set<User> getUniqueUsersForGroup(final Group group, final Set<Principal> seenGroups, final boolean recurse) {

		seenGroups.add(group);

		final Set<User> uniqueUsers = new HashSet<>();

		for (Principal member : group.getMembers()) {

//			if (User.class.isAssignableFrom(member.getClass())) {
			if (member.is(StructrTraits.USER)) {

				uniqueUsers.add((User)member);

			} else if (recurse) {

				if (!seenGroups.contains(member)) {

					uniqueUsers.addAll(getUniqueUsersForGroup(member.as(Group.class), seenGroups, recurse));
				}
			}
		}

		return uniqueUsers;
	}

	public static boolean sendEvent(final String name, final String data, final Group target) {
		return sendEvent(name, data, Set.of(target));
	}

	public static boolean sendEvent(final String name, final String data, final User target) {

		boolean targetSeen = false;

		for (StructrEventSource es : eventSources) {

			if (shouldReceiveMessage(es, target)) {

				es.sendEvent(name, data);
				targetSeen = true;
			}
		}

		return targetSeen;
	}

	private static boolean shouldReceiveMessage(final StructrEventSource es, final User target) {

		final String[] ids = target.getSessionIds();

		return ids != null && Arrays.asList(ids).contains(es.getSessionId());
	}

	// ---- interface Feature -----
	@Override
	public String getModuleName() {
		return "ui";
	}

	protected void assertInitialized() throws FrameworkException {

		final Services services = Services.getInstance();
		if (!services.isInitialized()) {
			throw new FrameworkException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, services.getUnavailableMessage());
		}
	}

	// ----- nested classes -----
	private class StructrEventSource implements EventSource {

		private Emitter emitter = null;
		private final String sessionId;

		public StructrEventSource(final String sessionId) {

			super();

			this.sessionId = sessionId;
		}

		@Override
		public void onOpen(EventSource.Emitter emtr) throws IOException {

			emitter = emtr;

			eventSources.add(this);
		}

		@Override
		public void onClose() {
			remove();
		}

		public String getSessionId() {
			return sessionId;
		}

		public void sendEvent(final String name, final String data) {
			try {
				emitter.event(name, data);
			} catch (IOException ioe) {
				emitter.close();
				remove();
			}
		}

		public void close() {
			emitter.close();
		}

		public void remove() {
			eventSources.remove(this);
		}
	}
}
