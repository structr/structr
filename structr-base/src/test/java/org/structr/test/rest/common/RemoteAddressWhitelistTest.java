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
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;
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

	// ----- CIDR ranges -----

	private static final Set<String> BRIDGE = Set.of("127.0.0.1", "localhost", "::1", "10.0.0.0/24");

	@Test
	public void testAnAddressInsideACidrRangeIsAdmitted() {

		// the case this was built for: a container health-checked from its bridge gateway, whose address
		// changes whenever the container is recreated, so only the subnet can be configured for it
		assertTrue("an address inside the configured range was refused",
			RemoteAddressWhitelist.isWhitelisted(request("10.0.0.2", Map.of()), BRIDGE, "test"));

		assertTrue("the first address of the range was refused",
			RemoteAddressWhitelist.isWhitelisted(request("10.0.0.0", Map.of()), BRIDGE, "test"));

		assertTrue("the last address of the range was refused",
			RemoteAddressWhitelist.isWhitelisted(request("10.0.0.255", Map.of()), BRIDGE, "test"));
	}

	@Test
	public void testAnAddressOutsideACidrRangeIsRefused() {

		assertFalse("the address one past the end of the range was admitted",
			RemoteAddressWhitelist.isWhitelisted(request("10.0.1.0", Map.of()), BRIDGE, "test"));

		assertFalse("an address in a different private network was admitted",
			RemoteAddressWhitelist.isWhitelisted(request("192.168.0.2", Map.of()), BRIDGE, "test"));
	}

	@Test
	public void testThePrefixIsComparedBitwiseAndNotBytewise() {

		// /23 covers 10.0.0.0 to 10.0.1.255: the boundary falls inside a byte, which a bytewise
		// comparison would get wrong in one direction or the other
		final Set<String> whitelist = Set.of("10.0.0.0/23");

		assertTrue("an address in the second half of a /23 was refused",
			RemoteAddressWhitelist.isWhitelisted(request("10.0.1.7", Map.of()), whitelist, "test"));

		assertFalse("an address past the end of a /23 was admitted",
			RemoteAddressWhitelist.isWhitelisted(request("10.0.2.7", Map.of()), whitelist, "test"));
	}

	@Test
	public void testARangeOfOneFamilyNeverCoversTheOther() {

		assertFalse("an IPv4 range admitted an IPv6 address",
			RemoteAddressWhitelist.isWhitelisted(request("::1", Map.of()), Set.of("0.0.0.0/0"), "test"));

		assertFalse("an IPv6 range admitted an IPv4 address",
			RemoteAddressWhitelist.isWhitelisted(request("10.0.0.2", Map.of()), Set.of("::/0"), "test"));
	}

	@Test
	public void testAnIpv6RangeWorksTheSameWay() {

		final Set<String> whitelist = Set.of("fd00::/8");

		assertTrue("an address inside the IPv6 range was refused",
			RemoteAddressWhitelist.isWhitelisted(request("fd00::1", Map.of()), whitelist, "test"));

		assertFalse("an address outside the IPv6 range was admitted",
			RemoteAddressWhitelist.isWhitelisted(request("fe80::1", Map.of()), whitelist, "test"));
	}

	@Test
	public void testAnUnusableRangeCoversNothing() {

		// the failure this feature exists to prevent is a configuration that READS like a fix. An entry
		// that cannot be parsed must not fall back to admitting anything, and it is reported rather than ignored
		for (final String unusable : List.of("10.0.0.0/99", "10.0.0.0/-1", "10.0.0.0/eight", "not-an-address/24")) {

			assertFalse("the unusable entry '" + unusable + "' admitted an address",
				RemoteAddressWhitelist.isWhitelisted(request("10.0.0.2", Map.of()), Set.of(unusable), "test"));
		}
	}

	@Test
	public void testAnExactEntryStillWorksBesideARange() {

		// every existing configuration is a list of exact entries, and a hostname is not an address at all
		assertTrue("an exact address was refused once ranges were understood",
			RemoteAddressWhitelist.isWhitelisted(request("127.0.0.1", Map.of()), BRIDGE, "test"));

		assertTrue("a single address written as a /32 range was refused",
			RemoteAddressWhitelist.isWhitelisted(request("10.0.0.2", Map.of()), Set.of("10.0.0.2/32"), "test"));

		assertFalse("a hostname entry admitted an unrelated address",
			RemoteAddressWhitelist.isWhitelisted(request("10.0.0.2", Map.of()), Set.of("localhost"), "test"));
	}

	// ----- reading the setting -----

	@Test
	public void testParseKeepsWhatItCanApplyAndDropsWhatItCannot() {

		final Set<String> parsed = RemoteAddressWhitelist.parse(
			"127.0.0.1, localhost , ::1, 10.0.0.0/24, fd00::/8, 10.0.0.0/99, not-an-address/24, , 10.0.0.0/eight",
			"test.whitelist");

		assertEquals("parse kept the wrong number of entries: " + parsed, 5, parsed.size());

		// exact entries survive untouched, including a host name, which cannot be told from a typo
		assertTrue("an exact address was dropped",  parsed.contains("127.0.0.1"));
		assertTrue("a host name was dropped",       parsed.contains("localhost"));
		assertTrue("an IPv6 address was dropped",   parsed.contains("::1"));
		assertTrue("a valid IPv4 range was dropped", parsed.contains("10.0.0.0/24"));
		assertTrue("a valid IPv6 range was dropped", parsed.contains("fd00::/8"));

		// a range the code cannot apply is dropped rather than kept as a string that matches nothing
		assertFalse("a prefix wider than the address family was kept", parsed.contains("10.0.0.0/99"));
		assertFalse("a range with no network address was kept",        parsed.contains("not-an-address/24"));
		assertFalse("a range with a non-numeric prefix was kept",      parsed.contains("10.0.0.0/eight"));
	}

	@Test
	public void testAnUnusableEntryNeitherAdmitsNorBlocksTheRestOfTheList() {

		// the decision: a mistyped entry is ignored, never fatal. The usable entries beside it keep working
		final Set<String> parsed = RemoteAddressWhitelist.parse("10.0.0.0/99, 10.0.0.0/24", "test.whitelist");

		assertTrue("a usable range stopped working because another entry was mistyped",
			RemoteAddressWhitelist.isWhitelisted(request("10.0.0.2", Map.of()), parsed, "test"));

		assertFalse("a mistyped entry admitted an address outside every usable range",
			RemoteAddressWhitelist.isWhitelisted(request("192.168.0.2", Map.of()), parsed, "test"));
	}

	@Test
	public void testParseAnswersForAnEmptyOrAbsentSetting() {

		assertTrue("a null setting produced entries", RemoteAddressWhitelist.parse(null, "test.whitelist").isEmpty());
		assertTrue("a blank setting produced entries", RemoteAddressWhitelist.parse("  , ,", "test.whitelist").isEmpty());
	}

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
