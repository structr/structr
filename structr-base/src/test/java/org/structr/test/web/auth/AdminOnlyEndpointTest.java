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
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.SchemaMethodTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.structr.web.auth.UiAuthenticator;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.testng.AssertJUnit.fail;

/**
 * Ticket 1597: four endpoints that hand out more than the caller is entitled to, each of which has a
 * sibling in the same code base that gets it right.
 *
 * <ul>
 * <li>{@code /maintenance/_schemaJson} reads the whole schema, method source included, as the superuser
 * - while the maintenance endpoint's own dispatch refuses a non-administrator before it reaches any of
 * the mapped commands, which this handler is routed past.</li>
 * <li>{@code /resolver} looks nodes up through StructrApp.getInstance(), which is the superuser
 * instance, so it answers for any uuid the caller can name.</li>
 * <li>A user-defined function marked private is callable over REST, while the static, instance and
 * /me method resources all refuse one.</li>
 * <li>{@code /_runtimeEventLog} hands out failed logins, refused URIs and maintenance parameters to
 * anyone with the grant - and the dashboard uses exactly that signature, so granting it "for the UI"
 * is an easy mistake to make.</li>
 * </ul>
 *
 * <p>The grants are deliberately in place throughout: what is being tested is that holding the grant is
 * not enough, so a refusal means the endpoint refused rather than that it was unreachable. The refusal
 * is 403 and not 401: the caller is authenticated and simply not permitted, which is what
 * NotAllowedException carries and what the maintenance endpoint has always answered.
 */
public class AdminOnlyEndpointTest extends StructrUiTest {

	private static final String PASSWORD = "correct-horse-battery-staple";

	@Test
	public void testSchemaJsonIsRefusedForNonAdmins() {

		createUser("schemareader");

		grant("maintenance/_schemaJson", UiAuthenticator.AUTH_USER_GET | UiAuthenticator.AUTH_USER_POST, true);

		RestAssured
			.given()
				.headers(X_USER_HEADER, "schemareader", X_PASSWORD_HEADER, PASSWORD)
			.expect()
				.statusCode(403)
			.when()
				.get("/maintenance/_schemaJson");

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, "schemareader", X_PASSWORD_HEADER, PASSWORD)
				.body("{ 'schema': '{}' }")
			.expect()
				.statusCode(403)
			.when()
				.post("/maintenance/_schemaJson");
	}

	/**
	 * The counterpart: an administrator still gets the schema, or the endpoint would just be broken. Both
	 * verbs, because both are guarded and a guard that refuses everyone is not a fix.
	 *
	 * <p>The POST carries no schema, which is the point: it gets past the guard and is then turned away
	 * by the handler itself with 400, so an administrator is demonstrably not refused while nothing is
	 * written. A body with a schema in it would have replaceDatabaseSchema() replace the schema of the
	 * running instance, which is a great deal more than this test needs to know.
	 */
	@Test
	public void testSchemaJsonStillWorksForAdmins() {

		createAdminUser();

		grant("maintenance/_schemaJson", UiAuthenticator.AUTH_USER_GET | UiAuthenticator.AUTH_USER_POST, true);

		RestAssured
			.given()
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
			.when()
				.get("/maintenance/_schemaJson");

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.body("{}")
			.expect()
				.statusCode(400)
			.when()
				.post("/maintenance/_schemaJson");
	}

	/**
	 * The resolver answers for a node the caller cannot read. "secret-user" is invisible to everyone but
	 * an administrator, so its name must not come back.
	 */
	@Test
	public void testResolverDoesNotAnswerForUnreadableNodes() {

		final String hiddenId = createUser("secret-user");

		createUser("resolver-caller");

		grant("resolver", UiAuthenticator.AUTH_USER_POST, true);

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, "resolver-caller", X_PASSWORD_HEADER, PASSWORD)
				.body("{ 'ids': [ '" + hiddenId + "' ] }")
			.expect()
				.statusCode(200)
				.body(not(containsString("secret-user")))
			.when()
				.post("/resolver");
	}

	/**
	 * The counterpart to the test above, and the reason it is needed: that one asserts an absence, which
	 * an endpoint answering nothing at all would satisfy just as well. Running the resolver in the
	 * caller's context has to keep it a resolver - a node the caller may read still comes back, and a
	 * user may read itself.
	 */
	@Test
	public void testResolverStillAnswersForReadableNodes() {

		final String callerId = createUser("resolver-reader");

		grant("resolver", UiAuthenticator.AUTH_USER_POST, true);

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, "resolver-reader", X_PASSWORD_HEADER, PASSWORD)
				.body("{ 'ids': [ '" + callerId + "' ] }")
			.expect()
				.statusCode(200)
				.body(containsString("resolver-reader"))
			.when()
				.post("/resolver");
	}

	@Test
	public void testRuntimeEventLogIsRefusedForNonAdmins() {

		createUser("logreader");

		grant("_runtimeEventLog", UiAuthenticator.AUTH_USER_GET | UiAuthenticator.AUTH_USER_POST, true);

		RestAssured
			.given()
				.headers(X_USER_HEADER, "logreader", X_PASSWORD_HEADER, PASSWORD)
			.expect()
				.statusCode(403)
			.when()
				.get("/_runtimeEventLog");

		/* POST is the acknowledge operation, and it is guarded separately - reading the log and clearing
		   the record of what the instance has been attacked with are two different things to be refused. */
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, "logreader", X_PASSWORD_HEADER, PASSWORD)
				.body("{ 'action': 'acknowledge' }")
			.expect()
				.statusCode(403)
			.when()
				.post("/_runtimeEventLog");
	}

	/** The counterpart, so the assertions above cannot pass because the endpoint is simply gone. */
	@Test
	public void testRuntimeEventLogStillWorksForAdmins() {

		createAdminUser();

		grant("_runtimeEventLog", UiAuthenticator.AUTH_USER_GET | UiAuthenticator.AUTH_USER_POST, true);

		RestAssured
			.given()
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
			.when()
				.get("/_runtimeEventLog");

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
				.body("{ 'action': 'acknowledge' }")
			.expect()
				.statusCode(200)
			.when()
				.post("/_runtimeEventLog");
	}

	@Test
	public void testServerLogIsRefusedForNonAdmins() {

		createUser("serverlogreader");

		grant("_serverLog", UiAuthenticator.AUTH_USER_GET, true);

		RestAssured
			.given()
				.headers(X_USER_HEADER, "serverlogreader", X_PASSWORD_HEADER, PASSWORD)
			.expect()
				.statusCode(403)
			.when()
				.get("/_serverLog");
	}

	/** The counterpart, plus the two parameter errors an administrator can run into. */
	@Test
	public void testServerLogStillWorksForAdmins() {

		createAdminUser();

		RestAssured
			.given()
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
				.body("result.availableLogFiles", notNullValue())
				.body("result.lines", notNullValue())
			.when()
				.get("/_serverLog?lines=10&filter=INFO");

		RestAssured
			.given()
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(422)
			.when()
				.get("/_serverLog?lines=-1");

		RestAssured
			.given()
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(422)
			.when()
				.get("/_serverLog?logFile=/etc/passwd");
	}

	/**
	 * A user-defined function marked private must not be callable over REST. StaticMethodResource,
	 * InstanceMethodResource and MeMethodResource all refuse one; the two resources that serve
	 * user-defined functions did not look at the flag at all, so marking a function private meant
	 * nothing there.
	 */
	@Test
	public void testPrivateUserDefinedFunctionsAreNotCallableOverRest() {

		createPrivateFunction("privateFunction");

		createUser("caller");

		grant("privateFunction", UiAuthenticator.AUTH_USER_POST, true);
		grant("maintenance/globalSchemaMethods/privateFunction", UiAuthenticator.AUTH_USER_POST, false);

		// the current path
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, "caller", X_PASSWORD_HEADER, PASSWORD)
				.body("{}")
			.expect()
				.statusCode(404)
			.when()
				.post("/privateFunction");

		// and the deprecated one, which resolves the same method
		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, "caller", X_PASSWORD_HEADER, PASSWORD)
				.body("{}")
			.expect()
				.statusCode(404)
			.when()
				.post("/maintenance/globalSchemaMethods/privateFunction");
	}

	/** The counterpart: a function that is not private stays callable. */
	@Test
	public void testPublicUserDefinedFunctionsStayCallable() {

		createFunction("openFunction", false);

		createUser("caller");

		grant("openFunction", UiAuthenticator.AUTH_USER_POST, true);

		RestAssured
			.given()
				.contentType("application/json; charset=UTF-8")
				.headers(X_USER_HEADER, "caller", X_PASSWORD_HEADER, PASSWORD)
				.body("{}")
			.expect()
				.statusCode(200)
			.when()
				.post("/openFunction");
	}

	// ----- private methods -----
	private void createPrivateFunction(final String name) {

		createFunction(name, true);
	}

	private void createFunction(final String name, final boolean isPrivate) {

		final Traits traits = Traits.of(StructrTraits.SCHEMA_METHOD);

		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.SCHEMA_METHOD,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
				new NodeAttribute<>(traits.key(SchemaMethodTraitDefinition.SOURCE_PROPERTY), "'called'"),
				new NodeAttribute<>(traits.key(SchemaMethodTraitDefinition.IS_PRIVATE_PROPERTY), isPrivate)
			);

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception creating the function: " + fex.getMessage());
		}
	}

	private String createUser(final String name) {

		final Traits traits = Traits.of(StructrTraits.USER);

		try (final Tx tx = app.tx()) {

			final NodeInterface user = app.create(StructrTraits.USER,
				new NodeAttribute<>(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY), name),
				new NodeAttribute<>(traits.key(PrincipalTraitDefinition.PASSWORD_PROPERTY), PASSWORD),
				new NodeAttribute<>(traits.key(GraphObjectTraitDefinition.VISIBLE_TO_AUTHENTICATED_USERS_PROPERTY), false)
			);

			final String id = user.getUuid();

			tx.success();

			return id;

		} catch (FrameworkException fex) {

			fail("Unexpected exception creating the user: " + fex.getMessage());

			return null;
		}
	}
}
