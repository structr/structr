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
import org.structr.api.config.Setting;
import org.structr.common.LogThrottle;
import org.structr.api.config.Settings;

import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
	 * These three endpoints are unauthenticated, so the rate of refusals is the caller's to choose and a
	 * line per refusal is an amplification vector. One line per endpoint, address and reason per window.
	 */
	private static final LogThrottle deniedEndpointLog = new LogThrottle("Denied endpoint access", 1);

	/**
	 * The headers a reverse proxy uses to pass the original client address on. Their presence is what
	 * tells this code that getRemoteAddr() is reporting a hop rather than a caller.
	 */
	private static final String[] FORWARDING_HEADERS = { "X-Forwarded-For", "Forwarded" };

	/** setting name to the value it last held and what that parsed to; bounded by the number of settings. */
	private static final Map<String, Map.Entry<String, Set<String>>> parsedBySetting = new ConcurrentHashMap<>();


	public static boolean isWhitelisted(final HttpServletRequest request, final Set<String> whitelist, final String endpoint) {

		final String remoteAddress = request.getRemoteAddr();
		if (remoteAddress == null) {

			if (deniedEndpointLog.allow(endpoint + " no address")) {

				logger.warn("Access to {} denied: the request has no remote address to check against the whitelist.", endpoint);
			}

			return false;
		}

		if (!Settings.ForwardedForEnabled.getValue() && hasForwardingHeader(request)) {

			if (deniedEndpointLog.allow(endpoint + " forwarded " + remoteAddress)) {

				logger.warn("Access to {} denied for remote address {}: the request carries a forwarding header, so this address is the proxy's and not the caller's, and the whitelist cannot be applied to it. "
				+ "Set {} to true so that the caller's address is used - and make sure the proxy overwrites the forwarding headers it receives.",
					endpoint, remoteAddress, Settings.ForwardedForEnabled.getKey());
			}

			return false;
		}

		if (!matches(remoteAddress, whitelist)) {

			if (deniedEndpointLog.allow(endpoint + " " + remoteAddress)) {

				logger.warn("Access to {} denied for remote address {}: not in whitelist.", endpoint, remoteAddress);
			}

			return false;
		}

		return true;
	}

	/**
	 * Reads a whitelist setting into the entries the check can use.
	 *
	 * <p>An entry that states a range the code cannot make sense of is dropped and reported here, once,
	 * when the setting is read. It is dropped rather than refused because a mistyped allowlist must not
	 * stop an instance from starting, and it is reported rather than ignored because an entry like
	 * {@code 10.0.0.0/8} written before ranges were understood reads exactly like a fix while covering
	 * nothing. An entry without a prefix is kept as it stands, since a host name cannot be told apart
	 * from a typo.</p>
	 */
	/**
	 * The entries of a whitelist setting, parsed once per value it has held.
	 *
	 * <p>Every caller of this reads its setting on a request that is not authenticated, so parsing (and
	 * the reporting that goes with it) must not happen per request: a caller who can reach the endpoint
	 * could otherwise turn one bad entry into a log line per attempt. The cache is keyed by setting name,
	 * so what it holds is bounded by the configuration rather than by anything a caller supplies.</p>
	 */
	public static Set<String> parsed(final Setting<String> setting) {

		final String source                        = setting.getValue("");
		final Map.Entry<String, Set<String>> known = parsedBySetting.get(setting.getKey());

		if (known != null && known.getKey().equals(source)) {

			return known.getValue();
		}

		final Set<String> entries = Collections.unmodifiableSet(parse(source, setting.getKey()));

		parsedBySetting.put(setting.getKey(), Map.entry(source, entries));

		return entries;
	}

	public static Set<String> parse(final String source, final String settingKey) {

		final Set<String> entries = new LinkedHashSet<>();

		if (source == null) {

			return entries;
		}

		for (final String candidate : source.split(",")) {

			final String entry = candidate.trim();

			if (StringUtils.isBlank(entry)) {

				continue;
			}

			if (isUsable(entry)) {

				entries.add(entry);

			} else {

				logger.warn("Ignoring entry '{}' of {}: it is not an address range this can apply. Write a range as "
					+ "network/prefix, for example 10.0.0.0/24 or fd00::/8, and an exact address without a prefix. "
					+ "The entry is ignored and covers no address.", entry, settingKey);
			}
		}

		return entries;
	}

	/**
	 * Whether the address is covered by the whitelist, as a literal entry or by a CIDR range.
	 *
	 * <p>An entry without a slash is matched as the exact string it is, which is what keeps a hostname
	 * like {@code localhost} working and what every existing configuration relies on. An entry with one
	 * is read as {@code network/prefix} and compared bit by bit, so a container health-checked from its
	 * bridge gateway can be admitted by the subnet it appears on rather than by an address that changes
	 * whenever the container is recreated.</p>
	 */
	public static boolean matches(final String remoteAddress, final Set<String> whitelist) {

		if (whitelist.contains(remoteAddress)) {

			return true;
		}

		InetAddress candidate = null;

		for (final String entry : whitelist) {

			final int slash = entry.indexOf('/');
			if (slash < 0) {

				// an exact entry, and the check above already answered for it
				continue;
			}

			if (candidate == null) {

				candidate = literal(remoteAddress);

				if (candidate == null) {

					// not an address this can compare bitwise, so no range can cover it
					return false;
				}
			}

			if (covers(entry, slash, candidate)) {

				return true;
			}
		}

		return false;
	}

	// ----- private methods -----

	/**
	 * Whether the {@code network/prefix} entry covers the address.
	 *
	 * An entry that is not a range at all is reported once and then treated as covering nothing, because
	 * silently ignoring it is how a configuration that reads like a fix turns out not to be one.
	 */
	private static boolean covers(final String entry, final int slash, final InetAddress candidate) {

		final InetAddress network = literal(entry.substring(0, slash));
		final Integer prefix      = prefixLength(entry.substring(slash + 1));

		if (network == null || prefix == null || prefix > network.getAddress().length * 8) {

			// parse() drops these, so reaching one here means an unvalidated set: cover nothing
			return false;
		}

		final byte[] address = candidate.getAddress();
		final byte[] bounds  = network.getAddress();

		// an IPv4 range never covers an IPv6 address, nor the other way round
		if (address.length != bounds.length) {

			return false;
		}

		final int wholeBytes = prefix / 8;
		final int spareBits  = prefix % 8;

		for (int i = 0; i < wholeBytes; i++) {

			if (address[i] != bounds[i]) {

				return false;
			}
		}

		if (spareBits > 0) {

			final int mask = 0xFF << (8 - spareBits);

			return (address[wholeBytes] & mask) == (bounds[wholeBytes] & mask);
		}

		return true;
	}

	/** Whether an entry can be applied: anything without a prefix is taken as given, a range has to parse. */
	private static boolean isUsable(final String entry) {

		final int slash = entry.indexOf('/');

		if (slash < 0) {

			// an exact entry, which may be a host name and cannot be validated as an address
			return true;
		}

		final InetAddress network = literal(entry.substring(0, slash));
		final Integer prefix      = prefixLength(entry.substring(slash + 1));

		return network != null && prefix != null && prefix <= network.getAddress().length * 8;
	}

	/** Parses an address literal. Never a hostname, so checking an address cannot trigger a DNS lookup. */
	private static InetAddress literal(final String address) {

		try {

			return InetAddress.ofLiteral(address);

		} catch (final IllegalArgumentException notAnAddress) {

			return null;
		}
	}

	private static Integer prefixLength(final String prefix) {

		try {

			final int length = Integer.parseInt(prefix.trim());

			return length >= 0 ? length : null;

		} catch (final NumberFormatException notANumber) {

			return null;
		}
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
