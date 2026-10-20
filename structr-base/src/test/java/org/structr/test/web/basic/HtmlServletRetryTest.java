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
package org.structr.test.web.basic;

import io.restassured.RestAssured;
import org.structr.api.RetryException;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.entity.Relation;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.TraitsInstance;
import org.structr.core.traits.TraitsManager;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.test.web.StructrUiTest;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.dom.Template;
import org.structr.web.traits.definitions.dom.ContentTraitDefinition;
import org.structr.web.traits.definitions.dom.PageTraitDefinition;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * Repeats page requests whose transaction the database terminated. The page creates a RenderRetryItem while it renders,
 * and the type throws a RetryException from onCreation as often as a test asks for, which is what a driver does when
 * the database terminates the transaction at commit. A page that creates raw data is rendered synchronously, so it can
 * be repeated, any other page is rendered asynchronously and sent while it renders, so it is not.
 */
public class HtmlServletRetryTest extends StructrUiTest {

	private static final String ITEM_TYPE = "RenderRetryItem";

	// how many of the next commits fail
	private static final AtomicInteger commitFailures = new AtomicInteger();

	@BeforeMethod
	public void resetFailures() {

		commitFailures.set(0);
	}

	@Override
	protected void createSchema() {

		super.createSchema();

		StructrTraits.registerTrait(new RenderRetryItemTraitDefinition());
		StructrTraits.registerNodeType(ITEM_TYPE, ITEM_TYPE);

		TraitsManager.replaceCurrentInstance(TraitsManager.createCopyOfRootInstance());
	}

	@Test
	public void testRepeatedRenderingCommitsOnce() {

		createPage("retryPage", true);

		commitFailures.set(1);

		RestAssured.basePath = htmlUrl;

		final String body = RestAssured
			.given()
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(200)
			.when()
				.get("/retryPage")
				.body().asString();

		assertEquals("The terminated attempt was not repeated", 0, commitFailures.get());
		assertEquals("The page of the repeated attempt was not sent, or the one of the terminated attempt as well", "rendered", body.trim());
		assertEquals("The terminated attempt was committed", 1, countItems());
	}

	@Test
	public void testRenderingFailsAfterTheLastAttempt() {

		createPage("failingPage", true);

		// more failures than attempts, so the request fails for good
		commitFailures.set(RetryException.MAX_ATTEMPTS * 2);

		RestAssured.basePath = htmlUrl;

		RestAssured
			.given()
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.expect()
				.statusCode(500)
			.when()
				.get("/failingPage");

		assertEquals("The request was not repeated until the attempts ran out", RetryException.MAX_ATTEMPTS, (RetryException.MAX_ATTEMPTS * 2) - commitFailures.get());
		assertEquals("A terminated attempt was committed", 0, countItems());
	}

	@Test
	public void testAsynchronousRenderingIsNotRepeated() {

		createPage("asyncPage", false);

		commitFailures.set(2);

		RestAssured.basePath = htmlUrl;

		// the status depends on whether the page was sent before the commit failed, so only the attempts are checked
		RestAssured
			.given()
				.headers(X_USER_HEADER, ADMIN_USERNAME, X_PASSWORD_HEADER, ADMIN_PASSWORD)
			.when()
				.get("/asyncPage");

		assertEquals("An asynchronous rendering was repeated", 1, commitFailures.get());
		assertEquals("The terminated attempt was committed", 0, countItems());
	}

	// ----- private methods -----
	/**
	 * Creates a page that creates an item and prints "rendered", synchronously rendered if it creates raw data.
	 */
	private void createPage(final String name, final boolean createsRawData) {

		try (final Tx tx = app.tx()) {

			createAdminUser();

			final Page page = Page.createNewPage(securityContext, name);

			page.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.PAGE_CREATES_RAW_DATA_PROPERTY), createsRawData);
			page.setProperty(Traits.of(StructrTraits.PAGE).key(PageTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain");

			// a page only accepts an html element, a comment or a template as its child
			page.appendChild(app.create(StructrTraits.TEMPLATE,
				new NodeAttribute<>(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_PROPERTY), "${{ $.create('" + ITEM_TYPE + "', { name: 'item' }); $.print('rendered'); }}"),
				new NodeAttribute<>(Traits.of(StructrTraits.TEMPLATE).key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), "text/plain")
			).as(Template.class));

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	private int countItems() {

		try (final Tx tx = app.tx()) {

			final int count = app.nodeQuery(ITEM_TYPE).getAsList().size();

			tx.success();

			return count;

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		return -1;
	}

	/**
	 * Consumes one of the failures and returns true if there was one left.
	 */
	private static boolean consumeFailure() {

		return commitFailures.getAndUpdate(value -> value > 0 ? value - 1 : 0) > 0;
	}

	// ----- nested classes -----
	private static class RenderRetryItemTraitDefinition extends AbstractNodeTraitDefinition {

		RenderRetryItemTraitDefinition() {

			super(ITEM_TYPE);
		}

		@Override
		public Map<Class, LifecycleMethod> createLifecycleMethods(final TraitsInstance traitsInstance) {

			return Map.of(

				OnCreation.class, new OnCreation() {

					@Override
					public void onCreation(final GraphObject graphObject, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

						if (consumeFailure()) {

							throw new RetryException("Simulated termination of the transaction");
						}
					}
				}
			);
		}

		@Override
		public Set<PropertyKey> createPropertyKeys(final TraitsInstance traitsInstance) {

			return Set.of();
		}

		@Override
		public Map<String, Set<String>> getViews() {

			return Map.of(PropertyView.Public, newSet("name"));
		}

		@Override
		public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

			return Map.of();
		}

		@Override
		public Relation getRelation() {

			return null;
		}
	}
}
