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

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * $.login() after the same request wrote the user node: the counter reset in its own transaction waited for
 * the lock that the request held, while the request waited for the reset, and the login hung for good.
 */
public class LoginWriteDeadlockTest extends StructrUiTest {

	private static final String PASSWORD = "correct-horse-battery-staple";

	@Test
	public void testLoginAfterStoringANewPasswordInTheSameTransaction() throws Exception {

		createUser("migrated", PASSWORD, 0);

		// the legacy migration of an application: a failed login, then the new password and the login in one transaction
		createFunction("migrateAndLogin", """
			{
				return $.doPrivileged(() => {
					const u = $.first($.findPrivileged('User', 'name', 'migrated'));
					$.login(u, 'not-the-password');
					u.password = 'new-horse-battery-staple';
					u.passwordAttempts = 0;
					return $.login(u, 'new-horse-battery-staple');
				});
			}
			""");

		assertEquals(Boolean.TRUE, callWithin("migrateAndLogin", 30).jsonPath().get("result"));
		assertEquals(Integer.valueOf(0), getPasswordAttempts("migrated"));
	}

	@Test
	public void testLoginAfterAnotherWriteToTheUserDoesNotHangForever() throws Exception {

		createUser("written", PASSWORD, 2);

		// the counter has to be reset here, so the reset cannot be skipped and has to give up waiting instead
		createFunction("writeAndLogin", """
			{
				return $.doPrivileged(() => {
					const u = $.first($.findPrivileged('User', 'name', 'written'));
					u.eMail = 'written@example.com';
					return $.login(u, '%s');
				});
			}
			""".formatted(PASSWORD));

		assertEquals(Boolean.TRUE, callWithin("writeAndLogin", 30).jsonPath().get("result"));

		// the reset is applied once the request's transaction has released the node
		for (int i = 0; i < 50 && getPasswordAttempts("written") != 0; i++) {

			Thread.sleep(100);
		}

		assertEquals(Integer.valueOf(0), getPasswordAttempts("written"));
	}

	// ----- private methods -----
	private Response callWithin(final String function, final int seconds) throws Exception {

		final ExecutorService executor = Executors.newSingleThreadExecutor();

		try {

			final Future<Response> response = executor.submit(() -> RestAssured.given().contentType("application/json; charset=UTF-8").body("{}").post("/" + function));

			return response.get(seconds, TimeUnit.SECONDS);

		} catch (TimeoutException tex) {

			fail(function + " did not answer within " + seconds + " s, the login is deadlocked");

			return null;

		} finally {

			executor.shutdownNow();
		}
	}

	private void createUser(final String name, final String password, final int failedAttempts) {

		final Traits traits = Traits.of(StructrTraits.USER);

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.USER,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
				new NodeAttribute<>(traits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY), password),
				new NodeAttribute<>(traits.key(PrincipalTraitDefinition.PASSWORD_ATTEMPTS_PROPERTY), failedAttempts)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception creating the user: " + fex.getMessage());
		}
	}

	private void createFunction(final String name, final String source) {

		final Traits traits = Traits.of(StructrTraits.SCHEMA_METHOD);

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
				new NodeAttribute<>(traits.key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), source)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception creating the function: " + fex.getMessage());
		}

		grant(name, UiAuthenticator.NON_AUTH_USER_POST, false);
	}

	private Integer getPasswordAttempts(final String name) {

		try (final Tx tx = app.tx()) {

			final NodeInterface user = app.nodeQuery(StructrTraits.USER).name(name).getFirst();
			final Integer attempts   = user.getProperty(Traits.of(StructrTraits.PRINCIPAL).key(PrincipalTraitDefinition.PASSWORD_ATTEMPTS_PROPERTY));

			tx.success();

			return attempts != null ? attempts : 0;

		} catch (FrameworkException fex) {

			fail("Unexpected exception reading the user: " + fex.getMessage());

			return null;
		}
	}
}
