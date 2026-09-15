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
package org.structr.test.web.advanced;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.filter.session.SessionFilter;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.UserTraitDefinition;
import org.structr.rest.auth.AuthHelper;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.servlet.HtmlServlet;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.testng.AssertJUnit.*;

/**
 *
 */
public class UserSelfRegistrationTest extends StructrUiTest {

	@Test
	public void testUserSelfRegistration() {

		// since we cannot test the mail confirmation workflow, we just disable sending an e-mail
		Settings.SmtpTesting.setValue(true);

		// enable self-registration and auto-login
		Settings.RestUserAutocreate.setValue(true);
		Settings.RestUserAutologin.setValue(true);

		final String eMail = uniqueEMail();
		String id          = null;
		String confKey     = null;

		// switch to REST servlet
		RestAssured.basePath = restUrl;

		grant("_registration", UiAuthenticator.NON_AUTH_USER_POST, true);
		grant("_login",        UiAuthenticator.NON_AUTH_USER_POST, false);

		// verify self registration
		RestAssured
			.given()
				.body("{ name: '" + eMail + "',  eMail: '" + eMail + "' }")
			.expect()
			.statusCode(201)
			.when()
			.post("/registration");

		try (final Tx tx = app.tx()) {

			final NodeInterface user = app.nodeQuery(StructrTraits.USER).getFirst();

			assertNotNull("User was not created", user);

			// store ID for later user
			id      = user.getProperty(Traits.of(StructrTraits.USER).key(GraphObjectTraitDefinition.ID_PROPERTY));
			confKey = user.getProperty(Traits.of(StructrTraits.USER).key(UserTraitDefinition.CONFIRMATION_KEY_PROPERTY));

			assertNotNull("Confirmation key was not set", confKey);

			tx.success();

		} catch (FrameworkException t) {

			fail("Unexpected exception.");
		}

		// switch to HTML servlet
		RestAssured.basePath = htmlUrl;

		// access the user confirmation page
		RestAssured
			.given()
				.param(HtmlServlet.CONFIRMATION_KEY_KEY, confKey)
			.expect()
			.statusCode(200)
			.when()
			.get(HtmlServlet.CONFIRM_REGISTRATION_PAGE);

		// verify that the user has no confirmation key
		try (final Tx tx = app.tx()) {

			final NodeInterface user = app.nodeQuery(StructrTraits.USER).getFirst();

			assertNotNull("User was not created", user);

			// store ID for later user
			id      = user.getProperty(Traits.of(StructrTraits.USER).key(GraphObjectTraitDefinition.ID_PROPERTY));
			confKey = user.getProperty(Traits.of(StructrTraits.USER).key(UserTraitDefinition.CONFIRMATION_KEY_PROPERTY));

			assertNull("Confirmation key was set after confirmation", confKey);

			tx.success();

		} catch (FrameworkException t) {

			fail("Unexpected exception.");
		}
	}

	@Test
	public void testUserSelfRegistrationWithRedirect() {

		// since we cannot test the mail confirmation workflow, we just disable sending an e-mail
		Settings.SmtpTesting.setValue(true);

		// enable self-registration and auto-login
		Settings.RestUserAutocreate.setValue(true);
		Settings.RestUserAutologin.setValue(true);

		final SessionFilter sessionFilter = new SessionFilter();
		final String eMail                = uniqueEMail();
		String id                         = null;
		String confKey                    = null;

		// switch to REST servlet
		RestAssured.basePath = restUrl;

		grant("_registration", UiAuthenticator.NON_AUTH_USER_POST, true);
		grant("_login",        UiAuthenticator.NON_AUTH_USER_POST, false);

		// verify self registration
		RestAssured
			.given()
				.filter(sessionFilter)
				.body("{ name: '" + eMail + "',  eMail: '" + eMail + "' }")
			.expect()
			.statusCode(201)
			.when()
			.post("/registration");

		try (final Tx tx = app.tx()) {

			final NodeInterface user = app.nodeQuery(StructrTraits.USER).getFirst();

			assertNotNull("User was not created", user);

			// store ID for later user
			id      = user.getProperty(Traits.of(StructrTraits.USER).key(GraphObjectTraitDefinition.ID_PROPERTY));
			confKey = user.getProperty(Traits.of(StructrTraits.USER).key(UserTraitDefinition.CONFIRMATION_KEY_PROPERTY));

			assertNotNull("Confirmation key was not set", confKey);

			tx.success();

		} catch (FrameworkException t) {

			fail("Unexpected exception.");
		}

		// create redirect page
		try (final Tx tx = app.tx()) {

			makeVisible(Page.createSimplePage(securityContext, "error"), true);
			makeVisible(Page.createSimplePage(securityContext, "success"), false);

			tx.success();

		} catch (FrameworkException fex) {}

		// switch to HTML servlet
		RestAssured.basePath = htmlUrl;

		/* Confirming logs the user in, and a login rotates the session id (ticket 1594), so the browser
		   leaves this request with a different session than it arrived with. The redirect is therefore
		   followed by hand with the id the server just handed out - following it inside RestAssured makes
		   the test depend on whether its SessionFilter notices the new cookie, which is a property of the
		   test harness and not of Structr. The target page is visible to authenticated users only, so
		   rendering it is what proves the new session is the logged-in one. */
		final Response confirmation = RestAssured
			.given()
				.filter(sessionFilter)
				.redirects().follow(false)
				.param(HtmlServlet.CONFIRMATION_KEY_KEY, confKey)
				.param(HtmlServlet.TARGET_PATH_KEY, "success")
			.expect()
			.statusCode(302)
			.when()
			.get(HtmlServlet.CONFIRM_REGISTRATION_PAGE)
			.andReturn();

		final String rotatedSessionId = confirmation.getSessionId();

		assertNotNull("Confirmation did not answer with a session", rotatedSessionId);

		RestAssured
			.given()
				.sessionId(rotatedSessionId)
			.expect()
			.statusCode(200)
			.body("html.head.title", Matchers.equalTo("Success"))
			.body("html.body.h1", Matchers.equalTo("Success"))
			.body("html.body.div", Matchers.equalTo("Initial body text"))
			.when()
			.get(confirmation.getHeader("Location"));

		// verify that the user has no confirmation key
		try (final Tx tx = app.tx()) {

			final NodeInterface user = app.nodeQuery(StructrTraits.USER).getFirst();

			assertNotNull("User was not created", user);

			assertNull("Confirmation key was set after confirmation", user.getProperty(Traits.of(StructrTraits.USER).key(UserTraitDefinition.CONFIRMATION_KEY_PROPERTY)));

			final String[] sessionIds  = user.getProperty(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.SESSION_IDS_PROPERTY));

			assertEquals("Invalid number of sessions after user confirmation", 1, sessionIds.length);
			// the rotated id, not the one the browser brought along - that one is worthless now
			assertEquals("Invalid session ID after user confirmation", StringUtils.substringBeforeLast(rotatedSessionId, "."), sessionIds[0]);

			tx.success();

		} catch (FrameworkException t) {

			fail("Unexpected exception.");
		}
	}

	@Test
	public void testResetPassword() {

		final String eMail = uniqueEMail();
		String id          = null;

		// since we cannot test the mail confirmation workflow, we just disable sending an e-mail
		Settings.SmtpTesting.setValue(true);

		// switch to REST servlet
		RestAssured.basePath = restUrl;

		grant("_resetPassword", UiAuthenticator.NON_AUTH_USER_POST, true);
		grant("_login",          UiAuthenticator.NON_AUTH_USER_POST, false);

		try (final Tx tx = app.tx()) {

			final NodeInterface user = app.create(StructrTraits.USER,
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(NodeInterfaceTraitDefinition.NAME_PROPERTY), "tester"),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.EMAIL_PROPERTY), eMail),
				new NodeAttribute<>(Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY), "correct")
			);

			// store ID for later user
			id = user.getUuid();

			tx.success();

		} catch (Throwable t) {

			fail("Unexpected exception.");
		}

		// verify failing login
		RestAssured
			.given()
				.body("{ eMail: '" + eMail + "', password: 'incorrect' }")
			.expect()
			.statusCode(401)
			.body("code", equalTo(401))
			.body("message", equalTo(AuthHelper.STANDARD_ERROR_MSG))
			.when()
			.post("/login");

		// verify successful login
		RestAssured
			.given()
				.body("{ eMail: '" + eMail + "', password: 'correct' }")
			.expect()
			.statusCode(200)
			.body("result.type",   equalTo(StructrTraits.USER))
			.body("result.name",   equalTo("tester"))
			.body("result.isUser", equalTo(true))
			.body("result.id",     equalTo(id))
			.when()
			.post("/login");

		// verify reset password doesn't disclose information about existing users
		RestAssured
			.given()
				.body("{ eMail: 'unknown@structr.com' }")
			.expect()
			.statusCode(200)
			.when()
			.post("/reset-password");

		RestAssured
			.given()
				.body("{ eMail: '" + eMail + "' }")
			.expect()
			.statusCode(200)
			.when()
			.post("/reset-password");

	}

	// ----- private methods -----
	private <T extends DOMNode> T makeVisible(final T src, final boolean publicToo) {

		try {

			src.setVisibility(publicToo, true);

		} catch (FrameworkException fex) {}

		src.getAllChildNodes().stream().forEach((n) -> {

			try {

				n.setVisibility(publicToo, true);

			} catch (FrameworkException fex) {}
		} );

		return src;
	}

	/**
	 * A registration address nobody has used before, per test method and per attempt.
	 *
	 * <p>EmailRateLimiter allows three registrations per address per hour, and its cache is static, so
	 * it outlives a test method: with every method registering "test@structr.com", the budget was shared
	 * across the class and a retry could spend the last of it. The endpoint answers 201 either way -
	 * deliberately, so a caller cannot tell "accepted" from "throttled" - so the test failed later, at
	 * "User was not created", which points nowhere near the cause.</p>
	 */
	private String uniqueEMail() {

		return "test-" + java.util.UUID.randomUUID().toString().replace("-", "") + "@structr.com";
	}
}
