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
package org.structr.rest.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;

import java.util.Set;

/**
 * The address check behind the health, histogram and metrics endpoints, which are reachable without
 * authentication and are therefore guarded by nothing but the caller's address.
 *
 * <p>Ticket 1599: the three servlets each compared getRemoteAddr() against their own allowlist, and the
 * address that produces is the one the TCP connection came from. Put a reverse proxy in front - the
 * ordinary deployment - and that address is the proxy's. On the same host it is 127.0.0.1, which is
 * exactly what an allowlist tends to contain, so every client in the world matched it and /structr/health
 * (memory, threads, per-endpoint statistics), /structr/histogram (the text of executed Cypher statements)
 * and /structr/metrics were public in all but name.
 *
 * <p>The answer here is not to authenticate them - that is deliberately not on the table for endpoints a
 * monitoring system scrapes - but to refuse the case the allowlist cannot speak about. When forwarding
 * headers are present while {@code httpservice.forwardedfor} is off, the address being compared belongs
 * to the proxy rather than to the caller, and a match means nothing. That is refused rather than served,
 * because a check that cannot be made is not a check that passed.
 */
public class RemoteAddressWhitelist {

	private static final Logger logger = LoggerFactory.getLogger(RemoteAddressWhitelist.class.getName());

	/**
	 * The headers a reverse proxy uses to pass the original client address on. Their presence is what
	 * tells this code that getRemoteAddr() is reporting a hop rather than a caller.
	 */
	private static final String[] FORWARDING_HEADERS = { "X-Forwarded-For", "Forwarded" };

	public static boolean isWhitelisted(final HttpServletRequest request, final Set<String> whitelist, final String endpoint) {

		final String remoteAddress = request.getRemoteAddr();
		if (remoteAddress == null) {

			logger.warn("Access to {} denied: the request has no remote address to check against the whitelist.", endpoint);

			return false;
		}

		if (!Settings.ForwardedForEnabled.getValue() && hasForwardingHeader(request)) {

			logger.warn("Access to {} denied for remote address {}: the request carries a forwarding header, so this address is the proxy's and not the caller's, and the whitelist cannot be applied to it. "
				+ "Set {} to true so that the caller's address is used - and make sure the proxy overwrites the forwarding headers it receives.",
				endpoint, remoteAddress, Settings.ForwardedForEnabled.getKey());

			return false;
		}

		if (!whitelist.contains(remoteAddress)) {

			logger.warn("Access to {} denied for remote address {}: not in whitelist.", endpoint, remoteAddress);

			return false;
		}

		return true;
	}

	// ----- private methods -----

	private static boolean hasForwardingHeader(final HttpServletRequest request) {

		for (final String header : FORWARDING_HEADERS) {

			if (request.getHeader(header) != null) {

				return true;
			}
		}

		return false;
	}
}
