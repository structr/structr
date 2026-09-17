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
package org.structr.web.auth;

import com.github.scribejava.core.pkce.PKCE;
import com.github.scribejava.core.pkce.PKCEService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.core.graph.NodeServiceCommand;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * One authorization round trip to an OAuth2 provider, from /oauth/&lt;provider&gt;/login to the
 * callback at /oauth/&lt;provider&gt;/auth.
 *
 * <p>The point of this class is that the round trip belongs to <b>one browser</b>. Before ticket 1593,
 * the state parameter was a random UUID in a static cache and nothing else: any browser that arrived
 * at the callback with a state - or with no state at all - was logged in as whoever the authorization
 * code belonged to. An attacker who runs the flow with their own account at the provider, intercepts
 * their own {@code code} and then sends the victim a link to
 * {@code https://app/oauth/&lt;provider&gt;/auth?code=...} has the victim's browser logged into the
 * attacker's account, and everything the victim types from then on is stored in it. That is login CSRF,
 * and the state parameter exists precisely to prevent it - but only if it is bound to the browser that
 * started the flow.
 *
 * <p>The binding is a short-lived, HttpOnly, SameSite=Lax cookie holding the state value, Secure
 * whenever the session cookie is. A flow is only accepted when the state in the callback URL and the
 * state in the cookie are both present and equal, so an attacker who cannot write cookies for this host
 * cannot produce an acceptable callback. What that does not cover is a sibling subdomain, which can
 * write cookies for this host: an installation that shares a registrable domain with something
 * untrusted is outside what this defends. The {@code __Host-} cookie prefix would close that, and is
 * not used because it forces Secure and would take plain-HTTP installations with it.
 *
 * <p>The cookie is used rather than the HTTP session because the
 * session cookie's SameSite attribute is configurable ({@code httpservice.cookies.samesite}) and Strict
 * would keep the browser from sending it on the cross-site redirect back from the provider - that is,
 * the hardening would silently turn into a broken login.
 *
 * <p>The flow also carries the PKCE code verifier, which makes an intercepted authorization code
 * useless to anyone who did not start this flow, and the OIDC nonce, which ties an id_token the
 * provider returns to this request rather than to a replayed earlier one. Both of those depend on the
 * provider taking part: against one that implements neither, the state binding above is the whole of
 * the protection.
 */
public class OAuth2Flow {

	private static final Logger logger = LoggerFactory.getLogger(OAuth2Flow.class.getName());

	/**
	 * The cookie that binds a pending flow to the browser that started it. Deliberately not the session
	 * cookie: see the class comment.
	 */
	public static final String STATE_COOKIE_NAME = "structr_oauth_state";

	/**
	 * How long a started flow stays redeemable. Long enough for a user to type a password and answer an
	 * MFA prompt at the provider, short enough that an abandoned flow does not stay open all day.
	 */
	private static final int FLOW_TIMEOUT_SECONDS = 600;

	/* Bounded on both axes: a caller can start flows as fast as it likes, and every started flow that is
	   never redeemed would otherwise sit here forever. */
	private static final Cache<String, OAuth2Flow> pendingFlows = CacheBuilder.newBuilder()
		.maximumSize(1000)
		.expireAfterWrite(FLOW_TIMEOUT_SECONDS, TimeUnit.SECONDS)
		.build();

	private final Map<String, String[]> parameters;
	private final String provider;
	private final String state;
	private final String nonce;
	private final PKCE pkce;

	private OAuth2Flow(final String provider, final Map<String, String[]> parameters) {

		/* Copied, not referenced: the map a servlet container hands out belongs to the request, and the
		   request object is recycled when the request completes - while this flow lives on until the
		   callback, minutes later and on a request of its own. */
		this.parameters = new HashMap<>(parameters);
		this.provider   = provider;
		this.state      = NodeServiceCommand.getNextUuid();
		this.nonce      = NodeServiceCommand.getNextUuid();
		this.pkce       = PKCEService.defaultInstance().generatePKCE();
	}

	/**
	 * Starts a flow for the given provider, remembers the parameters of the original request and binds
	 * the flow to the calling browser by setting the state cookie on the response.
	 */
	public static OAuth2Flow start(final HttpServletRequest request, final HttpServletResponse response, final String provider) {

		final OAuth2Flow flow = new OAuth2Flow(provider, request.getParameterMap());

		pendingFlows.put(flow.state, flow);

		response.addHeader("Set-Cookie", stateCookie(flow.state, FLOW_TIMEOUT_SECONDS));

		return flow;
	}

	/**
	 * Redeems the flow a callback refers to, or null if the request does not carry a state that this
	 * browser started for this provider. A flow that is found is taken out of the cache as it is handed
	 * out, and the cookie is cleared in the same breath, so a state is good for exactly one callback - a
	 * replayed callback URL, from a browser history entry or a proxy log, finds nothing. A callback that
	 * matches nothing leaves both alone; see the comment at the cookie.
	 */
	public static OAuth2Flow consume(final HttpServletRequest request, final HttpServletResponse response, final String provider) {

		final String state      = request.getParameter("state");
		final String boundState = stateCookieValue(request);

		if (StringUtils.isBlank(state) || StringUtils.isBlank(boundState)) {

			logger.warn("Refusing OAuth callback for provider {}: the request carries {}, so it cannot be the answer to a flow this browser started. "
				+ "This is what a login CSRF attempt looks like; it is also what a callback replayed from a browser history entry looks like.",
				provider, StringUtils.isBlank(state) ? "no state parameter" : "no state cookie");

			return null;
		}

		if (!StringUtils.equals(state, boundState)) {

			logger.warn("Refusing OAuth callback for provider {}: the state parameter does not belong to the flow this browser started.", provider);

			return null;
		}

		/* Cleared here and not before the checks above: a callback that does not match the cookie belongs
		   to no flow of this browser's, and clearing on the way past would take down whatever login the
		   browser does have in flight - a second tab, or the one a stray link arrived during. Only a
		   cookie that has just been used up is worth removing. */
		response.addHeader("Set-Cookie", stateCookie("", 0));

		/* Removed and read in one step, so two callbacks racing on the same state cannot both come away
		   with the flow. Guava's cache is thread-safe per operation, which a getIfPresent() followed by an
		   invalidate() is not. */
		final OAuth2Flow flow = pendingFlows.asMap().remove(state);
		if (flow == null) {

			logger.warn("Refusing OAuth callback for provider {}: no pending flow for the given state. It was redeemed already, was older than {} seconds, or was pushed out of the cache by newer flows.", provider, FLOW_TIMEOUT_SECONDS);

			return null;
		}

		if (!StringUtils.equals(flow.provider, provider)) {

			logger.warn("Refusing OAuth callback for provider {}: the flow was started for provider {}.", provider, flow.provider);

			return null;
		}

		return flow;
	}

	public Map<String, String[]> getParameters() {

		return parameters;
	}

	public String getState() {

		return state;
	}

	public String getNonce() {

		return nonce;
	}

	public PKCE getPKCE() {

		return pkce;
	}

	public String getCodeVerifier() {

		return pkce.getCodeVerifier();
	}

	// ----- private methods -----

	private static String stateCookieValue(final HttpServletRequest request) {

		final Cookie[] cookies = request.getCookies();
		if (cookies == null) {

			return null;
		}

		for (final Cookie cookie : cookies) {

			if (STATE_COOKIE_NAME.equals(cookie.getName())) {

				return cookie.getValue();
			}
		}

		return null;
	}

	/**
	 * Builds the Set-Cookie header value, written out here rather than through response.addCookie() so
	 * that every attribute this cookie carries is visible in one place.
	 *
	 * <p>SameSite is the one that does the work: Lax is what makes the browser send the cookie on the
	 * top-level redirect coming back from the provider while keeping it away from cross-site requests
	 * that are not that redirect.
	 */
	private static String stateCookie(final String value, final int maxAge) {

		final StringBuilder buf = new StringBuilder();

		buf.append(STATE_COOKIE_NAME).append("=").append(value);
		buf.append("; Path=/");
		buf.append("; Max-Age=").append(maxAge);
		buf.append("; HttpOnly");
		buf.append("; SameSite=Lax");

		// governed by the same switch as the session cookie, so an installation that has decided its
		// cookies do not travel in the clear does not have to decide it a second time for this one
		if (Settings.CookieSecure.getValue()) {

			buf.append("; Secure");
		}

		return buf.toString();
	}
}
