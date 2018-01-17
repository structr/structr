/**
 * Copyright (C) 2010-2018 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.advanced;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.session.SessionFilter;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.equalTo;
import org.junit.Assert;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.servlet.HtmlServlet;

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

		final String eMail = "test@structr.com";
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

			final User user = app.nodeQuery(User.class).getFirst();

			assertNotNull("User was not created", user);

			// store ID for later user
			id      = user.getProperty(User.id);
			confKey = user.getProperty(User.confirmationKey);

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
				.param(HtmlServlet.CONFIRM_KEY_KEY, confKey)
			.expect()
			.statusCode(200)
			.when()
			.get(HtmlServlet.CONFIRM_REGISTRATION_PAGE);

		// verify that the user has no confirmation key
		try (final Tx tx = app.tx()) {

			final User user = app.nodeQuery(User.class).getFirst();

			assertNotNull("User was not created", user);

			// store ID for later user
			id      = user.getProperty(User.id);
			confKey = user.getProperty(User.confirmationKey);

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
		final String eMail                = "test@structr.com";
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

			final User user = app.nodeQuery(User.class).getFirst();

			assertNotNull("User was not created", user);

			// store ID for later user
			id      = user.getProperty(User.id);
			confKey = user.getProperty(User.confirmationKey);

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

		// expect 404 Not Found when logging in because Jetty or
		// RestAssured don't preserve the session ID
		RestAssured
			.given()
				.filter(sessionFilter)
				.param(HtmlServlet.CONFIRM_KEY_KEY, confKey)
				.param(HtmlServlet.TARGET_PAGE_KEY, "success")
			.expect()
			.statusCode(200)
			.body("html.head.title", Matchers.equalTo("Success"))
			.body("html.body.h1", Matchers.equalTo("Success"))
			.body("html.body.div", Matchers.equalTo("Initial body text"))
			.when()
			.get(HtmlServlet.CONFIRM_REGISTRATION_PAGE);

		// verify that the user has no confirmation key
		try (final Tx tx = app.tx()) {

			final User user = app.nodeQuery(User.class).getFirst();

			assertNotNull("User was not created", user);

			assertNull("Confirmation key was set after confirmation", user.getProperty(User.confirmationKey));

			final String[] sessionIds  = user.getProperty(User.sessionIds);

			Assert.assertEquals("Invalid number of sessions after user confirmation", 1, sessionIds.length);
			Assert.assertEquals("Invalid session ID after user confirmation", StringUtils.substringBeforeLast(sessionFilter.getSessionId(), "."), sessionIds[0]);

			tx.success();

		} catch (FrameworkException t) {
			fail("Unexpected exception.");
		}
	}

	@Test
	public void testResetPassword() {

		final String eMail = "test@structr.com";
		String id          = null;

		// since we cannot test the mail confirmation workflow, we just disable sending an e-mail
		Settings.SmtpTesting.setValue(true);

		// switch to REST servlet
		RestAssured.basePath = restUrl;

		grant("_resetPassword", UiAuthenticator.NON_AUTH_USER_POST, true);
		grant("_login",          UiAuthenticator.NON_AUTH_USER_POST, false);

		try (final Tx tx = app.tx()) {

			final User user = app.create(User.class,
				new NodeAttribute<>(User.name, "tester"),
				new NodeAttribute<>(User.eMail, eMail),
				new NodeAttribute<>(User.password, "correct")
			);

			// store ID for later user
			id = user.getProperty(User.id);

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
			.body("message", equalTo("Wrong username or password, or user is blocked. Check caps lock. Note: Username is case sensitive!"))
			.when()
			.post("/login");

		// verify successful login
		RestAssured
			.given()
				.body("{ eMail: '" + eMail + "', password: 'correct' }")
			.expect()
			.statusCode(200)
			.body("result.type",   equalTo("User"))
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

			src.setProperty(DOMNode.visibleToAuthenticatedUsers, true);
			if (publicToo) {

				src.setProperty(DOMNode.visibleToPublicUsers, true);
			}

		} catch (FrameworkException fex) {}

		src.getAllChildNodes().stream().forEach((n) -> {

			try {
				n.setProperty(DOMNode.visibleToAuthenticatedUsers, true);
				if (publicToo) {

					src.setProperty(DOMNode.visibleToPublicUsers, true);
				}

			} catch (FrameworkException fex) {}
		} );

		return src;
	}
}
