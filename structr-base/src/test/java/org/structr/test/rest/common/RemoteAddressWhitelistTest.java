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
package org.structr.test.rest.common;

import jakarta.servlet.http.HttpServletRequest;
import org.structr.api.config.Settings;
import org.structr.rest.common.RemoteAddressWhitelist;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Set;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Ticket 1599: the health, histogram and metrics endpoints are reachable without authentication and are
 * guarded by nothing but the caller's address - and the address they compared was the one the TCP
 * connection came from.
 *
 * <p>Behind a reverse proxy that is the proxy, and on the same host the proxy is 127.0.0.1, which is what
 * every one of those allowlists contains by default. So every client in the world matched, and
 * /structr/health, /structr/histogram and /structr/metrics were public in all but name - memory and
 * thread figures, per-endpoint statistics and the text of executed Cypher statements.
 *
 * <p>These tests use a stand-in request rather than a server, because what is being decided here is a
 * question about two header values and one address, and that is the whole of it.
 */
public class RemoteAddressWhitelistTest {

	private static final Set<String> WHITELIST = Set.of("127.0.0.1", "localhost", "::1");

	private Boolean previousForwardedFor;

	@BeforeMethod
	public void setUp() {

		previousForwardedFor = Settings.ForwardedForEnabled.getValue();

		Settings.ForwardedForEnabled.setValue(false);
	}

	@AfterMethod
	public void tearDown() {

		Settings.ForwardedForEnabled.setValue(previousForwardedFor);
	}

	@Test
	public void testAnAddressOnTheListIsAdmitted() {

		assertTrue("a whitelisted address was refused", RemoteAddressWhitelist.isWhitelisted(request("127.0.0.1", Map.of()), WHITELIST, "test"));
	}

	@Test
	public void testAnAddressThatIsNotOnTheListIsRefused() {

		assertFalse("an address outside the whitelist was admitted", RemoteAddressWhitelist.isWhitelisted(request("203.0.113.7", Map.of()), WHITELIST, "test"));
	}

	/**
	 * The attack, and it needs nothing but a request through the proxy the deployment already has: the
	 * address being compared is the proxy's, the allowlist contains the proxy's address, and so the check
	 * passes for everyone. Refusing is the only honest answer, because the question the allowlist asks
	 * cannot be answered from this request at all.
	 */
	@Test
	public void testARequestThatCameThroughAProxyIsRefusedWhileTheProxyAddressIsNotResolved() {

		assertFalse("a request forwarded by a proxy was admitted on the proxy's own address (X-Forwarded-For)",
			RemoteAddressWhitelist.isWhitelisted(request("127.0.0.1", Map.of("X-Forwarded-For", "203.0.113.7")), WHITELIST, "test"));

		assertFalse("a request forwarded by a proxy was admitted on the proxy's own address (Forwarded)",
			RemoteAddressWhitelist.isWhitelisted(request("127.0.0.1", Map.of("Forwarded", "for=203.0.113.7")), WHITELIST, "test"));
	}

	/**
	 * The counterpart, or the rule above would simply break every installation behind a proxy: with
	 * httpservice.forwardedfor switched on, getRemoteAddr() is the caller's address rather than the
	 * proxy's, and the allowlist means what it says again.
	 */
	@Test
	public void testAForwardedRequestIsCheckedNormallyWhenTheCallerAddressIsResolved() {

		Settings.ForwardedForEnabled.setValue(true);

		assertTrue("a resolved caller address on the whitelist was refused",
			RemoteAddressWhitelist.isWhitelisted(request("127.0.0.1", Map.of("X-Forwarded-For", "127.0.0.1")), WHITELIST, "test"));

		assertFalse("a resolved caller address outside the whitelist was admitted",
			RemoteAddressWhitelist.isWhitelisted(request("203.0.113.7", Map.of("X-Forwarded-For", "203.0.113.7")), WHITELIST, "test"));
	}

	/**
	 * A request with no address to check is a request the whitelist cannot admit. Two of the three
	 * servlets used to skip their check entirely in that case and serve the data.
	 */
	@Test
	public void testARequestWithoutARemoteAddressIsRefused() {

		assertFalse("a request without a remote address was admitted", RemoteAddressWhitelist.isWhitelisted(request(null, Map.of()), WHITELIST, "test"));
	}

	// ----- private methods -----

	/**
	 * A request that answers the two questions the check asks and nothing else. Anything the check does
	 * not ask for answers null, so a call this test did not anticipate shows up as such rather than as a
	 * quietly plausible value.
	 */
	private static HttpServletRequest request(final String remoteAddress, final Map<String, String> headers) {

		return (HttpServletRequest) Proxy.newProxyInstance(RemoteAddressWhitelistTest.class.getClassLoader(), new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> {

				if ("getRemoteAddr".equals(method.getName())) {

					return remoteAddress;
				}

				if ("getHeader".equals(method.getName())) {

					return headers.get((String) args[0]);
				}

				return null;
			});
	}
}
