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

import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.rest.common.HttpHelper;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URI;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1595: the address filter looked at the URL a caller passed in, and nothing looked at where the
 * request actually ended up. Redirects were followed by the client itself
 * (HttpHelper.getClient installed a plain LaxRedirectStrategy), so
 * {@code https://attacker/redir -> http://169.254.169.254/latest/meta-data/} passed every check there
 * was - and through RemoteDocument the answer comes back out again, indexed into extractedContent.
 *
 * <p>Literal addresses throughout, so nothing here depends on DNS or on reaching a network.
 */
public class SsrfProtectionTest {

	private Boolean previousSetting;

	@BeforeMethod
	public void enableProtection() {

		previousSetting = Settings.SsrfProtection.getValue();

		Settings.SsrfProtection.setValue(true);
	}

	@AfterMethod
	public void restoreSetting() {

		Settings.SsrfProtection.setValue(previousSetting);
	}

	/**
	 * The ranges an SSRF is aimed at. 169.254.169.254 is the cloud metadata service, which is the one
	 * that turns "the server fetches a URL for me" into credentials.
	 */
	@Test
	public void testInternalAddressesAreRefused() {

		for (final String address : new String[] {
			"http://169.254.169.254/latest/meta-data/",
			"http://127.0.0.1:7687/",
			"http://[::1]/",
			"http://10.0.0.1/",
			"http://192.168.1.1/",
			"http://172.16.0.1/",
			"http://100.64.0.1/",
			"http://[fd00::1]/",
			"http://0.0.0.0/"
		}) {

			try {

				HttpHelper.validateUrl(address);

				fail("validateUrl accepted an internal address: " + address);

			} catch (final FrameworkException expected) {
			}
		}
	}

	/** Not an address filter for everything: a public address stays usable. */
	@Test
	public void testPublicAddressesArePermitted() {

		try {

			HttpHelper.validateUrl("http://93.184.216.34/");

		} catch (final FrameworkException fex) {

			fail("validateUrl refused a public address: " + fex.getMessage());
		}
	}

	/**
	 * The question the redirect strategy asks on every hop, and the direction is what it asks about: a
	 * request pulled from outside into the internal network is the attack, a request that already
	 * started inside and stays there is an internal service talking to itself.
	 */
	@Test
	public void testRedirectsFromOutsideIntoTheInternalNetworkAreBlocked() {

		assertTrue("a redirect from a public host to the metadata service must be blocked",
			HttpHelper.isBlockedRedirect("93.184.216.34", URI.create("http://169.254.169.254/latest/meta-data/")));

		assertTrue("a redirect from a public host to loopback must be blocked",
			HttpHelper.isBlockedRedirect("93.184.216.34", URI.create("http://127.0.0.1:8082/structr/rest/User")));

		assertTrue("a redirect from a public host to a unique local address must be blocked",
			HttpHelper.isBlockedRedirect("93.184.216.34", URI.create("http://[fd00::1]/")));
	}

	/**
	 * The counterpart, and the reason the rule is about direction: refusing these would break an
	 * internal service that redirects, which is an ordinary thing for one to do.
	 */
	@Test
	public void testRedirectsThatDoNotCrossIntoTheInternalNetworkArePermitted() {

		assertFalse("a redirect between internal hosts must stay allowed",
			HttpHelper.isBlockedRedirect("127.0.0.1", URI.create("http://127.0.0.1:8082/target")));

		assertFalse("a redirect from an internal host outwards must stay allowed",
			HttpHelper.isBlockedRedirect("127.0.0.1", URI.create("http://93.184.216.34/")));

		assertFalse("a redirect between public hosts must stay allowed",
			HttpHelper.isBlockedRedirect("93.184.216.34", URI.create("http://93.184.216.35/")));
	}

	/**
	 * Non-http schemes never reach the address check at all. file: turns a fetcher into a local file
	 * reader; gopher: is the classic way of smuggling a payload of one's own into a plain-text service,
	 * which is what the memcached port below stands for.
	 */
	@Test
	public void testOnlyHttpSchemesAreAccepted() {

		for (final String address : new String[] { "file:///etc/passwd", "gopher://127.0.0.1:11211/", "ftp://127.0.0.1/" }) {

			try {

				HttpHelper.validateUrl(address);

				fail("validateUrl accepted a non-http scheme: " + address);

			} catch (final FrameworkException expected) {
			}
		}
	}

	/**
	 * The proxy is a second address the request goes to, and the one it actually connects to: whatever
	 * URL was asked for, the TCP connection is opened to the proxy's host and port. A caller-supplied
	 * proxy pointing inside is therefore the same attack by another route, and it had no test at all.
	 *
	 * <p>The refusal is asserted by its message, not just by an exception: validateProxyUrl refuses a
	 * malformed proxy URL as well, so a test that only asks for a FrameworkException would still pass
	 * with the address filter taken out.
	 */
	@Test
	public void testInternalProxyAddressesAreRefused() {

		for (final String proxyUrl : new String[] { "http://169.254.169.254:3128", "http://127.0.0.1:3128", "127.0.0.1:3128", "http://10.0.0.1:3128", "http://[fd00::1]:3128", "http://100.64.0.1:3128" }) {

			try {

				HttpHelper.validateProxyUrl(proxyUrl);

				fail("validateProxyUrl accepted an internal proxy address: " + proxyUrl);

			} catch (final FrameworkException expected) {

				assertTrue("refused " + proxyUrl + " for the wrong reason: " + expected.getMessage(), String.valueOf(expected.getMessage()).contains("internal network"));
			}
		}
	}

	/**
	 * The counterpart, so the rule above cannot pass by refusing everything: an ordinary outbound proxy
	 * stays usable, and no proxy at all is not an error.
	 */
	@Test
	public void testPublicProxyAddressesArePermitted() {

		for (final String proxyUrl : new String[] { "http://93.184.216.34:3128", "93.184.216.34:3128", "", null }) {

			try {

				HttpHelper.validateProxyUrl(proxyUrl);

			} catch (final FrameworkException fex) {

				fail("validateProxyUrl refused a usable proxy address " + proxyUrl + ": " + fex.getMessage());
			}
		}
	}

	/**
	 * A proxy address the client cannot connect to is refused before anything is attempted. Worth
	 * pinning because of the first entry: HttpHost.create, which parses exactly what the client will
	 * parse, does not accept a trailing slash - so a proxy configured as "http://proxy:3128/" is refused
	 * as malformed rather than used. That is existing behaviour and this test states it rather than
	 * leaving the next reader to find out.
	 */
	@Test
	public void testUnusableProxyAddressesAreRefused() {

		for (final String proxyUrl : new String[] { "http://93.184.216.34:3128/", "http:///", "://nonsense" }) {

			try {

				HttpHelper.validateProxyUrl(proxyUrl);

				fail("validateProxyUrl accepted an address it cannot connect to: " + proxyUrl);

			} catch (final FrameworkException expected) {

				// refused, which is the point
			}
		}
	}

	/**
	 * The whitelist check every outbound function goes through, now reachable for the ones that build
	 * their own request - HTTPPostMultiPartFunction and the Geoserver functions used to skip it.
	 */
	@Test
	public void testOutgoingAddressValidationRejectsNonHttpSchemes() {

		try {

			HttpHelper.validateOutgoingAddress("file:///etc/passwd");

			fail("validateOutgoingAddress accepted a file: URL");

		} catch (final FrameworkException expected) {
		}
	}
}
