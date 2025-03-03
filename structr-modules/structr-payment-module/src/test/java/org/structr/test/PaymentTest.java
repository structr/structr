/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.test;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.apache.commons.lang.StringUtils;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.testng.AssertJUnit.fail;

/**
 */
public class PaymentTest extends StructrUiTest {

	@Test
	public void testSimplePayment() {

		final Traits userTraits = Traits.of(StructrTraits.USER);

		// create test user
		try (final Tx tx = app.tx()) {

			app.create(StructrTraits.USER,
				new NodeAttribute<>(userTraits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),     "admin"),
				new NodeAttribute<>(userTraits.key("password"), "admin"),
				new NodeAttribute<>(userTraits.key(PrincipalTraitDefinition.IS_ADMIN_PROPERTY),  true)
			);

			tx.success();

		} catch (FrameworkException fex) {
			fex.printStackTrace();
			fail("Unexpected exception");
		}

		// create payment
		final String uuid = StringUtils.substringAfterLast(RestAssured
			.given()
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.body("{ description: 'Test payment', currency: 'EUR' }")
			.expect()
			.statusCode(201)
			.when()
			.post("/PaymentNode")
			.getHeader("Location"), "/");

		// create payment item
		RestAssured
			.given()
			.filter(RequestLoggingFilter.logRequestTo(System.out))
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.body("{ payment: '" + uuid + "', amount: 3, quantity: 1, description: 'item 1' }")
			.expect()
			.statusCode(201)
			.when()
			.post("/PaymentItemNode");

		// check payment node
		RestAssured
			.given()
			.filter(ResponseLoggingFilter.logResponseTo(System.out))
			.header("X-User", "admin")
			.header("X-Password", "admin")
			.expect()
			.statusCode(200)
			.when()
			.get("/PaymentNode");

		// test error (wrong token)
		{
			// begin new checkout
			final String token = RestAssured
				.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.header("X-User", "admin")
				.header("X-Password", "admin")
				.body("{ providerName: 'test', successUrl: 'success', errorUrl: 'error' }")
				.expect()
				.statusCode(200)
				.when()
				.post("/PaymentNode/" + uuid + "/beginCheckout")
				.body()
				.path("result.token");


			// confirm checkout with wrong token (provoke error response)
			RestAssured
				.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.header("X-User", "admin")
				.header("X-Password", "admin")
				.body("{ providerName: 'test', token: 'error', successUrl: 'success', payerId: 'errorPayer' }")
				.expect()
				.statusCode(422)
				.when()
				.post("/PaymentNode/" + uuid + "/confirmCheckout");

			// check payment state (must be "open", since the checkout error causes the transaction to be rolled back)
			// this is not ideal....
			RestAssured
				.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.header("X-User", "admin")
				.header("X-Password", "admin")
				.expect()
				.body("result.state", equalTo("open"))
				.body("result.token", equalTo(token))
				.statusCode(200)
				.when()
				.get("/PaymentNode/" + uuid);
		}

		// test cancel
		{
			// begin new checkout
			final String token = RestAssured
				.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.header("X-User", "admin")
				.header("X-Password", "admin")
				.body("{ providerName: 'test', successUrl: 'success', errorUrl: 'error' }")
				.expect()
				.statusCode(200)
				.when()
				.post("/PaymentNode/" + uuid + "/beginCheckout")
				.body()
				.path("result.token");

			// cancel checkout
			RestAssured
				.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.header("X-User", "admin")
				.header("X-Password", "admin")
				.body("{ providerName: 'test', token: '" + token + "' }")
				.expect()
				.statusCode(200)
				.when()
				.post("/PaymentNode/" + uuid + "/cancelCheckout");

			// check payment state (must be "cancelled")
			RestAssured
				.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.header("X-User", "admin")
				.header("X-Password", "admin")
				.expect()
				.body("result.state", equalTo("cancelled"))
				.body("result.token", equalTo(null))
				.statusCode(200)
				.when()
				.get("/PaymentNode/" + uuid);
		}

		// test success
		{
			// begin new checkout
			final String token = RestAssured
				.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.header("X-User", "admin")
				.header("X-Password", "admin")
				.body("{ providerName: 'test', successUrl: 'success', errorUrl: 'error' }")
				.expect()
				.statusCode(200)
				.when()
				.post("/PaymentNode/" + uuid + "/beginCheckout")
				.body()
				.path("result.token");

			// confirm checkout with correct token
			RestAssured
				.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.header("X-User", "admin")
				.header("X-Password", "admin")
				.body("{ providerName: 'test', notifyUrl: 'notify', token: '" + token + "', payerId: 'successPayer' }")
				.expect()
				.statusCode(200)
				.when()
				.post("/PaymentNode/" + uuid + "/confirmCheckout");

			// check payment state (must be "completed")
			RestAssured
				.given()
				.filter(ResponseLoggingFilter.logResponseTo(System.out))
				.header("X-User", "admin")
				.header("X-Password", "admin")
				.expect()
				.body("result.payer", equalTo("successPayer"))
				.body("result.state", equalTo("completed"))
				.body("result.token", equalTo(null))
				.statusCode(200)
				.when()
				.get("/PaymentNode/" + uuid);
		}

	}
}
