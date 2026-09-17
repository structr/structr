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
package org.structr.test.web.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.structr.api.config.Settings;
import org.structr.web.auth.OAuth2ProviderRegistry;
import org.structr.web.auth.provider.GithubOAuthClient;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Proxy;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Ticket 1593, the GitHub half of the unverified-address rule.
 *
 * <p>AbstractOAuth2Client refuses an address the provider reports as unverified, but it looks for the
 * OIDC claim {@code email_verified} - and GitHub sends no such claim, nor does it go through the base
 * class at all: GithubOAuthClient has its own getClientCredentials and its own parseUserCredentials.
 * What GitHub does report is a {@code verified} flag per address on {@code /user/emails}, which this
 * client already fetches for a different reason. An address it does not vouch for is an address anybody
 * with a GitHub account can type into their profile, and Structr looks local accounts up by it.
 *
 * <p>No container and no network here: both pieces under test take the response body as a string, which
 * is the whole of what the decision rests on. What is therefore not covered is the wiring in
 * getClientCredentials that calls them - that one needs GitHub or a stand-in for it, and the round trip
 * it performs is unchanged.
 */
public class GithubEmailVerificationTest {

	private static final String EMAILS = """
		[
		  { "email": "primary@example.com",   "primary": true,  "verified": true  },
		  { "email": "Second@Example.com",    "primary": false, "verified": true  },
		  { "email": "unverified@example.com","primary": false, "verified": false }
		]
		""";

	private String previousBaseUrl;
	private String previousClientId;
	private String previousClientSecret;

	@BeforeMethod
	public void setUp() {

		/* The client resolves its redirect, return, error and logout URIs in the constructor, and with a
		   base URL configured it never looks at the request while doing so - which is what lets this test
		   build one without a servlet container underneath. The client id is needed because ScribeJava's
		   ServiceBuilder refuses a blank id or secret; none of the three is used by what is tested here. */
		previousBaseUrl      = Settings.BaseUrlOverride.getValue();
		previousClientId     = oauthSetting("client_id").getValue();
		previousClientSecret = oauthSetting("client_secret").getValue();

		Settings.BaseUrlOverride.setValue("http://localhost:8082/");
		oauthSetting("client_id").setValue("test-client-id");
		oauthSetting("client_secret").setValue("test-client-secret");
	}

	@AfterMethod
	public void tearDown() {

		Settings.BaseUrlOverride.setValue(previousBaseUrl);
		oauthSetting("client_id").setValue(previousClientId);
		oauthSetting("client_secret").setValue(previousClientSecret);
	}

	private static org.structr.api.config.Setting<String> oauthSetting(final String key) {

		return Settings.getOrCreateStringSetting("oauth", "github", key);
	}

	@Test
	public void testOnlyVerifiedAddressesAreCollected() {

		final Set<String> verified = client().verifiedAddresses(EMAILS);

		assertTrue("the verified primary address is missing", verified.contains("primary@example.com"));
		assertTrue("addresses are compared lower-cased, so the mixed-case one has to be stored that way", verified.contains("second@example.com"));
		assertFalse("an address GitHub reports as unverified was collected", verified.contains("unverified@example.com"));
		assertEquals("unexpected number of verified addresses", 2, verified.size());
	}

	/**
	 * The rule itself. The primary address is the one the client falls back to when the public profile
	 * carries none, so "primary" must not be read as "verified" - GitHub reports the two separately.
	 */
	@Test
	public void testAnUnverifiedAddressIsRefused() {

		final GithubClient client   = client();
		final Set<String> verified  = client.verifiedAddresses(EMAILS);

		assertTrue("a verified address was refused", client.isVerified("primary@example.com", verified));
		assertTrue("the comparison is case sensitive, so an address from the profile would be refused", client.isVerified("SECOND@example.com", verified));

		assertFalse("an address GitHub does not vouch for was accepted", client.isVerified("unverified@example.com", verified));
		assertFalse("an address that is not on the account at all was accepted", client.isVerified("attacker@example.com", verified));
	}

	/**
	 * An account with no verified address is an answer, not a gap: nothing on it may identify a local
	 * account.
	 */
	@Test
	public void testAnEmptyListRefusesEverything() {

		final GithubClient client  = client();
		final Set<String> verified = client.verifiedAddresses("[]");

		assertEquals("an empty list should produce an empty set, not null", 0, verified.size());
		assertFalse("an address was accepted although the account lists none", client.isVerified("anyone@example.com", verified));
	}

	/**
	 * And the case that must NOT refuse: a token whose scope does not include {@code user:email} gets an
	 * error object from that endpoint instead of a list. That is the absence of information rather than
	 * a statement that nothing is verified, and refusing there would lock out an installation that has
	 * been running on a narrower scope.
	 */
	@Test
	public void testAnUnreadableResponseIsNotTakenAsARefusal() {

		final GithubClient client = client();

		for (final String body : new String[] { "{ \"message\": \"Requires authentication\" }", "", "not json at all", null }) {

			final Set<String> verified = client.verifiedAddresses(body);

			assertNull("an unusable /user/emails response should carry no information, but produced a set: " + body, verified);
			assertTrue("an unusable /user/emails response was taken as a refusal: " + body, client.isVerified("someone@example.com", verified));
		}
	}

	// ----- private methods -----

	private GithubClient client() {

		return new GithubClient();
	}

	/**
	 * Reaches the two protected members the decision is made of. Subclassing rather than reflection, so
	 * a change to either signature breaks this at compile time.
	 */
	private static final class GithubClient extends GithubOAuthClient {

		private GithubClient() {

			super(requestThatIsNeverUsed(), OAuth2ProviderRegistry.get("github"));
		}

		private Set<String> verifiedAddresses(final String emailResponse) {

			return getVerifiedEmailAddresses(emailResponse);
		}

		private boolean isVerified(final String credential, final Set<String> verifiedAddresses) {

			return isVerifiedAddress(credential, verifiedAddresses);
		}
	}

	/**
	 * A request the constructor does not touch, because BaseUrlOverride is set. It answers defaults for
	 * primitives rather than null so that an accidental call fails as a wrong value instead of as a
	 * NullPointerException somewhere unrelated.
	 */
	private static HttpServletRequest requestThatIsNeverUsed() {

		return (HttpServletRequest) Proxy.newProxyInstance(GithubEmailVerificationTest.class.getClassLoader(), new Class<?>[] { HttpServletRequest.class }, (proxy, method, args) -> {

				final Class<?> returnType = method.getReturnType();
				if (int.class.equals(returnType)) {

					return 0;
				}

				if (boolean.class.equals(returnType)) {

					return false;
				}

				return null;
			});
	}
}
